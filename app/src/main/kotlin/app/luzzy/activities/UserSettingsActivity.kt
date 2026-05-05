package app.luzzy.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.goodwy.commons.extensions.toast
import com.goodwy.commons.extensions.viewBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import app.luzzy.R
import app.luzzy.auth.GoogleAuthRepository
import app.luzzy.billing.PremiumRepository
import app.luzzy.databinding.ActivityUserSettingsBinding
import app.luzzy.network.RetrofitClient
import app.luzzy.utils.SharedPrefsManager
import kotlinx.coroutines.launch

class UserSettingsActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityUserSettingsBinding::inflate)
    private lateinit var premiumRepository: PremiumRepository
    private lateinit var googleAuthRepository: GoogleAuthRepository
    private var isPremium = false

    private val googleLoginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            toast(R.string.google_login_success)
            updateGoogleLoginUI()
            loadConfiguration()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupToolbar(binding.toolbar)
        setupEdgeToEdge(padBottomSystem = listOf(binding.nestedScrollView))

        premiumRepository = PremiumRepository(this)
        googleAuthRepository = GoogleAuthRepository(this)
        isPremium = premiumRepository.isPremium()

        updateGoogleLoginUI()
        loadConfiguration()
        setupUI()
    }

    private fun setupUI() {
        binding.saveButton.setOnClickListener { saveConfiguration() }
        binding.upgradePremiumButton.setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
        }
        binding.googleLoginButton.setOnClickListener {
            googleLoginLauncher.launch(Intent(this, GoogleLoginActivity::class.java))
        }
        binding.googleLogoutButton.setOnClickListener { showLogoutConfirmationDialog() }
        binding.agregarServicioBtn.setOnClickListener { addServiceRow() }
        binding.agregarDuracionBtn.setOnClickListener { addDuracionRow() }
        binding.deleteAccountButton.setOnClickListener { showDeleteAccountDialog() }

        updatePremiumStatus(isPremium)
    }

    private fun addServiceRow(servicio: String = "", precio: String = "") {
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp8 }
        }

        val servicioLayout = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = getString(R.string.servicio_hint)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
        }
        val servicioInput = TextInputEditText(servicioLayout.context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            maxLines = 1
            setText(servicio)
        }
        servicioLayout.addView(servicioInput)

        val precioLayout = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = getString(R.string.precio_hint)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = dp8
            }
        }
        val precioInput = TextInputEditText(precioLayout.context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            maxLines = 1
            setText(precio)
        }
        precioLayout.addView(precioInput)

        val deleteBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.gravity = android.view.Gravity.CENTER_VERTICAL
                it.marginStart = dp8
            }
            contentDescription = "Eliminar servicio"
            setOnClickListener { binding.serviciosContainer.removeView(row) }
        }

        row.addView(servicioLayout)
        row.addView(precioLayout)
        row.addView(deleteBtn)
        binding.serviciosContainer.addView(row)
    }

    private fun addDuracionRow(duracion: String = "", precio: String = "") {
        val dp8 = (8 * resources.displayMetrics.density).toInt()
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp8 }
        }

        val duracionLayout = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = getString(R.string.duracion_hint)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
        }
        val duracionInput = TextInputEditText(duracionLayout.context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            maxLines = 1
            setText(duracion)
        }
        duracionLayout.addView(duracionInput)

        val precioLayout = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            hint = getString(R.string.precio_hint)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = dp8
            }
        }
        val precioInput = TextInputEditText(precioLayout.context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            maxLines = 1
            setText(precio)
        }
        precioLayout.addView(precioInput)

        val deleteBtn = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                it.gravity = android.view.Gravity.CENTER_VERTICAL
                it.marginStart = dp8
            }
            contentDescription = "Eliminar duración"
            setOnClickListener { binding.duracionesContainer.removeView(row) }
        }

        row.addView(duracionLayout)
        row.addView(precioLayout)
        row.addView(deleteBtn)
        binding.duracionesContainer.addView(row)
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message_detail)
            .setPositiveButton(R.string.logout) { _, _ -> performLogout() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun performLogout() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val success = googleAuthRepository.logout(revokeAccess = false)
                toast(if (success) R.string.google_logout_success else R.string.logout_failed)
                updateGoogleLoginUI()
            } catch (e: Exception) {
                toast(R.string.logout_failed)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updateGoogleLoginUI() {
        val isLoggedIn = googleAuthRepository.isLoggedIn()
        binding.googleLoggedInLayout.visibility  = if (isLoggedIn) View.VISIBLE else View.GONE
        binding.googleLoggedOutLayout.visibility = if (isLoggedIn) View.GONE   else View.VISIBLE
        if (isLoggedIn) {
            val email = googleAuthRepository.getUserEmail() ?: ""
            binding.googleAccountEmail.text = getString(R.string.logged_in_with_google, email)
        }
    }

    private fun loadConfiguration() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val authHeader = SharedPrefsManager.getAuthHeader(this@UserSettingsActivity)
                if (authHeader != null) {
                    val response = RetrofitClient.apiService.getSettings(authHeader)
                    if (response.isSuccessful && response.body() != null) {
                        updateUI(response.body()!!)
                    }
                }
            } catch (e: Exception) {
                // silencioso — la UI queda en blanco si no hay conexión
            } finally {
                setLoading(false)
            }
        }
    }

    private fun updateUI(settings: Map<String, Any>) {
        // Campos generales (sincronizados con web)
        binding.nombreUsuarioInput.setText(settings["nombre_usuario"]?.toString() ?: "")

        // Campos locales del dispositivo
        binding.notificacionesSilenciosasSwitch.isChecked = settings["notificaciones_silenciosas"] as? Boolean ?: false
        binding.modoOscuroSwitch.isChecked               = settings["modo_oscuro"] as? Boolean ?: false

        // Campos premium (sincronizados con web)
        binding.autoRespuestaSwitch.isChecked = settings["auto_respuesta_activada"] as? Boolean ?: false
        binding.usarEmojisSwitch.isChecked    = settings["usar_emojis"] as? Boolean ?: true

        // Perfil profesional
        binding.serviciosContainer.removeAllViews()
        val serviciosList = settings["servicios"] as? List<*>
        serviciosList?.forEach { item ->
            val map = item as? Map<*, *>
            addServiceRow(
                servicio = map?.get("servicio")?.toString() ?: "",
                precio   = map?.get("precio")?.toString()   ?: ""
            )
        }
        if (binding.serviciosContainer.childCount == 0) addServiceRow()

        binding.duracionesContainer.removeAllViews()
        val duracionesList = settings["duraciones"] as? List<*>
        duracionesList?.forEach { item ->
            val map = item as? Map<*, *>
            addDuracionRow(
                duracion = map?.get("duracion")?.toString() ?: "",
                precio   = map?.get("precio")?.toString()   ?: ""
            )
        }

        binding.lugarTrabajoInput.setText(settings["lugar_trabajo"]?.toString() ?: "")
        binding.infoGeneralInput.setText(settings["info_general"]?.toString() ?: "")
        binding.adnIaInput.setText(settings["adn"]?.toString() ?: "")
    }

    private fun updatePremiumStatus(premium: Boolean) {
        binding.premiumBanner.visibility = if (premium) View.GONE else View.VISIBLE
        enablePremiumFeatures(premium)
    }

    private fun enablePremiumFeatures(enabled: Boolean) {
        val alpha = if (enabled) 1.0f else 0.5f

        binding.autoRespuestaSwitch.isEnabled  = enabled
        binding.autoRespuestaSwitch.alpha      = alpha
        binding.usarEmojisSwitch.isEnabled     = enabled
        binding.usarEmojisSwitch.alpha         = alpha

        binding.agregarServicioBtn.isEnabled   = enabled
        binding.agregarServicioBtn.alpha       = alpha
        binding.serviciosContainer.alpha       = alpha
        binding.agregarDuracionBtn.isEnabled   = enabled
        binding.agregarDuracionBtn.alpha       = alpha
        binding.duracionesContainer.alpha      = alpha

        binding.perfilProfesionalTitle.alpha   = alpha
        binding.lugarTrabajoLayout.isEnabled   = enabled
        binding.lugarTrabajoLayout.alpha       = alpha
        binding.lugarTrabajoInput.isEnabled    = enabled
        binding.infoGeneralLayout.isEnabled    = enabled
        binding.infoGeneralLayout.alpha        = alpha
        binding.infoGeneralInput.isEnabled     = enabled
        binding.adnIaLayout.isEnabled          = enabled
        binding.adnIaLayout.alpha              = alpha
        binding.adnIaInput.isEnabled           = enabled
    }

    private fun saveConfiguration() {
        setLoading(true)

        val settings = mutableMapOf<String, Any>()

        // Generales
        val nombreUsuario = binding.nombreUsuarioInput.text.toString().trim()
        if (nombreUsuario.isNotBlank()) settings["nombre_usuario"] = nombreUsuario

        // Locales (solo dispositivo)
        settings["notificaciones_silenciosas"] = binding.notificacionesSilenciosasSwitch.isChecked
        settings["modo_oscuro"]                = binding.modoOscuroSwitch.isChecked

        // Premium — sincronizados con web
        if (isPremium) {
            settings["auto_respuesta_activada"] = binding.autoRespuestaSwitch.isChecked
            settings["usar_emojis"]             = binding.usarEmojisSwitch.isChecked

            val servicios = mutableListOf<Map<String, String>>()
            for (i in 0 until binding.serviciosContainer.childCount) {
                val row          = binding.serviciosContainer.getChildAt(i) as? LinearLayout ?: continue
                val servicioLayout = row.getChildAt(0) as? TextInputLayout
                val precioLayout   = row.getChildAt(1) as? TextInputLayout
                val nombre = (servicioLayout?.editText as? TextInputEditText)?.text?.toString()?.trim() ?: ""
                val precio = (precioLayout?.editText  as? TextInputEditText)?.text?.toString()?.trim() ?: ""
                if (nombre.isNotEmpty()) servicios.add(mapOf("servicio" to nombre, "precio" to precio))
            }
            settings["servicios"] = servicios

            val duraciones = mutableListOf<Map<String, String>>()
            for (i in 0 until binding.duracionesContainer.childCount) {
                val row          = binding.duracionesContainer.getChildAt(i) as? LinearLayout ?: continue
                val durLayout    = row.getChildAt(0) as? TextInputLayout
                val precioLayout = row.getChildAt(1) as? TextInputLayout
                val dur   = (durLayout?.editText  as? TextInputEditText)?.text?.toString()?.trim() ?: ""
                val precio = (precioLayout?.editText as? TextInputEditText)?.text?.toString()?.trim() ?: ""
                if (dur.isNotEmpty()) duraciones.add(mapOf("duracion" to dur, "precio" to precio))
            }
            settings["duraciones"] = duraciones

            val lugarTrabajo = binding.lugarTrabajoInput.text.toString().trim()
            if (lugarTrabajo.isBlank()) {
                binding.lugarTrabajoLayout.error = getString(R.string.lugar_trabajo_required)
                binding.lugarTrabajoInput.requestFocus()
                setLoading(false)
                return
            }
            binding.lugarTrabajoLayout.error = null
            settings["lugar_trabajo"] = lugarTrabajo

            val infoGeneral = binding.infoGeneralInput.text.toString().trim()
            if (infoGeneral.isNotBlank()) settings["info_general"] = infoGeneral

            val adn = binding.adnIaInput.text.toString().trim()
            if (adn.isNotBlank()) settings["adn"] = adn
        }

        lifecycleScope.launch {
            try {
                val authHeader = SharedPrefsManager.getAuthHeader(this@UserSettingsActivity)
                if (authHeader != null) {
                    val response = RetrofitClient.apiService.updateSettings(authHeader, settings)
                    if (response.isSuccessful) {
                        toast(R.string.settings_saved)
                        finish()
                    } else {
                        toast(R.string.error_saving_settings)
                    }
                } else {
                    toast(R.string.settings_saved)
                    finish()
                }
            } catch (e: Exception) {
                toast(R.string.error_saving_settings)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_account_confirm_title)
            .setMessage(R.string.delete_account_confirm_msg)
            .setPositiveButton(R.string.delete_account) { _, _ -> deleteAccount() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deleteAccount() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val authHeader = SharedPrefsManager.getAuthHeader(this@UserSettingsActivity)
                if (authHeader != null) {
                    val response = RetrofitClient.apiService.deleteAccount(authHeader)
                    if (response.isSuccessful) {
                        SharedPrefsManager.clearAll(this@UserSettingsActivity)
                        toast(R.string.delete_account_success)
                        val intent = Intent(this@UserSettingsActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        toast(R.string.delete_account_error)
                    }
                }
            } catch (e: Exception) {
                toast(R.string.delete_account_error)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressIndicator.visibility   = if (loading) View.VISIBLE else View.GONE
        binding.saveButton.isEnabled           = !loading
        binding.nombreUsuarioInput.isEnabled   = !loading
        binding.notificacionesSilenciosasSwitch.isEnabled = !loading
        binding.modoOscuroSwitch.isEnabled     = !loading
        if (!loading && isPremium) enablePremiumFeatures(true)
    }
}
