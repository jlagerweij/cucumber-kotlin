import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    extra["kotlinVersion"] = "1.3.31"

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra.properties["kotlinVersion"]}")
    }
}
plugins {
    base
    kotlin("jvm") version "1.3.31"
    id("org.jetbrains.intellij") version "0.4.8"
}
val kotlinVersion = extra.properties["kotlinVersion"] as String
val ideaVersion = extra.properties["ideaVersion"] as? String ?: "2019.1.3"

apply {
    plugin("org.jetbrains.intellij")
}

intellij {
    pluginName = "cucumber-kotlin"
    version = ideaVersion

    downloadSources = true
    updateSinceUntilBuild = false //Disables updating since-build attribute in plugin.xml

    when (ideaVersion) {
        "2018.3" ->
            setPlugins(
                    "gherkin:183.4284.148",
                    "org.jetbrains.kotlin:$kotlinVersion-release-IJ2018.3-1"
            )
        "2019.1.3" ->
            setPlugins(
                    "gherkin:191.6707.7",
                    "org.jetbrains.kotlin:$kotlinVersion-release-IJ2019.1-1"
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
    compile("org.jetbrains.kotlin:kotlin-stdlib")
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions.jvmTarget = "1.8"