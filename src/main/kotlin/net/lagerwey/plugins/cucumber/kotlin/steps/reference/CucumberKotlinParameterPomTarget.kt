package net.lagerwey.plugins.cucumber.kotlin.steps.reference

import com.intellij.openapi.util.TextRange
import com.intellij.pom.PomRenameableTarget
import com.intellij.psi.DelegatePsiTarget
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.pom.PsiDeclaredTarget
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Counterpart of `CucumberJavaParameterPomTarget` in the Cucumber for Java plugin: the declaration of a custom
 * parameter type, i.e. the name argument of `ParameterType("color", "red|green") { ... }`.
 *
 * @see net.lagerwey.plugins.cucumber.kotlin.steps.search.CucumberKotlinPomDeclarationSearcher
 */
class CucumberKotlinParameterPomTarget(element: KtStringTemplateExpression, private val name: String) :
    DelegatePsiTarget(element), PomRenameableTarget<PsiElement>, PsiDeclaredTarget {

    override fun getName(): String = name

    override fun getNameIdentifierRange(): TextRange = ElementManipulators.getValueTextRange(navigationElement)

    override fun isWritable() = true

    override fun setName(newName: String): PsiElement? {
        val element = navigationElement as? KtStringTemplateExpression ?: return null
        return element.replace(KtPsiFactory(element.project).createStringTemplate(newName))
    }

    override fun equals(other: Any?) = other is CucumberKotlinParameterPomTarget && super.equals(other) && name == other.name

    override fun hashCode() = 31 * super.hashCode() + name.hashCode()
}
