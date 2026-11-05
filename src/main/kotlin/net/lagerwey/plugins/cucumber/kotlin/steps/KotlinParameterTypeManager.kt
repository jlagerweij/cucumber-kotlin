package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.ParameterTypeManager

class KotlinParameterTypeManager(private val parameterTypes: Map<String, KotlinParameterType>) : ParameterTypeManager {

    data class KotlinParameterType(val regex: String, val declaration: SmartPsiElementPointer<PsiElement>)

    override fun getParameterTypeValue(name: String): String? {
        return parameterTypes[name]?.regex ?: builtInParameterTypes[name]
    }

    override fun getParameterTypeDeclaration(name: String): PsiElement? {
        return parameterTypes[name]?.declaration?.element
    }

    companion object {
        private const val DEFAULT_INTEGER_PATTERN = "-?\\d+"
        private const val DEFAULT_DECIMAL_PATTERN = "-?\\d*[.,]?\\d+"
        private val defaultJvmParameterTypes = mapOf(
            "biginteger" to DEFAULT_INTEGER_PATTERN,
            "bigdecimal" to DEFAULT_DECIMAL_PATTERN,
            "byte" to DEFAULT_INTEGER_PATTERN,
            "short" to DEFAULT_INTEGER_PATTERN,
            "long" to DEFAULT_INTEGER_PATTERN,
            "double" to DEFAULT_DECIMAL_PATTERN
        )
        private val builtInParameterTypes = CucumberUtil.STANDARD_PARAMETER_TYPES + defaultJvmParameterTypes

        /**
         * Parameter type manager for elements outside a module, knowing only the built-in parameter types.
         */
        val DEFAULT = KotlinParameterTypeManager(emptyMap())

        /**
         * Returns the parameter types visible from the given module, cached until the PSI changes.
         */
        fun forModule(module: Module): KotlinParameterTypeManager {
            return CachedValuesManager.getManager(module.project).getCachedValue(module) {
                CachedValueProvider.Result.create(
                    KotlinParameterTypeManager(findParameterTypes(module)),
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            }
        }

        private fun kotlinFilesVisibleFrom(module: Module): GlobalSearchScope {
            val searchScope = module.getModuleWithDependenciesAndLibrariesScope(true)
                .uniteWith(ProjectScope.getLibrariesScope(module.project))
            return GlobalSearchScope.getScopeRestrictedByFileTypes(searchScope, KotlinFileType.INSTANCE)
        }

        private fun findParameterTypes(module: Module): Map<String, KotlinParameterType> {
            val parameterTypes = mutableMapOf<String, KotlinParameterType>()
            // Called from within a read action (e.g. reference resolving), so search directly instead of starting a task
            PsiSearchHelper.getInstance(module.project).processElementsWithWord(
                { element, _ ->
                    toParameterType(element)?.let { parameterTypes += it }
                    true
                },
                kotlinFilesVisibleFrom(module),
                "ParameterType",
                UsageSearchContext.IN_CODE,
                true
            )
            return parameterTypes
        }

        private fun toParameterType(occurrence: PsiElement): Pair<String, KotlinParameterType>? {
            val callExpression = occurrence.parent as? KtCallExpression ?: return null
            if (!CucumberKotlinUtil.isParameterType(callExpression)) return null
            return runCatching {
                val name = (callExpression.valueArguments[0].getArgumentExpression() as KtStringTemplateExpression).entries[0].text
                val regex = (callExpression.valueArguments[1].getArgumentExpression() as KtStringTemplateExpression).entries.joinToString("") { x -> x.text }
                val pointer = SmartPointerManager.getInstance(callExpression.project).createSmartPsiElementPointer<PsiElement>(callExpression)
                name to KotlinParameterType(StringEscapeUtils.unescapeJava(regex), pointer)
            }.getOrNull()
        }
    }
}
