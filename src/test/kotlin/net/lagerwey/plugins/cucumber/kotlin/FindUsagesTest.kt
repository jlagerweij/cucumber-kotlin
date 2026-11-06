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

    fun testFindSingularAndPluralStepsUsingOptionalText() {
        myFixture.addFileToProject(
            "issues.feature",
            """
            Feature: Issues

              Scenario: Optional text
                And I have 42 cukes in my stomach
                And I have 1 cuke in my stomach
                And I have 1 cucumber in my stomach
            """.trimIndent()
        )
        myFixture.configureByText(
            "IssuesSteps.kt",
            """
            package steps

            import io.cucumber.java8.En

            class IssuesSteps : En {
                init {
                    And("I have {int} cu<caret>ke(s) in my stomach") { i: Int -> }
                }
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("And I have 1 cuke in my stomach", "And I have 42 cukes in my stomach"),
            findStepUsagesAtCaret()
        )
    }

    fun testFindGermanFeatureStepsUsingGermanStepDefinition() {
        myFixture.addFileToProject(
            "german.feature",
            """
            # language: de
            Funktionalität: Gurken

              Szenario: German steps
                Angenommen ich habe 42 Gurken in meinem Bauch
                Dann ich habe wirklich 42 Gurken in meinem Bauch
            """.trimIndent()
        )
        myFixture.configureByText(
            "GermanSteps.kt",
            """
            package steps

            import io.cucumber.java8.De

            class GermanSteps : De {
                init {
                    Angenommen("ich habe {long} Gur<caret>ken in meinem Bauch") { n: Long -> }
                }
            }
            """.trimIndent()
        )

        assertEquals(listOf("Angenommen ich habe 42 Gurken in meinem Bauch"), findStepUsagesAtCaret())
    }

    private fun findStepUsagesAtCaret(): List<String> =
        myFixture.findUsages(myFixture.elementAtCaret)
            .map { it.element }
            .filterIsInstance<GherkinStep>()
            .map { it.text }
            .sorted()
}
