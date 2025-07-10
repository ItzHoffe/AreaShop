dependencies {
    compileOnly(libs.paper)
    compileOnly(projects.areashopInterface)
    compileOnly(libs.fastasyncworldeditCore) {
        exclude("net.kyori", "adventure-text-minimessage")
    }
    compileOnly(libs.fastasyncworldeditBukkit) {
        exclude("net.kyori", "adventure-text-minimessage")
    }
    compileOnly(libs.worldguardCore)
    compileOnly(libs.worldguardBukkit)
}

description = "AreaShop FastAsyncWorldEdit"