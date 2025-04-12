package com.merlin.basketballanimationgame


import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager class for handling sound effects in basketball game
 * Uses SoundPool for efficient playback of short sound effects
 */
class BasketballSoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var applauseSoundId: Int = -1
    private var isLoaded = false

    init {
        initializeSoundPool()
    }

    private fun initializeSoundPool() {
        // Modern way to build SoundPool with AudioAttributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        // Load applause sound and set load completion listener
        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == applauseSoundId) {
                isLoaded = true
            }
        }

        // Load the applause sound from resources
        applauseSoundId = soundPool?.load(context, R.raw.applause, 1) ?: -1
    }

    /**
     * Play the applause sound when a basket is made
     * Can be called from coroutine scope for async sound playback
     */
    fun playApplauseSound() {
        if (isLoaded && applauseSoundId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                soundPool?.play(
                    applauseSoundId,
                    1.0f, // left volume
                    1.0f, // right volume
                    1, // priority
                    0, // no loop
                    1.0f // normal playback rate
                )
            }
        }
    }

    /**
     * Release resources when no longer needed
     */
    fun release() {
        soundPool?.release()
        soundPool = null
    }
}

/**
 * Composable function to remember and manage the sound manager lifecycle
 * Returns a SoundManager instance that gets cleaned up when the composable leaves composition
 */
@Composable
fun rememberSoundManager(): BasketballSoundManager {
    val context = LocalContext.current
    val soundManager = remember { BasketballSoundManager(context) }

    // Clean up resources when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }

    return soundManager
}