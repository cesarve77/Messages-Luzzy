package app.luzzy.network.models

data class RegisterDeviceRequest(
    val phone: String,
    val registrationToken: String
)

data class RegisterDeviceResponse(
    val token: String
)

data class Message(
    val from: String,
    val message: String,
    val timestamp: String
)

data class SendMessagesRequest(
    val from: String,
    val to: String,
    val messages: List<Message>
)

data class PremiumActivateRequest(
    val purchaseToken: String
)

data class PremiumActivateResponse(
    val ok: Boolean
)

data class BillingRestoreRequest(
    val purchaseToken: String
)

data class BillingRestoreResponse(
    val token: String,
    val phone: String
)

data class BillingRegisterRequest(
    val purchaseToken: String,
    val registrationToken: String? = null
)

data class AccountResponse(
    val email: String?,
    val hasPassword: Boolean
)

data class SetCredentialsRequest(
    val email: String,
    val password: String? = null
)
