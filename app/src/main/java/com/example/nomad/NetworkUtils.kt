package com.example.nomad

import android.content.Context
import android.net.ConnectivityManager

fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetworkInfo
    return network != null && network.isConnected
}
