package net.lagerwey.plugins.cucumber.kotlin

/**
 * Minimal stand-in for the cucumber-java8 API, so the tests do not need the real jar.
 */
internal object CucumberJava8Stubs {
    /** Type parameters and body type for each supported lambda arity. */
    private val ARITIES = listOf(
        "" to "A0",
        "<T1> " to "A1<T1>",
        "<T1, T2> " to "A2<T1, T2>",
        "<T1, T2, T3> " to "A3<T1, T2, T3>",
        "<T1, T2, T3, T4> " to "A4<T1, T2, T3, T4>",
    )

    /**
     * Generates the overloads of the given step definition methods for every lambda arity.
     */
    private fun stepMethods(vararg keywords: String): String = keywords.joinToString("\n") { keyword ->
        ARITIES.joinToString("\n") { (typeParameters, bodyType) ->
            "    default ${typeParameters}void $keyword(String expression, StepDefinitionBody.$bodyType body) {}"
        }
    }

    private val EN_STUB = """
        |package io.cucumber.java8;
        |
        |public interface En extends LambdaGlue {
        |${stepMethods("Given", "When", "Then", "And", "But")}
        |}
    """.trimMargin()

    private val DE_STUB = """
        |package io.cucumber.java8;
        |
        |public interface De extends LambdaGlue {
        |${stepMethods("Angenommen", "Wenn", "Dann", "Und", "Aber")}
        |}
    """.trimMargin()

    private val SCENARIO_STUB = """
        package io.cucumber.java8;

        public interface Scenario {}
    """.trimIndent()

    private val DATA_TABLE_STUB = """
        package io.cucumber.datatable;

        public class DataTable {}
    """.trimIndent()

    private val LAMBDA_GLUE_STUB = """
        package io.cucumber.java8;

        import java.util.Map;

        public interface LambdaGlue {
            default <R> void ParameterType(String name, String regexp, ParameterDefinitionBody.A1<R> body) {}
            default <T> void DataTableType(DataTableEntryDefinitionBody<T> body) {}
            default void Before(HookBody body) {}
            default void BeforeStep(HookBody body) {}
            default void After(HookBody body) {}
            default void AfterStep(HookBody body) {}

            interface StepDefinitionBody {
                interface A0 { void accept() throws Throwable; }
                interface A1<T1> { void accept(T1 p1) throws Throwable; }
                interface A2<T1, T2> { void accept(T1 p1, T2 p2) throws Throwable; }
                interface A3<T1, T2, T3> { void accept(T1 p1, T2 p2, T3 p3) throws Throwable; }
                interface A4<T1, T2, T3, T4> { void accept(T1 p1, T2 p2, T3 p3, T4 p4) throws Throwable; }
            }

            interface ParameterDefinitionBody {
                interface A1<R> { R accept(String p1) throws Throwable; }
            }

            interface DataTableEntryDefinitionBody<T> { T accept(Map<String, String> entry) throws Throwable; }

            interface HookBody { void accept(Scenario scenario) throws Throwable; }
        }
    """.trimIndent()

    /** The stub sources, keyed by their path relative to a source root. */
    val FILES = mapOf(
        "io/cucumber/java8/LambdaGlue.java" to LAMBDA_GLUE_STUB,
        "io/cucumber/java8/En.java" to EN_STUB,
        "io/cucumber/java8/De.java" to DE_STUB,
        "io/cucumber/java8/Scenario.java" to SCENARIO_STUB,
        "io/cucumber/datatable/DataTable.java" to DATA_TABLE_STUB,
    )
}
