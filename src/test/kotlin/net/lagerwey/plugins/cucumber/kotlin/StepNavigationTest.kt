package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.psi.KtCallExpression

class StepNavigationTest : LightJavaCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = DESCRIPTOR

    fun testNavigateFromFeatureStepToKotlinStepDefinition() {
        // Minimal stand-in for the cucumber-java8 API, so the test does not need the real jar
        myFixture.addFileToProject(
            "io/cucumber/java8/En.java",
            """
            package io.cucumber.java8;

            public interface En {
                default void Given(String expression, StepDefinitionBody body) {}

                interface StepDefinitionBody {
                    void accept() throws Throwable;
                }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "steps/MySteps.kt",
            """
            package steps

            import io.cucumber.java8.En

            class MySteps : En {
                init {
                    Given("I have a cucumber") { }
                }
            }
            """.trimIndent()
        )
        myFixture.configureByText(
            "navigation.feature",
            """
            Feature: Navigation

              Scenario: Navigate to step definition
                Given I have a <caret>cucumber
            """.trimIndent()
        )

        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
        assertNotNull("No reference found for the Gherkin step", reference)

        val targets = (reference as PsiPolyVariantReference).multiResolve(false).mapNotNull { it.element }
        assertEquals("Expected exactly one step definition", 1, targets.size)

        val target = targets.single()
        assertInstanceOf(target, KtCallExpression::class.java)
        assertEquals("Given(\"I have a cucumber\") { }", target.text)
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
