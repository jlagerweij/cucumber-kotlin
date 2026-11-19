package net.lagerwey.plugins.cucumber.kotlin.steps.search

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesHandlerFactory
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import net.lagerwey.plugins.cucumber.kotlin.steps.reference.CucumberKotlinLambdaStepPomTarget

/**
 * Counterpart of `CucumberJavaFindUsagesHandlerFactory` in the Cucumber for Java plugin.
 */
class CucumberKotlinFindUsagesHandlerFactory : FindUsagesHandlerFactory() {
    override fun canFindUsages(element: PsiElement) = getStepDefinition(element) != null

    private fun getStepDefinition(element: PsiElement): PsiElement? {
        if (element is PomTargetPsiElement) {
            val target = element.target
            if (target is CucumberKotlinLambdaStepPomTarget) {
                return target.navigationElement
            }
        }
        return null
    }

    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler {
        return object : FindUsagesHandler(element) {}
    }
}
