plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`
    signing
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

version = "1.0.2"
group = "io.github.elfogre"

dependencies {
    testImplementation(libs.kotest)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
    withSourcesJar()
    withJavadocJar()
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

publishing {
    publications {
        create<MavenPublication>("lhsParser") {
            groupId = "io.github.elfogre"
            artifactId = "lhs-parser"
            version = "1.0.2"

            from(components["java"])

            pom {
                name = "lhs-parser"
                description = "Kotlin parser for LHS brackets query params"
                url = "https://github.com/elfogre/lhsk"
                inceptionYear = "2024"

                licenses {
                    license {
                        name.set("GPL-3.0 license")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.html")
                    }
                }

                developers {
                    developer {
                        id = "elfogre"
                        name = "Jose Escobar"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/elfogre/lhsk.git"
                    developerConnection = "scm:git:ssh://github.com/elfogre/lhsk.git"
                    url = "https://github.com/elfogre/lhsk"
                }
            }
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["lhsParser"])
}

artifacts {
    add("archives", tasks.named("jar").get())
    add("archives", tasks.named("sourcesJar").get())
    add("archives", tasks.named("javadocJar").get())
}