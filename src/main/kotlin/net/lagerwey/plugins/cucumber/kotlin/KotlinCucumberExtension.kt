package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinStepDefinition
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.cucumber.BDDFrameworkType
import org.jetbrains.plugins.cucumber.StepDefinitionCreator
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.steps.AbstractCucumberExtension
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition


class KotlinCucumberExtension : AbstractCucumberExtension() {

    private val stepDefinitionCreator = KotlinStepDefinitionCreator()

    override fun getStepDefinitionCreator(): StepDefinitionCreator = stepDefinitionCreator

    override fun isStepLikeFile(child: PsiElement, parent: PsiElement) = child is KtFile

    override fun isWritableStepLikeFile(child: PsiElement, parent: PsiElement) = isStepLikeFile(child, parent)

    override fun getStepFileType() = BDDFrameworkType(KotlinFileType.INSTANCE)

    override fun getGlues(file: GherkinFile, jGluesFromOtherFiles: MutableSet<String>?): MutableCollection<String> {
        val glues = mutableSetOf<String>()
        jGluesFromOtherFiles?.let {
            glues.addAll(it)
        }

        getStepDefinitionContainers(file).forEach {
            if (it is KtFile) {
                val packageName = it.packageFqName.asString()
                if (packageName.isNotBlank()) {
                    glues.add(packageName)
                }
            }
        }

        return glues
    }

    override fun getStepDefinitionContainers(featureFile: GherkinFile): MutableCollection<out PsiFile> {
        val module = ModuleUtilCore.findModuleForPsiElement(featureFile) ?: return hashSetOf()
        val stepDefs = loadStepsFor(featureFile, module)

        val result = hashSetOf<PsiFile>()
        for (stepDef in stepDefs) {
            val stepDefElement = stepDef.element
            if (stepDefElement != null) {
                val psiFile = stepDefElement.containingFile
                val psiDirectory = psiFile.parent
                if (psiDirectory != null && isWritableStepLikeFile(psiFile, psiDirectory)) {
                    result.add(psiFile)
                }
            }
        }
        return result
    }

    override fun loadStepsFor(featureFile: PsiFile?, module: Module): MutableList<AbstractStepDefinition> {
        val result = mutableListOf<AbstractStepDefinition>()
        val dependenciesScope = module.moduleContentWithDependenciesScope
        val kotlinFiles = GlobalSearchScope.getScopeRestrictedByFileTypes(dependenciesScope, KotlinFileType.INSTANCE)
        for (method in (featureFile as GherkinFile).stepKeywords.filter { it != "*" }) {
            val occurrencesProcessor: (PsiElement, Int) -> Boolean = { element, _ ->
                val parent = element.parent
                if (parent != null) {
                    val references = parent.references
                    for (ref in references) {
                        if (ref is KtInvokeFunctionReference) {
                            result.add(KotlinStepDefinition(parent))
                            break
                        }
                    }
                }
                true
            }
            PsiSearchHelper.getInstance(module.project).processElementsWithWord(occurrencesProcessor, kotlinFiles, method, UsageSearchContext.IN_CODE, true)
        }
        return result
    }

}