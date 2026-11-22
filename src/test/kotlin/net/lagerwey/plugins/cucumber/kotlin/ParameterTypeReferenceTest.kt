package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import net.lagerwey.plugins.cucumber.kotlin.steps.reference.CucumberKotlinParameterPomTarget
import net.lagerwey.plugins.cucumber.kotlin.steps.reference.CucumberKotlinParameterTypeReference

class ParameterTypeReferenceTest : CucumberKotlinTestCase() {

    fun testParameterTypeInStepTextResolvesToDeclaration() {
        addParameterTypes()
        configureSteps("""Given("I have a {co<caret>lor} cucumber") { color: String -> }""")

        val declaration = resolveParameterTypeAtCaret()

        assertEquals("color", ((declaration as PomTargetPsiElement).target as CucumberKotlinParameterPomTarget).name)
        assertEquals("\"color\"", declaration.navigationElement.text)
    }

    fun testParameterTypeAfterEscapedCharactersResolves() {
        addParameterTypes()
        configureSteps("""Given("I have \\(really\\) a {co<caret>lor} cucumber") { color: String -> }""")

        assertEquals("color", ((resolveParameterTypeAtCaret() as PomTargetPsiElement).target as CucumberKotlinParameterPomTarget).name)
    }

    fun testParameterTypeInRawStringResolves() {
        addParameterTypes()
        configureSteps("Given(\"\"\"I have a {co<caret>lor} cucumber\"\"\") { color: String -> }")

        assertEquals("color", ((resolveParameterTypeAtCaret() as PomTargetPsiElement).target as CucumberKotlinParameterPomTarget).name)
    }

    fun testBuiltInParameterTypeDoesNotResolve() {
        configureSteps("""Given("I have {i<caret>nt} cucumbers") { count: Int -> }""")

        assertNull(parameterTypeReferenceAtCaret().resolve())
    }

    fun testNoReferenceInRegexStepText() {
        addParameterTypes()
        configureSteps("""Given("^I have a {co<caret>lor} cucumber$") { color: String -> }""")

        assertEmpty(parameterTypeReferencesAtCaret())
    }

    fun testNoReferenceInOtherStrings() {
        addParameterTypes()
        configureSteps("""Given("I have a cucumber") { println("{co<caret>lor}") }""")

        assertEmpty(parameterTypeReferencesAtCaret())
    }

    fun testFindUsagesOfParameterType() {
        addSteps(
            """Given("I have a {color} cucumber") { color: String -> }""",
            """Given("I have a {color} and a {color} tomato") { first: String, second: String -> }""",
            """Given("I have a {int} tomatoes") { count: Int -> }""",
        )
        configureSteps("""ParameterType("co<caret>lor", "red|green") { color: String -> color }""")

        val usages = myFixture.findUsages(myFixture.elementAtCaret).map { it.element!!.text }.sorted()

        assertEquals(
            listOf("\"I have a {color} and a {color} tomato\"", "\"I have a {color} and a {color} tomato\"", "\"I have a {color} cucumber\""),
            usages
        )
    }

    fun testRenameParameterType() {
        addSteps("""Given("I have a {color} cucumber") { color: String -> }""")
        configureSteps("""ParameterType("co<caret>lor", "red|green") { color: String -> color }""")

        myFixture.renameElementAtCaret("colour")

        assertTrue(myFixture.file.text.contains("""ParameterType("colour", "red|green")"""))
        assertTrue(myFixture.findFileInTempDir("steps/MySteps.kt").let { psiManager.findFile(it)!!.text }
            .contains("""Given("I have a {colour} cucumber")"""))
    }

    private fun addParameterTypes() = addStepsFile(
        "ParameterTypes", "En",
        """ParameterType("color", "red|green") { color: String -> color }"""
    )

    private fun configureSteps(vararg steps: String) = myFixture.configureByText(
        "OtherSteps.kt",
        """
        |package steps
        |
        |import io.cucumber.java8.En
        |
        |class OtherSteps : En {
        |    init {
        |${steps.joinToString("\n") { "        $it" }}
        |    }
        |}
        """.trimMargin()
    )

    private fun parameterTypeReferencesAtCaret(): List<CucumberKotlinParameterTypeReference> {
        val element = myFixture.file.findElementAt(myFixture.caretOffset)!!
        return generateSequence<PsiElement>(element) { it.parent }
            .take(3)
            .flatMap { it.references.asSequence() }
            .filterIsInstance<CucumberKotlinParameterTypeReference>()
            .filter { it.absoluteRange.containsOffset(myFixture.caretOffset) }
            .toList()
    }

    private fun parameterTypeReferenceAtCaret(): CucumberKotlinParameterTypeReference =
        parameterTypeReferencesAtCaret().single()

    private fun resolveParameterTypeAtCaret(): PsiElement {
        val declaration = parameterTypeReferenceAtCaret().resolve()
        assertNotNull("Parameter type did not resolve", declaration)
        assertTrue((declaration as PomTargetPsiElement).target is CucumberKotlinParameterPomTarget)
        return declaration
    }
}
