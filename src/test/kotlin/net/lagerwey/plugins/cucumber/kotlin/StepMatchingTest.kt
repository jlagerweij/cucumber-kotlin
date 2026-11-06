package net.lagerwey.plugins.cucumber.kotlin

import org.jetbrains.kotlin.psi.KtCallExpression

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

    fun testCustomParameterType() {
        addSteps(
            """ParameterType("color", "red|green") { color: String -> color }""",
            """Given("I pick a {color} tomato") { color: String -> }""",
        )
        configureFeature("Given I pick a <caret>red tomato")

        assertSingleTarget("""Given("I pick a {color} tomato") { color: String -> }""")
    }

    fun testCustomParameterTypeDoesNotMatchOtherValues() {
        addSteps(
            """ParameterType("color", "red|green") { color: String -> color }""",
            """Given("I pick a {color} tomato") { color: String -> }""",
        )
        configureFeature("Given I pick a <caret>blue tomato")

        assertEmpty(resolveStepAtCaret())
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

    // Variants from sample-project/src/test/kotlin/io/cucumber/kotlin/IssuesStepDefinition.kt

    fun testOptionalTextMatchesPlural() {
        addSteps("""And("I have {int} cuke(s) in my stomach") { i: Int -> }""")
        configureFeature("And I have 42 <caret>cukes in my stomach")

        assertSingleTarget("""And("I have {int} cuke(s) in my stomach") { i: Int -> }""")
    }

    fun testOptionalTextMatchesSingular() {
        addSteps("""And("I have {int} cuke(s) in my stomach") { i: Int -> }""")
        configureFeature("And I have 1 <caret>cuke in my stomach")

        assertSingleTarget("""And("I have {int} cuke(s) in my stomach") { i: Int -> }""")
    }

    fun testAlternativeTextMatchesFirstAlternative() {
        addSteps("""And("I have {int} cucumber(s) in my belly/stomach") { i: Int -> }""")
        configureFeature("And I have 1 cucumber in my <caret>belly")

        assertSingleTarget("""And("I have {int} cucumber(s) in my belly/stomach") { i: Int -> }""")
    }

    fun testAlternativeTextMatchesSecondAlternative() {
        addSteps("""And("I have {int} cucumber(s) in my belly/stomach") { i: Int -> }""")
        configureFeature("And I have 42 cucumbers in my <caret>stomach")

        assertSingleTarget("""And("I have {int} cucumber(s) in my belly/stomach") { i: Int -> }""")
    }

    fun testRegularExpressionWithAnchors() {
        addSteps("""And("^the product id is (\\d+)$") { number: Int -> }""")
        configureFeature("And the product id is <caret>34")

        assertSingleTarget("""And("^the product id is (\\d+)$") { number: Int -> }""")
    }

    fun testRegularExpressionWithOptionalGroupPresent() {
        addSteps("""And("^Product(?: with id (\\d+))? is valid") { number: Int? -> }""")
        configureFeature("And Product with id <caret>25 is valid")

        assertSingleTarget("""And("^Product(?: with id (\\d+))? is valid") { number: Int? -> }""")
    }

    fun testRegularExpressionWithOptionalGroupAbsent() {
        addSteps("""And("^Product(?: with id (\\d+))? is valid") { number: Int? -> }""")
        configureFeature("And Product is <caret>valid")

        assertSingleTarget("""And("^Product(?: with id (\\d+))? is valid") { number: Int? -> }""")
    }

    fun testSingleWordStep() {
        addSteps("""Then("test") {}""")
        configureFeature("Then te<caret>st")

        assertSingleTarget("""Then("test") {}""")
    }

    fun testStepInScenarioOutline() {
        addSteps("""And("^the product id is (\\d+)$") { number: Int -> }""")
        myFixture.configureByText(
            "test.feature",
            """
            Feature: Test

              Scenario Outline: Test
                And the product id is <caret>34

                Examples:
                  | id  |
                  | 123 |
            """.trimIndent()
        )

        assertSingleTarget("""And("^the product id is (\\d+)$") { number: Int -> }""")
    }

    // Variants from sample-project/src/test/kotlin/io/cucumber/kotlin/LambdaStepDefinitions.kt

    fun testCucumberExpressionWithLongParameter() {
        addSteps("""Given("I have {long} cukes in my belly") { n: Long -> }""")
        configureFeature("Given I have <caret>42 cukes in my belly")

        assertSingleTarget("""Given("I have {long} cukes in my belly") { n: Long -> }""")
    }

    fun testCucumberExpressionWithFourParametersOfDifferentTypes() {
        val definition = """Given("I will give you {int} and {float} and {word} and {int}") { a: Int, b: Float, c: String, d: Int -> }"""
        addSteps(definition)
        configureFeature("Given I will give you 1 and 2.2 and <caret>three and 4")

        assertSingleTarget(definition)
    }

    fun testLambdaPassedInsideParentheses() {
        addSteps("""Given("A statement with a simple match", { })""")
        configureFeature("Given A statement with a <caret>simple match")

        assertSingleTarget("""Given("A statement with a simple match", { })""")
    }

    fun testLambdaWithBodyExpression() {
        addSteps("""Given("A statement with a body expression") { check(true) }""")
        configureFeature("Given A statement with a <caret>body expression")

        assertSingleTarget("""Given("A statement with a body expression") { check(true) }""")
    }

    fun testStepWithDataTable() {
        addSteps("""Given("this data table:") { peopleTable: io.cucumber.datatable.DataTable -> }""")
        configureFeature(
            "Given this data <caret>table:",
            "  | first  | last     |",
            "  | Aslak  | Hellesøy |",
        )

        assertSingleTarget("""Given("this data table:") { peopleTable: io.cucumber.datatable.DataTable -> }""")
    }

    fun testStepDefinitionsBetweenLocalDeclarations() {
        addSteps(
            "val alreadyHadThisManyCukes = 1",
            """Given("I have {long} cukes in my belly") { n: Long -> check(alreadyHadThisManyCukes == 1) }""",
            "val localState = \"hello\"",
            """Then("I really have {int} cukes in my belly") { i: Int -> check(localState == "hello") }""",
        )
        configureFeature(
            "Given I have 42 cukes in my belly",
            "Then I <caret>really have 42 cukes in my belly",
        )

        assertSingleTarget("""Then("I really have {int} cukes in my belly") { i: Int -> check(localState == "hello") }""")
    }

    fun testHooksAndTypeRegistrationsAreNotStepDefinitions() {
        addSteps(
            """ParameterType("color", "red|green") { color: String -> color }""",
            """DataTableType { entry: Map<String, String> -> entry["first"] }""",
            """Before { scenario: io.cucumber.java8.Scenario -> }""",
            """BeforeStep { scenario: io.cucumber.java8.Scenario -> }""",
            """AfterStep { scenario: io.cucumber.java8.Scenario -> }""",
            """After { scenario: io.cucumber.java8.Scenario -> }""",
            """Given("I have a cucumber") { }""",
        )
        configureFeature("Given I have a <caret>cucumber")

        assertSingleTarget("""Given("I have a cucumber") { }""")
    }

    fun testCustomParameterTypeWithComplexRegex() {
        addSteps(
            STRING_LIST_PARAMETER_TYPE,
            """Then("I have {stringList} in my basket") { strings: List<String> -> }""",
        )
        configureFeature("Then I have \"foo\" and <caret>\"bar\" in my basket")

        assertSingleTarget("""Then("I have {stringList} in my basket") { strings: List<String> -> }""")
    }

    fun testCustomParameterTypeDeclaredInOtherFile() {
        addStepsFile("ParameterTypes", "En", """ParameterType("color", "red|green") { color: String -> color }""")
        addSteps("""Given("I pick a {color} tomato") { color: String -> }""")
        configureFeature("Given I pick a <caret>green tomato")

        assertSingleTarget("""Given("I pick a {color} tomato") { color: String -> }""")
    }

    // Variants from sample-project/src/test/kotlin/io/cucumber/kotlin/GermanStepDefinitions.kt

    fun testGermanStepDefinition() {
        addStepsFile("GermanSteps", "De", """Angenommen("ich habe {long} Gurken in meinem Bauch") { n: Long -> }""")
        configureGermanFeature("Angenommen ich habe <caret>42 Gurken in meinem Bauch")

        assertSingleTarget("""Angenommen("ich habe {long} Gurken in meinem Bauch") { n: Long -> }""")
    }

    fun testGermanStepDefinitionWithOtherKeyword() {
        addStepsFile("GermanSteps", "De", """Dann("ich habe wirklich {int} Gurken in meinem Bauch") { i: Int -> }""")
        configureGermanFeature(
            "Angenommen ich habe 42 Gurken in meinem Bauch",
            "Dann ich habe <caret>wirklich 42 Gurken in meinem Bauch",
        )

        assertSingleTarget("""Dann("ich habe wirklich {int} Gurken in meinem Bauch") { i: Int -> }""")
    }

    fun testGermanStepDefinitionWithParameterTypeFromEnglishGlue() {
        addSteps(STRING_LIST_PARAMETER_TYPE)
        addStepsFile(
            "GermanSteps",
            "De",
            """Dann("ich habe wirklich {stringList} Gurken in meinem Bauch") { strings: List<String> -> }""",
        )
        configureGermanFeature("Dann ich habe wirklich \"foo\" and <caret>\"bar\" Gurken in meinem Bauch")

        assertSingleTarget("""Dann("ich habe wirklich {stringList} Gurken in meinem Bauch") { strings: List<String> -> }""")
    }

    private fun assertSingleTarget(expectedText: String) {
        val targets = resolveStepAtCaret()
        assertEquals("Expected exactly one step definition, got: ${targets.map { it.text }}", 1, targets.size)
        val target = targets.single()
        assertInstanceOf(target, KtCallExpression::class.java)
        assertEquals(expectedText, target.text)
    }

    companion object {
        /** The `stringList` parameter type from the sample project, matching e.g. `"foo" and "bar"`. */
        private const val STRING_LIST_PARAMETER_TYPE =
            """ParameterType("stringList", "(\"([^\"]+)\"(\\s*(([,]?\\s*)|(and\\s?))\"[^\"]+\")*)") { strings: String -> listOf(strings) }"""
    }
}
