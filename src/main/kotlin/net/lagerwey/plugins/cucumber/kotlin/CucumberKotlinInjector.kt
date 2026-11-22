package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.plugins.cucumber.CucumberUtil

/**
 * Counterpart of `CucumberJavaInjector` in the Cucumber for Java plugin: injects the RegExp language into the step text
 * of a step definition written as a regex, e.g. `Given("^I have (\\d+) cucumbers$") { count: Int -> }`.
 *
 * Step texts written as a Cucumber expression are left alone.
 */
class CucumberKotlinInjector : MultiHostInjector {
    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val language = regexpLanguage ?: return
        if (context !is KtStringTemplateExpression || !context.isValidHost) return

        val (value, _) = CucumberKotlinUtil.getStringValue(context) ?: return
        if (value.isEmpty() || CucumberUtil.isCucumberExpression(value)) return
        if (CucumberKotlinUtil.getLambdaStepDefinitionOfStepText(context) == null) return

        registrar.startInjecting(language)
            .addPlace(null, null, context, ElementManipulators.getValueTextRange(context))
            .doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> = listOf(KtStringTemplateExpression::class.java)

    companion object {
        val regexpLanguage: Language? = Language.findLanguageByID("RegExp")
    }
}
