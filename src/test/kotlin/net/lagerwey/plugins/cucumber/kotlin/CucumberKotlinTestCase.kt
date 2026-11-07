package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.PathUtil
import org.jetbrains.plugins.cucumber.psi.GherkinStep

/**
 * Base class for tests that need Kotlin step definitions written against the cucumber-java8 API.
 */
abstract class CucumberKotlinTestCase : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = DESCRIPTOR

    override fun setUp() {
        super.setUp()
        // Minimal stand-in for the cucumber-java8 API, so the tests do not need the real jar
        CucumberJava8Stubs.FILES.forEach { (path, text) -> myFixture.addFileToProject(path, text) }
    }

    /**
     * Adds `steps/MySteps.kt` with the given statements inside the `init` block.
     */
    protected fun addSteps(vararg steps: String): PsiFile = addStepsFile("MySteps", "En", *steps)

    /**
     * Adds `steps/<className>.kt` implementing the given cucumber-java8 interface (e.g. `En` or `De`),
     * with the given statements inside the `init` block.
     */
    protected fun addStepsFile(className: String, cucumberInterface: String, vararg steps: String): PsiFile =
        myFixture.addFileToProject(
            "steps/$className.kt",
            """
            |package steps
            |
            |import io.cucumber.java8.$cucumberInterface
            |
            |class $className : $cucumberInterface {
            |    init {
            |${steps.joinToString("\n") { "        $it" }}
            |    }
            |}
            """.trimMargin()
        )

    /**
     * Configures a feature file containing a single scenario with the given step lines.
     * One of the steps is expected to contain `<caret>`.
     */
    protected fun configureFeature(vararg steps: String): PsiFile = myFixture.configureByText(
        "test.feature",
        """
        |Feature: Test
        |
        |  Scenario: Test
        |${steps.joinToString("\n") { "    $it" }}
        """.trimMargin()
    )

    /**
     * Configures a German (`# language: de`) feature file containing a single scenario with the given step lines.
     * One of the steps is expected to contain `<caret>`.
     */
    protected fun configureGermanFeature(vararg steps: String): PsiFile = myFixture.configureByText(
        "test.feature",
        """
        |# language: de
        |Funktionalität: Test
        |
        |  Szenario: Test
        |${steps.joinToString("\n") { "    $it" }}
        """.trimMargin()
    )

    /**
     * Returns the Gherkin step at the caret.
     */
    protected fun stepAtCaret(): GherkinStep =
        PsiTreeUtil.getParentOfType(myFixture.file.findElementAt(myFixture.caretOffset), GherkinStep::class.java)!!

    /**
     * Resolves the Gherkin step at the caret to its step definitions.
     */
    protected fun resolveStepAtCaret(): List<PsiElement> {
        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
        assertNotNull("No reference found for the Gherkin step", reference)
        return (reference as PsiPolyVariantReference).multiResolve(false).mapNotNull { it.element }
    }

    companion object {
        private val DESCRIPTOR = object : DefaultLightProjectDescriptor() {
            override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
                super.configureModule(module, model, contentEntry)
                // Kotlin stdlib is needed to resolve the step definition lambdas
                val stdlib = PathUtil.getJarPathForClass(Unit::class.java)
                PsiTestUtil.addLibrary(model, "kotlin-stdlib", PathUtil.getParentPath(stdlib), PathUtil.getFileName(stdlib))
            }
        }
    }
}
