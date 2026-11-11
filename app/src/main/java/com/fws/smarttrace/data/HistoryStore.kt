package com.fws.smarttrace.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.fws.smarttrace.model.HistoryItem
import com.fws.smarttrace.model.ScanResult
import com.fws.smarttrace.model.Verdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** Image thumbnails kept in app-private storage (never shared, never uploaded in Offline mode). */
object ScanFiles {
    private fun dir(ctx: Context) = File(ctx.filesDir, "scans").apply { mkdirs() }
    fun file(ctx: Context, id: String) = File(dir(ctx), "$id.jpg")

    fun save(ctx: Context, id: String, bmp: Bitmap) {
        file(ctx, id).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 82, it) }
    }

    fun load(ctx: Context, id: String, sample: Int = 1): Bitmap? {
        val f = file(ctx, id)
        if (!f.exists()) return null
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun delete(ctx: Context, id: String) { file(ctx, id).delete() }
}

/** Local scan history (SQLite). Everything stays on the device. */
class HistoryStore(private val ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "history.db", null, 1) {

    private val _items = MutableStateFlow<List<HistoryItem>>(emptyList())
    val items: StateFlow<List<HistoryItem>> = _items.asStateFlow()

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE scans(id TEXT PRIMARY KEY, created INTEGER NOT NULL, title TEXT, brand TEXT, " +
                "category TEXT, verdict TEXT, score INTEGER, mode TEXT, favorite INTEGER NOT NULL DEFAULT 0, json TEXT NOT NULL)"
        )
        db.execSQL("CREATE INDEX idx_scans_created ON scans(created DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    private fun reload() {
        val list = ArrayList<HistoryItem>()
        readableDatabase.rawQuery(
            "SELECT id, created, title, brand, category, verdict, score, favorite FROM scans ORDER BY created DESC", null
        ).use { c ->
            while (c.moveToNext()) {
                list += HistoryItem(
                    id = c.getString(0),
                    createdAt = c.getLong(1),
                    title = c.getString(2) ?: "",
                    brand = c.getString(3) ?: "",
                    category = c.getString(4) ?: "other",
                    verdict = runCatching { Verdict.valueOf(c.getString(5) ?: "") }.getOrDefault(Verdict.CHECK),
                    score = c.getInt(6),
                    favorite = c.getInt(7) == 1,
                )
            }
        }
        _items.value = list
    }

    suspend fun refresh() = withContext(Dispatchers.IO) { reload() }

    suspend fun insert(r: ScanResult) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("id", r.id); put("created", r.createdAt); put("title", r.title); put("brand", r.label.brand)
            put("category", r.label.category); put("verdict", r.verdict.name); put("score", r.score)
            put("mode", r.mode.name); put("favorite", 0); put("json", r.toJson().toString())
        }
        writableDatabase.insertWithOnConflict("scans", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        reload()
    }

    /** Rewrites a stored result (e.g. after translation) and keeps its favorite flag. */
    suspend fun update(r: ScanResult) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL(
            "UPDATE scans SET title = ?, brand = ?, category = ?, json = ? WHERE id = ?",
            arrayOf<Any>(r.title, r.label.brand, r.label.category, r.toJson().toString(), r.id)
        )
        reload()
    }

    suspend fun get(id: String): ScanResult? = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT json FROM scans WHERE id = ?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) runCatching { ScanResult.fromJson(JSONObject(c.getString(0))) }.getOrNull() else null
        }
    }

    suspend fun setFavorite(id: String, fav: Boolean) = withContext(Dispatchers.IO) {
        writableDatabase.execSQL("UPDATE scans SET favorite = ? WHERE id = ?", arrayOf<Any>(if (fav) 1 else 0, id))
        reload()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("scans", "id = ?", arrayOf(id))
        ScanFiles.delete(ctx, id)
        reload()
    }

    /** Removes every scan that is not a favorite. */
    suspend fun clearNonFavorites() = withContext(Dispatchers.IO) {
        val ids = ArrayList<String>()
        readableDatabase.rawQuery("SELECT id FROM scans WHERE favorite = 0", null).use { c ->
            while (c.moveToNext()) ids += c.getString(0)
        }
        writableDatabase.delete("scans", "favorite = 0", null)
        ids.forEach { ScanFiles.delete(ctx, it) }
        reload()
    }
}
