package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

class KotlinStepDefinition(method: PsiElement) : AbstractStepDefinition(method) {

    override fun getVariableNames(): MutableList<String> = mutableListOf()

    override fun getCucumberRegexFromElement(element: PsiElement?): String? {
        if (element is KtCallExpression) {
            val argumentExpression = element.valueArguments[0]?.getArgumentExpression() ?: return null

            val text = argumentExpression.text
            return text.removePrefix("\"").removeSuffix("\"")
        }
        return null
    }
}