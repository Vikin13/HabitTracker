package com.habittracker.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BackgroundType { SYSTEM, COLOR, IMAGE }

data class BackgroundSettings(
    val type: BackgroundType = BackgroundType.SYSTEM,
    val colorIndex: Int = 0,
    val imageUri: String? = null,
    val imageScale: Float = 1.0f,
    val imageOffsetXDp: Float = 0f,
    val imageOffsetYDp: Float = 0f
)

object BackgroundManager {
    private val _settings = MutableStateFlow(BackgroundSettings())
    val settings: StateFlow<BackgroundSettings> = _settings.asStateFlow()

    fun setColorIndex(index: Int) {
        _settings.value = BackgroundSettings(
            type = BackgroundType.COLOR,
            colorIndex = index,
            imageUri = null
        )
    }

    fun setImageUri(uri: String, scale: Float = 1.0f, offsetXDp: Float = 0f, offsetYDp: Float = 0f) {
        _settings.value = BackgroundSettings(
            type = BackgroundType.IMAGE,
            colorIndex = 0,
            imageUri = uri,
            imageScale = scale,
            imageOffsetXDp = offsetXDp,
            imageOffsetYDp = offsetYDp
        )
    }

    fun resetToSystem() {
        _settings.value = BackgroundSettings()
    }

    fun updateFromSettings(
        type: BackgroundType, colorIndex: Int, imageUri: String?,
        imageScale: Float = 1.0f, imageOffsetXDp: Float = 0f, imageOffsetYDp: Float = 0f
    ) {
        _settings.value = BackgroundSettings(
            type = type,
            colorIndex = colorIndex,
            imageUri = imageUri,
            imageScale = imageScale,
            imageOffsetXDp = imageOffsetXDp,
            imageOffsetYDp = imageOffsetYDp
        )
    }
}

val backgroundPresets = listOf(
    0xFFFFFFFF to "Default",
    0xFFFFF3E0 to "Warm",
    0xFFE3F2FD to "Sky",
    0xFFE8F5E9 to "Mint",
    0xFFF3E5F5 to "Lavender",
    0xFFFFFDE7 to "Lemon"
)

@Composable
fun BackgroundSettings.currentBackgroundColor(): Color {
    return when (type) {
        BackgroundType.COLOR -> Color(backgroundPresets.getOrElse(colorIndex) { 0xFFFFFFFF to "Default" }.first)
        else -> Color.Transparent
    }
}

@Composable
fun RememberBackgroundImage(): androidx.compose.ui.graphics.painter.Painter? {
    val bg by BackgroundManager.settings.collectAsState()
    if (bg.type != BackgroundType.IMAGE || bg.imageUri == null) return null

    val context = LocalContext.current
    val uri = bg.imageUri!!
    return rememberUriPainter(uri, context)
}

@Composable
fun rememberUriPainter(uri: String, context: Context): androidx.compose.ui.graphics.painter.Painter? {
    return androidx.compose.runtime.remember(uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                androidx.compose.ui.graphics.painter.BitmapPainter(bitmap.asImageBitmap())
            } else null
        } catch (_: Exception) { null }
    }
}
