package net.lagerwey.plugins.cucumber.kotlin

class CucumberKotlinInjectorTest : CucumberKotlinTestCase() {

    fun testRegexIsInjectedIntoRegexStepText() {
        configureSteps("""Given("^I have (\\d+) cu<caret>cumbers$") { count: Int -> }""")

        assertEquals("RegExp", languageAtCaret())
    }

    fun testRegexIsInjectedIntoRawStringStepText() {
        configureSteps("Given(\"\"\"^I have (\\d+) cu<caret>cumbers$\"\"\") { count: Int -> }")

        assertEquals("RegExp", languageAtCaret())
    }

    fun testNothingIsInjectedIntoCucumberExpression() {
        configureSteps("""Given("I have {int} cu<caret>cumbers") { count: Int -> }""")

        assertEquals("kotlin", languageAtCaret())
    }

    fun testNothingIsInjectedIntoOtherStrings() {
        configureSteps("""Given("I have a cucumber") { println("^not a st<caret>ep$") }""")

        assertEquals("kotlin", languageAtCaret())
    }

    private fun configureSteps(step: String) = myFixture.configureByText(
        "MySteps.kt",
        """
        |package steps
        |
        |import io.cucumber.java8.En
        |
        |class MySteps : En {
        |    init {
        |        $step
        |    }
        |}
        """.trimMargin()
    )

    /**
     * Returns the language at the caret: the fixture returns the injected file when the caret is inside an injection.
     */
    private fun languageAtCaret(): String = myFixture.file.language.id
}
