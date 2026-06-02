package app.luzzy.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.goodwy.commons.extensions.getProperTextColor
import com.goodwy.commons.extensions.toast
import com.goodwy.commons.extensions.updateTextColors
import com.goodwy.commons.extensions.viewBinding
import app.luzzy.R
import app.luzzy.auth.GoogleAuthRepository
import app.luzzy.billing.BillingManager
import app.luzzy.billing.PremiumRepository
import app.luzzy.databinding.ActivityUserSettingsBinding
import app.luzzy.network.RetrofitClient
import app.luzzy.network.models.SetCredentialsRequest
import app.luzzy.utils.SharedPrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserSettingsActivity : SimpleActivity() {

    companion object {
        const val EXTRA_JUST_LOGGED_IN = "just_logged_in"
    }

    private val binding by viewBinding(ActivityUserSettingsBinding::inflate)
    private lateinit var premiumRepository: PremiumRepository
    private lateinit var billingManager: BillingManager
    private lateinit var googleAuthRepository: GoogleAuthRepository
    private var isPremium = false
    private var justLoggedIn = false
    private var accountHasPassword = false

    private val dp4 get() = (4 * resources.displayMetrics.density).toInt()
    private val dp8 get() = (8 * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        googleAuthRepository = GoogleAuthRepository(this)
        justLoggedIn = intent.getBooleanExtra(EXTRA_JUST_LOGGED_IN, false)

        if (!googleAuthRepository.isLoggedIn()) {
            if (!app.luzzy.BuildConfig.DEBUG) {
                startActivity(Intent(this, GoogleLoginActivity::class.java))
                finish()
                return
            }
        }

        googleAuthRepository.getToken()?.let { token ->
            if (token.isNotBlank()) SharedPrefsManager.saveGoogleAuthToken(this, token)
        }

        setContentView(binding.root)
        setupToolbar(binding.toolbar)
        setupEdgeToEdge(padBottomSystem = listOf(binding.nestedScrollView))

        premiumRepository = PremiumRepository(this)
        isPremium = premiumRepository.isPremium()

        billingManager = BillingManager(this, premiumRepository)
        billingManager.onPremiumStatusChanged = { premium ->
            runOnUiThread {
                isPremium = premium
                updatePremiumStatus(premium)
            }
        }
        billingManager.initialize()

        setupUI()
        loadConfiguration()
        checkCalendarStatus()
    }

    override fun onResume() {
        super.onResume()
        updateTextColors(binding.nestedScrollView)
        updateSessionButton()
        updateFieldsAccess()
        checkCalendarStatus()
    }

    /** El botón alterna entre "Iniciar sesión" y "Cerrar sesión" según el estado de login. */
    private fun updateSessionButton() {
        if (googleAuthRepository.isLoggedIn()) {
            binding.logoutButton.text = getString(R.string.logout_google)
            binding.logoutButton.setOnClickListener { showLogoutConfirmationDialog() }
        } else {
            binding.logoutButton.text = getString(R.string.login_button)
            binding.logoutButton.setOnClickListener {
                startActivity(Intent(this, GoogleLoginActivity::class.java))
            }
        }
    }

    private fun setupUI() {
        binding.saveButton.setOnClickListener { saveConfiguration() }
        binding.upgradePremiumButton.setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
        }
        updateSessionButton()
        binding.deleteAccountButton.setOnClickListener { showDeleteAccountDialog() }
        binding.calendarConnectButton.setOnClickListener { onCalendarButtonClick() }

        binding.notificacionesSilenciosasHolder.setOnClickListener {
            binding.notificacionesSilenciosasSwitch.toggle()
        }
        binding.autoRespuestaHolder.setOnClickListener {
            if (isPremium) binding.autoRespuestaSwitch.toggle()
        }
        binding.usarEmojisHolder.setOnClickListener {
            if (isPremium) binding.usarEmojisSwitch.toggle()
        }

        binding.agregarServicioBtn.setOnClickListener { addServicioRow() }
        binding.agregarDuracionBtn.setOnClickListener { addDuracionRow() }

        updatePremiumStatus(isPremium)
        updateFieldsAccess()
    }

    // ── Servicios inline ──────────────────────────────────────────────────────

    private fun addServicioRow(servicio: String = "", precio: String = "") {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp4 }
        }
        val textColor = getProperTextColor()
        val servicioInput = EditText(this).apply {
            setText(servicio); hint = getString(R.string.servicio_hint)
            setSingleLine(true); background = null
            setTextColor(textColor)
            setHintTextColor(textColor and 0x00FFFFFF or 0x66000000)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
        }
        val precioInput = EditText(this).apply {
            setText(precio); hint = getString(R.string.precio_hint)
            setSingleLine(true); background = null
            setTextColor(textColor)
            setHintTextColor(textColor and 0x00FFFFFF or 0x66000000)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = dp8
            }
        }
        val deleteBtn = android.widget.ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = dp8 }
            setOnClickListener { binding.serviciosContainer.removeView(row) }
        }
        row.addView(servicioInput); row.addView(precioInput); row.addView(deleteBtn)
        binding.serviciosContainer.addView(row)
    }

    // ── Duraciones inline ─────────────────────────────────────────────────────

    private fun addDuracionRow(duracion: String = "", precio: String = "") {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp4 }
        }
        val textColor = getProperTextColor()
        val duracionInput = EditText(this).apply {
            setText(duracion); hint = getString(R.string.duracion_hint)
            setSingleLine(true); background = null
            setTextColor(textColor)
            setHintTextColor(textColor and 0x00FFFFFF or 0x66000000)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
        }
        val precioInput = EditText(this).apply {
            setText(precio); hint = getString(R.string.precio_hint)
            setSingleLine(true); background = null
            setTextColor(textColor)
            setHintTextColor(textColor and 0x00FFFFFF or 0x66000000)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = dp8
            }
        }
        val deleteBtn = android.widget.ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = dp8 }
            setOnClickListener { binding.duracionesContainer.removeView(row) }
        }
        row.addView(duracionInput); row.addView(precioInput); row.addView(deleteBtn)
        binding.duracionesContainer.addView(row)
    }

    // ── Carga del servidor ────────────────────────────────────────────────────

    private fun loadConfiguration() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val authHeader = googleAuthRepository.getAuthHeader()
                if (authHeader == null) {
                    android.util.Log.w("UserSettings", "loadConfiguration: no authHeader available, skipping")
                    return@launch
                }
                val response = RetrofitClient.apiService.getSettings(authHeader)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val notifDisabled = body["notificaciones_silenciosas"] as? Boolean ?: false
                    SharedPrefsManager.setNotificationsDisabled(this@UserSettingsActivity, notifDisabled)
                    val sub = body["subscription"] as? Map<*, *>
                    val serverPremium = sub?.get("status")?.toString() == "active" &&
                        sub["plan"]?.toString() == "pro"
                    if (serverPremium && !isPremium) {
                        isPremium = true
                        runOnUiThread { updatePremiumStatus(true) }
                    }
                    runOnUiThread { updateUI(body) }
                } else {
                    android.util.Log.w("UserSettings", "loadConfiguration: HTTP ${response.code()}")
                }
                loadAccountCredentials(authHeader)
            } catch (e: Exception) {
                android.util.Log.e("UserSettings", "loadConfiguration error: ${e.message}", e)
            } finally {
                setLoading(false)
            }
        }
    }

    /** Carga el email web configurado (si existe) para precargar la sección Acceso web. */
    private suspend fun loadAccountCredentials(authHeader: String) {
        try {
            val resp = RetrofitClient.apiService.getAccount(authHeader)
            if (resp.isSuccessful && resp.body() != null) {
                val body = resp.body()!!
                accountHasPassword = body.hasPassword
                withContext(Dispatchers.Main) {
                    if (!body.email.isNullOrBlank()) binding.emailInput.setText(body.email)
                    if (accountHasPassword) {
                        binding.passwordInput.hint = getString(R.string.password_hint_existente)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("UserSettings", "getAccount error: ${e.message}")
        }
    }

    private fun updateUI(settings: Map<String, Any>) {
        binding.nombreUsuarioInput.setText(settings["nombre_usuario"]?.toString() ?: "")
        binding.notificacionesSilenciosasSwitch.isChecked =
            settings["notificaciones_silenciosas"] as? Boolean ?: false
        binding.autoRespuestaSwitch.isChecked = settings["auto_respuesta_activada"] as? Boolean ?: false
        binding.usarEmojisSwitch.isChecked = settings["usar_emojis"] as? Boolean ?: true

        // Servicios
        binding.serviciosContainer.removeAllViews()
        val servicios = settings["servicios"] as? List<*>
        if (!servicios.isNullOrEmpty()) {
            servicios.forEach { item ->
                val map = item as? Map<*, *> ?: return@forEach
                val s = map["servicio"]?.toString() ?: ""
                val p = map["precio"]?.toString() ?: ""
                if (s.isNotEmpty()) addServicioRow(s, p)
            }
        } else {
            addServicioRow()
        }

        // Duraciones
        binding.duracionesContainer.removeAllViews()
        val duraciones = settings["duraciones"] as? List<*>
        if (!duraciones.isNullOrEmpty()) {
            duraciones.forEach { item ->
                val map = item as? Map<*, *> ?: return@forEach
                val d = map["duracion"]?.toString() ?: ""
                val p = map["precio"]?.toString() ?: ""
                if (d.isNotEmpty()) addDuracionRow(d, p)
            }
        } else {
            addDuracionRow()
        }

        binding.lugarTrabajoInput.setText(settings["lugar_trabajo"]?.toString() ?: "")
        binding.infoGeneralInput.setText(settings["info_general"]?.toString() ?: "")
        binding.adnIaInput.setText(settings["adn"]?.toString() ?: "")

        updateFieldsAccess()
    }

    // ── Premium ───────────────────────────────────────────────────────────────

    private fun updatePremiumStatus(premium: Boolean) {
        binding.premiumBanner.visibility = if (premium) View.GONE else View.VISIBLE
    }

    /** Los campos de configuración solo se pueden editar cuando hay sesión iniciada. */
    private fun updateFieldsAccess() {
        val enabled = googleAuthRepository.isLoggedIn()
        val alpha = if (enabled) 1.0f else 0.5f

        binding.notificacionesSilenciosasHolder.isEnabled = enabled
        binding.notificacionesSilenciosasSwitch.isEnabled = enabled
        binding.autoRespuestaHolder.isEnabled = enabled
        binding.autoRespuestaSwitch.isEnabled = enabled
        binding.usarEmojisHolder.isEnabled = enabled
        binding.usarEmojisSwitch.isEnabled = enabled
        binding.nombreUsuarioInput.isEnabled = enabled
        binding.lugarTrabajoInput.isEnabled = enabled
        binding.infoGeneralInput.isEnabled = enabled
        binding.adnIaInput.isEnabled = enabled
        binding.emailInput.isEnabled = enabled
        binding.passwordInput.isEnabled = enabled
        binding.agregarServicioBtn.isEnabled = enabled
        binding.agregarDuracionBtn.isEnabled = enabled
        binding.calendarConnectButton.isEnabled = enabled
        binding.saveButton.isEnabled = enabled
        binding.deleteAccountButton.isEnabled = enabled

        setRowsEnabled(binding.serviciosContainer, enabled)
        setRowsEnabled(binding.duracionesContainer, enabled)

        binding.perfilProfesionalTitle.alpha = alpha
        binding.perfilCard.alpha = alpha
    }

    /** Habilita/deshabilita los campos de las filas dinámicas de servicios y duraciones. */
    private fun setRowsEnabled(container: LinearLayout, enabled: Boolean) {
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i) as? LinearLayout ?: continue
            for (j in 0 until row.childCount) {
                row.getChildAt(j).isEnabled = enabled
            }
        }
    }

    // ── Guardar ───────────────────────────────────────────────────────────────

    private fun saveConfiguration() {
        android.util.Log.d("UserSettings", "saveConfiguration start, isLoggedIn=${googleAuthRepository.isLoggedIn()}")
        setLoading(true)
        val settings = mutableMapOf<String, Any>()

        val nombre = binding.nombreUsuarioInput.text.toString().trim()
        if (nombre.isNotBlank()) settings["nombre_usuario"] = nombre

        val notifDisabled = binding.notificacionesSilenciosasSwitch.isChecked
        settings["notificaciones_silenciosas"] = notifDisabled
        SharedPrefsManager.setNotificationsDisabled(this, notifDisabled)

        if (googleAuthRepository.isLoggedIn()) {
            settings["auto_respuesta_activada"] = binding.autoRespuestaSwitch.isChecked
            settings["usar_emojis"] = binding.usarEmojisSwitch.isChecked

            // Recoger servicios del contenedor inline
            val servicios = mutableListOf<Map<String, String>>()
            for (i in 0 until binding.serviciosContainer.childCount) {
                val row = binding.serviciosContainer.getChildAt(i) as? LinearLayout ?: continue
                val s = (row.getChildAt(0) as? EditText)?.text?.toString()?.trim() ?: ""
                val p = (row.getChildAt(1) as? EditText)?.text?.toString()?.trim() ?: ""
                if (s.isNotEmpty()) servicios.add(mapOf("servicio" to s, "precio" to p))
            }
            settings["servicios"] = servicios

            // Recoger duraciones del contenedor inline
            val duraciones = mutableListOf<Map<String, String>>()
            for (i in 0 until binding.duracionesContainer.childCount) {
                val row = binding.duracionesContainer.getChildAt(i) as? LinearLayout ?: continue
                val d = (row.getChildAt(0) as? EditText)?.text?.toString()?.trim() ?: ""
                val p = (row.getChildAt(1) as? EditText)?.text?.toString()?.trim() ?: ""
                if (d.isNotEmpty()) duraciones.add(mapOf("duracion" to d, "precio" to p))
            }
            settings["duraciones"] = duraciones

            val lugarTrabajo = binding.lugarTrabajoInput.text.toString().trim()
            if (lugarTrabajo.isBlank()) {
                toast(R.string.lugar_trabajo_required); setLoading(false); return
            }
            settings["lugar_trabajo"] = lugarTrabajo

            val infoGeneral = binding.infoGeneralInput.text.toString().trim()
            if (infoGeneral.isNotBlank()) settings["info_general"] = infoGeneral

            val adnIa = binding.adnIaInput.text.toString().trim()
            if (adnIa.isNotBlank()) settings["adn"] = adnIa
        }

        lifecycleScope.launch {
            try {
                val authHeader = googleAuthRepository.getAuthHeader()
                android.util.Log.d("UserSettings", "saveConfiguration authHeader=${if (authHeader != null) "present" else "null"}")
                if (authHeader != null) {
                    val response = RetrofitClient.apiService.updateSettings(authHeader, settings)
                    android.util.Log.d("UserSettings", "saveConfiguration response=${response.code()} success=${response.isSuccessful}")
                    if (!response.isSuccessful) {
                        android.util.Log.e("UserSettings", "saveConfiguration HTTP error: ${response.code()} ${response.errorBody()?.string()}")
                        toast(R.string.error_saving_settings)
                        return@launch
                    }
                    // Guardar credenciales web si el usuario llenó el email
                    if (!saveWebCredentials(authHeader)) return@launch
                    toast(R.string.settings_saved)
                    finish()
                } else {
                    toast(R.string.login_required)
                }
            } catch (e: Exception) {
                android.util.Log.e("UserSettings", "saveConfiguration exception: ${e.javaClass.simpleName}: ${e.message}", e)
                toast(R.string.error_saving_settings)
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Guarda email + contraseña web si el usuario llenó el campo email.
     * Devuelve true si todo fue bien (o si no había nada que guardar), false si hubo error.
     */
    private suspend fun saveWebCredentials(authHeader: String): Boolean {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()

        // Sin email → el usuario no configuró acceso web
        if (email.isEmpty()) return true

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast(R.string.credentials_email_invalido)
            return false
        }
        // Contraseña requerida la primera vez; opcional si la cuenta ya tenía una
        if (password.isEmpty() && !accountHasPassword) {
            toast(R.string.credentials_password_corta)
            return false
        }
        if (password.isNotEmpty() && password.length < 6) {
            toast(R.string.credentials_password_corta)
            return false
        }

        return try {
            val resp = RetrofitClient.apiService.setCredentials(
                authHeader,
                SetCredentialsRequest(email, password.ifEmpty { null })
            )
            when {
                resp.isSuccessful -> { accountHasPassword = true; true }
                resp.code() == 409 -> { toast(R.string.credentials_email_en_uso); false }
                else -> { toast(R.string.credentials_error); false }
            }
        } catch (e: Exception) {
            android.util.Log.e("UserSettings", "setCredentials error: ${e.message}", e)
            toast(R.string.credentials_error)
            false
        }
    }

    // ── Diálogos ──────────────────────────────────────────────────────────────

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_confirmation_title)
            .setMessage(R.string.logout_confirmation_message_detail)
            .setPositiveButton(R.string.logout) { _, _ -> performLogout() }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun performLogout() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val success = googleAuthRepository.logout(revokeAccess = false)
                toast(if (success) R.string.google_logout_success else R.string.logout_failed)
                if (success) {
                    startActivity(Intent(this@UserSettingsActivity, GoogleLoginActivity::class.java))
                    finish()
                }
            } catch (_: Exception) { toast(R.string.logout_failed) }
            finally { setLoading(false) }
        }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_account_confirm_title)
            .setMessage(R.string.delete_account_confirm_msg)
            .setPositiveButton(R.string.delete_account) { _, _ -> deleteAccount() }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun deleteAccount() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val authHeader = googleAuthRepository.getAuthHeader() ?: return@launch
                val response = RetrofitClient.apiService.deleteAccount(authHeader)
                if (response.isSuccessful) {
                    SharedPrefsManager.clearAll(this@UserSettingsActivity)
                    toast(R.string.delete_account_success)
                    startActivity(Intent(this@UserSettingsActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                } else toast(R.string.delete_account_error)
            } catch (_: Exception) { toast(R.string.delete_account_error) }
            finally { setLoading(false) }
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private fun setLoading(loading: Boolean) {
        binding.progressIndicator.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            binding.saveButton.isEnabled = false
        } else {
            updateFieldsAccess()
        }
    }

    private var calendarConnected = false

    private fun checkCalendarStatus() {
        lifecycleScope.launch {
            try {
                val authHeader = googleAuthRepository.getAuthHeader() ?: return@launch
                val response = RetrofitClient.apiService.getCalendarStatus(authHeader)
                if (response.isSuccessful) {
                    calendarConnected = response.body()?.get("connected") as? Boolean ?: false
                    updateCalendarUI(calendarConnected)
                    if (justLoggedIn && !calendarConnected) { justLoggedIn = false; connectCalendar() }
                }
            } catch (_: Exception) {}
        }
    }

    private fun updateCalendarUI(connected: Boolean) {
        binding.calendarStatusText.text =
            getString(if (connected) R.string.calendar_connected else R.string.calendar_not_connected)
        binding.calendarConnectButton.text =
            getString(if (connected) R.string.calendar_disconnect else R.string.calendar_connect)
    }

    private fun onCalendarButtonClick() { if (calendarConnected) disconnectCalendar() else connectCalendar() }

    private fun connectCalendar() {
        lifecycleScope.launch {
            try {
                val authHeader = googleAuthRepository.getAuthHeader() ?: return@launch
                val url = RetrofitClient.apiService.getCalendarConnectUrl(authHeader).body()?.get("url") ?: return@launch
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) { toast(R.string.calendar_disconnect_error) }
        }
    }

    private fun disconnectCalendar() {
        lifecycleScope.launch {
            try {
                val authHeader = googleAuthRepository.getAuthHeader() ?: return@launch
                RetrofitClient.apiService.disconnectCalendar(authHeader)
                calendarConnected = false; updateCalendarUI(false)
            } catch (_: Exception) { toast(R.string.calendar_disconnect_error) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::billingManager.isInitialized) billingManager.destroy()
    }
}
