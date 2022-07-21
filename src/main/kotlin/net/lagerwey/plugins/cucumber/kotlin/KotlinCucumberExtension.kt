package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinParameterTypeManager
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinStepDefinition
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinStepDefinitionCreator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.plugins.cucumber.BDDFrameworkType
import org.jetbrains.plugins.cucumber.StepDefinitionCreator
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.steps.AbstractCucumberExtension
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class KotlinCucumberExtension : AbstractCucumberExtension() {
    private val stepDefinitionCreator = KotlinStepDefinitionCreator()

    override fun getStepDefinitionCreator(): StepDefinitionCreator = stepDefinitionCreator

    override fun isStepLikeFile(child: PsiElement, parent: PsiElement) = child is KtFile

    override fun isWritableStepLikeFile(child: PsiElement, parent: PsiElement): Boolean {
        return isStepLikeFile(child, parent) && (child as KtFile).virtualFile.isWritable
    }

    override fun getStepFileType() = BDDFrameworkType(KotlinFileType.INSTANCE)

    override fun getStepDefinitionContainers(featureFile: GherkinFile): MutableCollection<out PsiFile> {
        val module = ModuleUtilCore.findModuleForPsiElement(featureFile) ?: return hashSetOf()
        val stepDefinitions = loadStepsFor(featureFile, module)

        val result = hashSetOf<PsiFile>()
        stepDefinitions.forEach { stepDefinition ->
            stepDefinition.element?.let { element ->
                val psiFile = element.containingFile
                val psiDirectory = psiFile.parent
                if (psiDirectory != null && isWritableStepLikeFile(psiFile, psiDirectory)) {
                    result.add(psiFile)
                }
            }
        }

        return result
    }

    override fun loadStepsFor(featureFile: PsiFile?, module: Module): MutableList<AbstractStepDefinition> {
        val fileBasedIndex = FileBasedIndex.getInstance()
        val project = module.project

        val searchScope = module.getModuleWithDependenciesAndLibrariesScope(true)
            .uniteWith(ProjectScope.getLibrariesScope(project))
        val kotlinFiles = GlobalSearchScope.getScopeRestrictedByFileTypes(searchScope, KotlinFileType.INSTANCE)

        val elements = mutableListOf<KtCallExpression>()
        fileBasedIndex.processValues(
            KotlinCucumberStepIndex.INDEX_ID,
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

        findParameterTypes(module, kotlinFiles)

        return elements.mapNotNull { stepElement ->
            if (CucumberKotlinUtil.isStepDefinition(stepElement)) {
                stepElement.references.firstOrNull { it is KtInvokeFunctionReference }?.let {
                    KotlinStepDefinition(stepElement)
                }
            } else null
        }.toMutableList()
    }

    private fun findParameterTypes(module: Module, kotlinFiles: GlobalSearchScope) {
        val occurrencesProcessor: (PsiElement, Int) -> Boolean = { element, _ ->
            element.parent?.let { parent ->
                parent.references.forEach { ref ->
                    if (ref is KtInvokeFunctionReference) {
                        handleParameterType(parent)
                    }
                }
            }
            true
        }

        ProgressManager.getInstance()
            .run(
                object : Task.Backgroundable(module.project, "Process elements with word", false) {
                    override fun run(indicator: ProgressIndicator) {
                        indicator.isIndeterminate = true
                        indicator.text = "Process elements with word..."
                        PsiSearchHelper.getInstance(module.project).processElementsWithWord(
                            occurrencesProcessor,
                            kotlinFiles,
                            "ParameterType",
                            UsageSearchContext.IN_CODE,
                            true
                        )
                    }
                })
    }

    private fun handleParameterType(element: PsiElement) {
        runCatching {
            val pointer = SmartPointerManager.getInstance(element.project).createSmartPsiElementPointer(element)

            val callExpression = element as? KtCallExpression
            callExpression?.let {
                val name = (it.valueArguments[0].getArgumentExpression() as KtStringTemplateExpression).entries[0].text
                val regex = (it.valueArguments[1].getArgumentExpression() as KtStringTemplateExpression).entries.joinToString("") { x -> x.text }
                val unescapedRegex = org.apache.commons.lang.StringEscapeUtils.unescapeJava(regex)
                KotlinParameterTypeManager.addParameterType(name, unescapedRegex, pointer)
            }
        }
    }
}
