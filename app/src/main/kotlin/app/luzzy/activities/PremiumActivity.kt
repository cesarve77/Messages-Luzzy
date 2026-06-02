package app.luzzy.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import app.luzzy.R
import app.luzzy.auth.GoogleAuthRepository
import app.luzzy.billing.BillingManager
import app.luzzy.billing.PremiumRepository
import app.luzzy.databinding.ActivityPremiumBinding
import app.luzzy.network.RetrofitClient
import app.luzzy.network.models.BillingRegisterRequest
import app.luzzy.network.models.PremiumActivateRequest
import app.luzzy.utils.SharedPrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PremiumActivity : SimpleActivity() {

    private lateinit var binding: ActivityPremiumBinding
    private lateinit var billingManager: BillingManager
    private lateinit var premiumRepository: PremiumRepository
    private lateinit var googleAuthRepository: GoogleAuthRepository

    // true solo mientras hay una compra iniciada explícitamente por el usuario
    private var purchaseInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        googleAuthRepository = GoogleAuthRepository(this)
        setupToolbar()
        initBilling()
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initBilling() {
        premiumRepository = PremiumRepository(this)
        billingManager = BillingManager(this, premiumRepository)

        billingManager.onPremiumStatusChanged = {
            runOnUiThread { hideLoading() }
        }
        billingManager.onPurchaseError = { error ->
            runOnUiThread {
                purchaseInProgress = false
                hideLoading()
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
        billingManager.onProductPriceLoaded = { price ->
            runOnUiThread { binding.premiumPriceText.text = price }
        }
        billingManager.onPurchaseSucceeded = { purchaseToken ->
            // Solo registrar/vincular cuando el usuario acaba de comprar.
            // Las compras viejas detectadas al abrir la pantalla se ignoran.
            if (purchaseInProgress) {
                purchaseInProgress = false
                runOnUiThread { handleNewPurchase(purchaseToken) }
            }
        }

        billingManager.initialize()
    }

    private fun setupUI() {
        // "Actualizar a Premium" — lanza el flujo de pago de Google Play
        binding.purchaseButton.setOnClickListener {
            purchaseInProgress = true
            showLoading()
            billingManager.launchPurchaseFlow(this)
        }
        // "Recuperar compra" — inicia sesión para recuperar la cuenta
        binding.restorePurchasesButton.setOnClickListener {
            startActivity(Intent(this, GoogleLoginActivity::class.java))
        }
    }

    /**
     * Una compra recién completada por el usuario:
     * - Con sesión: vincula el purchaseToken a la cuenta logueada.
     * - Sin sesión: crea una cuenta nueva anclada al purchaseToken (sin email/contraseña).
     *   Esa cuenta pasa a ser la sesión; el usuario puede añadir email/contraseña
     *   más tarde desde Configuraciones → Acceso web.
     */
    private fun handleNewPurchase(purchaseToken: String) {
        if (googleAuthRepository.isLoggedIn()) {
            activatePremiumOnServer(purchaseToken)
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fcmToken = SharedPrefsManager.getCurrentToken(this@PremiumActivity)
                val resp = RetrofitClient.apiService.billingRegister(
                    BillingRegisterRequest(purchaseToken, fcmToken)
                )
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    withContext(Dispatchers.Main) {
                        // La cuenta creada por la compra pasa a ser la sesión del usuario
                        googleAuthRepository.saveGoogleAuthData(body.token, "", null, null)
                        refreshUi()
                    }
                    Log.d("PremiumActivity", "✅ Cuenta creada por compra: ${body.phone}")
                } else {
                    Log.w("PremiumActivity", "billing-register HTTP ${resp.code()}")
                }
            } catch (e: Exception) {
                Log.w("PremiumActivity", "billing-register: ${e.message}")
            }
        }
    }

    /**
     * El estado premium y los botones dependen de la sesión iniciada:
     * - Sin sesión: no se muestra premium; el botón "Recuperar compra" está visible.
     * - Con sesión: el estado premium se consulta al backend con la cuenta logueada.
     */
    private fun refreshUi() {
        binding.accountEmail.text = googleAuthRepository.getUserEmail()
            ?.takeIf { it.isNotBlank() } ?: ""

        if (googleAuthRepository.isLoggedIn()) {
            binding.restorePurchasesButton.visibility = View.GONE
            checkBackendPremiumStatus()
        } else {
            binding.restorePurchasesButton.visibility = View.VISIBLE
            setPremiumUi(false)
        }
    }

    private fun setPremiumUi(isPremium: Boolean) {
        if (isPremium) {
            binding.premiumStatusText.text = getString(R.string.premium_status_active)
            binding.purchaseButton.isEnabled = false
            binding.purchaseButton.text = getString(R.string.already_premium)
        } else {
            binding.premiumStatusText.text = getString(R.string.premium_status_inactive)
            binding.purchaseButton.isEnabled = true
            binding.purchaseButton.text = getString(R.string.upgrade_to_premium)
        }
    }

    /** Consulta el backend con la cuenta logueada. Sin sesión no hace nada. */
    private fun checkBackendPremiumStatus() {
        val authHeader = googleAuthRepository.getAuthHeader() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = RetrofitClient.apiService.getPremiumStatus(authHeader)
                val premium = resp.isSuccessful && resp.body()?.get("isPremium") == true
                withContext(Dispatchers.Main) { setPremiumUi(premium) }
            } catch (e: Exception) {
                Log.w("PremiumActivity", "premium/status: ${e.message}")
            }
        }
    }

    /** Vincula el purchaseToken a la cuenta logueada (Google). */
    private fun activatePremiumOnServer(purchaseToken: String) {
        val authHeader = googleAuthRepository.getAuthHeader() ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                RetrofitClient.apiService.activatePremium(authHeader, PremiumActivateRequest(purchaseToken))
                Log.d("PremiumActivity", "✅ Premium activado en servidor")
                withContext(Dispatchers.Main) { checkBackendPremiumStatus() }
            } catch (e: Exception) {
                Log.w("PremiumActivity", "⚠️ No se pudo activar premium: ${e.message}")
            }
        }
    }

    private fun showLoading() {
        binding.progressIndicator.visibility = View.VISIBLE
        binding.purchaseButton.isEnabled = false
        binding.restorePurchasesButton.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressIndicator.visibility = View.GONE
        binding.restorePurchasesButton.isEnabled = true
        refreshUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
    }

    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher, R.mipmap.ic_launcher_one, R.mipmap.ic_launcher_two,
        R.mipmap.ic_launcher_three, R.mipmap.ic_launcher_four, R.mipmap.ic_launcher_five,
        R.mipmap.ic_launcher_six, R.mipmap.ic_launcher_seven, R.mipmap.ic_launcher_eight,
        R.mipmap.ic_launcher_nine, R.mipmap.ic_launcher_ten, R.mipmap.ic_launcher_eleven
    )

    override fun getAppLauncherName() = getString(R.string.messages)
    override fun getRepositoryName() = "Messages"
}
