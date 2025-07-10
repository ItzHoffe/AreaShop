repositories {
    maven {
        name = "essentialsx"
        url = uri("https://repo.essentialsx.net/releases/")
        mavenContent {
            releasesOnly()
        }

    }
}

dependencies {
    compileOnly(projects.areashop)
    compileOnly("net.essentialsx:EssentialsX:2.21.1") {
        exclude("org.spigotmc", "spigot-api")
    }
}