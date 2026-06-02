package app.luzzy.network.models

data class WebLoginRequest(
    val email: String,
    val password: String
)

data class WebRegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

data class WebAuthResponse(
    val token: String,
    val user: WebUserData
)

data class WebUserData(
    val email: String,
    val displayName: String?
)

data class PhoneLoginRequest(
    val idToken: String
)

data class PhoneLoginResponse(
    val token: String,
    val phone: String
)
