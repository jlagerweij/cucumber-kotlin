// template: https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/build.gradle.kts

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/version_catalogs.html
dependencies {
    implementation(libs.cucumberJava)
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("262.4852.50")
        bundledPlugins(listOf("com.intellij.java", "org.jetbrains.kotlin"))
        plugins(listOf("gherkin:262.4852.34", "cucumber-java:262.4852.50"))
        testFramework(TestFrameworkType.Platform)
    }
}

