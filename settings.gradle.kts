import java.util.*

val githubProperties = Properties().apply {
    val githubPropertiesFile = rootDir.resolve("github.properties")
    if (githubPropertiesFile.exists()) {
        load(githubPropertiesFile.inputStream())
    }
}


pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/julian-baumann/*")

            credentials {
                username = "julian-baumann"
                password = "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"
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "InterShare"
include(":app")
