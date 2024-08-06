package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.plugins.cucumber.CucumberUtil
import org.jetbrains.plugins.cucumber.ParameterTypeManager

object KotlinParameterTypeManager : ParameterTypeManager {

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
    private val nameToParameterTypeMap = mutableMapOf<String, String>()
    private val nameToDeclarationMap = mutableMapOf<String, SmartPsiElementPointer<PsiElement>>()

    init {
        nameToParameterTypeMap.putAll(CucumberUtil.STANDARD_PARAMETER_TYPES)
        nameToParameterTypeMap.putAll(defaultJvmParameterTypes)
    }

    fun addParameterType(name: String, parameterType: String, declaration: SmartPsiElementPointer<PsiElement>) {
        synchronized(this) {
            nameToParameterTypeMap[name] = parameterType
            nameToDeclarationMap[name] = declaration
        }
    }

    override fun getParameterTypeValue(name: String): String? {
        synchronized(this) {
            return nameToParameterTypeMap[name]
        }
    }

    override fun getParameterTypeDeclaration(name: String): PsiElement? {
        synchronized(this) {
            return nameToDeclarationMap[name]?.element
        }
    }
}
