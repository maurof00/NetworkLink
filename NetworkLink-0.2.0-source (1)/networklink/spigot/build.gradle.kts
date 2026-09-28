dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    implementation(project(":common"))
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { file -> if (file.isDirectory) file else zipTree(file) })
}
