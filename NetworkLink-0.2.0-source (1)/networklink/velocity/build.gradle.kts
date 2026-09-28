dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.2.1-SNAPSHOT")
    implementation(project(":common"))
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { file -> if (file.isDirectory) file else zipTree(file) })
}
