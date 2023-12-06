plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

description = "AreaShop"

dependencies {
    // Platform
    compileOnlyApi(libs.spigot)
    compileOnlyApi(libs.worldeditCore) {
        exclude("com.google.guava", "guava")
    }
    compileOnlyApi(libs.worldeditBukkit) {
        exclude("com.google.guava", "guava")
    }
    compileOnlyApi(libs.worldguardCore) {
        exclude("com.google.guava", "guava")
    }
    compileOnlyApi(libs.worldguardBukkit) {
        exclude("com.google.guava", "guava")
    }
    compileOnlyApi("com.github.MilkBowl:VaultAPI:1.7") {
        exclude("com.google.guava", "guava")
    }

    // 3rd party libraries
    implementation("io.papermc:paperlib:1.0.8")
    implementation("com.github.NLthijs48:InteractiveMessenger:e7749258ca")
    implementation("com.github.NLthijs48:BukkitDo:819d51ec2b")
    implementation("io.github.baked-libs:dough-data:1.2.0")
    implementation("com.google.inject:guice:7.0.0") {
        exclude("com.google.guava", "guava")
    }
    implementation("com.google.inject.extensions:guice-assistedinject:7.0.0") {
        exclude("com.google.guava", "guava")
    }

    // Project submodules
    implementation(projects.areashopInterface)
    implementation(projects.areashopNms)

    if (!providers.environmentVariable("JITPACK").isPresent) {
        // We don't need these adapters if we are only publishing an api jar
        implementation(projects.adapters.platform.platformInterface)
        implementation(projects.adapters.platform.paper)

        runtimeOnly(projects.adapters.plugins.worldedit)
        runtimeOnly(projects.adapters.plugins.worldguard)
        runtimeOnly(projects.adapters.plugins.fastasyncworldedit)
        runtimeOnly(project(":adapters:platform:bukkit-1-17", "reobf"))
        runtimeOnly(projects.adapters.platform.bukkitModern)
    }

    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.57.1")
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
        relocate("me.wiefferink.interactivemessenger", "${base}.interactivemessenger")
        relocate("me.wiefferink.bukkitdo", "${base}.bukkitdo")
        relocate("io.papermc.lib", "${base}.paperlib")
        relocate("io.github.bakedlibs.dough", "${base}.dough")
        relocate("com.google.inject", "${base}.inject")
        relocate("com.google.errorprone", "${base}.errorprone")
        relocate("org.aopalliance", "${base}.aopalliance")
        relocate("javax.annotation", "${base}.javax.annotation")
        relocate("jakarta.inject", "${base}.jakarta.inject")
    }
}
