package net.lagerwey.plugins.cucumber.kotlin

import org.jetbrains.kotlin.psi.KtCallExpression

class StepNavigationTest : CucumberKotlinTestCase() {

    fun testNavigateFromFeatureStepToKotlinStepDefinition() {
        addSteps("""Given("I have a cucumber") { }""")
        configureFeature("Given I have a <caret>cucumber")

        val targets = resolveStepAtCaret()
        assertEquals("Expected exactly one step definition", 1, targets.size)

        val target = targets.single()
        assertInstanceOf(target, KtCallExpression::class.java)
        assertEquals("Given(\"I have a cucumber\") { }", target.text)
    }
}
