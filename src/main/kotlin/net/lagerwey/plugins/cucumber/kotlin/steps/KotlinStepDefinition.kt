package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.MapParameterTypeManager
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class KotlinStepDefinition(method: PsiElement) : AbstractStepDefinition(method) {

    override fun getVariableNames(): MutableList<String> = mutableListOf()

    override fun getCucumberRegexFromElement(element: PsiElement?): String? {
        val text = stepDefinitionText ?: return null
        return if (isCucumberExpression(text)) {
            CucumberUtil.buildRegexpFromCucumberExpression(text, MapParameterTypeManager.DEFAULT)
        } else {
            text
        }
    }

    override fun getStepDefinitionText(): String? {
        val callExpression = element as? KtCallExpression
        val argument = callExpression?.valueArguments?.getOrNull(0)?.getArgumentExpression() ?: return null
        return argument.text.removePrefix("\"").removeSuffix("\"").replace("\\\\", "\\")
    }
}