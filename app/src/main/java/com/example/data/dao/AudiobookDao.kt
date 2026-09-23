package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AudioBookmarkEntity
import com.example.data.model.AudiobookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiobookDao {
    @Query("SELECT * FROM audiobooks ORDER BY lastListenedTimestamp DESC")
    fun getAllAudiobooks(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteAudiobooks(): Flow<List<AudiobookEntity>>

    @Query("SELECT * FROM audiobooks WHERE id = :id LIMIT 1")
    fun getAudiobookById(id: Long): Flow<AudiobookEntity?>

    @Query("SELECT * FROM audiobooks WHERE id = :id LIMIT 1")
    suspend fun getAudiobookByIdSync(id: Long): AudiobookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudiobook(audiobook: AudiobookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudiobooks(audiobooks: List<AudiobookEntity>)

    @Update
    suspend fun updateAudiobook(audiobook: AudiobookEntity)

    @Query("UPDATE audiobooks SET currentPositionMs = :positionMs, durationMs = :durationMs, lastListenedTimestamp = :timestamp WHERE id = :id")
    suspend fun updatePlaybackProgress(id: Long, positionMs: Long, durationMs: Long, timestamp: Long)

    @Query("UPDATE audiobooks SET coverImageUrl = :coverUrl WHERE id = :id")
    suspend fun updateCoverImageUrl(id: Long, coverUrl: String)

    @Query("UPDATE audiobooks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Delete
    suspend fun deleteAudiobook(audiobook: AudiobookEntity)

    @Query("SELECT COUNT(*) FROM audiobooks")
    suspend fun getAudiobookCount(): Int

    // Audio Bookmarks
    @Query("SELECT * FROM audio_bookmarks WHERE audiobookId = :audiobookId ORDER BY timestampMs ASC")
    fun getBookmarksForAudiobook(audiobookId: Long): Flow<List<AudioBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioBookmark(bookmark: AudioBookmarkEntity): Long

    @Delete
    suspend fun deleteAudioBookmark(bookmark: AudioBookmarkEntity)
}
