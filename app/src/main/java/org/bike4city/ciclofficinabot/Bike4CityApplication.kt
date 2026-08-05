package org.bike4city.ciclofficinabot

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck

class Bike4CityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.FIREBASE_CONFIGURED) return
        FirebaseApp.initializeApp(this) ?: return
        AppCheckProviderInstaller.install(FirebaseAppCheck.getInstance())
    }
}
