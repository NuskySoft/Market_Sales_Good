// Configuración de repositorios y módulos de Gradle.
pluginManagement {
    // Repositorios utilizados para resolver plugins de compilación.
    repositories {
        // Restringe las búsquedas en Google a los grupos de artefactos indicados.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
// Centraliza los repositorios de dependencias para todos los módulos.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Nombre del proyecto y módulo Android incluido en la compilación.
rootProject.name = "Market Sales"
include(":app")
