// :tisam — the actual NeoForge mod
//
// moddev is applied in BOTH the root and :hellow, but the root project's
// neoForge.mods block reads `sourceSet(project(":tisam").sourceSets.main.get())`.
// The moddev plugin needs to be applied to :tisam too so this subproject's
// own compileJava task can resolve net.minecraft.* / net.neoforged.* —
// the root's plugin config doesn't propagate to the child subproject's
// compile classpath. Without re-applying it here, every TISAM source file
// fails to compile with "package net.minecraft does not exist".
//
// :hellow also applies moddev in "library mode" (no `mods { create(...) }`),
// which is fine — moddev tolerates the same plugin being applied to multiple
// projects in a multi-module build.
//
// META-INF entries (neoforge.mods.toml, accesstransformer.cfg) are still
// generated from templates/ via the generateModMetadata task.

plugins {
    id("net.neoforged.moddev").version("2.0.141")
}

neoForge {
    version = "21.1.241"

    parchment {
        mappingsVersion.set("2024.11.17")
        minecraftVersion.set("1.21.1")
    }

    // The root project ALSO declares this same mod via its own neoForge.mods
    // block (so the runs/client and runs/server run configs work). moddev
    // permits the same mod id to be registered in multiple subprojects as long
    // as the source sets are compatible — the root wins for the actual jar
    // assembly, and this one just hands :tisam the Minecraft classpath.
    mods {
        create("tisam-local") {
            sourceSet(sourceSets.main.get())
        }
    }
}

// Mod metadata (must match the root ModConfig.MOD_* values; the root
// declares the mod via neoForge.mods and reads these to fill the
// generated neoforge.mods.toml).
object ModConfig {
    const val MINECRAFT_VERSION = "1.21.1"
    const val MINECRAFT_VERSION_RANGE = "[1.21.1]"
    const val NEO_VERSION = "21.1.241"
    const val NEO_VERSION_RANGE = "[21.1.233,)"
    const val LOADER_VERSION_RANGE = "[1,)"
    const val MOD_ID = "tisam"
    const val MOD_NAME = "There Is Still A Mod"
    const val MOD_LICENSE = "Apache-2.0"
    const val MOD_VERSION = "1.0.0"
    const val MOD_DISPLAY_URL = "https://modrinth.com/mod/tisam"
    const val MOD_AUTHORS = "filowxy"
    const val MOD_DESCRIPTION = "TISAM — a compliance-friendly optional mod recommender for modpack authors. Never bundles any third-party mod files; all downloads are user-initiated through official channels."
}

version = ModConfig.MOD_VERSION
group = "com.tisam"

// META-INF/neoforge.mods.toml and accesstransformer.cfg live directly under
// src/main/resources/ as static files — no templating needed because the
// values match ModConfig below. An earlier revision generated them with
// a ProcessResources step that registered the output as a sourceDir for
// the main source set, which tripped Gradle's strict implicit-dependency
// check (moddev's createMinecraftArtifacts reads source-set inputs but
// didn't declare a dep on our generator). Skipping the indirection removes
// the whole class of build-script brittleness; the downside is that
// changing ModConfig requires editing the .toml too, but they're in the
// same file and grep will surface it.

dependencies {
    implementation(project(":hellow"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    // Gson is a runtime dep of Minecraft; the unit-test classpath doesn't inherit
    // it, and ModuleEntry's config tests exercise the lenient deserializer directly.
    testImplementation("com.google.code.gson:gson:2.11.0")
    // Gradle's jvm-test-suite plugin requires the launcher on the runtime
    // classpath, not just the test classpath. Without this you get a
    // "Failed to load JUnit Platform" error at test-execution time.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    // SLF4J is provided by the Minecraft launcher at runtime but is
    // missing from the unit-test runtime classpath. The constants in
    // TisamConstants reference LoggerFactory, which fails
    // NoClassDefFoundError at test time without this.
    testRuntimeOnly("org.slf4j:slf4j-api:2.0.18")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
