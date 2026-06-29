package com.habittracker.app.ui

import android.content.Context
import android.content.SharedPreferences
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
import java.io.File
import java.util.UUID

enum class BackgroundType { SYSTEM, COLOR, IMAGE }

/** How the Material color scheme should be selected. */
enum class SchemeMode { SYSTEM, LIGHT, DARK }

data class BackgroundSettings(
    val type: BackgroundType = BackgroundType.SYSTEM,
    val colorIndex: Int = 0,
    val imageUri: String? = null,
    val imageScale: Float = 1.0f,
    val imageOffsetXDp: Float = 0f,
    val imageOffsetYDp: Float = 0f,
    /** Override for Material color scheme (text/surface colors). */
    val schemeMode: SchemeMode = SchemeMode.SYSTEM
)

object BackgroundManager {
    private const val PREFS_NAME = "background_settings"
    private const val KEY_TYPE = "type"
    private const val KEY_COLOR_INDEX = "colorIndex"
    private const val KEY_IMAGE_URI = "imageUri"
    private const val KEY_IMAGE_SCALE = "imageScale"
    private const val KEY_IMAGE_OFFSET_X = "imageOffsetX"
    private const val KEY_IMAGE_OFFSET_Y = "imageOffsetY"
    private const val KEY_SCHEME_MODE = "schemeMode"

    private var prefs: SharedPreferences? = null
    private val _settings = MutableStateFlow(BackgroundSettings())
    val settings: StateFlow<BackgroundSettings> = _settings.asStateFlow()

    /** Initialize from persisted settings. Call once at app startup. */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = load()
        if (saved != null) {
            _settings.value = saved
        }
    }

    private fun load(): BackgroundSettings? {
        val p = prefs ?: return null
        if (!p.contains(KEY_TYPE)) return null
        return BackgroundSettings(
            type = BackgroundType.valueOf(p.getString(KEY_TYPE, BackgroundType.SYSTEM.name)!!),
            colorIndex = p.getInt(KEY_COLOR_INDEX, 0),
            imageUri = p.getString(KEY_IMAGE_URI, null),
            imageScale = p.getFloat(KEY_IMAGE_SCALE, 1.0f),
            imageOffsetXDp = p.getFloat(KEY_IMAGE_OFFSET_X, 0f),
            imageOffsetYDp = p.getFloat(KEY_IMAGE_OFFSET_Y, 0f),
            schemeMode = SchemeMode.valueOf(p.getString(KEY_SCHEME_MODE, SchemeMode.SYSTEM.name)!!)
        )
    }

    private fun save() {
        val p = prefs ?: return
        val s = _settings.value
        p.edit()
            .putString(KEY_TYPE, s.type.name)
            .putInt(KEY_COLOR_INDEX, s.colorIndex)
            .putString(KEY_IMAGE_URI, s.imageUri)
            .putFloat(KEY_IMAGE_SCALE, s.imageScale)
            .putFloat(KEY_IMAGE_OFFSET_X, s.imageOffsetXDp)
            .putFloat(KEY_IMAGE_OFFSET_Y, s.imageOffsetYDp)
            .putString(KEY_SCHEME_MODE, s.schemeMode.name)
            .apply()
    }

    fun setSchemeMode(mode: SchemeMode) {
        _settings.value = _settings.value.copy(schemeMode = mode)
        save()
    }

    fun setColorIndex(index: Int) {
        _settings.value = _settings.value.copy(
            type = BackgroundType.COLOR,
            colorIndex = index,
            imageUri = null
        )
        save()
    }

    fun setImageUri(uri: String, scale: Float = 1.0f, offsetXDp: Float = 0f, offsetYDp: Float = 0f) {
        _settings.value = _settings.value.copy(
            type = BackgroundType.IMAGE,
            colorIndex = 0,
            imageUri = uri,
            imageScale = scale,
            imageOffsetXDp = offsetXDp,
            imageOffsetYDp = offsetYDp
        )
        save()
    }

    /**
     * Copy a content:// URI to internal storage and save the path.
     * content:// URIs lose read permission after restart, so we must persist the file.
     */
    fun setImageFromContentUri(context: Context, contentUri: Uri, scale: Float = 1.0f, offsetXDp: Float = 0f, offsetYDp: Float = 0f) {
        val internalPath = copyToInternalStorage(context, contentUri)
        if (internalPath != null) {
            setImageUri(internalPath, scale, offsetXDp, offsetYDp)
        }
    }

    private fun copyToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "backgrounds")
            dir.mkdirs()
            val destFile = File(dir, "bg_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun resetToSystem() {
        _settings.value = BackgroundSettings()
        save()
    }

    fun updateFromSettings(
        type: BackgroundType, colorIndex: Int, imageUri: String?,
        imageScale: Float = 1.0f, imageOffsetXDp: Float = 0f, imageOffsetYDp: Float = 0f
    ) {
        _settings.value = _settings.value.copy(
            type = type,
            colorIndex = colorIndex,
            imageUri = imageUri,
            imageScale = imageScale,
            imageOffsetXDp = imageOffsetXDp,
            imageOffsetYDp = imageOffsetYDp
        )
        save()
    }
}

val backgroundPresets = listOf(
    0xFFFFFFFF to "Default",
    0xFFFFF3E0 to "Warm",
    0xFFE3F2FD to "Sky",
    0xFFE8F5E9 to "Mint",
    0xFFF3E5F5 to "Lavender",
    // Dark presets
    0xFF1C1B1F to "Dark",
    0xFF2D2D2D to "Charcoal",
    0xFF1B5E20 to "Forest"
)

/**
 * Returns the scrim opacity needed when this background is applied.
 * A dark background needs a dark overlay so MaterialTheme colors remain readable.
 */
/** Returns true if the current background is perceived as dark (should use dark color scheme). */
fun BackgroundSettings.isBackgroundDark(): Boolean {
    return when (type) {
        BackgroundType.IMAGE -> true  // image backgrounds always get dark scheme + scrim
        BackgroundType.COLOR -> {
            val colorInt = backgroundPresets.getOrElse(colorIndex) { 0xFFFFFFFF to "Default" }.first
            val r = android.graphics.Color.red(colorInt) / 255f
            val g = android.graphics.Color.green(colorInt) / 255f
            val b = android.graphics.Color.blue(colorInt) / 255f
            val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
            luminance < 0.5f
        }
        BackgroundType.SYSTEM -> false
    }
}

fun BackgroundSettings.scrimAlpha(): Float {
    return when (type) {
        BackgroundType.IMAGE -> 0.18f
        BackgroundType.COLOR -> {
            val colorInt = backgroundPresets.getOrElse(colorIndex) { 0xFFFFFFFF to "Default" }.first
            val r = android.graphics.Color.red(colorInt) / 255f
            val g = android.graphics.Color.green(colorInt) / 255f
            val b = android.graphics.Color.blue(colorInt) / 255f
            // sRGB relative luminance (simplified)
            val luminance = 0.2126f * r + 0.7152f * g + 0.0722f * b
            if (luminance < 0.5f) 0.30f else 0f
        }
        BackgroundType.SYSTEM -> 0f
    }
}

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
            val inputStream = if (uri.startsWith("/")) {
                java.io.FileInputStream(uri)
            } else {
                context.contentResolver.openInputStream(Uri.parse(uri))
            }
            if (inputStream == null) return@remember null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap != null) {
                androidx.compose.ui.graphics.painter.BitmapPainter(bitmap.asImageBitmap())
            } else null
        } catch (_: Exception) { null }
    }
}
