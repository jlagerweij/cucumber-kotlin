package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.idea.core.appendElement
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.idea.util.sourceRoots
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.plugins.cucumber.AbstractStepDefinitionCreator
import org.jetbrains.plugins.cucumber.psi.GherkinFile
import org.jetbrains.plugins.cucumber.psi.GherkinStep

class KotlinStepDefinitionCreator : AbstractStepDefinitionCreator() {
    private var lastObservedLanguage = "en"

    override fun createStepDefinitionContainer(directory: PsiDirectory, name: String): PsiFile {
        val file = runWriteAction { directory.createFile(name) } as KtFile
        val ktPsiFactory = KtPsiFactory(file.project, markGenerated = true)
        val psiPackage = directory.getPackage()?.qualifiedName
        val apiClassName = lastObservedLanguage.capitalize()
        val importDirective = ktPsiFactory.createImportDirective(ImportPath.fromString("cucumber.api.java8.$apiClassName"))
        val newLines = ktPsiFactory.createNewLine(2)
        val ktClass = ktPsiFactory.createClass("""
            class ${name.replace(".kt", "")} : $apiClassName {
                init {
                }
            }
            """.trimIndent())

        runWriteAction {
            if (psiPackage != null && psiPackage != "") {
                file.add(ktPsiFactory.createPackageDirective(FqName(psiPackage)))
                file.add(newLines)
            }
            file.add(importDirective)
            file.add(newLines)
            file.add(ktClass)
        }

        return file
    }

    override fun validateNewStepDefinitionFileName(project: Project, name: String): Boolean {
        // TODO: Actually validate (check for collision)
        return true
    }

    override fun createStepDefinition(step: GherkinStep, file: PsiFile, withTemplate: Boolean): Boolean {
        val ktFile = (file as? KtFile) ?: return false
        val ktPsiFactory = KtPsiFactory(file.project, markGenerated = true)
        // TODO: Kotlin files can have multiple classes. Make sure to find correct one.
        val ktClass = (ktFile.classes.firstOrNull() as? KtLightClassForSourceDeclaration) ?: return false
        val initializer = ktClass.kotlinOrigin.getAnonymousInitializers()[0].body as? KtBlockExpression
        val expression = ktPsiFactory.createExpression("""
            ${step.keyword.text}("${step.name.replace("\"", "\\\"")}") {

            }
            """.trimIndent())

        runWriteAction {
            initializer?.appendElement(expression)
        }

        file.navigate(true)
        return true
    }

    override fun getDefaultStepFileName(step: GherkinStep): String {
        lastObservedLanguage = step.localeLanguage

        val basename = step.containingFile?.name?.replace(".feature", "") ?: "Cucumber"
        return "${basename}Steps.kt"
    }

    override fun getDefaultStepDefinitionFolderPath(step: GherkinStep): String {
        return getDefaultKotlinStepDefinitionFolderPath(step).virtualFile.path
    }
    private fun getDefaultKotlinStepDefinitionFolderPath(step: GherkinStep): PsiDirectory {
        lastObservedLanguage = step.localeLanguage

        val stepDir = step.containingFile.containingDirectory
        val sourceRoots = step.module?.sourceRoots ?: return stepDir
        val root = sourceRoots.find { it.path.endsWith("kotlin") } ?: sourceRoots.find { it.path.endsWith("java") }
        val rootDir = root?.toPsiDirectory(step.project) ?: return stepDir
        val packageName = stepDir.getPackage()?.qualifiedName
        if (packageName.isNullOrBlank()) return rootDir
        var dir = rootDir
        packageName.split(".").forEach { subdirName ->
            var subDir = dir.findSubdirectory(subdirName)
            if (subDir == null) {
                subDir = runWriteAction {
                    dir.createSubdirectory(subdirName)
                }
            }
            dir = subDir
        }
        return dir
    }
}

val GherkinStep.localeLanguage: String
    get() = (this.containingFile as GherkinFile).localeLanguage
