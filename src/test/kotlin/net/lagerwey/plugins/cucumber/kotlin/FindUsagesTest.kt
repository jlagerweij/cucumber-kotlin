package net.lagerwey.plugins.cucumber.kotlin

import org.jetbrains.plugins.cucumber.psi.GherkinStep

class FindUsagesTest : CucumberKotlinTestCase() {

    fun testFindFeatureStepsUsingStepDefinition() {
        myFixture.addFileToProject(
            "first.feature",
            """
            Feature: First

              Scenario: Uses the step
                Given I have 3 cucumbers
                And I have a tomato
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "second.feature",
            """
            Feature: Second

              Scenario: Uses the step too
                Given I have 7 cucumbers
            """.trimIndent()
        )
        myFixture.configureByText(
            "MySteps.kt",
            """
            package steps

            import io.cucumber.java8.En

            class MySteps : En {
                init {
                    Given("I have {int} cu<caret>cumbers") { count: Int -> }
                    Given("I have a tomato") { }
                }
            }
            """.trimIndent()
        )

        val usages = myFixture.findUsages(myFixture.elementAtCaret)

        val steps = usages.map { it.element }.filterIsInstance<GherkinStep>().map { it.text }.sorted()
        assertEquals(listOf("Given I have 3 cucumbers", "Given I have 7 cucumbers"), steps)
    }
}
