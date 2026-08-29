pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
}

rootProject.name = "tisam-parent"

// Subprojects
//   :hellow — pure UI window manager library (no Minecraft-specific mod bindings)
//   :tisam  — the actual NeoForge mod that depends on :hellow
include(":hellow")
include(":tisam")
