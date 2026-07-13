// template: https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/build.gradle.kts

import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/version_catalogs.html
dependencies {
    implementation("io.cucumber:cucumber-java:7.34.3")
    implementation("org.apache.commons:commons-text:1.15.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-dgradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("262.8665.176")
        bundledPlugins(listOf("com.intellij.java", "org.jetbrains.kotlin"))

        // IntelliJ IDEA next version: https://www.jetbrains.com/idea/nextversion/
        // Gherkin plugin version: https://plugins.jetbrains.com/plugin/9164-gherkin/versions
        // Cucumber for Java version: https://plugins.jetbrains.com/plugin/7212-cucumber-for-java/versions

        plugins(listOf("gherkin:262.8665.173", "cucumber-java:262.8665.176"))
        testFramework(TestFrameworkType.Platform)
    }
}

