plugins {
    id("com.android.application") version "9.2.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
    id("com.google.devtools.ksp") version "2.3.10" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
}

// Workaround per Android Studio 2025.3/Kotlin scripting (KTIJ-35330):
// l'IDE richiede questo task durante il sync anche con Kotlin integrato in AGP 9.
subprojects {
    tasks.register("prepareKotlinBuildScriptModel") {
        group = "ide"
        description = "Compatibility task used by Android Studio during Kotlin DSL sync"
    }
}
