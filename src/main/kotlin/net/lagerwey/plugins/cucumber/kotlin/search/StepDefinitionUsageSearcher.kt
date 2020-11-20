package net.lagerwey.plugins.cucumber.kotlin.search

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import net.lagerwey.plugins.cucumber.kotlin.inReadAction
import org.jetbrains.plugins.cucumber.CucumberUtil

class StepDefinitionUsageSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val elementToSearch = queryParameters.elementToSearch
        if (elementToSearch !is PomTargetPsiElement) return

        val declaration = elementToSearch.target
        if (declaration !is StepDeclaration) return

        inReadAction {
            CucumberUtil.findGherkinReferencesToElement(
                declaration.element,
                declaration.stepName,
                consumer,
                queryParameters.effectiveSearchScope
            )
        }
    }
}
