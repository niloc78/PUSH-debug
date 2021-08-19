package com.example.push

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.push.db.EContact
import com.example.push.db.EContactDAO
import com.example.push.db.EContactDB
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EContactDBTest : TestCase() {

    private lateinit var db : EContactDB
    private lateinit var dao : EContactDAO


    @Before //called before each test function
    public override fun setUp() {
        Log.d("setup", "called")
       val context = ApplicationProvider.getApplicationContext<Context>() // application context
       db = Room.inMemoryDatabaseBuilder(context, EContactDB::class.java).build() // db is killed when process is killed
       dao = db.econtactDAO()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertTestEContactAndRead()  = runBlocking { // use runBlocking when calling any suspend function
        val testId = 4234
        val testPic = BitmapFactory.decodeResource(ApplicationProvider.getApplicationContext<Context>().resources, R.drawable.default_contact_pic)
        val testName = "Cosie"
        val testPhone = "(347) 986-3937"
        val testPriority = 0
        val testEC = EContact(testId, testPic, testName, testPhone, testPriority)
        val numInserted = dao.insertAll(testEC)[0]
        Log.d("id of contacts inserted", "" + numInserted)
        //val result = dao.findById(4243)
        val results = dao.getAllEContacts()
        Log.d("results", results[0].toString())
        assertNotNull(results)
    }

    @Test
    fun insertAndFindById() = runBlocking {
        val testId = 4234
        val testPic = BitmapFactory.decodeResource(ApplicationProvider.getApplicationContext<Context>().resources, R.drawable.default_contact_pic)
        val testName = "Cosie"
        val testPhone = "(347) 986-3937"
        val testPriority = 0
        val testEC = EContact(testId, testPic, testName, testPhone, testPriority)
        val numInserted = dao.insertAll(testEC)[0]
        Log.d("id of contacts inserted", "" + numInserted)
        val result = dao.findById(4234)
        Log.d("result", result.toString())
        assertEquals(result, testEC)
    }

    @Test
    fun testGetNull()  = runBlocking {
        val result = dao.findById(4234)
        Log.d("result", result.toString())
        assertNotNull(result)
    }

    @Test
    fun insertMultipleAndGetOrdered() = runBlocking {
        val testPic = BitmapFactory.decodeResource(ApplicationProvider.getApplicationContext<Context>().resources, R.drawable.default_contact_pic)
        val testEC1 = EContact(1234, testPic, "cosie", "(347) 986-3937", 1)
        val testEC2 = EContact(4234, testPic, "brosie", "(347) 996-3457", 0)
        val testEC3 = EContact(5312, testPic, "kotlin", "(347) 864-2445", 4)
        val testEC4 = EContact(6224, testPic, "design", "(347) 296-3357", 3)
        val testEC5 = EContact(7234, testPic, "god", "(347) 976-3447", 2)

        dao.insertAll(testEC1, testEC2, testEC3, testEC4, testEC5)
        val numsInserted = dao.insertAll(testEC1, testEC2, testEC3, testEC4, testEC5)
        Log.d("id of contacts inserted", numsInserted.joinToString())

        val results = dao.getSortedListByPriority()
        Log.d("results", results.joinToString())

        assertTrue(results.isNotEmpty())
    }

    @Test
    fun updateAllPriorities() = runBlocking {
        val testPic = BitmapFactory.decodeResource(ApplicationProvider.getApplicationContext<Context>().resources, R.drawable.default_contact_pic)
        val testEC1 = EContact(1234, testPic, "cosie", "(347) 986-3937", 1)
        val testEC2 = EContact(4234, testPic, "brosie", "(347) 996-3457", 0)
        val testEC3 = EContact(5312, testPic, "kotlin", "(347) 864-2445", 4)
        val testEC4 = EContact(6224, testPic, "design", "(347) 296-3357", 3)
        val testEC5 = EContact(7234, testPic, "god", "(347) 976-3447", 2)

        dao.insertAll(testEC1, testEC2, testEC3, testEC4, testEC5)
        val numsInserted = dao.insertAll(testEC1, testEC2, testEC3, testEC4, testEC5)
        Log.d("id of contacts inserted", numsInserted.joinToString())

        testEC1.priority = 2
        testEC2.priority = 4
        testEC3.priority = 1
        testEC4.priority = 0
        testEC5.priority = 3

        dao.updateAll(testEC1, testEC2, testEC3, testEC4, testEC5)

        val results = dao.getSortedListByPriority()
        Log.d("results", results.joinToString())

        assertTrue(results.isNotEmpty())

    }

    @Test
    fun updateOnePriority() = runBlocking {
        val testEC = EContact(1234, BitmapFactory.decodeResource(ApplicationProvider.getApplicationContext<Context>().resources, R.drawable.default_contact_pic),
            "cosie", "(347) 986-3937", 1)
        dao.insertAll(testEC)

        Log.d("EC to Insert: ", testEC.toString())

        val numInserted = dao.insertAll(testEC)
        Log.d("id of contact inserted", numInserted.joinToString())

        dao.updatePriority(0, 1234)
        val result = dao.findById(1234)
        Log.d("result", result.toString())

        assertTrue(result.priority == 0)
    }

    @Test
    fun getFromEmptyDB() = runBlocking {
        val result = dao.getSortedListByPriority()
        assertNotNull(result)
    }


}