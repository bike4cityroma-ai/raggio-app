plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val firebaseConfigPresent = file("google-services.json").exists()
if (firebaseConfigPresent) apply(plugin = "com.google.gms.google-services")

android {
    namespace = "org.bike4city.ciclofficinabot"
    compileSdk = 36
    defaultConfig {
        applicationId = "org.bike4city.ciclofficinabot"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigPresent.toString())
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        debug {
            // Anche l'APK distribuito ai tester viene ridotto e offuscato.
            // La firma debug mantiene semplice l'installazione fuori dal Play Store.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    if (firebaseConfigPresent) {
        implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
        implementation("com.google.firebase:firebase-auth")
        implementation("com.google.firebase:firebase-firestore")
        implementation("com.google.firebase:firebase-functions")
        implementation("com.google.firebase:firebase-storage")
        implementation("com.google.firebase:firebase-appcheck")
        debugImplementation("com.google.firebase:firebase-appcheck-debug")
        releaseImplementation("com.google.firebase:firebase-appcheck-playintegrity")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    } else {
        // Consente la compilazione degli adapter anche prima di aggiungere la
        // configurazione Firebase, senza includere gli SDK nel pacchetto finale.
        compileOnly(platform("com.google.firebase:firebase-bom:34.16.0"))
        compileOnly("com.google.firebase:firebase-auth")
        compileOnly("com.google.firebase:firebase-firestore")
        compileOnly("com.google.firebase:firebase-functions")
        compileOnly("com.google.firebase:firebase-storage")
        compileOnly("com.google.firebase:firebase-appcheck")
        debugCompileOnly("com.google.firebase:firebase-appcheck-debug")
        releaseCompileOnly("com.google.firebase:firebase-appcheck-playintegrity")
        compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    }
    ksp("androidx.room:room-compiler:2.8.4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
}
