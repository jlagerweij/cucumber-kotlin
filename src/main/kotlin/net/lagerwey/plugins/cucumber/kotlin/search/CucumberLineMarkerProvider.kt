package net.lagerwey.plugins.cucumber.kotlin.search

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import icons.CucumberIcons
import net.lagerwey.plugins.cucumber.kotlin.getParameterName
import net.lagerwey.plugins.cucumber.kotlin.getStepName
import net.lagerwey.plugins.cucumber.kotlin.isHook
import net.lagerwey.plugins.cucumber.kotlin.isParameterType
import net.lagerwey.plugins.cucumber.kotlin.isStepDefinition
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class CucumberLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is LeafPsiElement) return null
        val expression = element.parent
        if (expression !is KtNameReferenceExpression) return null
        val method = expression.parent

        return when {
            isStepDefinition(method) -> createMarker(element, getStepName(method as KtCallExpression))
            isParameterType(method) -> createMarker(element, getParameterName(method as KtCallExpression))
            isHook(method) -> createMarker(element, "Cucumber Hook")
            else -> null
        }
    }

    private fun createMarker(element: PsiElement, toolTip: String?): LineMarkerInfo<*> {
        return LineMarkerInfo(
            element,
            element.textRange,
            CucumberIcons.Cucumber,
            { toolTip },
            null,
            GutterIconRenderer.Alignment.RIGHT
        )
    }
}
