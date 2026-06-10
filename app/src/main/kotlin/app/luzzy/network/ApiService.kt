package app.luzzy.network

import app.luzzy.network.models.GoogleLoginRequest
import app.luzzy.network.models.GoogleLoginResponse
import app.luzzy.network.models.PremiumActivateRequest
import app.luzzy.network.models.PremiumActivateResponse
import app.luzzy.network.models.BillingRestoreRequest
import app.luzzy.network.models.BillingRestoreResponse
import app.luzzy.network.models.BillingRegisterRequest
import app.luzzy.network.models.AccountResponse
import app.luzzy.network.models.SetCredentialsRequest
import app.luzzy.network.models.RegisterDeviceRequest
import app.luzzy.network.models.RegisterDeviceResponse
import app.luzzy.network.models.SendMessagesRequest
import app.luzzy.network.models.ContactsSyncRequest
import app.luzzy.network.models.PhoneLoginRequest
import app.luzzy.network.models.PhoneLoginResponse
import app.luzzy.network.models.WebAuthResponse
import app.luzzy.network.models.WebLoginRequest
import app.luzzy.network.models.WebRegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.DELETE
import retrofit2.http.POST

interface ApiService {

    @POST("register")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): Response<RegisterDeviceResponse>

    @POST("auth/google-login")
    suspend fun googleLogin(
        @Body request: GoogleLoginRequest
    ): Response<GoogleLoginResponse>

    @POST("auth/web-login")
    suspend fun webLogin(
        @Body request: WebLoginRequest
    ): Response<WebAuthResponse>

    @POST("auth/web-register")
    suspend fun webRegister(
        @Body request: WebRegisterRequest
    ): Response<WebAuthResponse>

    @GET("settings")
    suspend fun getSettings(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @POST("settings")
    suspend fun updateSettings(
        @Header("Authorization") token: String,
        @Body settings: @JvmSuppressWildcards Map<String, Any>
    ): Response<String>

    @POST("messages")
    suspend fun sendMessages(
        @Header("Authorization") token: String,
        @Body request: SendMessagesRequest
    ): Response<String>

    @POST("contacts/sync")
    suspend fun syncContacts(
        @Header("Authorization") token: String,
        @Body request: ContactsSyncRequest
    ): Response<String>

    @DELETE("account")
    suspend fun deleteAccount(
        @Header("Authorization") token: String
    ): Response<String>

    @GET("auth/google-calendar/connect")
    suspend fun getCalendarConnectUrl(
        @Header("Authorization") token: String
    ): Response<Map<String, String>>

    @GET("calendar/status")
    suspend fun getCalendarStatus(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @POST("calendar/disconnect")
    suspend fun disconnectCalendar(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @POST("premium/activate")
    suspend fun activatePremium(
        @Header("Authorization") token: String,
        @Body request: PremiumActivateRequest
    ): Response<PremiumActivateResponse>

    @POST("auth/phone-login")
    suspend fun phoneLogin(
        @Body request: PhoneLoginRequest
    ): Response<PhoneLoginResponse>

    @POST("auth/billing-restore")
    suspend fun billingRestore(
        @Body request: BillingRestoreRequest
    ): Response<BillingRestoreResponse>

    @POST("auth/billing-register")
    suspend fun billingRegister(
        @Body request: BillingRegisterRequest
    ): Response<BillingRestoreResponse>

    @GET("premium/status")
    suspend fun getPremiumStatus(
        @Header("Authorization") token: String
    ): Response<Map<String, Boolean>>

    @GET("account")
    suspend fun getAccount(
        @Header("Authorization") token: String
    ): Response<AccountResponse>

    @POST("account/credentials")
    suspend fun setCredentials(
        @Header("Authorization") token: String,
        @Body request: SetCredentialsRequest
    ): Response<Map<String, Any>>
}
