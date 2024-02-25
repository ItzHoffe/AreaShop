import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsPlugin

plugins {
    java
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.5.11" apply false
    id("com.github.spotbugs") version "5.1.3"
    idea
    eclipse
}

group = "me.wiefferink"
version = "2.7.18-SNAPSHOT"

val targetJavaVersion = 17
val encoding = Charsets.UTF_8
val encodingName: String = encoding.name()

java.toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))

subprojects {

    group = rootProject.group
    version = rootProject.version

    apply {
        plugin<JavaPlugin>()
        plugin<JavaLibraryPlugin>()
        if (project.name != "platform-bukkit-1-17") {
            plugin<MavenPublishPlugin>()
        }
        plugin<IdeaPlugin>()
        plugin<EclipsePlugin>()
        // plugin<SpotBugsPlugin>()
    }
    
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven {
            name = "jitpack"
            url = uri("https://jitpack.io")
            content {
                includeGroupByRegex("com\\.github.*")
            }
        }
        maven("https://repo.aikar.co/content/groups/aikar/")
        maven("https://maven.enginehub.org/repo/")
    }

    dependencies {
        implementation("org.jetbrains:annotations:24.0.1")
    }

    java.toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))

    tasks {
        withType(JavaCompile::class) {
            options.release.set(targetJavaVersion)
            options.encoding = encodingName
            options.isFork = true
            options.isDeprecation = true
        }

        withType(Javadoc::class) {
            options.encoding = encodingName
        }

        withType(ProcessResources::class) {
            filteringCharset = encodingName
        }
    }

    if (project.name != "platform-bukkit-1-17") {
        publishing {
            publications {
                create<MavenPublication>(project.name) {
                    from(components["java"])
                    pom {
                        scm {
                            connection.set("scm:git:git://github.com/md5sha256/AreaShop.git")
                            developerConnection.set("scm:git:ssh://github.com/md5sha256/AreaShop.git")
                            url.set("https://github.com/md5sha256/AreaShop/tree/dev/bleeding")
                        }
                        licenses {
                            license {
                                name.set("GNU General Public License v3.0")
                                url.set("https://github.com/md5sha256/AreaShop/blob/dev/bleeding/LICENSE")
                            }
                        }
                    }
                }
            }
        }
    }
}
