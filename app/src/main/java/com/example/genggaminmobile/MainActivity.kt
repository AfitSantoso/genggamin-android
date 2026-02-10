package com.example.genggaminmobile

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.genggaminmobile.core.navigation.NavGraph
import com.example.genggaminmobile.core.util.SystemCheck
import com.example.genggaminmobile.ui.theme.GenggaminmobileTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (locationGranted) {
            fetchLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent Screen Capture / Screen Recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        // Root Detection Check
        if (SystemCheck.isDeviceRooted(this)) {
            showRootedDeviceAlert()
            return // Stop initialization
        }

        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkAndRequestPermissions()

        setContent {
            GenggaminmobileTheme {
                MainNavigation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Ambil lokasi setiap kali aplikasi dibuka (masuk ke foreground)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocation()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            fetchLocation()
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                // Lokasi berhasil didapat.
                // Di sini bisa disimpan ke repository atau SharedPref jika diperlukan secara global.
            }
    }

    private fun showRootedDeviceAlert() {
        AlertDialog.Builder(this)
            .setTitle("Security Warning")
            .setMessage("This device appears to be rooted. For security reasons, this application cannot run on rooted devices.")
            .setCancelable(false)
            .setPositiveButton("Exit") { _, _ ->
                finishAffinity()
                System.exit(0)
            }
            .show()
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    NavGraph(
        navController = navController,
        modifier = Modifier.fillMaxSize(),
    )
}
