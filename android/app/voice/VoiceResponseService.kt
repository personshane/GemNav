package com.gemnav.app.voice

interface VoiceResponseService {
    fun speak(text: String, interrupt: Boolean = false)
    fun stop()
    fun shutdown()
}
