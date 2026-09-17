package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Ordem
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdemDao {

    @Query("SELECT * FROM ordens WHERE removido = 0 ORDER BY id DESC")
    fun getAllActive(): Flow<List<Ordem>>

    @Query("SELECT * FROM ordens ORDER BY id DESC")
    fun getAllWithRemoved(): Flow<List<Ordem>>

    @Query("SELECT * FROM ordens WHERE id = :id")
    fun getById(id: Long): Flow<Ordem?>

    @Query("SELECT * FROM ordens WHERE id = :id")
    suspend fun getByIdImmediate(id: Long): Ordem?

    @Query("SELECT * FROM ordens WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getByCloudId(cloudId: String): Ordem?

    @Query("SELECT MAX(num) FROM ordens")
    suspend fun getMaxNumero(): Int?

    @Query("SELECT * FROM ordens WHERE sincronizado = 0")
    suspend fun getPendingSync(): List<Ordem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ordem: Ordem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ordens: List<Ordem>)

    @Update
    suspend fun update(ordem: Ordem)

    @Query("UPDATE ordens SET removido = 1, atualizadoEm = :now, sincronizado = 0 WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM ordens WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM ordens")
    suspend fun clearAll()
}
