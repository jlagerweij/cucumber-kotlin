package net.lagerwey.plugins.cucumber.kotlin.steps.reference

import com.intellij.pom.PomDescriptionProvider
import com.intellij.pom.PomTarget
import com.intellij.psi.ElementDescriptionLocation
import com.intellij.usageView.UsageViewNodeTextLocation
import com.intellij.usageView.UsageViewTypeLocation

/**
 * Counterpart of `CucumberJavaParameterPomTargetDescriptionProvider` in the Cucumber for Java plugin: describes a
 * parameter type in "Find Usages" and "Rename".
 */
class CucumberKotlinParameterPomTargetDescriptionProvider : PomDescriptionProvider() {
    override fun getElementDescription(element: PomTarget, location: ElementDescriptionLocation): String? {
        if (element !is CucumberKotlinParameterPomTarget) return null
        return when (location) {
            UsageViewTypeLocation.INSTANCE -> "Cucumber parameter type"
            UsageViewNodeTextLocation.INSTANCE -> element.name
            else -> null
        }
    }
}
