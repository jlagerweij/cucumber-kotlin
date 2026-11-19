package net.lagerwey.plugins.cucumber.kotlin.steps.search

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.pom.PomDeclarationSearcher
import com.intellij.pom.PomTarget
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Consumer
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import net.lagerwey.plugins.cucumber.kotlin.inReadAction
import net.lagerwey.plugins.cucumber.kotlin.steps.reference.CucumberKotlinLambdaStepPomTarget
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList

/**
 * Counterpart of `CucumberJavaPomDeclarationSearcher` in the Cucumber for Java plugin: makes the step text of a step
 * definition a declaration, so "Find Usages" on it finds the Gherkin steps using it.
 */
class CucumberKotlinPomDeclarationSearcher : PomDeclarationSearcher() {
    override fun findDeclarationsAt(element: PsiElement, offsetInElement: Int, consumer: Consumer<in PomTarget>) {
        // The step text may contain an injected language, e.g. a regex
        val injectionHostOrElement = InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: element

        ProgressManager.checkCanceled()
        inReadAction { handleLambdaStepDeclaration(injectionHostOrElement, consumer) }
    }

    private fun handleLambdaStepDeclaration(element: PsiElement, consumer: Consumer<in PomTarget>) {
        val candidate = element.parent as? KtExpression ?: return
        val arguments = PsiTreeUtil.getParentOfType(candidate, KtValueArgumentList::class.java) ?: return
        val callExpression = arguments.parent as? KtCallExpression ?: return
        if (CucumberKotlinUtil.isLambdaStepDefinition(callExpression) && CucumberKotlinUtil.getLambdaStepName(callExpression) != null) {
            consumer.consume(CucumberKotlinLambdaStepPomTarget(callExpression))
        }
    }
}
