// :hellow — condition-driven window manager for NeoForge mods.
// v2: now produces a proper NeoForge mod jar with neoforge.mods.toml
// so that TISAM (and other mods) can depend on it at runtime.

plugins {
    id("net.neoforged.moddev").version("2.0.141")
}

neoForge {
    version = "21.1.241"

    parchment {
        mappingsVersion.set("2024.11.17")
        minecraftVersion.set("1.21.1")
    }

    // Hellow IS a NeoForge mod — just a library mod with no @Mod annotation.
    // The neoforge.mods.toml makes it a valid mod jar that NeoForge's
    // ModuleClassLoader can discover, so TISAM can reference com.hellow.*
    // classes at runtime.
    mods {
        create("hellow") {
            sourceSet(sourceSets.main.get())
        }
    }
}

version = "1.0.0"
group = "com.hellow"

dependencies {
    // HellowConstants uses SLF4J; at runtime the Minecraft launcher provides it.
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Prism (color library) — REQUIRED runtime dependency of Hellow, declared in
    // neoforge.mods.toml. Vendored as a jar because the Modrinth Maven's
    // maven.modrinth:prism-lib coordinates resolve to the Fabric/intermediary
    // file of the version, not the NeoForge one. Source of this jar:
    //   https://modrinth.com/mod/prism-lib (version 1.0.11, NeoForge file)
    // License: CC BY-NC-ND 4.0 — Hellow depends on it at runtime but NEVER
    // bundles it into its own jar; players install Prism from Modrinth.
    // To update: download Prism-1.21.1-neoforge-<v>.jar from Modrinth into libs/
    // and bump the file name below.
    api(files("libs/Prism-1.21.1-neoforge-1.0.11.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
