package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.psi.PsiElement
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class KotlinStepDefinition(private val method: KtCallExpression) : AbstractStepDefinition(method) {
    companion object {
        const val MULTILINE_STRING = "\"\"\""
        const val STRING = "\""
        const val DOUBLE_SLASHES = "\\\\"
        const val SINGLE_SLASH = "\\"
    }

    override fun getVariableNames() = CucumberKotlinUtil.getStepArguments(method).mapNotNull { it.name }

    override fun getCucumberRegexFromElement(element: PsiElement?): String? {
        val text = getStepDefinitionText() ?: return null
        return if (CucumberUtil.isCucumberExpression(text)) {
            CucumberUtil.buildRegexpFromCucumberExpression(text, KotlinParameterTypeManager)
        } else {
            text
        }
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
