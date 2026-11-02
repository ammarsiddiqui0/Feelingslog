package com.example.feelingslog

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioPlaybackManager(private val context: Context) : DefaultLifecycleObserver {
    private var themePlayer: MediaPlayer? = null
    private var inlinePlayer: MediaPlayer? = null

    private val _isThemeAudioPlaying = MutableStateFlow(false)
    val isThemeAudioPlaying: StateFlow<Boolean> = _isThemeAudioPlaying

    private val _isInlineAudioPlaying = MutableStateFlow(false)
    val isInlineAudioPlaying: StateFlow<Boolean> = _isInlineAudioPlaying

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var fadeJob: Job? = null
    private var activeThemeVolume = 1.0f
    private var isThemeManuallyPaused = false

    fun playThemeAudio(file: File, autoPlay: Boolean = true) {
        if (!file.exists()) return
        stopThemeAudio()
        try {
            themePlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                isLooping = true
                setVolume(0f, 0f)
            }
            if (autoPlay) {
                isThemeManuallyPaused = false
                themePlayer?.start()
                _isThemeAudioPlaying.value = true
                fadeInThemeAudio()
            }
        } catch (e: Exception) {
            stopThemeAudio()
        }
    }

    fun toggleThemeAudio() {
        themePlayer?.let { player ->
            if (player.isPlaying) {
                isThemeManuallyPaused = true
                fadeOutThemeAudio {
                    player.pause()
                    _isThemeAudioPlaying.value = false
                }
            } else {
                isThemeManuallyPaused = false
                player.start()
                _isThemeAudioPlaying.value = true
                fadeInThemeAudio()
            }
        }
    }

    fun playInlineAudio(file: File, onCompletion: () -> Unit) {
        if (!file.exists()) return
        stopInlineAudio()
        try {
            inlinePlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isInlineAudioPlaying.value = false
                    restoreThemeAudioVolume()
                    onCompletion()
                }
            }
            duckThemeAudio()
            inlinePlayer?.start()
            _isInlineAudioPlaying.value = true
        } catch (e: Exception) {
            stopInlineAudio()
            restoreThemeAudioVolume()
        }
    }

    fun pauseInlineAudio() {
        inlinePlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isInlineAudioPlaying.value = false
                restoreThemeAudioVolume()
            }
        }
    }

    fun stopInlineAudio() {
        inlinePlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        inlinePlayer = null
        _isInlineAudioPlaying.value = false
        restoreThemeAudioVolume()
    }

    private fun fadeInThemeAudio(targetVol: Float = 1.0f) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            activeThemeVolume = targetVol
            var vol = 0.0f
            val steps = 10
            val stepVol = targetVol / steps
            while (vol < targetVol && themePlayer?.isPlaying == true) {
                vol += stepVol
                val currentVol = minOf(vol, targetVol)
                themePlayer?.setVolume(currentVol, currentVol)
                delay(30)
            }
        }
    }

    private fun fadeOutThemeAudio(onComplete: () -> Unit) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            var vol = activeThemeVolume
            val steps = 10
            val stepVol = vol / steps
            while (vol > 0.0f && themePlayer?.isPlaying == true) {
                vol -= stepVol
                val currentVol = maxOf(vol, 0.0f)
                themePlayer?.setVolume(currentVol, currentVol)
                delay(30)
            }
            onComplete()
        }
    }

    private fun duckThemeAudio() {
        if (themePlayer?.isPlaying == true) {
            fadeJob?.cancel()
            fadeJob = scope.launch {
                val duckTarget = activeThemeVolume * 0.30f
                themePlayer?.setVolume(duckTarget, duckTarget)
            }
        }
    }

    private fun restoreThemeAudioVolume() {
        if (!isThemeManuallyPaused && themePlayer?.isPlaying == true) {
            fadeJob?.cancel()
            fadeJob = scope.launch {
                themePlayer?.setVolume(activeThemeVolume, activeThemeVolume)
            }
        }
    }

    fun stopThemeAudio() {
        fadeJob?.cancel()
        themePlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        themePlayer = null
        _isThemeAudioPlaying.value = false
    }

    override fun onPause(owner: LifecycleOwner) {
        stopThemeAudio()
        stopInlineAudio()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stopThemeAudio()
        stopInlineAudio()
    }
}
