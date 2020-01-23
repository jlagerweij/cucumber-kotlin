import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.3.61"
    id("org.jetbrains.intellij") version "0.4.15"
}
val ideaVersion = extra.properties["ideaVersion"] as? String ?: "2019.3.2"

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
        "2018.3" ->
            setPlugins(
                    "gherkin:183.4284.148",
                    "org.jetbrains.kotlin:1.3.31-release-IJ2018.3-1"
            )
        "2019.1.3" ->
            setPlugins(
                    "gherkin:191.6707.7",
                    "org.jetbrains.kotlin:1.3.31-release-IJ2019.1-1"
            )
        "2019.3.2" ->
            setPlugins(
                    "java",
                    "gherkin:193.6015.9",
                    "org.jetbrains.kotlin:1.3.61-release-IJ2019.3-1"
            )
    }
}

inline operator fun <T : Task> T.invoke(a: T.() -> Unit): T = apply(a)
val publishPlugin: PublishTask by tasks
publishPlugin {
    setUsername(extra.properties["jetbrainsPublishUsername"])
    password(extra.properties["jetbrainsPublishPassword"])
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