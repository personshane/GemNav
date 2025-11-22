package com.gemnav.app.voice

interface SpeechRecognitionService {
    fun startListening()
    fun stopListening()
    fun setOnTranscriptListener(listener: (String) -> Unit)
    fun setOnErrorListener(listener: (SpeechRecognitionError) -> Unit)
}

enum class SpeechRecognitionError {
    NO_MATCH,
    NETWORK_ERROR,
    PERMISSION_DENIED,
    INSUFFICIENT_PERMISSIONS,
    SERVICE_UNAVAILABLE,
    RECOGNIZER_BUSY,
    AUDIO_ERROR,
    UNKNOWN
}
