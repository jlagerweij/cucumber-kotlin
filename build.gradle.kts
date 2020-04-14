import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.3.71"
    id("org.jetbrains.intellij") version "0.4.18"
}
val ideaVersion = extra.properties["ideaVersion"] as? String ?: "2020.1"
val jetbrainsPublishToken: String by project

apply {
    plugin("org.jetbrains.intellij")
}

// See: https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    pluginName = "cucumber-kotlin"
    version = ideaVersion

    downloadSources = true
    instrumentCode = true

    when (ideaVersion) {
        // Gherkin plugin version: https://plugins.jetbrains.com/plugin/9164-gherkin/versions
        "2020.1" ->
            setPlugins(
                    "java",
                    "gherkin:201.6668.60",
                    "Kotlin"
            )
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions.jvmTarget = "1.8"

tasks {
    named<PublishTask>("publishPlugin") {
        token(jetbrainsPublishToken)
    }
    named<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
        pluginDescription("""
              <p>
                This plugin enables <a href="http://cukes.info/">Cucumber</a> support with step definitions written in Kotlin.
              </p>
              <p>
                The following coding assistance features are available:
              </p>
              <ul>
                <li>Navigation in the source code.
              </ul>
        """)
        changeNotes("""
      <ul>
        <li><b>1.1.2</b> <em>(2020-01-17)</em> - Compatible with Intellij IDEA 2020.1</li>
        <li><b>1.1.2</b> <em>(2020-01-17)</em> - ArrayOutOfBoundsException in Intellij IDEA 2019.3.2</li>
        <li><b>1.1.1</b> <em>(2019-05-30)</em> - Fix NoSuchMethodError on 2019.1</li>
        <li><b>1.1.0</b> <em>(2019-05-24)</em> - Create step definitions</li>
        <li><b>1.0.2</b> <em>(2018-03-14)</em> - Running features now populates the glue automatically</li>
        <li><b>1.0.1</b> <em>(2018-03-14)</em> - Support regex shorthand character classes</li>
        <li><b>1.0.0</b> <em>(2018-03-13)</em> - Initial release</li>
      </ul>
    """)
    }
}