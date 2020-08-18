import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "0.4.21"
    java
    kotlin("jvm") version "1.4.0"
}

group = "org.jetbrains.research.jem"
version = "1.0"

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}
listOf("compileKotlin", "compileTestKotlin").forEach {
    tasks.getByName<KotlinCompile>(it) {
        kotlinOptions.jvmTarget = "1.8"
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://plugins.jetbrains.com/maven")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit", "junit", "4.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.4.0")
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.4.0")
    implementation("org.jetbrains.plugins:com.thomas.checkMate:2.1")
    implementation("org.javassist:javassist:3.27.0-GA")
    implementation("com.google.code.gson:gson:2.8.6")
}

intellij {
    version = "2020.2"
    setPlugins("java", "Kotlin")
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes("")
}