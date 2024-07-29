plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

version = "1.0.0"
group = "org.elfogre"

dependencies {
    testImplementation(libs.kotest)
    testImplementation(libs.kotesttestcontainers)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainersmysql)
    testImplementation(libs.mysql)
    testImplementation(libs.exposedjdbc)

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api(project(":lhs-parser"))

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation(libs.exposed)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
    withSourcesJar()
}

tasks.jar {
    manifest {
        attributes(mapOf("Implementation-Title" to project.name,
            "Implementation-Version" to project.version))
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
