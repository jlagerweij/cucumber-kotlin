package net.lagerwey.plugins.cucumber.kotlin

class StepMatchingTest : CucumberKotlinTestCase() {

    fun testCucumberExpressionWithIntParameter() {
        addSteps("""Given("I have {int} cucumbers") { count: Int -> }""")
        configureFeature("Given I have <caret>42 cucumbers")

        assertSingleTarget("""Given("I have {int} cucumbers") { count: Int -> }""")
    }

    fun testCucumberExpressionWithStringParameter() {
        addSteps("""Given("my name is {string}") { name: String -> }""")
        configureFeature("Given my name is <caret>\"Cuke\"")

        assertSingleTarget("""Given("my name is {string}") { name: String -> }""")
    }

    fun testCucumberExpressionWithMultipleParameters() {
        addSteps("""Given("{int} cucumbers cost {double} euro") { count: Int, price: Double -> }""")
        configureFeature("Given 3 cucumbers <caret>cost 1.50 euro")

        assertSingleTarget("""Given("{int} cucumbers cost {double} euro") { count: Int, price: Double -> }""")
    }

    fun testRegularExpression() {
        addSteps("""Given("^I have (\\d+) cucumbers?$") { count: Int -> }""")
        configureFeature("Given I have <caret>1 cucumber")

        assertSingleTarget("""Given("^I have (\\d+) cucumbers?$") { count: Int -> }""")
    }

    fun testMultilineString() {
        addSteps("Given(\"\"\"^a \"quoted\" step$\"\"\") { }")
        configureFeature("Given a <caret>\"quoted\" step")

        assertSingleTarget("Given(\"\"\"^a \"quoted\" step$\"\"\") { }")
    }

    fun testAndStepMatchesGivenDefinition() {
        addSteps(
            """Given("I have a cucumber") { }""",
            """Given("I have a tomato") { }""",
        )
        configureFeature(
            "Given I have a cucumber",
            "And I have a <caret>tomato",
        )

        assertSingleTarget("""Given("I have a tomato") { }""")
    }

    fun testKeywordIsIgnoredWhenMatching() {
        addSteps("""Then("the salad is ready") { }""")
        configureFeature("Given the salad is <caret>ready")

        assertSingleTarget("""Then("the salad is ready") { }""")
    }

    fun testOnlyMatchingDefinitionIsResolved() {
        addSteps(
            """Given("I have a cucumber") { }""",
            """Given("I have a cucumber salad") { }""",
        )
        configureFeature("Given I have a <caret>cucumber")

        assertSingleTarget("""Given("I have a cucumber") { }""")
    }

    fun testUnknownStepDoesNotResolve() {
        addSteps("""Given("I have a cucumber") { }""")
        configureFeature("Given I have a <caret>pickle")

        assertEmpty(resolveStepAtCaret())
    }

    fun testCallOutsideCucumberApiIsNotAStepDefinition() {
        myFixture.addFileToProject(
            "other/NotCucumber.kt",
            """
            package other

            fun Given(expression: String, body: () -> Unit) {}

            fun setup() {
                Given("I have a cucumber") { }
            }
            """.trimIndent()
        )
        configureFeature("Given I have a <caret>cucumber")

        assertEmpty(resolveStepAtCaret())
    }

    private fun assertSingleTarget(expectedText: String) {
        val targets = resolveStepAtCaret()
        assertEquals("Expected exactly one step definition, got: ${targets.map { it.text }}", 1, targets.size)
        assertEquals(expectedText, targets.single().text)
    }
}
