package net.lagerwey.plugins.cucumber.kotlin.search

import com.intellij.psi.PsiElement
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProvider
import org.jetbrains.plugins.cucumber.psi.impl.GherkinStepImpl

class StepUsageTypeProvider : UsageTypeProvider {
    override fun getUsageType(element: PsiElement?): UsageType? {
        return if (element is GherkinStepImpl) gherkinStepUsageType else null
    }

    companion object {
        private val gherkinStepUsageType = UsageType { "Gherkin step" }
    }
}
