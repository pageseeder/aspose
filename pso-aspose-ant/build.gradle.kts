plugins {
    alias(libs.plugins.shadow)
}

description = "ANT tasks and definition for ASPOSE API"

dependencies {
    implementation(libs.aspose.words.cloud)
    implementation(libs.okio)
    implementation(libs.jmail)
    compileOnly(libs.ant)

    testImplementation(libs.ant)
    testImplementation(libs.junit)
    testImplementation(libs.commons.io)
}

val shadowPrefix = "org.pageseeder.aspose.shadow"

tasks.shadowJar {
    archiveClassifier.set("standalone")
    mergeServiceFiles()
    exclude("META-INF/LICENSE.*")
    exclude("META-INF/NOTICE.*")
    exclude("META-INF/license.*")
    from(rootProject.file("LICENSE")) {
        into("META-INF")
    }
    relocate("com.aspose.words.cloud", "$shadowPrefix.com.aspose.words.cloud")
    relocate("com.google.gson", "$shadowPrefix.com.google.gson")
    relocate("com.squareup.okhttp", "$shadowPrefix.com.squareup.okhttp")
    relocate("okio", "$shadowPrefix.okio")
    relocate("jakarta.mail", "$shadowPrefix.jakarta.mail")
    relocate("com.sun.mail", "$shadowPrefix.com.sun.mail")
    relocate("jakarta.activation", "$shadowPrefix.jakarta.activation")
    relocate("com.sun.activation", "$shadowPrefix.com.sun.activation")
    relocate("org.threeten.bp", "$shadowPrefix.org.threeten.bp")
    relocate("io.gsonfire", "$shadowPrefix.io.gsonfire")
    relocate("io.swagger", "$shadowPrefix.io.swagger")
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
