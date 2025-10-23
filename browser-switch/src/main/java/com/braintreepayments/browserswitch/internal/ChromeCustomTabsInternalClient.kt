package com.braintreepayments.browserswitch.internal

import android.app.Activity
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.customtabs.EngagementSignalsCallback
import com.braintreepayments.browserswitch.OnCustomTabSessionEndedListener

internal class ChromeCustomTabsInternalClient(
    private val listener: OnCustomTabSessionEndedListener? = null
) {

    private var client: CustomTabsClient? = null
    private var session: CustomTabsSession? = null
    private var connection: CustomTabsServiceConnection? = null

    fun bind(activity: Activity, packageName: String?) {
        if (packageName == null) return

        if (connection != null) return
        connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, c: CustomTabsClient) {
                client = c
                session = client?.newSession(object : CustomTabsCallback() {})
                tryRegisterEngagementSignals()
            }
            override fun onServiceDisconnected(name: ComponentName) {
                client = null
                session = null
                connection = null
            }
        }
        CustomTabsClient.bindCustomTabsService(activity, packageName, connection!!)
    }

    fun unbind(activity: Activity) {
        connection?.let { activity.unbindService(it) }
        connection = null
        client = null
        session = null
    }

    fun buildIntent(builder: CustomTabsIntent.Builder): CustomTabsIntent {
        session?.let { builder.setSession(it) }
        return builder.build()
    }

    private fun tryRegisterEngagementSignals() {
        val s = session ?: return
        try {
            if (s.isEngagementSignalsApiAvailable(Bundle.EMPTY)) {
                s.setEngagementSignalsCallback(object : EngagementSignalsCallback() {
                    override fun onSessionEnded(didUserInteract: Boolean, extras: Bundle) {
                        // This fires when the tab is closed OR minimized (PiP)
                        listener?.onSessionEnded(didUserInteract)
                    }
                }, Bundle.EMPTY)
            }
        } catch (_: Throwable) {
            // Best-effort only; older browsers/devices won’t support this.
        }
    }

    @VisibleForTesting internal fun currentSession(): CustomTabsSession? = session
}
