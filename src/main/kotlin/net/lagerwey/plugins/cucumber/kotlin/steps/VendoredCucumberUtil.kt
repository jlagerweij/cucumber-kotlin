package net.lagerwey.plugins.cucumber.kotlin.steps

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.plugins.cucumber.CucumberUtil

// Vendored code due to removal of isCucumberExpression in 2019.1 versions of the gherkin plugin
// isCucumberExpression was later restored, so this can be deleted down the line
// See https://github.com/jlagerweij/cucumber-kotlin/issues/7 for details

// https://github.com/JetBrains/intellij-plugins/blob/9338364a06/cucumber/src/org/jetbrains/plugins/cucumber/CucumberUtil.java#L336
fun isCucumberExpression(stepDefinitionPattern: String): Boolean {
    if (stepDefinitionPattern.startsWith("^") && stepDefinitionPattern.endsWith("$")) {
        return false
    }
    val containsParameterTypes = booleanArrayOf(false)
    CucumberUtil.processParameterTypesInCucumberExpression(stepDefinitionPattern) { textRange ->
        if (textRange.length < 2) {
            // at least "{}" expected here
            return@processParameterTypesInCucumberExpression true
        }
        val parameterTypeCandidate = stepDefinitionPattern.substring(textRange.startOffset + 1, textRange.endOffset - 1)
        if (!isNotNegativeNumber(parameterTypeCandidate) && !parameterTypeCandidate.contains(",")) {
            containsParameterTypes[0] = true
        }
        true
    }

    return containsParameterTypes[0]
}

// From 2019.1 version of com.intellij.openapi.util.text.StringUtil (i.e. not available in 2018.3):
private fun isNotNegativeNumber(s: CharSequence?): Boolean {
    if (s == null) {
        return false
    }
    for (i in 0 until s.length) {
        if (!StringUtil.isDecimalDigit(s[i])) {
            return false
        }
    }
    return true
}
