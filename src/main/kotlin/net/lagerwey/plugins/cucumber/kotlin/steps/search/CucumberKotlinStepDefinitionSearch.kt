package net.lagerwey.plugins.cucumber.kotlin.steps.search

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import net.lagerwey.plugins.cucumber.kotlin.steps.reference.CucumberKotlinLambdaStepPomTarget
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.plugins.cucumber.CucumberUtil

/**
 * Counterpart of `CucumberJavaStepDefinitionSearch` in the Cucumber for Java plugin: finds the Gherkin steps using a
 * step definition.
 */
class CucumberKotlinStepDefinitionSearch : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val elementToSearch = queryParameters.elementToSearch
        val searchScope = queryParameters.effectiveSearchScope

        if (elementToSearch is PomTargetPsiElement) {
            val pomTarget = elementToSearch.target as? CucumberKotlinLambdaStepPomTarget ?: return
            val regexp = pomTarget.name ?: return
            val callExpression = pomTarget.navigationElement as? KtCallExpression ?: return
            CucumberUtil.findGherkinReferencesToElement(callExpression, regexp, consumer, searchScope)
        }
    }
}
