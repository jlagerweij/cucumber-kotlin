package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.util.PathUtil
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinParameterTypeManager
import org.jetbrains.kotlin.psi.KtCallExpression
import java.io.File

/**
 * Step matching in a project where the glue is spread over modules that do not depend on each other,
 * but are combined by a module running the features:
 *
 * ```
 * app ──> steps ──> cucumber-api
 *    └──> types ──> cucumber-api
 * ```
 *
 * The `types` module declares the custom parameter types used by the step definitions in the `steps` module.
 */
class MultiModuleStepMatchingTest : UsefulTestCase() {

    private lateinit var myFixture: JavaCodeInsightTestFixture
    private lateinit var modules: Map<String, Module>

    override fun setUp() {
        super.setUp()
        val projectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(name)
        myFixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.fixture)

        // Kotlin stdlib is needed to resolve the step definition lambdas
        val stdlib = PathUtil.getJarPathForClass(Unit::class.java)
        val builders = MODULE_NAMES.associateWith { moduleName ->
            projectBuilder.addModule(JavaModuleFixtureBuilder::class.java).apply {
                val root = File(myFixture.tempDirPath, moduleName).apply { mkdirs() }
                addSourceContentRoot(root.path)
                addLibraryJars("kotlin-stdlib", PathUtil.getParentPath(stdlib), PathUtil.getFileName(stdlib))
            }
        }
        myFixture.setUp()
        modules = builders.mapValues { it.value.fixture.module }

        addDependency(STEPS, API)
        addDependency(TYPES, API)
        addDependency(APP, STEPS)
        addDependency(APP, TYPES)

        CucumberJava8Stubs.FILES.forEach { (path, text) -> myFixture.addFileToProject("$API/$path", text) }
    }

    override fun tearDown() {
        try {
            myFixture.tearDown()
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    fun testCustomParameterTypeDeclaredInOtherModule() {
        addParameterTypes()
        addSteps()
        configureFeature("Given I pick a <caret>green tomato")

        val targets = resolveStepAtCaret()
        assertEquals("Expected exactly one step definition, got: ${targets.map { it.text }}", 1, targets.size)
        assertInstanceOf(targets.single(), KtCallExpression::class.java)
        assertEquals(STEP, targets.single().text)
    }

    fun testCustomParameterTypeDeclaredInOtherModuleNavigatesToDeclaration() {
        addParameterTypes()
        addSteps()

        val declaration = KotlinParameterTypeManager.forModule(modules.getValue(STEPS)).getParameterTypeDeclaration("color")
        assertEquals(PARAMETER_TYPE, declaration?.text)
    }

    private fun addDependency(from: String, to: String) =
        ModuleRootModificationUtil.addDependency(modules.getValue(from), modules.getValue(to))

    private fun addParameterTypes() = addGlue(TYPES, "ParameterTypes", PARAMETER_TYPE)

    private fun addSteps() = addGlue(STEPS, "MySteps", STEP)

    private fun addGlue(moduleName: String, className: String, statement: String) {
        myFixture.addFileToProject(
            "$moduleName/glue/$className.kt",
            """
            |package glue
            |
            |import io.cucumber.java8.En
            |
            |class $className : En {
            |    init {
            |        $statement
            |    }
            |}
            """.trimMargin()
        )
    }

    /**
     * Opens a feature file in the `app` module with a single scenario containing the given step, which contains `<caret>`.
     */
    private fun configureFeature(step: String) {
        val text = "Feature: Test\n\n  Scenario: Test\n    $step\n"
        val caret = text.indexOf(CARET)
        val file = myFixture.addFileToProject("$APP/test.feature", text.replace(CARET, ""))
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        myFixture.editor.caretModel.moveToOffset(caret)
    }

    private fun resolveStepAtCaret(): List<com.intellij.psi.PsiElement> {
        val reference = myFixture.file.findReferenceAt(myFixture.caretOffset)
        assertNotNull("No reference found for the Gherkin step", reference)
        return (reference as PsiPolyVariantReference).multiResolve(false).mapNotNull { it.element }
    }

    companion object {
        private const val CARET = "<caret>"
        private const val API = "cucumber-api"
        private const val TYPES = "types"
        private const val STEPS = "steps"
        private const val APP = "app"
        private val MODULE_NAMES = listOf(API, TYPES, STEPS, APP)

        private const val PARAMETER_TYPE = """ParameterType("color", "red|green") { color: String -> color }"""
        private const val STEP = """Given("I pick a {color} tomato") { color: String -> }"""
    }
}
