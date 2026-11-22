plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.amie.refactor"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
        bundledPlugin("com.intellij.java")
    }
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("243.*")
    }
}
