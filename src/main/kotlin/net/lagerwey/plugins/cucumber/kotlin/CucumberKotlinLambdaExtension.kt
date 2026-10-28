package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinLambdaStepDefinition
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinStepDefinitionCreator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.plugins.cucumber.BDDFrameworkType
import org.jetbrains.plugins.cucumber.StepDefinitionCreator
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class CucumberKotlinLambdaExtension : AbstractCucumberKotlinExtension() {

    override fun getStepFileType() = BDDFrameworkType(KotlinFileType.INSTANCE)

    override fun getStepDefinitionCreator(): StepDefinitionCreator = KotlinStepDefinitionCreator()

    override fun loadStepsFor(module: Module): MutableList<AbstractStepDefinition> {
        val fileBasedIndex = FileBasedIndex.getInstance()
        val project = module.project

        val searchScope = module.getModuleWithDependenciesAndLibrariesScope(true)
            .uniteWith(ProjectScope.getLibrariesScope(project))
        val kotlinFiles = GlobalSearchScope.getScopeRestrictedByFileTypes(searchScope, KotlinFileType.INSTANCE)

        val elements = mutableListOf<KtCallExpression>()
        fileBasedIndex.processValues(
            INDEX_ID,
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
            if (CucumberKotlinUtil.isStepDefinition(stepElement)) {
                stepElement.references.firstOrNull { it is KtInvokeFunctionReference }?.let {
                    KotlinLambdaStepDefinition(stepElement)
                }
            } else null
        }.toMutableList()
    }
}
