package com.habittracker.app.data.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.habittracker.app.data.repository.WidgetRepository
import com.habittracker.app.ui.widget.WidgetUpdateHelper
import com.habittracker.app.util.DateUtils
import kotlinx.coroutines.runBlocking

/**
 * ContentProvider that allows AppWidgets to toggle habit records via a simple URI scheme.
 *
 * URI format:
 *   content://com.habittracker.app.widget/toggle/{habitId}
 *
 * The provider toggles the record for today's date and returns the new state as the result.
 */
class HabitContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    /**
     * insert() is used for toggling:
     *   content://com.habittracker.app.widget/toggle/{habitId}
     */
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val habitId = uri.lastPathSegment?.toLongOrNull()
            ?: return null
        val context = context ?: return null

        val repo = WidgetRepository(context)
        val dateMillis = values?.getAsLong("date") ?: DateUtils.todayMillis()

        runBlocking {
            repo.toggleRecord(habitId, dateMillis)
        }

        WidgetUpdateHelper.notifyDataChanged(context)

        // Append dateMillis so the caller can read back the new state
        return uri.buildUpon().appendPath(dateMillis.toString()).build()
    }

    /**
     * update() is a no-op read-back: pass {habitId}/{dateMillis} to check completion.
     * Returns 1 if completed, 0 otherwise.
     */
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?
    ): Int {
        val segments = uri.pathSegments ?: return 0
        if (segments.size < 3) return 0
        val habitId = segments[1].toLongOrNull() ?: return 0
        val dateMillis = segments[2].toLongOrNull() ?: return 0
        val context = context ?: return 0

        val repo = WidgetRepository(context)
        return runBlocking {
            if (repo.toggleRecord(habitId, dateMillis)) 1 else 0
        }
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.habittracker.toggle"
}
