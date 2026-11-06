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
        myFixture.addFileToProject("io/cucumber/java8/LambdaGlue.java", LAMBDA_GLUE_STUB)
        myFixture.addFileToProject("io/cucumber/java8/En.java", EN_STUB)
        myFixture.addFileToProject("io/cucumber/java8/De.java", DE_STUB)
        myFixture.addFileToProject("io/cucumber/java8/Scenario.java", SCENARIO_STUB)
        myFixture.addFileToProject("io/cucumber/datatable/DataTable.java", DATA_TABLE_STUB)
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
        /** Type parameters and body type for each supported lambda arity. */
        private val ARITIES = listOf(
            "" to "A0",
            "<T1> " to "A1<T1>",
            "<T1, T2> " to "A2<T1, T2>",
            "<T1, T2, T3> " to "A3<T1, T2, T3>",
            "<T1, T2, T3, T4> " to "A4<T1, T2, T3, T4>",
        )

        /**
         * Generates the overloads of the given step definition methods for every lambda arity.
         */
        private fun stepMethods(vararg keywords: String): String = keywords.joinToString("\n") { keyword ->
            ARITIES.joinToString("\n") { (typeParameters, bodyType) ->
                "    default ${typeParameters}void $keyword(String expression, StepDefinitionBody.$bodyType body) {}"
            }
        }

        private val EN_STUB = """
            |package io.cucumber.java8;
            |
            |public interface En extends LambdaGlue {
            |${stepMethods("Given", "When", "Then", "And", "But")}
            |}
        """.trimMargin()

        private val DE_STUB = """
            |package io.cucumber.java8;
            |
            |public interface De extends LambdaGlue {
            |${stepMethods("Angenommen", "Wenn", "Dann", "Und", "Aber")}
            |}
        """.trimMargin()

        private val SCENARIO_STUB = """
            package io.cucumber.java8;

            public interface Scenario {}
        """.trimIndent()

        private val DATA_TABLE_STUB = """
            package io.cucumber.datatable;

            public class DataTable {}
        """.trimIndent()

        private val LAMBDA_GLUE_STUB = """
            package io.cucumber.java8;

            import java.util.Map;

            public interface LambdaGlue {
                default <R> void ParameterType(String name, String regexp, ParameterDefinitionBody.A1<R> body) {}
                default <T> void DataTableType(DataTableEntryDefinitionBody<T> body) {}
                default void Before(HookBody body) {}
                default void BeforeStep(HookBody body) {}
                default void After(HookBody body) {}
                default void AfterStep(HookBody body) {}

                interface StepDefinitionBody {
                    interface A0 { void accept() throws Throwable; }
                    interface A1<T1> { void accept(T1 p1) throws Throwable; }
                    interface A2<T1, T2> { void accept(T1 p1, T2 p2) throws Throwable; }
                    interface A3<T1, T2, T3> { void accept(T1 p1, T2 p2, T3 p3) throws Throwable; }
                    interface A4<T1, T2, T3, T4> { void accept(T1 p1, T2 p2, T3 p3, T4 p4) throws Throwable; }
                }

                interface ParameterDefinitionBody {
                    interface A1<R> { R accept(String p1) throws Throwable; }
                }

                interface DataTableEntryDefinitionBody<T> { T accept(Map<String, String> entry) throws Throwable; }

                interface HookBody { void accept(Scenario scenario) throws Throwable; }
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
