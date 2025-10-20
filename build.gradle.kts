import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    `maven-publish`
    signing
    id("io.freefair.lombok") version "8.6"
    id("com.gradleup.shadow") version "8.3.2"
    id("org.sonarqube") version "6.1.0.5360"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "net.guizhanss"

allprojects {
    repositories {
        mavenCentral()
        maven("https://jitpack.io/")
        maven(url = "https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.alessiodp.com/releases/")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "org.sonarqube")

    dependencies {
        fun compileOnlyAndTestImplementation(dependencyNotation: Any) {
            compileOnly(dependencyNotation)
            testImplementation(dependencyNotation)
        }

        api("com.google.code.findbugs:jsr305:3.0.2")
        compileOnlyAndTestImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.1")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<ShadowJar> {
        archiveClassifier.set("all")
    }

    sonar {
        properties {
            property("sonar.projectKey", "ybw0014_GuizhanLib")
            property("sonar.organization", "ybw0014")
            property("sonar.host.url", "https://sonarcloud.io")
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                groupId = rootProject.group as String
                artifactId = project.name
                version = rootProject.version as String

                pom {
                    name.set("guizhanlib")
                    description.set("A library for Slimefun addon development.")
                    url.set("https://github.com/ybw0014/guizhanlib")

                    licenses {
                        license {
                            name.set("GPL-3.0 license")
                            url.set("https://github.com/ybw0014/guizhanlib/blob/master/LICENSE")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            name.set("ybw0014")
                            url.set("https://ybw0014.dev/")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/ybw0014/guizhanlib.git")
                        developerConnection.set("scm:git:ssh://github.com:ybw0014/guizhanlib.git")
                        url.set("https://github.com/ybw0014/guizhanlib/tree/master")
                    }
                }
            }
        }
    }

    signing {
        val key = System.getenv("SIGNING_KEY") ?: findProperty("signing.key") as String?
        val pass = System.getenv("SIGNING_PASSWORD") ?: findProperty("signing.password") as String?

        if (!key.isNullOrBlank() && !pass.isNullOrBlank()) {
            useInMemoryPgpKeys(key, pass)
            sign(publishing.publications["maven"])
        } else {
            setRequired { false }
            logger.lifecycle("Signing disabled (no PGP key/password)")
        }
    }

}

tasks.matching { it.name == "generateMetadataFileForMavenPublication" }.configureEach {
    dependsOn(tasks.named("jar"))
    dependsOn(tasks.named("shadowJar"))
}

tasks.withType<org.gradle.plugins.signing.Sign>().configureEach {
    onlyIf { signing.isRequired }
    if (!signing.isRequired) {
        enabled = false
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}
