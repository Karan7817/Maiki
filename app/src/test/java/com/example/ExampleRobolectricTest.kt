package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.VoiceConfig
import com.example.data.model.VoiceSelectionMode
import com.example.data.repository.VoiceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MAIKI AI", appName)
    }

    @Test
    fun `manual voice id takes priority over automatic voice`() {
        val config = VoiceConfig(
            customVoiceId = "custom_12345",
            selectedAutoVoiceId = "auto_67890",
            selectionMode = VoiceSelectionMode.MANUAL
        )
        assertEquals("custom_12345", config.effectiveVoiceId)
    }

    @Test
    fun `automatic voice is used when in automatic mode`() {
        val config = VoiceConfig(
            customVoiceId = "custom_12345",
            selectedAutoVoiceId = "auto_67890",
            selectionMode = VoiceSelectionMode.AUTOMATIC
        )
        assertEquals("auto_67890", config.effectiveVoiceId)
    }

    @Test
    fun `repository trims accidental whitespace and rejects empty input`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = VoiceRepository(context)

        // Empty input rejected
        assertFalse(repo.saveCustomVoiceId("   "))

        // Accidental whitespace trimmed
        assertTrue(repo.saveCustomVoiceId("  21m00Tcm4TlvDq8ikWAM  ", "Rachel Voice"))
        val loaded = repo.loadVoiceConfig()
        assertEquals("21m00Tcm4TlvDq8ikWAM", loaded.customVoiceId)
        assertEquals("Rachel Voice", loaded.voiceName)

        // Delete custom voice
        repo.deleteCustomVoiceId()
        val afterDelete = repo.loadVoiceConfig()
        assertTrue(afterDelete.customVoiceId.isEmpty())
    }

    @Test
    fun `save and load gemini configuration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = VoiceRepository(context)

        val config = VoiceConfig(
            geminiApiKey = "test_gemini_key_123",
            geminiModel = "gemini-1.5-pro",
            isBackgroundListeningEnabled = true
        )
        repo.saveVoiceConfig(config)

        val loaded = repo.loadVoiceConfig()
        assertEquals("test_gemini_key_123", loaded.geminiApiKey)
        assertEquals("gemini-1.5-pro", loaded.geminiModel)
        assertTrue(loaded.isBackgroundListeningEnabled)
    }
}
