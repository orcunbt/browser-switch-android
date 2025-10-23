package com.braintreepayments.browserswitch

fun interface OnCustomTabSessionEndedListener {
    fun onSessionEnded(didUserInteract: Boolean)
}
