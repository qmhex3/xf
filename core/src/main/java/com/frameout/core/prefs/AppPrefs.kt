package com.frameout.core.prefs

import android.content.Context
import com.frameout.core.collage.COLLAGE_GAP_DEFAULT
import com.frameout.core.collage.sanitizeCollageGap
import com.frameout.core.storage.CaptureSize
import com.frameout.core.storage.FrameNaming
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPrefs(context: Context) : SessionPrefs(
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
) {
    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME, "Dark") ?: "Dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _accent = MutableStateFlow(prefs.getString(KEY_ACCENT, "indigo") ?: "indigo")
    val accent: StateFlow<String> = _accent.asStateFlow()

    private val _iconFollowsTheme = MutableStateFlow(prefs.getBoolean(KEY_ICON_THEME, false))
    val iconFollowsTheme: StateFlow<Boolean> = _iconFollowsTheme.asStateFlow()

    private val _glassIntensity = MutableStateFlow(prefs.getString(KEY_GLASS, "Full") ?: "Full")
    val glassIntensity: StateFlow<String> = _glassIntensity.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANG, "auto") ?: "auto")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _saveFormat = MutableStateFlow(migratedFormat())
    val saveFormat: StateFlow<String> = _saveFormat.asStateFlow()

    private val _jpegQuality = MutableStateFlow(prefs.getInt(KEY_JPEG_Q, 100))
    val jpegQuality: StateFlow<Int> = _jpegQuality.asStateFlow()

    private val _captureSize = MutableStateFlow(CaptureSize.normalize(prefs.getString(KEY_CAPTURE_SIZE, CaptureSize.ORIGINAL)))
    val captureSize: StateFlow<String> = _captureSize.asStateFlow()

    private val _jumpFrames = MutableStateFlow(prefs.getInt(KEY_JUMP_FRAMES, 10))
    val jumpFrames: StateFlow<Int> = _jumpFrames.asStateFlow()

    private val _autoSave = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SAVE, false))
    val autoSave: StateFlow<Boolean> = _autoSave.asStateFlow()

    private val _trimOutput = MutableStateFlow(prefs.getString(KEY_TRIM_OUTPUT, "frames") ?: "frames")
    val trimOutput: StateFlow<String> = _trimOutput.asStateFlow()

    private val _extractMode = MutableStateFlow(prefs.getString(KEY_EXTRACT, "every_frame") ?: "every_frame")
    val extractMode: StateFlow<String> = _extractMode.asStateFlow()

    private val _saveFolder = MutableStateFlow(prefs.getString(KEY_FOLDER, "Pictures/FrameOut") ?: "Pictures/FrameOut")
    private val _saveFolderUri = MutableStateFlow(prefs.getString(KEY_FOLDER_URI, "") ?: "")
    val saveFolderUri: StateFlow<String> = _saveFolderUri.asStateFlow()
    val saveFolder: StateFlow<String> = _saveFolder.asStateFlow()

    private val _datePolicy = MutableStateFlow(prefs.getString(KEY_DATE, "frame") ?: "frame")
    val datePolicy: StateFlow<String> = _datePolicy.asStateFlow()

    private val _keepLocation = MutableStateFlow(prefs.getBoolean(KEY_KEEP_LOCATION, false))
    val keepLocation: StateFlow<Boolean> = _keepLocation.asStateFlow()

    fun setKeepLocation(value: Boolean) = save(KEY_KEEP_LOCATION, value, _keepLocation)

    fun backupHintShown(): Boolean = prefs.getBoolean(KEY_BACKUP_HINT, false)

    fun markBackupHintShown() = prefs.edit().putBoolean(KEY_BACKUP_HINT, true).apply()

    fun speedHintShows(): Int = prefs.getInt(KEY_SPEED_HINT, 0)

    fun markSpeedHintShown() = prefs.edit().putInt(KEY_SPEED_HINT, speedHintShows() + 1).apply()

    fun markSpeedZoneUsed() = prefs.edit().putInt(KEY_SPEED_HINT, SPEED_HINT_LIMIT).apply()

    private val _skipBlurry = MutableStateFlow(prefs.getBoolean(KEY_SKIP_BLURRY, false))
    val skipBlurry: StateFlow<Boolean> = _skipBlurry.asStateFlow()

    fun setSkipBlurry(value: Boolean) = save(KEY_SKIP_BLURRY, value, _skipBlurry)

    private val _skipDuplicates = MutableStateFlow(prefs.getBoolean(KEY_SKIP_DUPLICATES, false))
    val skipDuplicates: StateFlow<Boolean> = _skipDuplicates.asStateFlow()

    fun setSkipDuplicates(value: Boolean) = save(KEY_SKIP_DUPLICATES, value, _skipDuplicates)

    private val _peopleOnly = MutableStateFlow(prefs.getBoolean(KEY_PEOPLE_ONLY, false))
    val peopleOnly: StateFlow<Boolean> = _peopleOnly.asStateFlow()

    fun setPeopleOnly(value: Boolean) = save(KEY_PEOPLE_ONLY, value, _peopleOnly)

    private val _peopleBody = MutableStateFlow(prefs.getBoolean(KEY_PEOPLE_BODY, true))
    val peopleBody: StateFlow<Boolean> = _peopleBody.asStateFlow()

    fun setPeopleBody(value: Boolean) = save(KEY_PEOPLE_BODY, value, _peopleBody)

    private val _playerMuted = MutableStateFlow(prefs.getBoolean(KEY_PLAYER_MUTED, false))
    val playerMuted: StateFlow<Boolean> = _playerMuted.asStateFlow()

    fun setPlayerMuted(value: Boolean) = save(KEY_PLAYER_MUTED, value, _playerMuted)

    private val _volumeNav = MutableStateFlow(prefs.getBoolean(KEY_VOLUME_NAV, false))
    val volumeNav: StateFlow<Boolean> = _volumeNav.asStateFlow()

    fun setVolumeNav(value: Boolean) = save(KEY_VOLUME_NAV, value, _volumeNav)

    private val _namingTemplate = MutableStateFlow(prefs.getString(KEY_NAMING, FrameNaming.DEFAULT) ?: FrameNaming.DEFAULT)
    val namingTemplate: StateFlow<String> = _namingTemplate.asStateFlow()

    fun setNamingTemplate(value: String) {
        val clean = FrameNaming.cleanTemplate(value)
        prefs.edit().putString(KEY_NAMING, clean).apply()
        _namingTemplate.value = clean
    }

    private val _collageGap = MutableStateFlow(sanitizeCollageGap(prefs.getInt(KEY_COLLAGE_GAP, COLLAGE_GAP_DEFAULT)))
    val collageGap: StateFlow<Int> = _collageGap.asStateFlow()

    private val _collageBgColor = MutableStateFlow(prefs.getInt(KEY_COLLAGE_BG, COLLAGE_BG_DEFAULT))
    val collageBgColor: StateFlow<Int> = _collageBgColor.asStateFlow()

    private val _collageRounded = MutableStateFlow(prefs.getBoolean(KEY_COLLAGE_ROUNDED, false))
    val collageRounded: StateFlow<Boolean> = _collageRounded.asStateFlow()

    private val _collageStamps = MutableStateFlow(prefs.getBoolean(KEY_COLLAGE_STAMPS, false))
    val collageStamps: StateFlow<Boolean> = _collageStamps.asStateFlow()

    fun setCollageGap(value: Int) = save(KEY_COLLAGE_GAP, sanitizeCollageGap(value), _collageGap)

    fun setCollageBgColor(value: Int) = save(KEY_COLLAGE_BG, value, _collageBgColor)

    fun setCollageRounded(value: Boolean) = save(KEY_COLLAGE_ROUNDED, value, _collageRounded)

    fun setCollageStamps(value: Boolean) = save(KEY_COLLAGE_STAMPS, value, _collageStamps)

    private fun migratedFormat(): String {
        val stored = prefs.getString(KEY_FORMAT, "png") ?: "png"
        if (!prefs.contains(KEY_SAVE_HDR)) return stored
        val upgraded = if (stored == "jpg" && prefs.getBoolean(KEY_SAVE_HDR, false)) "jpg_hdr" else stored
        prefs.edit().putString(KEY_FORMAT, upgraded).remove(KEY_SAVE_HDR).apply()
        return upgraded
    }

    fun setThemeMode(value: String) = save(KEY_THEME, value, _themeMode)

    fun setAccent(value: String) = save(KEY_ACCENT, value, _accent)

    fun setIconFollowsTheme(value: Boolean) = save(KEY_ICON_THEME, value, _iconFollowsTheme)

    fun setGlassIntensity(value: String) = save(KEY_GLASS, value, _glassIntensity)

    fun setLanguage(value: String) = save(KEY_LANG, value, _language)

    fun setSaveFormat(value: String) = save(KEY_FORMAT, value, _saveFormat)

    fun setJpegQuality(value: Int) = save(KEY_JPEG_Q, value, _jpegQuality)

    fun setCaptureSize(value: String) = save(KEY_CAPTURE_SIZE, CaptureSize.normalize(value), _captureSize)

    fun setJumpFrames(value: Int) = save(KEY_JUMP_FRAMES, value, _jumpFrames)

    fun setAutoSave(value: Boolean) = save(KEY_AUTO_SAVE, value, _autoSave)

    fun setTrimOutput(value: String) = save(KEY_TRIM_OUTPUT, value, _trimOutput)

    fun setExtractMode(value: String) = save(KEY_EXTRACT, value, _extractMode)

    fun setSaveFolderUri(value: String) = save(KEY_FOLDER_URI, value, _saveFolderUri)

    fun clearSaveFolderUri() = setSaveFolderUri("")

    fun setSaveFolder(value: String) = save(KEY_FOLDER, value, _saveFolder)

    fun setDatePolicy(value: String) = save(KEY_DATE, value, _datePolicy)

    companion object {
        fun peekAccent(context: Context): String =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ACCENT, "indigo") ?: "indigo"

        fun peekThemeMode(context: Context): String =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_THEME, "Dark") ?: "Dark"

        private const val PREFS_NAME = "frameout_prefs"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_GLASS = "glass_intensity"
        private const val KEY_ACCENT = "accent_color"
        private const val KEY_ICON_THEME = "icon_follows_theme"
        private const val KEY_LANG = "language"
        private const val KEY_FORMAT = "save_format"
        private const val KEY_JPEG_Q = "jpeg_quality"
        private const val KEY_SAVE_HDR = "save_hdr"
        private const val KEY_CAPTURE_SIZE = "capture_size"
        private const val KEY_JUMP_FRAMES = "jump_frames"
        private const val KEY_AUTO_SAVE = "auto_save"
        private const val KEY_EXTRACT = "extract_mode"
        private const val KEY_TRIM_OUTPUT = "trim_output"
        private const val KEY_FOLDER = "save_folder"
        private const val KEY_FOLDER_URI = "save_folder_uri"
        private const val KEY_DATE = "date_policy"
        private const val KEY_KEEP_LOCATION = "keep_location"
        private const val KEY_BACKUP_HINT = "backup_hint_shown"
        private const val KEY_SPEED_HINT = "speed_zone_hint"
        const val SPEED_HINT_LIMIT = 3
        private const val KEY_SKIP_BLURRY = "skip_blurry"
        private const val KEY_SKIP_DUPLICATES = "skip_duplicates"
        private const val KEY_PEOPLE_ONLY = "people_only"
        private const val KEY_PEOPLE_BODY = "people_body"
        private const val KEY_NAMING = "naming_template"
        private const val KEY_VOLUME_NAV = "volume_nav"
        private const val KEY_PLAYER_MUTED = "player_muted"
        private const val KEY_COLLAGE_GAP = "collage_gap"
        private const val KEY_COLLAGE_BG = "collage_bg_color"
        private const val KEY_COLLAGE_ROUNDED = "collage_rounded"
        private const val KEY_COLLAGE_STAMPS = "collage_stamps"
        const val COLLAGE_BG_DEFAULT = 0xFF000000.toInt()
    }
}
