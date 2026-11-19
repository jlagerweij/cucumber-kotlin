package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinLambdaStepDefinition
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinLambdaStepDefinitionCreator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.plugins.cucumber.BDDFrameworkType
import org.jetbrains.plugins.cucumber.StepDefinitionCreator
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

/**
 * Counterpart of `CucumberJava8Extension` in the Cucumber for Java plugin: step definitions in cucumber-java8 style,
 * e.g. `Given("I have {int} cucumbers") { count: Int -> }`.
 */
class CucumberKotlinLambdaExtension : AbstractCucumberKotlinExtension() {

    override fun getStepFileType() = BDDFrameworkType(KotlinFileType.INSTANCE)

    override fun getStepDefinitionCreator(): StepDefinitionCreator = KotlinLambdaStepDefinitionCreator()

    override fun loadStepsFor(module: Module): MutableList<AbstractStepDefinition> {
        val fileBasedIndex = FileBasedIndex.getInstance()
        val project = module.project

        val searchScope = module.getModuleWithDependenciesAndLibrariesScope(true)
            .uniteWith(ProjectScope.getLibrariesScope(project))
        val kotlinFiles = GlobalSearchScope.getScopeRestrictedByFileTypes(searchScope, KotlinFileType.INSTANCE)

        val elements = mutableListOf<KtCallExpression>()
        fileBasedIndex.processValues(
            CucumberKotlinLambdaStepIndex.INDEX_ID,
            true,
            null,
            { file, offsets ->
                ProgressManager.checkCanceled()
                PsiManager.getInstance(project).findFile(file)?.let { psiFile ->
                    offsets.forEach { offset ->
                        val element = psiFile.findElementAt(offset + 1)
                        PsiTreeUtil.getParentOfType(element, KtCallExpression::class.java)?.let { stepElement ->
                            elements.add(stepElement)
                        }
                    }
                }
                true
            },
            kotlinFiles
        )

        return elements.mapNotNull { stepElement ->
            if (CucumberKotlinUtil.isLambdaStepDefinition(stepElement)) {
                stepElement.references.firstOrNull { it is KtInvokeFunctionReference }?.let {
                    KotlinLambdaStepDefinition(stepElement, module)
                }
            } else null
        }.toMutableList()
    }
}
