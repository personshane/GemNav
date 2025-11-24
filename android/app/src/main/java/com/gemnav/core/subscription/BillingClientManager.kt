package com.gemnav.core.subscription

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * BillingClientManager - Manages Google Play Billing integration.
 * Handles subscription purchases, queries, and entitlement verification.
 */
object BillingClientManager : PurchasesUpdatedListener {
    
    private const val TAG = "BillingClientManager"
    
    private var billingClient: BillingClient? = null
    private var isConnected = false
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _availableProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableProducts: StateFlow<List<ProductDetails>> = _availableProducts
    
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
    
    /**
     * Initialize and connect to Google Play Billing.
     */
    fun initialize(context: Context) {
        if (billingClient != null) {
            Log.d(TAG, "BillingClient already initialized")
            return
        }
        
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        startConnection()
    }
    
    /**
     * Start connection to Google Play.
     */
    fun startConnection() {
        val client = billingClient ?: return
        
        if (isConnected) {
            Log.d(TAG, "Already connected to billing")
            return
        }
        
        _connectionState.value = ConnectionState.CONNECTING
        
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    _connectionState.value = ConnectionState.CONNECTED
                    Log.i(TAG, "BillingClient connected successfully")
                    
                    // Query existing purchases
                    queryPurchases()
                    
                    // Query available products
                    queryProductDetails()
                } else {
                    _connectionState.value = ConnectionState.ERROR
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }
            
            override fun onBillingServiceDisconnected() {
                isConnected = false
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.w(TAG, "BillingClient disconnected")
                
                // TODO: Implement retry logic with exponential backoff
            }
        })
    }
    
    /**
     * Query existing purchases to verify entitlements.
     */
    fun queryPurchases() {
        val client = billingClient ?: return
        
        if (!isConnected) {
            Log.w(TAG, "Cannot query purchases - not connected")
            return
        }
        
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        
        client.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            } else {
                Log.e(TAG, "Query purchases failed: ${billingResult.debugMessage}")
            }
        }
    }
    
    /**
     * Query available subscription products.
     */
    private fun queryProductDetails() {
        val client = billingClient ?: return
        
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Tier.SKU_PLUS_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Tier.SKU_PLUS_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Tier.SKU_PRO_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(Tier.SKU_PRO_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _availableProducts.value = productDetailsList
                Log.d(TAG, "Loaded ${productDetailsList.size} products")
            } else {
                Log.e(TAG, "Query products failed: ${billingResult.debugMessage}")
            }
        }
    }
    
    /**
     * Launch purchase flow for a subscription.
     */
    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails): BillingResult? {
        val client = billingClient ?: return null
        
        if (!isConnected) {
            Log.w(TAG, "Cannot purchase - not connected")
            return null
        }
        
        // Get subscription offer
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "No offer token available for ${productDetails.productId}")
            return null
        }
        
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        
        return client.launchBillingFlow(activity, billingFlowParams)
    }
    
    /**
     * Launch purchase flow by SKU ID.
     * TODO: Implement for simpler API usage
     */
    fun purchaseFlow(activity: Activity, skuId: String) {
        val product = _availableProducts.value.find { it.productId == skuId }
        if (product != null) {
            launchPurchaseFlow(activity, product)
        } else {
            Log.e(TAG, "Product not found: $skuId")
            // TODO: Show error to user
        }
    }
    
    /**
     * Handle purchases update callback from Google Play.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { handlePurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                // TODO: Show error UI
            }
        }
    }
    
    /**
     * Process and verify purchases.
     */
    private fun handlePurchases(purchases: List<Purchase>) {
        if (purchases.isEmpty()) {
            Log.d(TAG, "No active purchases found")
            TierManager.updateTier(Tier.FREE)
            return
        }
        
        var highestTier = Tier.FREE
        
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Acknowledge purchase if not already done
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                
                // Determine tier from product IDs
                for (productId in purchase.products) {
                    val tier = Tier.fromSku(productId)
                    if (tier > highestTier) {
                        highestTier = tier
                    }
                }
            }
        }
        
        TierManager.updateTier(highestTier)
        Log.i(TAG, "Purchases processed, tier set to: $highestTier")
    }
    
    /**
     * Acknowledge a purchase (required by Google Play).
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val client = billingClient ?: return
        
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        CoroutineScope(Dispatchers.IO).launch {
            val result = client.acknowledgePurchase(params)
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged: ${purchase.orderId}")
            } else {
                Log.e(TAG, "Failed to acknowledge: ${result.debugMessage}")
            }
        }
    }
    
    /**
     * End billing connection.
     */
    fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
        isConnected = false
        _connectionState.value = ConnectionState.DISCONNECTED
        Log.d(TAG, "BillingClient connection ended")
    }
    
    // ============================================================
    // TODO: Future enhancements
    // ============================================================
    // - Implement retry logic for connection failures
    // - Add intro pricing / free trial handling
    // - Implement subscription upgrade/downgrade flows
    // - Add proration mode support
    // - Implement grace period handling
    // - Add account hold state handling
    // - Server-side purchase verification
}
