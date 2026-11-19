package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.orNull
import io.cucumber.gherkin.GherkinDialects
import net.lagerwey.plugins.cucumber.kotlin.steps.KotlinParameterTypeManager
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Counterpart of `CucumberJavaUtil` in the Cucumber for Java plugin.
 *
 * Functions named `Lambda` handle the cucumber-java8 style (`Given("...") { }`), like the `Java8` functions there.
 */
object CucumberKotlinUtil {
    const val CUCUMBER_JAVA8_PACKAGE = "io.cucumber.java8"
    private val hookKeywords = listOf("Before", "BeforeStep", "After", "AfterStep")
    private val allKeywords = getAllKeywords()

    /**
     * Returns true if [candidate] is a call to one of the Cucumber methods that define steps in lambda style.
     */
    fun isLambdaStepDefinition(candidate: PsiElement): Boolean {
        if (candidate !is KtCallExpression) return false
        return candidate.children.firstOrNull { it is KtNameReferenceExpression }?.text?.let {
            isKeywordValid(it) && isCucumberLambdaMethod(candidate)
        } ?: false
    }

    fun isLambdaParameterType(candidate: PsiElement): Boolean {
        if (candidate !is KtCallExpression) return false
        return candidate.children.firstOrNull { it is KtNameReferenceExpression }?.text?.let {
            it == "ParameterType" && isCucumberLambdaMethod(candidate)
        } ?: false
    }

    fun isLambdaHook(candidate: PsiElement): Boolean {
        if (candidate !is KtCallExpression) return false
        return candidate.children.firstOrNull { it is KtNameReferenceExpression }?.let {
            it.text in hookKeywords && isCucumberLambdaMethod(candidate)
        } ?: false
    }

    private fun isCucumberLambdaMethod(method: KtCallExpression): Boolean {
        return method.children
            .filterIsInstance<KtReferenceExpression>()
            .firstOrNull()
            ?.mainReference
            ?.resolve()
            ?.let { (it as? KtLightElement<*, *>)?.kotlinOrigin ?: it }
            ?.let { PsiTreeUtil.getNonStrictParentOfType(it, PsiNamedElement::class.java) }
            ?.kotlinFqName
            ?.asString()
            ?.startsWith(CUCUMBER_JAVA8_PACKAGE)
            ?: false
    }

    /**
     * Returns the expression holding the step text of a lambda step definition, i.e. its first argument.
     */
    fun getLambdaStepNameExpression(stepDefinition: KtCallExpression): KtExpression? =
        stepDefinition.valueArguments.getOrNull(0)?.getArgumentExpression()

    /**
     * Returns the step text (a Cucumber expression or a regex) of a lambda step definition.
     */
    fun getLambdaStepName(stepDefinition: KtCallExpression): String? {
        val keyword = stepDefinition.children.firstOrNull { it is KtNameReferenceExpression }?.text?.trim()
        if (keyword == null || !isKeywordValid(keyword)) return null

        val stepNameExpression = getLambdaStepNameExpression(stepDefinition) as? KtStringTemplateExpression ?: return null
        return stepNameExpression.entries.getOrNull(0)?.text
    }

    fun getLambdaParameterTypeName(parameterDefinition: KtCallExpression): String? {
        return parameterDefinition.valueArguments.getOrNull(0)?.text
    }

    /**
     * Returns the parameter types available to step definitions in [module].
     */
    fun getAllParameterTypes(module: Module): KotlinParameterTypeManager = KotlinParameterTypeManager.forModule(module)

    private fun isKeywordValid(keyword: String) = allKeywords.contains(keyword)

    private fun getAllKeywords(): List<String> {
        val languages = GherkinDialects.getLanguages()
        val dialects = languages.map { GherkinDialects.getDialect(it) }

        return dialects.flatMap { optionalDialect ->
            optionalDialect.orNull()?.let {
                it.stepKeywords.map { keyword ->
                    keyword.trim()
                }
            } ?: emptyList()
        }
    }
}
