package net.lagerwey.plugins.cucumber.kotlin.search

import com.intellij.ide.util.EditSourceUtil
import com.intellij.pom.PomNamedTarget
import com.intellij.psi.PsiElement

data class StepDeclaration(val element: PsiElement, val stepName: String) : PomNamedTarget {
    override fun canNavigate() = EditSourceUtil.canNavigate(element)

    override fun canNavigateToSource() = canNavigate()

    override fun getName() = stepName

    override fun isValid() = element.isValid

    override fun navigate(requestFocus: Boolean) {
        EditSourceUtil.getDescriptor(element)?.navigate(requestFocus)
    }
}
