package net.lagerwey.plugins.cucumber.kotlin.steps.reference

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.pom.PomTargetPsiElement
import com.intellij.pom.references.PomService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Counterpart of `CucumberJavaParameterTypeReference` in the Cucumber for Java plugin: a reference from a parameter
 * type used in a Cucumber expression to its declaration, e.g. from `{color}` in
 *
 * ```
 * Given("I have a {color} cucumber") { color: Color -> }
 * ```
 *
 * to the name in
 *
 * ```
 * ParameterType("color", "red|green") { name: String -> Color(name) }
 * ```
 *
 * Built-in parameter types such as `{int}` have no declaration in the project, so the reference is soft.
 *
 * @param range the range of the parameter type in [element], including the braces
 */
class CucumberKotlinParameterTypeReference(element: KtStringTemplateExpression, range: TextRange) :
    PsiReferenceBase<KtStringTemplateExpression>(element, TextRange(range.startOffset + 1, range.endOffset - 1), true) {

    override fun resolve(): PsiElement? {
        val name = parameterTypeName
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
        val call = CucumberKotlinUtil.getAllParameterTypes(module).getParameterTypeDeclaration(name) as? KtCallExpression
        val nameExpression = call?.valueArguments?.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression
            ?: return null
        return PomService.convertToPsi(CucumberKotlinParameterPomTarget(nameExpression, name))
    }

    override fun getCanonicalText(): String = parameterTypeName

    override fun isReferenceTo(element: PsiElement): Boolean {
        val target = (element as? PomTargetPsiElement)?.target as? CucumberKotlinParameterPomTarget ?: return false
        return target.name == parameterTypeName && element.manager.areElementsEquivalent(element, resolve())
    }

    private val parameterTypeName: String
        get() = rangeInElement.substring(element.text)
}
