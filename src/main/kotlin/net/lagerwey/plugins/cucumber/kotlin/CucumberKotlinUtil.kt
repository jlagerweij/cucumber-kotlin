package net.lagerwey.plugins.cucumber.kotlin

import com.intellij.psi.PsiElement
import com.intellij.util.containers.orNull
import io.cucumber.gherkin.GherkinDialects
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.base.psi.kotlinFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*

object CucumberKotlinUtil {
    const val CUCUMBER_JAVA8_PACKAGE = "io.cucumber.java8"
    private val hookKeywords = listOf("Before", "BeforeStep", "After", "AfterStep")
    private val allKeywords = getAllKeywords()

    fun getStepArguments(stepDefinition: KtCallExpression): List<KtParameter> {
        val block = stepDefinition.valueArguments.first { it is KtBlockExpression }
        val function = block.children.first { it is KtNamedFunction } as KtNamedFunction

        return function.valueParameters
    }

    fun isStepDefinition(candidate: PsiElement): Boolean {
        return when (candidate) {
            is KtCallExpression -> isStepDefinition(candidate)
            else -> false
        }
    }

    fun isStepDefinition(candidate: KtCallExpression): Boolean {
        return candidate.children.firstOrNull { it is KtNameReferenceExpression }?.text?.let {
            isKeywordValid(it) && isCucumberMethod(candidate)
        } ?: false
    }

    fun isParameterType(candidate: PsiElement): Boolean {
        return when (candidate) {
            is KtCallExpression -> isParameterType(candidate)
            else -> false
        }
    }

    private fun isParameterType(candidate: KtCallExpression): Boolean {
        return candidate.children.firstOrNull { it is KtNameReferenceExpression }?.text?.let {
            it == "ParameterType" && isCucumberMethod(candidate)
        } ?: false
    }

    fun isHook(candidate: PsiElement): Boolean {
        return if (candidate is KtCallExpression) {
            candidate.children.firstOrNull { it is KtNameReferenceExpression }?.let {
                it.text in hookKeywords && isCucumberMethod(candidate)
            } ?: false
        } else false
    }

    private fun isCucumberMethod(method: KtCallExpression): Boolean {
        return method.children
            .filterIsInstance<KtReferenceExpression>()
            .firstOrNull()
            ?.mainReference
            ?.resolve()
            ?.namedUnwrappedElement
            ?.kotlinFqName
            ?.asString()
            ?.startsWith(CUCUMBER_JAVA8_PACKAGE)
            ?: false
    }

    fun getStepName(stepDefinition: KtCallExpression): String? {
        val keywordExpression = stepDefinition.children.firstOrNull { it is KtNameReferenceExpression }
        val keyword = keywordExpression?.text?.trim()

        if (keyword != null && isKeywordValid(keyword)) {
            return getStepRegex(stepDefinition)
        }
        return null
    }

    fun getParameterName(parameterDefinition: KtCallExpression): String? {
        return parameterDefinition.valueArguments[0].text
    }

    private fun getStepRegex(stepDefinition: KtCallExpression): String? {
        return runCatching {
            val argumentExpression = stepDefinition.valueArguments[0].getArgumentExpression()
            if (argumentExpression is KtStringTemplateExpression) {
                val text = argumentExpression.entries[0].text
                text
            } else {
                null
            }
        }.getOrNull()
    }

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
