package net.lagerwey.plugins.cucumber.kotlin.steps.reference

import com.intellij.pom.PomNamedTarget
import com.intellij.psi.DelegatePsiTarget
import net.lagerwey.plugins.cucumber.kotlin.CucumberKotlinUtil
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Counterpart of `CucumberJavaLambdaStepPomTarget` in the Cucumber for Java plugin: the declaration of a step in
 * cucumber-java8 style, named after its step text.
 */
class CucumberKotlinLambdaStepPomTarget(element: KtCallExpression) : DelegatePsiTarget(element), PomNamedTarget {
    override fun getName(): String? {
        val callExpression = navigationElement as? KtCallExpression ?: return null
        return CucumberKotlinUtil.getLambdaStepName(callExpression)
    }
}
