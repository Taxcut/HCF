pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://repo.lunarclient.dev")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

rootProject.name = "hcf"
