dependencies {
    compileOnly("net.md-5:bungeecord-api:1.21-R0.3")
    implementation(project(":common"))
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { file -> if (file.isDirectory) file else zipTree(file) })
}
