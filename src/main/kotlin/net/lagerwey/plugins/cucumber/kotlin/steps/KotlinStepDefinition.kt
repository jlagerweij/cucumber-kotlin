package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.psi.PsiElement
import net.lagerwey.plugins.cucumber.kotlin.getStepArguments
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class KotlinStepDefinition(private val method: KtCallExpression) : AbstractStepDefinition(method) {

    override fun getVariableNames() = getStepArguments(method).mapNotNull { it.name }

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
        return argument.text.removePrefix("\"").removeSuffix("\"").replace("\\\\", "\\")
    }
}
