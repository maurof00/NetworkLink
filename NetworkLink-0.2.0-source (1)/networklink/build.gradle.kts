plugins {
    base
}

allprojects {
    group = "it.networklink"
    version = providers.gradleProperty("projectVersion").orElse("0.1.0").get()
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    tasks.withType<Jar>().configureEach {
        archiveBaseName.set("NetworkLink-${project.name}")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest.attributes["Implementation-Version"] = project.version
    }
}

tasks.register("buildAll") {
    dependsOn(subprojects.map { it.tasks.named("build") })
}
