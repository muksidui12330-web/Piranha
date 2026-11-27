package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class GpsCoordinates(val latitude: Double, val longitude: Double)

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): Result<GpsCoordinates> =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume(Result.success(GpsCoordinates(loc.latitude, loc.longitude)))
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                if (last != null) {
                                    cont.resume(Result.success(GpsCoordinates(last.latitude, last.longitude)))
                                } else {
                                    cont.resume(Result.failure(IllegalStateException("Turn on device GPS and try again")))
                                }
                            }
                            .addOnFailureListener { e ->
                                cont.resume(Result.failure(e))
                            }
                    }
                }
                .addOnFailureListener { e ->
                    cont.resume(Result.failure(e))
                }
        }

    fun openGoogleMaps(context: Context, latitude: Double?, longitude: Double?, label: String) {
        val uri = if (latitude != null && longitude != null) {
            val encodedLabel = Uri.encode(label)
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
        } else {
            val encodedLabel = Uri.encode(label)
            Uri.parse("geo:0,0?q=$encodedLabel")
        }
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallbackUri = if (latitude != null && longitude != null) {
            Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
        } else {
            Uri.parse("https://maps.google.com/?q=${Uri.encode(label)}")
        }
        try {
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (_: Exception) {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }
}
