package net.lagerwey.plugins.cucumber.kotlin.steps.search

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.pom.PomDeclarationSearcher
import com.intellij.pom.PomTarget
import com.intellij.psi.PsiElement
import com.intellij.util.Consumer
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import net.lagerwey.plugins.cucumber.kotlin.inReadAction
import net.lagerwey.plugins.cucumber.kotlin.steps.reference.CucumberKotlinLambdaStepPomTarget
import net.lagerwey.plugins.cucumber.kotlin.steps.reference.CucumberKotlinParameterPomTarget
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Counterpart of `CucumberJavaPomDeclarationSearcher` in the Cucumber for Java plugin. Makes these strings declarations,
 * so "Find Usages" and "Rename" work on them:
 *
 * - the step text of a step definition, used by Gherkin steps;
 * - the name of a custom parameter type in `ParameterType("color", ...)`, used by Cucumber expressions in step texts.
 */
class CucumberKotlinPomDeclarationSearcher : PomDeclarationSearcher() {
    override fun findDeclarationsAt(element: PsiElement, offsetInElement: Int, consumer: Consumer<in PomTarget>) {
        // The step text may contain an injected language, e.g. a regex
        val injectionHostOrElement = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: element

        ProgressManager.checkCanceled()
        inReadAction {
            // Either the string itself (e.g. an injection host) or one of its entries
            val stringTemplate = injectionHostOrElement as? KtStringTemplateExpression
                ?: injectionHostOrElement.parent as? KtStringTemplateExpression
                ?: return@inReadAction
            handleLambdaStepDeclaration(stringTemplate, consumer)
            handleParameterTypeDeclaration(stringTemplate, consumer)
        }
    }

    private fun handleLambdaStepDeclaration(stringTemplate: KtStringTemplateExpression, consumer: Consumer<in PomTarget>) {
        val callExpression = CucumberKotlinUtil.getLambdaStepDefinitionOfStepText(stringTemplate) ?: return
        if (CucumberKotlinUtil.getLambdaStepName(callExpression) != null) {
            consumer.consume(CucumberKotlinLambdaStepPomTarget(callExpression))
        }
    }

    private fun handleParameterTypeDeclaration(stringTemplate: KtStringTemplateExpression, consumer: Consumer<in PomTarget>) {
        val (name, _) = CucumberKotlinUtil.getStringValue(stringTemplate) ?: return
        if (name.isEmpty() || CucumberKotlinUtil.getLambdaParameterTypeOfName(stringTemplate) == null) return
        consumer.consume(CucumberKotlinParameterPomTarget(stringTemplate, name))
    }
}
