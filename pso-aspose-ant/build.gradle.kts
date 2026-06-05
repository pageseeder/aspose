description = "ANT tasks and definition for ASPOSE API"

dependencies {
    implementation(libs.aspose.words.cloud)
    implementation(libs.gson)
    compileOnly(libs.ant)

    testImplementation(libs.ant)
    testImplementation(libs.junit)
    testImplementation(libs.commons.io)
}

tasks.register<Copy>("copyToLib") {
    group = "publishing"
    description = "Copy latest version plus dependencies to build/output/lib folder"
    dependsOn(tasks.jar)
    into(layout.buildDirectory.dir("output/lib"))
    from(configurations["runtimeClasspath"])
    from(tasks.jar)
    doFirst {
        delete(layout.buildDirectory.dir("output/lib"))
    }
}
