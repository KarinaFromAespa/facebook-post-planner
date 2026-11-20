package com.facebookpostplanner

import android.app.Application
import com.facebook.FacebookSdk

/**
 * The Facebook SDK does not auto-initialize in this setup — without this,
 * LoginManager throws "The SDK has not been initialized" the moment
 * "Connect with Facebook" is tapped. Initialize it here so every screen
 * can use it. The App ID comes from the manifest meta-data
 * (planner.facebook.app.id); with the "0" placeholder, login shows
 * Facebook's invalid-app-ID error instead of crashing.
 */
class PlannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FacebookSdk.sdkInitialize(this)
    }
}
