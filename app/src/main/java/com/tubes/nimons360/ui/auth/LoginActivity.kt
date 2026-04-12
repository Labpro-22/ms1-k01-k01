package com.tubes.nimons360.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tubes.nimons360.MainActivity
import com.tubes.nimons360.data.local.TokenManager
import com.tubes.nimons360.data.remote.RetrofitClient
import com.tubes.nimons360.data.remote.ApiService
import com.tubes.nimons360.data.repository.AuthRepository
import com.tubes.nimons360.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inisialisasi ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi dependency
        val apiService = RetrofitClient.getInstance(this).create(ApiService::class.java)
        val tokenManager = TokenManager(applicationContext)
        val repository = AuthRepository(apiService, tokenManager)
        val factory = AuthViewModelFactory(repository)

        // Inisialisasi ViewModel
        viewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        // Set click listener tombol Sign In
        // Regex: 8 digit diawali angka 1, diikuti domain ITB
        val emailRegex = Regex("^1\\d{7}@std\\.stei\\.itb\\.ac\\.id$")
        // Regex: password harus berupa NIM (8 digit diawali angka 1)
        val nimRegex = Regex("^1\\d{7}$")

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            var isValid = true

            // Validasi format email: harus {nim}@std.stei.itb.ac.id
            if (email.isEmpty()) {
                binding.tilEmail.error = "Email tidak boleh kosong"
                isValid = false
            } else if (!emailRegex.matches(email)) {
                binding.tilEmail.error = "Format: 1xxxxxxx@std.stei.itb.ac.id"
                isValid = false
            } else {
                binding.tilEmail.error = null
            }

            // Validasi format password: harus berupa NIM (8 digit diawali 1)
            if (password.isEmpty()) {
                binding.tilPassword.error = "Password tidak boleh kosong"
                isValid = false
            } else if (!nimRegex.matches(password)) {
                binding.tilPassword.error = "Password harus berupa NIM (contoh: 13523055)"
                isValid = false
            } else {
                binding.tilPassword.error = null
            }

            if (isValid) {
                viewModel.login(email, password)
            }
        }

        // Observasi StateFlow dari ViewModel
        observeLoginState()
    }

    private fun observeLoginState() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginUiState.Idle -> {
                        // State awal, tidak perlu aksi
                        binding.progressBar.visibility = View.GONE
                        binding.btnSignIn.isEnabled = true
                    }

                    is LoginUiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnSignIn.isEnabled = false
                    }

                    is LoginUiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSignIn.isEnabled = true

                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()

                        // Navigasi ke MainActivity dan tutup LoginActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }

                    is LoginUiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSignIn.isEnabled = true

                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
