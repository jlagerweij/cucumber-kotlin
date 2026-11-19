package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression

/**
 * Counterpart of `Java8StepDefinition` in the Cucumber for Java plugin: a step defined in cucumber-java8 style,
 * e.g. `Given("I have {int} cucumbers") { count: Int -> }`.
 */
class KotlinLambdaStepDefinition(element: KtCallExpression, module: Module) : AbstractKotlinStepDefinition(element, module) {

    /**
     * Returns the step text without quotes. Like in `Java8StepDefinition`, this can be a Cucumber expression as well as
     * a regex; [getCucumberRegex] turns it into a regex.
     */
    override fun getCucumberRegexFromElement(element: PsiElement?): String? {
        if (element !is KtCallExpression) return null
        val argument = CucumberKotlinUtil.getLambdaStepNameExpression(element) ?: return null
        return argument.text
            .removePrefix(MULTILINE_STRING).removeSuffix(MULTILINE_STRING)
            .removePrefix(STRING).removeSuffix(STRING)
            .replace(DOUBLE_SLASHES, SINGLE_SLASH)
    }

    override fun getElement(): KtCallExpression? = super.getElement() as? KtCallExpression

    override fun getVariableNames(): List<String> {
        val lambda = element?.lambdaArguments?.lastOrNull()?.getLambdaExpression()
            ?: element?.valueArguments?.getOrNull(1)?.getArgumentExpression() as? KtLambdaExpression
            ?: return emptyList()
        return lambda.valueParameters.mapNotNull { it.name }
    }

    override fun toString() = "KotlinLambdaStepDefinition{backed by element: $element}"

    private companion object {
        const val MULTILINE_STRING = "\"\"\""
        const val STRING = "\""
        const val DOUBLE_SLASHES = "\\\\"
        const val SINGLE_SLASH = "\\"
    }
}
