package app.luzzy.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    private val context: Context,
    private val premiumRepository: PremiumRepository
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
    }

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null

    var onPremiumStatusChanged: ((Boolean) -> Unit)? = null
    var onPurchaseError: ((String) -> Unit)? = null
    var onProductPriceLoaded: ((String) -> Unit)? = null
    var onPurchaseSucceeded: ((purchaseToken: String) -> Unit)? = null

    fun initialize() {
        Log.d(TAG, "🔧 Inicializando BillingClient...")

        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✓ BillingClient conectado exitosamente")
                    queryProductDetails()
                    queryPurchases()
                } else {
                    Log.e(TAG, "✗ Error al conectar BillingClient: ${billingResult.debugMessage}")
                    onPurchaseError?.invoke("Error al conectar con Play Store: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "⚠️ BillingClient desconectado, reintentando...")
            }
        })
    }

    private fun queryProductDetails() {
        Log.d(TAG, "📦 Consultando detalles del producto...")

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_PREMIUM)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val productDetailsResult = withContext(Dispatchers.IO) {
                    billingClient?.queryProductDetails(params)
                }

                withContext(Dispatchers.Main) {
                    if (productDetailsResult?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                        productDetails = productDetailsResult.productDetailsList?.firstOrNull()

                        if (productDetails != null) {
                            val offerDetails = productDetails?.subscriptionOfferDetails?.firstOrNull()
                            val pricingPhase = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
                            val price = pricingPhase?.formattedPrice ?: "N/A"

                            Log.d(TAG, "✓ Producto encontrado: ${BillingConstants.PRODUCT_PREMIUM}")
                            Log.d(TAG, "💰 Precio: $price")

                            onProductPriceLoaded?.invoke(price)
                        } else {
                            Log.w(TAG, "⚠️ Producto no encontrado: ${BillingConstants.PRODUCT_PREMIUM}")
                            onPurchaseError?.invoke("Producto no disponible en Play Store")
                        }
                    } else {
                        Log.e(TAG, "✗ Error al consultar productos: ${productDetailsResult?.billingResult?.debugMessage}")
                        onPurchaseError?.invoke("Error al cargar productos")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al consultar productos", e)
                withContext(Dispatchers.Main) {
                    onPurchaseError?.invoke("Error: ${e.message}")
                }
            }
        }
    }

    private fun queryPurchases() {
        Log.d(TAG, "🔍 Consultando compras existentes...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val purchasesResult = withContext(Dispatchers.IO) {
                    billingClient?.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                }

                withContext(Dispatchers.Main) {
                    if (purchasesResult?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                        val purchases = purchasesResult.purchasesList

                        if (purchases.isNotEmpty()) {
                            Log.d(TAG, "✓ Se encontraron ${purchases.size} compra(s)")
                            handlePurchases(purchases)
                        } else {
                            Log.d(TAG, "ℹ️ No hay compras activas")
                            if (premiumRepository.isPremium()) {
                                Log.w(TAG, "⚠️ Usuario marcado como premium pero sin compra activa, limpiando...")
                                premiumRepository.setPremium(false)
                                onPremiumStatusChanged?.invoke(false)
                            }
                        }
                    } else {
                        Log.e(TAG, "✗ Error al consultar compras: ${purchasesResult?.billingResult?.debugMessage}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al consultar compras", e)
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        if (productDetails == null) {
            Log.e(TAG, "✗ No se puede iniciar compra: producto no cargado")
            onPurchaseError?.invoke("Producto no disponible")
            return
        }

        val offerToken = productDetails?.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "✗ No se encontró offerToken para el producto")
            onPurchaseError?.invoke("Error al cargar planes de suscripción")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails!!)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        Log.d(TAG, "🚀 Iniciando flujo de compra...")
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)

        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "✗ Error al iniciar compra: ${billingResult?.debugMessage}")
            onPurchaseError?.invoke("Error al iniciar compra")
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    Log.d(TAG, "✓ Compra exitosa, procesando...")
                    handlePurchases(purchases)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "ℹ️ Usuario canceló la compra")
                onPurchaseError?.invoke("Compra cancelada")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "ℹ️ El usuario ya posee este producto")
                queryPurchases() // Verificar y restaurar
            }
            else -> {
                Log.e(TAG, "✗ Error en compra: ${billingResult.debugMessage}")
                onPurchaseError?.invoke("Error: ${billingResult.debugMessage}")
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.products.contains(BillingConstants.PRODUCT_PREMIUM)) {
                when (purchase.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> {
                        if (!purchase.isAcknowledged) {
                            acknowledgePurchase(purchase)
                        } else {
                            Log.d(TAG, "✓ Compra ya reconocida anteriormente")
                            premiumRepository.setPremium(true, purchase.purchaseToken)
                            onPremiumStatusChanged?.invoke(true)
                            onPurchaseSucceeded?.invoke(purchase.purchaseToken)
                        }
                    }
                    Purchase.PurchaseState.PENDING -> {
                        Log.d(TAG, "⏳ Compra pendiente de confirmación")
                        onPurchaseError?.invoke("Compra pendiente de confirmación")
                    }
                    else -> {
                        Log.w(TAG, "⚠️ Estado de compra desconocido: ${purchase.purchaseState}")
                    }
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        Log.d(TAG, "✅ Reconociendo compra...")

        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams)
                }

                withContext(Dispatchers.Main) {
                    if (result?.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "✓ Compra reconocida exitosamente")
                        premiumRepository.setPremium(true, purchase.purchaseToken)
                        onPremiumStatusChanged?.invoke(true)
                        onPurchaseSucceeded?.invoke(purchase.purchaseToken)
                    } else {
                        Log.e(TAG, "✗ Error al reconocer compra: ${result?.debugMessage}")
                        onPurchaseError?.invoke("Error al procesar compra")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción al reconocer compra", e)
                withContext(Dispatchers.Main) {
                    onPurchaseError?.invoke("Error: ${e.message}")
                }
            }
        }
    }

    fun isPremium(): Boolean {
        return premiumRepository.isPremium()
    }

    fun destroy() {
        Log.d(TAG, "🔌 Desconectando BillingClient...")
        billingClient?.endConnection()
        billingClient = null
    }

    // Alias para compatibilidad
    fun endConnection() = destroy()
    fun startPurchaseFlow(activity: Activity) = launchPurchaseFlow(activity)
}
