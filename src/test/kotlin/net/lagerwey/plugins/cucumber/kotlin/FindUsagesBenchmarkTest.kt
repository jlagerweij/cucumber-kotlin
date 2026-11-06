package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinParameterTypeManager
import org.jetbrains.plugins.cucumber.psi.GherkinStep

/**
 * Benchmarks the lookup from a Kotlin step definition to the Gherkin steps using it (find usages),
 * in a project with many step definitions and custom parameter types.
 *
 * Measures the lookup both with warm caches and directly after an unrelated PSI change (simulating typing),
 * and the parts of a lookup that are recomputed after a change: resolving the custom parameter types and loading
 * the step definitions. The same project using only built-in parameter types serves as a baseline.
 * Timings are only printed, as those depend on the machine.
 *
 * Step definitions are resolved concurrently during a lookup, so it also counts how often the custom parameter types
 * are collected per lookup. That count is asserted: at most once per lookup, and only when a parameter type changed.
 */
class FindUsagesBenchmarkTest : CucumberKotlinTestCase() {

    fun testFindUsagesWithCustomParameterTypes() {
        benchmark("custom parameter types", customParameterTypes = true)
    }

    fun testFindUsagesWithBuiltInParameterTypesOnly() {
        benchmark("built-in parameter types only", customParameterTypes = false)
    }

    private fun benchmark(name: String, customParameterTypes: Boolean) {
        val unrelatedFile = setUpProject(customParameterTypes)

        // First lookup builds the indices and caches
        assertEquals(FEATURE_FILES, findStepUsages())

        val warm = measure { findStepUsages() }
        val afterChange = measure {
            changePsi(unrelatedFile)
            findStepUsages()
        }
        val changeOnly = measure { changePsi(unrelatedFile) }
        val parameterTypesAfterChange = measure {
            changePsi(unrelatedFile)
            KotlinParameterTypeManager.forModule(module)
        }
        val stepsAfterChange = measure {
            changePsi(unrelatedFile)
            CucumberKotlinLambdaExtension().loadStepsFor(module)
        }

        val stepsFile = myFixture.findFileInTempDir("steps/Steps1.kt").let { psiManager.findFile(it)!! }
        val afterUnrelatedChange = countParameterTypeComputations { changePsi(unrelatedFile) }
        val afterStepFileChange = countParameterTypeComputations { changePsi(stepsFile) }
        val afterParameterTypeChange = if (customParameterTypes) {
            countParameterTypeComputations { replaceInFile(stepsFile, "\"red1|green1\"", "\"red1|green1|blue1\"") }.toString()
        } else "n/a"

        println(
            """
            |Find usages benchmark, $name ($STEP_FILES step files x $STEPS_PER_FILE steps, $FEATURE_FILES feature files)
            |  find usages, warm caches:          ${warm.describe()}
            |  find usages after a PSI change:    ${afterChange.describe()}
            |  PSI change only:                   ${changeOnly.describe()}
            |  parameter types after PSI change:  ${parameterTypesAfterChange.describe()}
            |  load steps after PSI change:       ${stepsAfterChange.describe()}
            |  parameter type computations per find usages:
            |    after an unrelated change:       $afterUnrelatedChange
            |    after a change in a step file:   $afterStepFileChange
            |    after a parameter type change:   $afterParameterTypeChange
            """.trimMargin()
        )

        assertEquals("Parameter types recomputed after an unrelated change", 0, afterUnrelatedChange)
        assertEquals("Parameter types recomputed after a change not touching parameter types", 0, afterStepFileChange)
        if (customParameterTypes) {
            assertEquals("Parameter types not computed exactly once after a parameter type change", "1", afterParameterTypeChange)
        }
    }

    /**
     * Applies the change, then runs a lookup and returns how often the parameter types were computed during it.
     */
    private fun countParameterTypeComputations(change: () -> Unit): Int {
        change()
        val before = KotlinParameterTypeManager.computationCount.get()
        assertEquals(FEATURE_FILES, findStepUsages())
        return KotlinParameterTypeManager.computationCount.get() - before
    }

    private fun setUpProject(customParameterTypes: Boolean): PsiFile {
        repeat(STEP_FILES) { file ->
            val parameterType = if (customParameterTypes) "{color$file}" else "{word}"
            addStepsFile(
                "Steps$file",
                "En",
                if (customParameterTypes) """ParameterType("color$file", "red$file|green$file") { color: String -> color }""" else "",
                *Array(STEPS_PER_FILE) { step ->
                    """Given("I pick step $file-$step with a $parameterType tomato") { color: String -> }"""
                },
            )
        }
        repeat(FEATURE_FILES) { feature ->
            myFixture.addFileToProject(
                "features/feature$feature.feature",
                """
                |Feature: Feature $feature
                |
                |  Scenario: Scenario $feature
                |${(0 until STEPS_PER_FILE).joinToString("\n") { step -> "    Given I pick step ${feature % STEP_FILES}-$step with a red${feature % STEP_FILES} tomato" }}
                """.trimMargin()
            )
        }
        val targetParameterType = if (customParameterTypes) "{color0}" else "{word}"
        myFixture.configureByText(
            "TargetSteps.kt",
            """
            package target

            import io.cucumber.java8.En

            class TargetSteps : En {
                init {
                    Given("I pick the tar<caret>get step with a $targetParameterType tomato") { color: String -> }
                }
            }
            """.trimIndent()
        )
        repeat(FEATURE_FILES) { feature ->
            myFixture.addFileToProject(
                "features/target$feature.feature",
                """
                |Feature: Target $feature
                |
                |  Scenario: Target $feature
                |    Given I pick the target step with a green0 tomato
                """.trimMargin()
            )
        }
        return myFixture.addFileToProject("other/Unrelated.kt", "package other\n\nval unrelated = 1\n")
    }

    private fun findStepUsages(): Int =
        myFixture.findUsages(myFixture.elementAtCaret).count { it.element is GherkinStep }

    private fun changePsi(file: PsiFile) {
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file)!!
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.textLength, " ")
            documentManager.commitDocument(document)
        }
    }

    private fun replaceInFile(file: PsiFile, oldText: String, newText: String) {
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(file)!!
        val offset = document.text.indexOf(oldText)
        assertTrue("'$oldText' not found in ${file.name}", offset >= 0)
        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(offset, offset + oldText.length, newText)
            documentManager.commitDocument(document)
        }
    }

    private fun measure(action: () -> Unit): Timings =
        Timings(List(ITERATIONS) {
            val start = System.nanoTime()
            action()
            (System.nanoTime() - start) / 1_000_000.0
        })

    private class Timings(private val millis: List<Double>) {
        fun describe(): String {
            val sorted = millis.sorted()
            return "median %.1f ms, min %.1f ms, max %.1f ms".format(sorted[sorted.size / 2], sorted.first(), sorted.last())
        }
    }

    companion object {
        private const val STEP_FILES = 50
        private const val STEPS_PER_FILE = 20
        private const val FEATURE_FILES = 50
        private const val ITERATIONS = 10
    }
}
