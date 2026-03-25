package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.steps.AbstractCucumberExtension

abstract class AbstractCucumberKotlinExtension : AbstractCucumberExtension() {
    override fun isStepLikeFile(child: PsiElement) = child is KtFile

    override
    fun isWritableStepLikeFile(child: PsiElement): Boolean {
        if (child is PsiClassOwner) {
            val file = child.containingFile
            if (file != null) {
                val vFile = file.virtualFile
                if (vFile != null) {
                    val rootForFile =
                        ProjectRootManager.getInstance(child.project).fileIndex.getSourceRootForFile(vFile)
                    return rootForFile != null
                }
            }
        }
        return false
    }

    override fun getStepDefinitionContainers(featureFile: GherkinFile): MutableCollection<out PsiFile> {
        val module = ModuleUtilCore.findModuleForPsiElement(featureFile) ?: return hashSetOf()
        val stepDefinitions = loadStepsFor(featureFile, module)

        val result = hashSetOf<PsiFile>()
        stepDefinitions.forEach { stepDefinition ->
            stepDefinition.element?.let { element ->
                val psiFile = element.containingFile
                if (isWritableStepLikeFile(psiFile)) {
                    result.add(psiFile)
                }
            }
        }

        return result
    }
}
