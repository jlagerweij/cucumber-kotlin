import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
// template: https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/build.gradle.kts

plugins {
    java
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.intellij") version "1.3.1"
}
val ideaVersion: String by project
val jetbrainsPublishToken: String by project

val pluginVersion: String by project

// See: https://github.com/JetBrains/gradle-intellij-plugin/ and https://github.com/JetBrains/intellij-platform-plugin-template
intellij {
    pluginName.set("cucumber-kotlin")
    version.set(ideaVersion)
    type.set("IC")
    downloadSources.set(true)
    instrumentCode.set(true)

    // Gherkin plugin version: https://plugins.jetbrains.com/plugin/9164-gherkin/versions
    val gherkinPlugin = when (ideaVersion) {
        "2020.2" -> "gherkin:202.6397.21"
        "2020.3" -> "gherkin:203.5981.155"
        "2021.1" -> "gherkin:211.6693.111"
        "2021.2" -> "gherkin:212.4746.57"
        "2021.3" -> "gherkin:213.5744.223"
        "201.8743.12" -> "gherkin:201.8538.45"
        else -> ""
    }
    plugins.set(
        listOf(
            "com.intellij.java",
            "Kotlin",
            gherkinPlugin
        )
    )
}

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions.jvmTarget = "11"

dependencies {
    implementation("io.cucumber:cucumber-java:7.2.3")
}

tasks {
    register<Exec>("tag") {
        commandLine = listOf("git", "tag", version.toString())
    }
    publishPlugin {
        dependsOn("tag")
        token.set(jetbrainsPublishToken)
        channels.set(listOf(version.toString().split('-').getOrElse(1) { "default" }.split('.').first()))
    }
    register<Exec>("publishTag") {
        dependsOn(publishPlugin)
        commandLine = listOf("git", "push", "origin", version.toString())
    }
    patchPluginXml {
        pluginDescription.set(
            """
              <p>
                This plugin enables <a href="https://cucumber.io/">Cucumber</a> support with step definitions written in Kotlin.
              </p>
              <p>
                The following coding assistance features are available:
              </p>
              <ul>
                <li>Navigation in the source code.
              </ul>
        """
        )
        changeNotes.set(
            """
      <ul>
        <li><b>2021.3.0</b> <em>(2021-11-30)</em> - Upgrade to 2021.3</li>
        <li><b>2021.2.1</b> <em>(2021-07-28)</em> - Fix NPE in plugin</li>
        <li><b>2021.2.0</b> <em>(2021-07-28)</em> - Upgrade to 2021.2</li>
        <li><b>2021.1.2</b> <em>(2021-06-10)</em> - Fix for regression on using regular expressions</li>
        <li><b>2021.1.1</b> <em>(2021-06-10)</em> - Support optional and alternative texts</li>
        <li><b>2021.1.0</b> <em>(2020-12-14)</em> - Add Not yet implemented TODO in a newly created step</li>
        <li><b>2020.3.2</b> <em>(2020-12-14)</em> - Support multiline string literals again</li>
        <li><b>2020.3.1</b> <em>(2020-12-14)</em> - Detect keywords using all languages not just English</li>
        <li><b>2020.3.0</b> <em>(2020-12-03)</em> - Upgrade to 2020.3</li>
        <li><b>2020.2.1</b> <em>(2020-12-02)</em> - Add support for JVM types by mrozanc. Thank you!</li>
        <li><b>2020.2.0</b> <em>(2020-11-21)</em> - Improvements from ErikVermunt-TomTom based on the Cucumber for Scala plugin. Thank you!</li>
        <li><b>1.1.6</b> <em>(2020-11-03)</em> - Upgrade to 2020.3-EAP</li>
        <li><b>1.1.5</b> <em>(2020-04-16)</em> - Upgrade to 2020.2</li>
        <li><b>1.1.4</b> <em>(2020-04-16)</em> - Fix warnings from JetBrains plugin compatibility check</li>
        <li><b>1.1.3</b> <em>(2020-04-14)</em> - Compatible with Intellij IDEA 2020.1</li>
        <li><b>1.1.2</b> <em>(2020-01-17)</em> - ArrayOutOfBoundsException in Intellij IDEA 2019.3.2</li>
        <li><b>1.1.1</b> <em>(2019-05-30)</em> - Fix NoSuchMethodError on 2019.1</li>
        <li><b>1.1.0</b> <em>(2019-05-24)</em> - Create step definitions</li>
        <li><b>1.0.2</b> <em>(2018-03-14)</em> - Running features now populates the glue automatically</li>
        <li><b>1.0.1</b> <em>(2018-03-14)</em> - Support regex shorthand character classes</li>
        <li><b>1.0.0</b> <em>(2018-03-13)</em> - Initial release</li>
      </ul>
    """
        )
    }
}
