package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class KotlinStepDefinition(private val method: KtCallExpression) : AbstractStepDefinition(method) {
    companion object {
        const val MULTILINE_STRING = "\"\"\""
        const val STRING = "\""
        const val DOUBLE_SLASHES = "\\\\"
        const val SINGLE_SLASH = "\\"
        const val REGEX_START = "^"
        const val REGEX_END = "$"
    }

    override fun getVariableNames() = emptyList<String>()

    override fun getCucumberRegexFromElement(element: PsiElement?): String? {
        val text = getStepDefinitionText() ?: return null
        if (text.startsWith(REGEX_START) || text.endsWith(REGEX_END)) {
            return text
        }
        return CucumberUtil.buildRegexpFromCucumberExpression(text, KotlinParameterTypeManager)
    }

    private fun getStepDefinitionText(): String? {
        val callExpression = element as? KtCallExpression
        val argument = callExpression?.valueArguments?.getOrNull(0)?.getArgumentExpression() ?: return null
        return argument.text
            .removePrefix(MULTILINE_STRING).removeSuffix(MULTILINE_STRING)
            .removePrefix(STRING).removeSuffix(STRING)
            .replace(DOUBLE_SLASHES, SINGLE_SLASH)
    }
}
