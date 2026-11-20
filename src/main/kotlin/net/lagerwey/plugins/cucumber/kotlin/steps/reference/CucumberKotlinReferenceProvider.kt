package net.lagerwey.plugins.cucumber.kotlin.steps.reference

import com.intellij.openapi.util.TextRange
import com.intellij.pom.PomTargetPsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.plugins.cucumber.CucumberUtil

/**
 * Counterpart of `CucumberJavaReferenceProvider` in the Cucumber for Java plugin: provides
 * [CucumberKotlinParameterTypeReference]s for the parameter types used in the step text of a lambda step definition.
 */
class CucumberKotlinReferenceProvider : PsiReferenceProvider() {
    override fun acceptsTarget(target: PsiElement): Boolean =
        target is PomTargetPsiElement && target.target is CucumberKotlinParameterPomTarget

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (element !is KtStringTemplateExpression) return PsiReference.EMPTY_ARRAY

        val (value, toOffsetInElement) = CucumberKotlinUtil.getStringValue(element) ?: return PsiReference.EMPTY_ARRAY
        // Custom parameter types can only be used in Cucumber expressions
        if (!value.contains('{') || !CucumberUtil.isCucumberExpression(value)) return PsiReference.EMPTY_ARRAY
        if (CucumberKotlinUtil.getLambdaStepDefinitionOfStepText(element) == null) return PsiReference.EMPTY_ARRAY

        val references = mutableListOf<PsiReference>()
        CucumberUtil.processParameterTypesInCucumberExpression(value) { range ->
            val start = toOffsetInElement(range.startOffset)
            val end = toOffsetInElement(range.endOffset)
            if (start >= 0 && end > start) {
                references.add(CucumberKotlinParameterTypeReference(element, TextRange(start, end)))
            }
            true
        }
        return references.toTypedArray()
    }
}
