plugins {
    id("SuperSUPlugin")
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)

    subprojects.forEach {
        dependsOn(":${it.name}:clean")
    }
}
