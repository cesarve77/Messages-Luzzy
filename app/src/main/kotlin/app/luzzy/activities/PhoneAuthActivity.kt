package app.luzzy.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import app.luzzy.R
import app.luzzy.databinding.ActivityPhoneAuthBinding
import app.luzzy.network.RetrofitClient
import app.luzzy.network.models.PhoneLoginRequest
import app.luzzy.utils.SharedPrefsManager
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PhoneAuthActivity : SimpleActivity() {

    private lateinit var binding: ActivityPhoneAuthBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        setupButtons()
    }

    private fun setupButtons() {
        binding.sendCodeButton.setOnClickListener {
            val phone = binding.phoneInput.text.toString().trim()
            if (phone.isBlank()) {
                binding.phoneInput.error = getString(R.string.enter_phone_number)
                return@setOnClickListener
            }
            sendVerificationCode(phone)
        }

        binding.verifyButton.setOnClickListener {
            val code = binding.codeInput.text.toString().trim()
            if (code.length < 6) {
                binding.codeInput.error = getString(R.string.enter_verification_code)
                return@setOnClickListener
            }
            val vid = verificationId ?: return@setOnClickListener
            signInWithCredential(PhoneAuthProvider.getCredential(vid, code))
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        showLoading(true)

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                showLoading(false)
                Toast.makeText(this@PhoneAuthActivity, e.message, Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = vid
                showLoading(false)
                binding.codeSubtitle.text = getString(R.string.phone_auth_code_subtitle, phoneNumber)
                binding.phoneInputLayout.visibility = View.GONE
                binding.codeInputLayout.visibility = View.VISIBLE
            }
        }

        PhoneAuthProvider.verifyPhoneNumber(
            PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()
        )
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        showLoading(true)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                lifecycleScope.launch {
                    val idToken = task.result?.user?.getIdToken(false)?.await()?.token
                    if (idToken != null) {
                        loginOnServer(idToken)
                    } else {
                        showLoading(false)
                        showError(getString(R.string.error_login_failed))
                    }
                }
            } else {
                showLoading(false)
                showError(task.exception?.message ?: getString(R.string.error_login_failed))
            }
        }
    }

    private suspend fun loginOnServer(idToken: String) {
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.apiService.phoneLogin(PhoneLoginRequest(idToken))
            }
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    val body = response.body()!!
                    SharedPrefsManager.saveAuthToken(this@PhoneAuthActivity, body.token)
                    SharedPrefsManager.savePhoneNumber(this@PhoneAuthActivity, body.phone)
                    SharedPrefsManager.saveAccountActive(this@PhoneAuthActivity, true)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    showLoading(false)
                    showError(getString(R.string.phone_auth_account_not_found))
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showLoading(false)
                showError(getString(R.string.error_connection))
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        binding.sendCodeButton.isEnabled = !loading
        binding.verifyButton.isEnabled = !loading
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
