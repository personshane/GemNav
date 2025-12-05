package com.gemnav.app.truck.domain

import com.gemnav.app.truck.model.HazmatClass
import com.gemnav.core.here.TruckConfig

/**
 * Converter between TruckRoutingConstraints and HereEngineManager.TruckConfig.
 * Maps units and hazmat classes for HERE SDK routing engine.
 */
object TruckConstraintsConverter {
    
    /**
     * Convert TruckRoutingConstraints to TruckConfig for HERE SDK.
     * Unit conversions:
     * - meters → centimeters
     * - metric tons → kilograms
     */
    fun toTruckConfig(constraints: TruckRoutingConstraints): TruckConfig {
        return TruckConfig(
            heightCm = (constraints.heightMeters * 100).toInt(),
            widthCm = (constraints.widthMeters * 100).toInt(),
            lengthCm = (constraints.lengthMeters * 100).toInt(),
            weightKg = (constraints.weightTons * 1000).toInt(),
            axleCount = constraints.axleCount,
            trailerCount = 1, // Default to 1 trailer
            hasHazmat = constraints.hazmatClass != HazmatClass.NONE,
            hazmatClasses = mapHazmatToStringCodes(constraints.hazmatClass)
        )
    }
    
    /**
     * Map HazmatClass enum to HERE SDK string codes.
     * HERE SDK uses standard UN/DOT hazmat classification codes.
     */
    private fun mapHazmatToStringCodes(hazmatClass: HazmatClass): List<String> {
        return when (hazmatClass) {
            HazmatClass.NONE -> emptyList()
            HazmatClass.CLASS_1_EXPLOSIVES -> listOf("explosive")
            HazmatClass.CLASS_2_GASES -> listOf("gas")
            HazmatClass.CLASS_3_FLAMMABLE_LIQUIDS -> listOf("flammableLiquid")
            HazmatClass.CLASS_4_FLAMMABLE_SOLIDS -> listOf("flammableSolid")
            HazmatClass.CLASS_5_OXIDIZERS -> listOf("oxidizing")
            HazmatClass.CLASS_6_POISONS -> listOf("poison")
            HazmatClass.CLASS_7_RADIOACTIVE -> listOf("radioactive")
            HazmatClass.CLASS_8_CORROSIVE -> listOf("corrosive")
            HazmatClass.CLASS_9_MISC -> listOf("other")
        }
    }
}
