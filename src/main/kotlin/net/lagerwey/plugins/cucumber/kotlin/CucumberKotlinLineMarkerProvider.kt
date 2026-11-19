package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import icons.CucumberIcons
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Shows a Cucumber icon in the gutter next to step definitions, parameter types and hooks.
 */
class CucumberKotlinLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is LeafPsiElement) return null
        val expression = element.parent
        if (expression !is KtNameReferenceExpression) return null
        val method = expression.parent

        return when {
            CucumberKotlinUtil.isLambdaStepDefinition(method) -> createMarker(element, CucumberKotlinUtil.getLambdaStepName(method as KtCallExpression))
            CucumberKotlinUtil.isLambdaParameterType(method) -> createMarker(element, CucumberKotlinUtil.getLambdaParameterTypeName(method as KtCallExpression))
            CucumberKotlinUtil.isLambdaHook(method) -> createMarker(element, "Cucumber Hook")
            else -> null
        }
    }

    private fun createMarker(element: PsiElement, toolTip: String?): LineMarkerInfo<*> {
        val anchor = PsiTreeUtil.getDeepestFirst(element)
        return LineMarkerInfo(
            anchor,
            anchor.textRange,
            CucumberIcons.Cucumber,
            { toolTip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { "" }
        )
    }
}
