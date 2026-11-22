package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.cucumber.java.run.CucumberJavaRunConfiguration
import org.jetbrains.plugins.cucumber.run.CucumberRunLineMarkerContributor

/**
 * Running features with Kotlin step definitions is provided by the Cucumber for Java plugin.
 * These tests make sure its gutter icons and run configurations work for Kotlin projects.
 */
class RunConfigurationTest : CucumberKotlinTestCase() {

    fun testRunLineMarkerOnFeature() {
        addSteps("""Given("I have a cucumber") { }""")
        configureFeature("Given I have a cucumber")

        assertNotNull(CucumberRunLineMarkerContributor().getInfo(elementAt("Feature")))
    }

    fun testRunLineMarkerOnScenario() {
        addSteps("""Given("I have a cucumber") { }""")
        configureFeature("Given I have a cucumber")

        assertNotNull(CucumberRunLineMarkerContributor().getInfo(elementAt("Scenario")))
    }

    fun testRunFeature() {
        addSteps("""Given("I have a cucumber") { }""")
        configureFeature("Given I have a cucumber")

        val configuration = cucumberConfigurationFor(elementAt("Feature"))

        assertEquals("Feature: test", configuration.name)
        assertEquals(myFixture.file.virtualFile.path, configuration.filePath)
        assertEquals("io.cucumber.core.cli.Main", configuration.mainClassName)
        assertTrue(configuration.programParameters!!.contains("--plugin teamcity"))
        assertEmpty(configuration.nameFilter)
    }

    fun testRunScenario() {
        addSteps("""Given("I have a cucumber") { }""")
        configureFeature("Given I have a <caret>cucumber")

        val configuration = cucumberConfigurationFor(myFixture.file.findElementAt(myFixture.caretOffset)!!)

        assertEquals("Scenario: Test", configuration.name)
        assertEquals(myFixture.file.virtualFile.path, configuration.filePath)
        assertEquals("^Test$", configuration.nameFilter)
        assertTrue(configuration.programParameters!!.contains("--name ^Test$"))
    }

    fun testRunAllFeaturesInFolder() {
        addSteps("""Given("I have a cucumber") { }""")
        val feature = myFixture.addFileToProject("features/test.feature", "Feature: Test")

        val folder = feature.containingDirectory
        val configuration = cucumberConfigurationFor(folder)

        assertEquals(folder.virtualFile.path, configuration.filePath)
    }

    fun testNoRunConfigurationForFolderWithoutFeatures() {
        val steps = addSteps("""Given("I have a cucumber") { }""")

        assertNull(findCucumberConfiguration(steps.containingDirectory))
    }

    private fun elementAt(text: String): PsiElement {
        val offset = myFixture.file.text.indexOf(text)
        assertTrue("'$text' not found in the feature file", offset >= 0)
        return myFixture.file.findElementAt(offset)!!
    }

    private fun cucumberConfigurationFor(element: PsiElement): CucumberJavaRunConfiguration {
        val configuration = findCucumberConfiguration(element)
        assertNotNull("No Cucumber run configuration created for ${element.text}", configuration)
        return configuration!!
    }

    /**
     * Asks all registered run configuration producers for configurations, like the IDE does for the gutter icon and
     * the context menu, and returns the Cucumber one.
     */
    private fun findCucumberConfiguration(element: PsiElement): CucumberJavaRunConfiguration? =
        ConfigurationContext.createEmptyContextForLocation(PsiLocation(element))
            .configurationsFromContext
            .orEmpty()
            .map { it.configuration }
            .filterIsInstance<CucumberJavaRunConfiguration>()
            .firstOrNull()
}
