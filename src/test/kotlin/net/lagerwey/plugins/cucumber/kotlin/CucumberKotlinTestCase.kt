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
        myFixture.addFileToProject("io/cucumber/java8/En.java", EN_STUB)
    }

    /**
     * Adds `steps/MySteps.kt` with the given statements inside the `init` block.
     */
    protected fun addSteps(vararg steps: String): PsiFile = myFixture.addFileToProject(
        "steps/MySteps.kt",
        """
        |package steps
        |
        |import io.cucumber.java8.En
        |
        |class MySteps : En {
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
        private val EN_STUB = """
            package io.cucumber.java8;

            public interface En {
                default void Given(String expression, StepDefinitionBody.A0 body) {}
                default <T1> void Given(String expression, StepDefinitionBody.A1<T1> body) {}
                default <T1, T2> void Given(String expression, StepDefinitionBody.A2<T1, T2> body) {}
                default void When(String expression, StepDefinitionBody.A0 body) {}
                default <T1> void When(String expression, StepDefinitionBody.A1<T1> body) {}
                default <T1, T2> void When(String expression, StepDefinitionBody.A2<T1, T2> body) {}
                default void Then(String expression, StepDefinitionBody.A0 body) {}
                default <T1> void Then(String expression, StepDefinitionBody.A1<T1> body) {}
                default <T1, T2> void Then(String expression, StepDefinitionBody.A2<T1, T2> body) {}
                default void And(String expression, StepDefinitionBody.A0 body) {}
                default <T1> void And(String expression, StepDefinitionBody.A1<T1> body) {}
                default <T1, T2> void And(String expression, StepDefinitionBody.A2<T1, T2> body) {}
                default void But(String expression, StepDefinitionBody.A0 body) {}
                default <T1> void But(String expression, StepDefinitionBody.A1<T1> body) {}
                default <T1, T2> void But(String expression, StepDefinitionBody.A2<T1, T2> body) {}
                default <R> void ParameterType(String name, String regexp, ParameterDefinitionBody.A1<R> body) {}

                interface StepDefinitionBody {
                    interface A0 { void accept() throws Throwable; }
                    interface A1<T1> { void accept(T1 p1) throws Throwable; }
                    interface A2<T1, T2> { void accept(T1 p1, T2 p2) throws Throwable; }
                }

                interface ParameterDefinitionBody {
                    interface A1<R> { R accept(String p1) throws Throwable; }
                }
            }
        """.trimIndent()

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
