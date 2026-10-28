package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinStepDefinitionCreator
import org.jetbrains.plugins.cucumber.inspections.CucumberStepInspection
import org.jetbrains.plugins.cucumber.psi.GherkinStep

class CreateStepDefinitionTest : CucumberKotlinTestCase() {

    fun testUndefinedStepOffersCreateStepDefinitionFix() {
        addSteps("""Given("I have a cucumber") { }""")
        myFixture.enableInspections(CucumberStepInspection::class.java)
        configureFeature("Given I have a <caret>tomato")

        val fixes = myFixture.getAllQuickFixes().map { it.text }
        assertContainsElements(fixes, "Create step definition")
    }

    fun testAddStepDefinitionToExistingFile() {
        val stepsFile = addSteps("""Given("I have a cucumber") { }""")
        configureFeature("When I eat the <caret>\"big\" tomato")

        val step = PsiTreeUtil.getParentOfType(myFixture.file.findElementAt(myFixture.caretOffset), GherkinStep::class.java)!!
        val created = WriteCommandAction.writeCommandAction(project).compute<Boolean, Throwable> {
            KotlinStepDefinitionCreator().createStepDefinition(step, stepsFile, false)
        }

        assertTrue("Step definition was not created", created)
        assertEquals(
            """
            package steps

            import io.cucumber.java8.En

            class MySteps : En {
                init {
                    Given("I have a cucumber") { }
                    When("I eat the \"big\" tomato") {
                        TODO("Not yet implemented")
                    }
                }
            }
            """.trimIndent(),
            stepsFile.text
        )
    }

    fun testCreateNewStepDefinitionFile() {
        configureFeature("Given I have a <caret>tomato")
        val step = PsiTreeUtil.getParentOfType(myFixture.file.findElementAt(myFixture.caretOffset), GherkinStep::class.java)!!
        val creator = KotlinStepDefinitionCreator()

        assertEquals("TestSteps", creator.getDefaultStepFileName(step))

        val directory = myFixture.psiManager.findDirectory(myFixture.tempDirFixture.findOrCreateDir("steps"))!!
        val file = WriteCommandAction.writeCommandAction(project).compute<_, Throwable> {
            creator.createStepDefinitionContainer(directory, "TestSteps")
        }

        assertEquals("TestSteps.kt", file.name)
        assertEquals(
            """
            package steps

            import io.cucumber.java8.En

            class TestSteps : En {
                init {
                }
            }
            """.trimIndent(),
            file.text
        )
    }

    fun testStepDefinitionFileNameValidation() {
        val creator = KotlinStepDefinitionCreator()

        assertTrue(creator.validateNewStepDefinitionFileName(project, "MySteps"))
        assertFalse(creator.validateNewStepDefinitionFileName(project, ""))
        assertFalse(creator.validateNewStepDefinitionFileName(project, "my-steps"))
    }
}
