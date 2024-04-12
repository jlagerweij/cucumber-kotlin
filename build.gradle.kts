// template: https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/build.gradle.kts

fun properties(key: String) = providers.gradleProperty(key)

plugins {
    java
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleIntelliJPlugin)
}

val jetbrainsPublishToken: String by project

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
}

dependencies {
    implementation(libs.cucumberJava)
}

kotlin {
    jvmToolchain(17)
}

// See: https://github.com/JetBrains/gradle-intellij-plugin/ and https://github.com/JetBrains/intellij-platform-plugin-template
intellij {
    pluginName = properties("pluginName")
    version = properties("platformVersion")
    type = properties("platformType")
    downloadSources = true

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
}

tasks {
    register<Exec>("tag") {
        commandLine = listOf("git", "tag", version.toString(), "-m", "Release version $version")
    }
    publishPlugin {
        dependsOn("tag")
        token.set(jetbrainsPublishToken)
        channels = properties("pluginVersion").map {
            listOf(
                it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" })
        }
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
       <li><b>2024.1.0</b> <em>(2024-04-13)</em> - Compatible with 2024.1</li>
       <li><b>2023.3.0</b> <em>(2023-12-07)</em> - Compatible with 2023.3</li>
       <li><b>2023.2.0</b> <em>(2023-08-01)</em> - Compatible with 2023.2</li>
       <li><b>2023.1.0</b> <em>(2023-03-29)</em> - Compatible with 2023.1</li>
       <li><b>2022.3.0</b> <em>(2022-08-01)</em> - Compatible with 2022.3</li>
       <li><b>2022.2.0</b> <em>(2022-08-01)</em> - Fix for issue #43, Upgrade to 2022.2</li>
        <li><b>2022.1.2</b> <em>(2022-06-22)</em> - Fix for issue #41, Add support for complex regex</li>
        <li><b>2022.1.1</b> <em>(2022-05-19)</em> - Fix for issue #30, Must be executed under progress indicator</li>
        <li><b>2022.1.0</b> <em>(2022-04-12)</em> - Upgrade to 2022.1</li>
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
