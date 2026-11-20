package com.facebookpostplanner

import android.app.Application
import com.facebook.FacebookSdk

/**
 * Facebook Login needs a real App ID + client token from the Facebook
 * Developer App before the SDK can start at all — with placeholders it
 * throws at launch and the app never opens. So init is skipped entirely
 * until both are real (gradle properties planner.facebook.app.id and
 * planner.facebook.client.token). Until then the app works normally
 * without Facebook, and the Connect Page screen says what is missing
 * instead of crashing.
 */
class PlannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val appId = getString(R.string.facebook_app_id)
        val clientToken = getString(R.string.facebook_client_token)
        if (appId != "0" && clientToken.isNotBlank() && clientToken != "0") {
            FacebookSdk.sdkInitialize(this)
        }
    }
}
