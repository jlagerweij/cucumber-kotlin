package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import net.lagerwey.plugins.cucumber.kotlin.PARAMETER_TYPE_INDEX_ID
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.ParameterTypeManager
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class KotlinParameterTypeManager(
    private val project: Project?,
    private val parameterTypes: Map<String, KotlinParameterType>,
) : ParameterTypeManager {

    data class KotlinParameterType(val regex: String, val file: VirtualFile)

    override fun getParameterTypeValue(name: String): String? {
        return parameterTypes[name]?.regex ?: builtInParameterTypes[name]
    }

    override fun getParameterTypeDeclaration(name: String): PsiElement? {
        val file = parameterTypes[name]?.file ?: return null
        val psiFile = project?.let { PsiManager.getInstance(it).findFile(file) } ?: return null
        return PsiTreeUtil.findChildrenOfType(psiFile, KtCallExpression::class.java).firstOrNull {
            CucumberKotlinUtil.isParameterType(it) && parameterTypeName(it) == name
        }
    }

    private fun parameterTypeName(callExpression: KtCallExpression): String? {
        val argument = callExpression.valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression
        return argument?.entries?.joinToString("") { it.text }
    }

    /**
     * The parameter types of a module, together with the stamps they were computed for.
     */
    private class CachedParameterTypes(
        val psiModificationCount: Long,
        val indexModificationStamp: Long,
        val manager: KotlinParameterTypeManager,
    )

    companion object {
        private const val LOCK_POLL_MILLIS = 10L
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
        val DEFAULT = KotlinParameterTypeManager(null, emptyMap())

        // Stored on the module itself, so the data lives exactly as long as the module
        private val CACHE_KEY = Key.create<CachedParameterTypes>("cucumber.kotlin.parameterTypes")
        private val LOCK_KEY = Key.create<ReentrantLock>("cucumber.kotlin.parameterTypes.lock")

        /**
         * Number of times the parameter types of a module were collected from the index, to verify the caching in tests.
         */
        @VisibleForTesting
        val computationCount = AtomicInteger()

        /**
         * Returns the parameter types visible from the given module.
         *
         * Step definitions are resolved concurrently, so the parameter types are computed by only one thread per module
         * while the others wait for its result. The result is reused until the parameter types in the index change,
         * so editing code elsewhere does not trigger a new computation.
         */
        fun forModule(module: Module): KotlinParameterTypeManager {
            val psiModificationCount = PsiModificationTracker.getInstance(module.project).modificationCount
            module.getUserData(CACHE_KEY)?.takeIf { it.psiModificationCount == psiModificationCount }?.let { return it.manager }

            val lock = module.getUserData(LOCK_KEY) ?: synchronized(LOCK_KEY) {
                module.getUserData(LOCK_KEY) ?: ReentrantLock().also { module.putUserData(LOCK_KEY, it) }
            }
            // Wait for a thread already computing the parameter types, while staying responsive to cancellation
            while (!lock.tryLock(LOCK_POLL_MILLIS, TimeUnit.MILLISECONDS)) {
                ProgressManager.checkCanceled()
            }
            try {
                return module.getUserData(CACHE_KEY)?.takeIf { it.psiModificationCount == psiModificationCount }?.manager
                    ?: computeIfIndexChanged(module, psiModificationCount)
            } finally {
                lock.unlock()
            }
        }

        private fun computeIfIndexChanged(module: Module, psiModificationCount: Long): KotlinParameterTypeManager {
            val fileBasedIndex = FileBasedIndex.getInstance()
            val scope = kotlinFilesVisibleFrom(module)
            // Brings the index up to date for the scope; ensureUpToDate itself is internal API
            fileBasedIndex.processAllKeys(PARAMETER_TYPE_INDEX_ID, { false }, scope, null)
            val indexModificationStamp = fileBasedIndex.getIndexModificationStamp(PARAMETER_TYPE_INDEX_ID, module.project)

            val previous = module.getUserData(CACHE_KEY)
            val manager = if (previous != null && previous.indexModificationStamp == indexModificationStamp) {
                previous.manager
            } else {
                KotlinParameterTypeManager(module.project, findParameterTypes(module, scope))
            }
            module.putUserData(CACHE_KEY, CachedParameterTypes(psiModificationCount, indexModificationStamp, manager))
            return manager
        }

        private fun kotlinFilesVisibleFrom(module: Module): GlobalSearchScope {
            val searchScope = module.getModuleWithDependenciesAndLibrariesScope(true)
                .uniteWith(ProjectScope.getLibrariesScope(module.project))
            return GlobalSearchScope.getScopeRestrictedByFileTypes(searchScope, KotlinFileType.INSTANCE)
        }

        private fun findParameterTypes(module: Module, scope: GlobalSearchScope): Map<String, KotlinParameterType> {
            computationCount.incrementAndGet()
            val fileBasedIndex = FileBasedIndex.getInstance()
            val parameterTypes = mutableMapOf<String, KotlinParameterType>()
            fileBasedIndex.getAllKeys(PARAMETER_TYPE_INDEX_ID, module.project).forEach { name ->
                fileBasedIndex.processValues(PARAMETER_TYPE_INDEX_ID, name, null, { file, regex ->
                    parameterTypes.putIfAbsent(name, KotlinParameterType(regex, file))
                    false
                }, scope)
            }
            return parameterTypes
        }
    }
}
