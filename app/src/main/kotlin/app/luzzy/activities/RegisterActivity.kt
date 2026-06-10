package app.luzzy.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.goodwy.commons.extensions.getProperTextColor
import com.goodwy.commons.extensions.toast
import com.goodwy.commons.extensions.viewBinding
import app.luzzy.R
import app.luzzy.auth.GoogleAuthRepository
import app.luzzy.databinding.ActivityRegisterBinding
import app.luzzy.network.RetrofitClient
import app.luzzy.network.models.GoogleLoginRequest
import app.luzzy.network.models.WebRegisterRequest
import app.luzzy.utils.SharedPrefsManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

class RegisterActivity : SimpleActivity() {

    companion object {
        private const val TAG = "RegisterActivity"
    }

    private val binding by viewBinding(ActivityRegisterBinding::inflate)
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleAuthRepository: GoogleAuthRepository

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleGoogleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        googleAuthRepository = GoogleAuthRepository(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.google_client_id))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.registerButton.setOnClickListener { attemptRegister() }
        binding.googleSignInButton.setOnClickListener {
            setLoading(true)
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
        binding.loginLinkButton.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        // Mismo color de texto que el resto de la app (igual que la pantalla Premium)
        val textColor = getProperTextColor()
        binding.titleText.setTextColor(textColor)
        binding.subtitleText.setTextColor(textColor)
        binding.orText.setTextColor(textColor)
    }

    private fun attemptRegister() {
        val name = binding.nameInput.text.toString().trim()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()

        binding.emailLayout.error = null
        binding.passwordLayout.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.error_invalid_email)
            return
        }
        if (password.length < 6) {
            binding.passwordLayout.error = getString(R.string.error_password_short)
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.webRegister(
                    WebRegisterRequest(
                        email = email,
                        password = password,
                        displayName = name.ifBlank { null }
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    googleAuthRepository.saveGoogleAuthData(
                        token = body.token,
                        email = body.user.email,
                        displayName = body.user.displayName,
                        photoUrl = null
                    )
                    SharedPrefsManager.saveGoogleAuthToken(this@RegisterActivity, body.token)
                    toast(R.string.register_success)
                    startActivity(Intent(this@RegisterActivity, UserSettingsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(UserSettingsActivity.EXTRA_JUST_LOGGED_IN, true)
                    })
                } else {
                    toast(R.string.error_register_failed)
                    setLoading(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en registro", e)
                toast(R.string.error_register_failed)
                setLoading(false)
            }
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            sendGoogleToServer(account)
        } catch (e: ApiException) {
            Log.e(TAG, "Error Google sign-in: ${e.statusCode}")
            setLoading(false)
            toast(R.string.google_login_failed)
        }
    }

    private fun sendGoogleToServer(account: GoogleSignInAccount) {
        lifecycleScope.launch {
            try {
                val fcmToken = SharedPrefsManager.getCurrentToken(this@RegisterActivity)
                    ?.takeIf { it.isNotBlank() } ?: "pending_fcm"

                val response = RetrofitClient.apiService.googleLogin(
                    GoogleLoginRequest(
                        email = account.email ?: "",
                        deviceToken = fcmToken,
                        displayName = account.displayName,
                        photoUrl = account.photoUrl?.toString()
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    googleAuthRepository.saveGoogleAuthData(
                        token = body.token,
                        email = body.user.email,
                        displayName = body.user.displayName,
                        photoUrl = body.user.photoUrl
                    )
                    SharedPrefsManager.saveGoogleAuthToken(this@RegisterActivity, body.token)
                    toast(R.string.google_login_success)
                    startActivity(Intent(this@RegisterActivity, UserSettingsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(UserSettingsActivity.EXTRA_JUST_LOGGED_IN, true)
                    })
                } else {
                    toast(R.string.google_login_server_error)
                    setLoading(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando Google al servidor", e)
                toast(R.string.google_login_server_error)
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.registerButton.isEnabled = !loading
        binding.googleSignInButton.isEnabled = !loading
        binding.loginLinkButton.isEnabled = !loading
        binding.progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
