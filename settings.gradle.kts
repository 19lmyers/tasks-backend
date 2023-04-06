pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://repo.repsy.io/mvn/chrynan/public")
    }
}

rootProject.name = "tasks-backend"