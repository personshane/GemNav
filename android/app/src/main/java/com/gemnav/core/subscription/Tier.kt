package com.gemnav.core.subscription

/**
 * Tier - Subscription tier enum for GemNav.
 * 
 * FREE: Gemini Nano (on-device), Maps intents only
 * PLUS: Cloud AI, Maps SDK in-app, advanced voice ($4.99/month)
 * PRO: All Plus features + HERE truck routing ($14.99/month)
 */
enum class Tier {
    FREE,
    PLUS,
    PRO;
    
    companion object {
        // Google Play SKU identifiers
        const val SKU_PLUS_MONTHLY = "gemnav_plus_monthly"
        const val SKU_PLUS_YEARLY = "gemnav_plus_yearly"
        const val SKU_PRO_MONTHLY = "gemnav_pro_monthly"
        const val SKU_PRO_YEARLY = "gemnav_pro_yearly"
        
        /**
         * Convert Google Play SKU to Tier.
         */
        fun fromSku(sku: String?): Tier {
            return when (sku) {
                SKU_PLUS_MONTHLY, SKU_PLUS_YEARLY -> PLUS
                SKU_PRO_MONTHLY, SKU_PRO_YEARLY -> PRO
                else -> FREE
            }
        }
        
        /**
         * Get display name for tier.
         */
        fun Tier.displayName(): String {
            return when (this) {
                FREE -> "Free"
                PLUS -> "Plus"
                PRO -> "Pro"
            }
        }
        
        /**
         * Get price string for tier (for UI display).
         * TODO: Fetch real prices from BillingClient
         */
        fun Tier.priceString(): String {
            return when (this) {
                FREE -> "Free"
                PLUS -> "$4.99/month"
                PRO -> "$14.99/month"
            }
        }
    }
}

/**
 * Extension functions for Tier.
 */
fun Tier.displayName(): String = when (this) {
    Tier.FREE -> "Free"
    Tier.PLUS -> "Plus"
    Tier.PRO -> "Pro"
}

fun Tier.priceString(): String = when (this) {
    Tier.FREE -> "Free"
    Tier.PLUS -> "$4.99/month"
    Tier.PRO -> "$14.99/month"
}

fun Tier.description(): String = when (this) {
    Tier.FREE -> "Basic navigation with on-device AI"
    Tier.PLUS -> "Cloud AI + in-app maps + advanced voice"
    Tier.PRO -> "Everything in Plus + commercial truck routing"
}
