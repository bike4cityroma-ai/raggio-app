package org.bike4city.ciclofficinabot

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckProviderInstaller {
    fun install(appCheck: FirebaseAppCheck) {
        appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
