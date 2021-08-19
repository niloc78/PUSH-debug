package com.example.push.db

import androidx.room.*

@Dao
interface EContactDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg contacts: EContact) : LongArray // returns row ids of inserted items

    @Delete
    suspend fun deleteAll(vararg contacts: EContact) : Int // return # of rows deleted

    @Update
    suspend fun updateAll(vararg contacts : EContact) : Int // return # of rows updated successfully

    @Query("SELECT * FROM econtacts")
    suspend fun getAllEContacts() : List<EContact>

    @Query("SELECT * FROM econtacts WHERE contactName LIKE :displayName LIMIT 1") // selects 1
    suspend fun findByName(displayName : String) : EContact

    @Query("SELECT * FROM econtacts WHERE priority <= :maxPriorityValue")
    suspend fun loadAllEContactsWithMorePriorityThanInclusive(maxPriorityValue : Int) : List<EContact> // less value priority = higher priority

    @Query("SELECT * FROM econtacts WHERE priority < :maxPriorityValue")
    suspend fun loadAllEContactsWithMorePriorityThanExclusive(maxPriorityValue : Int) : List<EContact> // less value priority = higher priority

    @Query("SELECT * FROM econtacts WHERE contactId = :id")
    suspend fun findById(id : Int) : EContact

    @Query("UPDATE econtacts SET priority = :newPrio WHERE contactId = :contactId")
    suspend fun updatePriority(newPrio : Int, contactId : Int)

    @Query("SELECT * FROM econtacts ORDER BY priority ASC")
    suspend fun getSortedListByPriority() : List<EContact>

    @Query("SELECT * FROM econtacts WHERE contactPhone = :phone")
    suspend fun findByPhone(phone : String) : EContact


}