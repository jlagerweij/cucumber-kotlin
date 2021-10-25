package net.lagerwey.plugins.cucumber.kotlin.search

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.pom.PomDeclarationSearcher
import com.intellij.pom.PomTarget
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Consumer
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import net.lagerwey.plugins.cucumber.kotlin.inReadAction
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList

class StepDeclarationSearcher : PomDeclarationSearcher() {
    override fun findDeclarationsAt(element: PsiElement, offsetInElement: Int, consumer: Consumer<PomTarget>) {
        val injectionHostOrElement = InjectedLanguageManager.getInstance(element.project)
                .getInjectionHost(element) ?: element

        ProgressManager.checkCanceled()
        val stepDeclaration = inReadAction {
            when (val candidate = injectionHostOrElement.parent) {
                is KtExpression -> findStepDeclaration(candidate)
                else -> null
            }
        }

        stepDeclaration?.let {
            consumer.consume(it)
        }
    }

    private fun findStepDeclaration(element: KtExpression): StepDeclaration? {
        PsiTreeUtil.getParentOfType(element, KtValueArgumentList::class.java)?.let { arguments ->
            val method = arguments.parent
            if (CucumberKotlinUtil.isStepDefinition(method)) {
                val stepName = CucumberKotlinUtil.getStepName(method as KtCallExpression) ?: return null
                return getStepDeclaration(method, stepName)
            }
        }

        return null
    }

    private fun getStepDeclaration(element: PsiElement, stepName: String?): StepDeclaration? {
        if (stepName == null) {
            return null
        }
        return CachedValuesManager.getCachedValue(element) {
            CachedValueProvider.Result.create(StepDeclaration(element, stepName), element)
        }
    }
}
