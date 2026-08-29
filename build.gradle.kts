// TISAM-parent
//
// Multi-module layout:
//   :hellow — pure UI window manager library (com.hellow.*)
//   :tisam  — the NeoForge mod (com.tisam.*) that depends on :hellow
//
// The moddev plugin is applied at the root so both subprojects get the
// Minecraft + NeoForge + Parchment classpath. The actual mod (mods.toml,
// META-INF, client/server runs) is declared here and points at the
// :tisam subproject's source set. :hellow produces a plain jar and
// becomes a runtime dependency of :tisam.
//
// Configuration order matters: the java-library plugin is applied to
// every subproject in the allprojects block at the TOP, before the
// neoForge.mods block, because that block reads
// project(":tisam").sourceSets.main and sourceSets only exists once
// java-library has been applied to the subproject.

plugins {
    id("net.neoforged.moddev").version("2.0.141")
}

// Apply java-library to all projects (root + subprojects) up front so
// the sourceSets extension is available before anything else references
// it. The root project becomes a no-op java-library (we never compile
// code in it; it just owns the mod metadata) but applying the plugin
// here is harmless and keeps the configuration symmetric.
allprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.ADOPTIUM)
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

object ModConfig {
    // Minecraft / NeoForge version pinning
    const val MINECRAFT_VERSION = "1.21.1"
    const val MINECRAFT_VERSION_RANGE = "[1.21.1]"
    const val NEO_VERSION = "21.1.241"
    const val NEO_VERSION_RANGE = "[21.1.233,)"
    const val PARCHMENT_MAPPING_VERSION = "2024.11.17"
    const val LOADER_VERSION_RANGE = "[1,)"

    // TISAM mod metadata
    const val MOD_ID = "tisam"
    const val MOD_NAME = "There Is Still A Mod"
    const val MOD_LICENSE = "Apache-2.0"
    const val MOD_VERSION = "1.0.0"
    const val MOD_GROUP_ID = "com.tisam"
    const val MOD_DISPLAY_URL = "https://modrinth.com/mod/tisam"
    const val MOD_AUTHORS = "filowxy"
    const val MOD_DESCRIPTION = "TISAM — a compliance-friendly optional mod recommender for modpack authors. Never bundles any third-party mod files; all downloads are user-initiated through official channels."
}

tasks.named<Wrapper>("wrapper").configure {
    distributionType = Wrapper.DistributionType.BIN
}

// Root project carries no version/group; each subproject sets its own.
version = ModConfig.MOD_VERSION
group = ModConfig.MOD_GROUP_ID

base {
    @Suppress("MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT")
    archivesName.set(ModConfig.MOD_NAME.replace(' ', '-'))
}

neoForge {
    version = ModConfig.NEO_VERSION

    parchment {
        mappingsVersion.set(ModConfig.PARCHMENT_MAPPING_VERSION)
        minecraftVersion.set(ModConfig.MINECRAFT_VERSION)
    }

    // Only :tisam produces a mod artifact. :hellow is a plain library jar
    // that TISAM's runtime classpath picks up via the :hellow subproject
    // dependency declared in tisam/build.gradle.kts.
    mods {
        create(ModConfig.MOD_ID) {
            sourceSet(project(":tisam").sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", ModConfig.MOD_ID)
            gameDirectory.set(file("runs/client"))
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", ModConfig.MOD_ID)
            gameDirectory.set(file("runs/server"))
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
        }
    }
}
