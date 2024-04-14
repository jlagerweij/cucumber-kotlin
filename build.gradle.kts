// template: https://github.com/JetBrains/intellij-platform-plugin-template/blob/main/build.gradle.kts
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)

plugins {
    java
    alias(libs.plugins.kotlin)
    alias(libs.plugins.gradleIntelliJPlugin)
    alias(libs.plugins.changelog)
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

changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
    versionPrefix = ""
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
        version = properties("pluginVersion")
        sinceBuild = properties("pluginSinceBuild")
        untilBuild = properties("pluginUntilBuild")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }
}
