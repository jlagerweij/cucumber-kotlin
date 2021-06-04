package net.lagerwey.plugins.cucumber.kotlin.search

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Function
import icons.CucumberIcons
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class CucumberLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is LeafPsiElement) return null
        val expression = element.parent
        if (expression !is KtNameReferenceExpression) return null
        val method = expression.parent

        return when {
            CucumberKotlinUtil.isStepDefinition(method) -> createMarker(element, CucumberKotlinUtil.getStepName(method as KtCallExpression))
            CucumberKotlinUtil.isParameterType(method) -> createMarker(element, CucumberKotlinUtil.getParameterName(method as KtCallExpression))
            CucumberKotlinUtil.isHook(method) -> createMarker(element, "Cucumber Hook")
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
