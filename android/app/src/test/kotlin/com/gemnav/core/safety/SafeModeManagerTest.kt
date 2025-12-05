package com.gemnav.core.safety

import android.content.Context
import io.mockk.mockk
import io.mockk.every
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * Unit tests for SafeModeManager
 * Tests safe mode activation/deactivation logic
 * Created from MP-009 test template
 */
class SafeModeManagerTest {
    private lateinit var safeModeManager: SafeModeManager
    private lateinit var mockContext: Context
    
    @BeforeEach
    fun setup() {
        mockContext = mockk<Context>(relaxed = true)
        safeModeManager = SafeModeManager()
    }
    
    @Test
    fun `test safe mode activation when criteria met`() {
        // Arrange
        val context = mockContext
        
        // Act
        val result = safeModeManager.evaluateSafeMode(context)
        
        // Assert
        assertTrue(result.isActivated, "Safe mode should activate when criteria met")
        assertNotNull(result.reason, "Activation reason should be provided")
    }
    
    @Test
    fun `test safe mode deactivation when conditions clear`() {
        // Arrange
        val context = mockContext
        every { context.getSystemService(any()) } returns null
        
        // Act
        val result = safeModeManager.evaluateSafeMode(context)
        
        // Assert
        assertFalse(result.isActivated, "Safe mode should deactivate when conditions clear")
    }
    
    @Test
    fun `test safe mode persists across checks`() {
        // Arrange
        val context = mockContext
        
        // Act
        val firstCheck = safeModeManager.evaluateSafeMode(context)
        val secondCheck = safeModeManager.evaluateSafeMode(context)
        
        // Assert
        assertEquals(firstCheck.isActivated, secondCheck.isActivated, 
            "Safe mode state should persist across consecutive checks")
    }
    
    @Test
    fun `test safe mode provides diagnostic information`() {
        // Arrange
        val context = mockContext
        
        // Act
        val result = safeModeManager.evaluateSafeMode(context)
        
        // Assert
        assertNotNull(result.diagnostics, "Diagnostics should be available")
        assertTrue(result.diagnostics.isNotEmpty(), "Diagnostics should contain information")
    }
}