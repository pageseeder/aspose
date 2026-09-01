plugins {
    alias(libs.plugins.versions)
    alias(libs.plugins.sonar)
    alias(libs.plugins.jreleaser)
    alias(libs.plugins.shadow)
    id("java-library")
    id("maven-publish")
}

val title: String by project
val gitName: String by project
val website: String by project

val appVersion = File("$rootDir/version.txt").readText(Charsets.UTF_8).trim()

group = "org.pageseeder.aspose"
version = appVersion

subprojects {
    group = "org.pageseeder.aspose"
    version = rootProject.version

    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "jacoco")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
        withJavadocJar()
        withSourcesJar()
    }

    repositories {
        mavenCentral {
            url = uri("https://maven-central.storage.googleapis.com/maven2")
        }
        maven { url = uri("https://s01.oss.sonatype.org/content/groups/public/") }
        maven { url = uri("https://releases.aspose.cloud/java/repo/") }
    }

    tasks.withType<Javadoc>().configureEach {
        isFailOnError = false
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    tasks.withType<Test>().configureEach {
        finalizedBy("jacocoTestReport")
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
            csv.required.set(false)
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                plugins.withId("com.gradleup.shadow") {
                    artifact(tasks.named("shadowJar")) {
                        classifier = "standalone"
                    }
                }
                pom {
                    name.set(title)
                    description.set(provider { project.description ?: "" })
                    url.set(website)
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    organization {
                        name.set("Allette Systems")
                        url.set("https://www.allette.com.au")
                    }
                    scm {
                        url.set("git@github.com:pageseeder/${gitName}.git")
                        connection.set("scm:git:git@github.com:pageseeder/${gitName}.git")
                        developerConnection.set("scm:git:git@github.com:pageseeder/${gitName}.git")
                    }
                    developers {
                        developer {
                            id.set("clauret")
                            name.set("Christophe Lauret")
                            email.set("clauret@weborganic.com")
                        }
                        developer {
                            id.set("jbreure")
                            name.set("Jean-Baptiste Reure")
                            email.set("jbreure@weborganic.com")
                        }
                        developer {
                            id.set("ccabral")
                            name.set("Carlos Cabral")
                            email.set("ccabral@allette.com.au")
                        }
                        developer {
                            id.set("philipr")
                            name.set("Philip Rutherford")
                            email.set("philipr@weborganic.com")
                        }
                    }
                }
            }
        }
        repositories {
            maven {
                url = rootProject.layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
            }
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "pageseeder_pso-aspose")
        property("sonar.organization", "pageseeder")
    }
}

jreleaser {
    configFile.set(file("jreleaser.toml"))
    distributions {
        subprojects.forEach { subproject ->
            register(subproject.name) {
                artifact {
                    path.set(subproject.layout.buildDirectory.file("libs/${subproject.name}-${project.version}.jar"))
                }
                artifact {
                    path.set(subproject.layout.buildDirectory.file("libs/${subproject.name}-${project.version}-sources.jar"))
                }
                artifact {
                    path.set(subproject.layout.buildDirectory.file("libs/${subproject.name}-${project.version}-javadoc.jar"))
                }
            }
        }
    }
}

tasks.wrapper {
    gradleVersion = "8.14.5"
    distributionType = Wrapper.DistributionType.BIN
}
