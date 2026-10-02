plugins {
    id("net.minecraftforge.gradle")
    id("org.spongepowered.mixin")
}

val mcVersion = stonecutter.current.version
val modId = property("mod.id") as String

group = property("mod.group") as String
version = "${property("mod.version")}+$mcVersion"
base.archivesName = modId

val javaVersion = if (stonecutter.eval(mcVersion, ">=1.18")) 17 else 8

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
}

minecraft {
    mappings("official", mcVersion)
    copyIdeResources = true

    runs {
        configureEach {
            // Each Minecraft version gets its own run dir (versions/<mc>/run) so worlds never cross versions.
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "debug")
            // Profile a dev run: ./gradlew "1.20.1:runServer" -Pjfr=C:/path/out.jfr
            findProperty("jfr")?.let { jvmArg("-XX:StartFlightRecording=filename=$it,settings=profile") }
            // Cross-check every optimization against vanilla: -Pverify (see Verify.java)
            if (hasProperty("verify")) property("bettertnt.verify", "true")
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }
        create("client")
        create("server") {
            args("--nogui")
        }
    }
}

mixin {
    add(sourceSets.main.get(), "$modId.refmap.json")
    config("$modId.mixins.json")
}

dependencies {
    "minecraft"("net.minecraftforge:forge:$mcVersion-${property("deps.forge")}")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf(
        "mod_id" to modId,
        "mod_name" to project.property("mod.name") as String,
        "mod_version" to project.property("mod.version") as String,
        "mod_authors" to project.property("mod.authors") as String,
        "mod_description" to project.property("mod.description") as String,
        "forge_compat" to project.property("mod.forge_compat") as String,
        "mc_compat" to project.property("mod.mc_compat") as String,
        "pack_format" to project.property("mod.pack_format") as String,
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) { expand(props) }
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to modId,
            "Specification-Vendor" to project.property("mod.authors"),
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to archiveVersion.get(),
            "Implementation-Vendor" to project.property("mod.authors"),
        )
    }
    finalizedBy("reobfJar")
}

// Copies the reobfuscated jars of every version into the root build/libs for release.
tasks.register<Copy>("buildAndCollect") {
    group = "build"
    dependsOn("reobfJar")
    from(tasks.jar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
}

// Lets the dedicated server console take commands when started through Gradle.
tasks.matching { it.name == "runServer" }.configureEach {
    (this as JavaExec).standardInput = System.`in`
}
