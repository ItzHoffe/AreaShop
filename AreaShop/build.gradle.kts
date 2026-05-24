plugins {
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

idea {
    module {
        isDownloadSources = true
    }
}

description = "AreaShop"

dependencies {
    // Platform
    compileOnlyApi(libs.paper)
    compileOnlyApi(libs.worldeditCore)
    compileOnlyApi(libs.worldeditBukkit)
    compileOnlyApi(libs.worldguardCore)
    compileOnlyApi(libs.worldguardBukkit)
    compileOnlyApi("com.github.MilkBowl:VaultAPI:1.7") {
        exclude("org.bukkit", "bukkit")
    }

    api("com.github.NLthijs48:InteractiveMessenger:e7749258ca")
    api("com.github.NLthijs48:BukkitDo:819d51ec2b")
    api("io.github.baked-libs:dough-data:1.2.0")
    api("com.google.inject:guice:7.0.0") {
        exclude("com.google.guava")
    }
    api("com.google.inject.extensions:guice-assistedinject:7.0.0") {
        exclude("com.google.guava")
    }
    implementation("org.incendo:cloud-paper:2.0.0-beta.15") {
        exclude("com.google.guava")
    }
    implementation("org.incendo:cloud-processors-confirmation:1.0.0-beta.3") {
        exclude("com.google.guava")
    }
    compileOnly("net.kyori:adventure-text-minimessage:5.1.1")
    implementation("org.spongepowered:configurate-yaml:4.1.2")

    // Project submodules
    api(projects.areashopInterface)
    api(projects.adapters.platform.platformInterface)
    api(projects.adapters.platform.paper)

    if (!providers.environmentVariable("JITPACK").isPresent) {
        // We don't need these adapters if we are only publishing an api jar
        runtimeOnly(projects.adapters.plugins.worldedit)
        runtimeOnly(projects.adapters.plugins.worldguard)
        runtimeOnly(projects.adapters.plugins.fastasyncworldedit)
        runtimeOnly(projects.adapters.plugins.essentials)
    }
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:4.113.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}



repositories {
    mavenCentral()
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    assemble {
        if (!providers.environmentVariable("JITPACK").isPresent) {
            dependsOn(shadowJar)
        }
    }

    jar {
        archiveBaseName.set("AreaShop")
        if (!providers.environmentVariable("JITPACK").isPresent) {
            archiveClassifier.set("original")
        } else {
            archiveClassifier.set("")
        }
    }

    if (providers.environmentVariable("JITPACK").isPresent) {
        artifacts {
            archives(jar)
        }
    }

    java {
        withSourcesJar()
    }

    val javaComponent = project.components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
        skip()
    }

    shadowJar {
        archiveClassifier.set("")
        base {
            archiveBaseName.set("AreaShop")
        }
        val base = "me.wiefferink.areashop.libraries"
        relocate("org.incendo.cloud", "${base}.cloud")
        relocate("me.wiefferink.interactivemessenger", "${base}.interactivemessenger")
        relocate("me.wiefferink.bukkitdo", "${base}.bukkitdo")
        relocate("io.papermc.lib", "${base}.paperlib")
        relocate("io.github.bakedlibs.dough", "${base}.dough")
        relocate("com.google.inject", "${base}.inject")
        relocate("com.google.errorprone", "${base}.errorprone")
        relocate("org.aopalliance", "${base}.aopalliance")
        relocate("javax.annotation", "${base}.javax.annotation")
        relocate("jakarta.inject", "${base}.jakarta.inject")
        relocate("org.jetbrains.annotations", "${base}.jetbrains.annotations")
        relocate("io.leangen.geantyref", "${base}.geantyref")
        relocate("org.checkerframework", "${base}.checkerframework")
        relocate("org.intellij", "${base}.intellij")
        relocate("org.spongepowered", "${base}.spongepowered")
        relocate("org.yaml.snakeyaml", "${base}.snakeyaml")
    }
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.1.2")

        downloadPlugins {
            url("https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.22.0-dev+112-5baf239.jar")
            github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
            // WorldEdit 7.3.9
            url("https://mediafilez.forgecdn.net/files/8037/361/worldedit-bukkit-7.4.3.jar")
            // WorldGuard 7.0.12
            url("https://mediafilez.forgecdn.net/files/7803/732/worldguard-bukkit-7.0.16.jar")
        }
    }
}
