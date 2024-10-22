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
            url = uri("https://maven.pkg.github.com/InterShare/InterShareSDK")
            credentials {
                username = githubProperties.getProperty("gpr.user")
                password = githubProperties.getProperty("gpr.token")
            }
        }
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "InterShare"
include(":app")
