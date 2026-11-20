package net.lagerwey.plugins.cucumber.kotlin.steps.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Counterpart of `CucumberJavaReferenceContributor` in the Cucumber for Java plugin.
 */
class CucumberKotlinReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(KtStringTemplateExpression::class.java),
            CucumberKotlinReferenceProvider()
        )
    }
}
