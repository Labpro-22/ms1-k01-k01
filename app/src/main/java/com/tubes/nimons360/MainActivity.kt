package com.tubes.nimons360

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.tubes.nimons360.utils.NetworkMonitor
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Variabel untuk menyimpan instance pop-up error agar bisa kita tutup nanti
    private var disconnectSnackbar: Snackbar? = null

    // Flag supaya tidak muncul notif "Kembali online" saat pertama kali buka app
    private var isInitialCheck = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        val networkMonitor = NetworkMonitor(this)

        lifecycleScope.launch {
            networkMonitor.isConnected.collect { hasInternet ->
                if (!hasInternet) {
                    // INTERNET MATI: Tampilkan Snackbar merah yang tidak mau hilang (INDEFINITE)
                    isInitialCheck = false

                    disconnectSnackbar = Snackbar.make(
                        findViewById(android.R.id.content),
                        "Koneksi internet terputus!",
                        Snackbar.LENGTH_INDEFINITE
                    ).apply {
                        setBackgroundTint(Color.parseColor("#D32F2F")) // Warna merah Material
                        setTextColor(Color.WHITE)
                        show()
                    }
                } else {
                    // INTERNET NYALA: Tutup Snackbar merah kalau ada
                    disconnectSnackbar?.dismiss()

                    // Kalau ini BUKAN pengecekan pertama (berarti habis dari kondisi mati), tampilkan Snackbar hijau
                    if (!isInitialCheck) {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "Kembali online",
                            Snackbar.LENGTH_SHORT // Hilang otomatis
                        ).apply {
                            setBackgroundTint(Color.parseColor("#388E3C")) // Warna hijau Material
                            setTextColor(Color.WHITE)
                            show()
                        }
                    }

                    isInitialCheck = false
                }
            }
        }
    }
}