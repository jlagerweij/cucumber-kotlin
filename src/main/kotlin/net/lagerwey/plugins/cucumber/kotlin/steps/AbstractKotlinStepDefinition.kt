package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition

/**
 * Counterpart of `AbstractJavaStepDefinition` in the Cucumber for Java plugin: a step definition in [module] whose
 * text is either a Cucumber expression or a regex.
 */
abstract class AbstractKotlinStepDefinition(element: PsiElement, val module: Module) : AbstractStepDefinition(element) {

    override fun getCucumberRegex(): String? {
        val definitionText = expression ?: return null
        if (element == null) return null

        if (CucumberUtil.isCucumberExpression(definitionText)) {
            val parameterTypes = CucumberKotlinUtil.getAllParameterTypes(module)
            return CucumberUtil.buildRegexpFromCucumberExpression(definitionText, parameterTypes)
        }

        return definitionText
    }
}
