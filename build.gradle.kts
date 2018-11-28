import org.gradle.internal.impldep.org.apache.maven.wagon.PathUtils.password
import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    extra["kotlinVersion"] = "1.3.10"

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${extra["kotlinVersion"]}")
    }
}
plugins {
    base
    kotlin("jvm") version "1.3.10"
    id ("org.jetbrains.intellij") version "0.3.12"
}
val kotlinVersion = extra["kotlinVersion"] as String
val jetbrainsPublishUsername by project
val jetbrainsPublishPassword by project

apply {
    plugin("org.jetbrains.intellij")
}

intellij {
    pluginName = "cucumber-kotlin"
    version = "2018.3"
    downloadSources = true
    updateSinceUntilBuild = false //Disables updating since-build attribute in plugin.xml

    setPlugins(
        "gherkin:183.4284.148",
        "org.jetbrains.kotlin:$kotlinVersion-release-IJ2018.3-1"
    )
}

inline operator fun <T : Task> T.invoke(a: T.() -> Unit): T = apply(a)
val publishPlugin: PublishTask by tasks
publishPlugin {
    setUsername(jetbrainsPublishUsername)
    password (jetbrainsPublishPassword)
}

repositories {
    mavenCentral()
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib")
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}