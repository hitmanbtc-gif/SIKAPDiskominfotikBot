package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val address: String
)

object LocationHelper {
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun captureCurrentLocation(context: Context): Result<GpsFix> = withContext(Dispatchers.IO) {
        try {
            if (!hasLocationPermission(context)) {
                return@withContext Result.failure(
                    SecurityException("Izin lokasi GPS belum diberikan.")
                )
            }
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location = try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            } catch (_: Exception) {
                null
            } ?: client.lastLocation.await()

            if (location == null) {
                return@withContext Result.failure(
                    IllegalStateException("Sinyal GPS belum tersedia. Pastikan GPS perangkat aktif dan coba lagi.")
                )
            }

            val resolvedAddress = reverseGeocode(context, location.latitude, location.longitude)
            Result.success(
                GpsFix(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy.toDouble(),
                    address = resolvedAddress
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
        return try {
            if (!Geocoder.isPresent()) return ""
            val geocoder = Geocoder(context, Locale("id", "ID"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        val addr = addresses.firstOrNull()
                        val text = listOfNotNull(
                            addr?.thoroughfare,
                            addr?.subLocality,
                            addr?.locality,
                            addr?.subAdminArea ?: addr?.adminArea
                        ).filter { it.isNotBlank() }.distinct().joinToString(", ")
                        if (cont.isActive) cont.resume(text)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val addr = addresses?.firstOrNull()
                listOfNotNull(
                    addr?.thoroughfare,
                    addr?.subLocality,
                    addr?.locality,
                    addr?.subAdminArea ?: addr?.adminArea
                ).filter { it.isNotBlank() }.distinct().joinToString(", ")
            }
        } catch (_: Exception) {
            ""
        }
    }
}
