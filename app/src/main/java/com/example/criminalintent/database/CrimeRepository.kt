package com.example.criminalintent.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Executors

private const val DATABASE_NAME = "crime-database"
class CrimeRepository private constructor(context: Context){

    private val  database:CrimeDatabase = Room.databaseBuilder(
        context.applicationContext,
        CrimeDatabase::class.java,
        DATABASE_NAME
    ).addMigrations(migration_1_2).build()


    private val crimeDao = database.crimeDao()



    private val executor = Executors.newSingleThreadExecutor()
    private val filesDir = context.applicationContext.filesDir

    fun getAllCrimes():LiveData< List<Crime>>  = crimeDao.getAllCrimes()

    fun getPhotoFile(crime: Crime):File = File(filesDir,crime.photoFileName)





    fun getCrime(id:UUID):LiveData<Crime?> {



        return crimeDao.getCrime(id)
    }

    fun updateCrime(crime: Crime){
        executor.execute {
            crimeDao.updateCrime(crime)
        }

    }

    fun addCrime(crime: Crime){
        executor.execute {
            crimeDao.addCrime(crime)

        }

    }

    companion object{
       private  var INSTANCE:CrimeRepository? = null

        fun initialize(context: Context){
            if (INSTANCE == null){
                INSTANCE = CrimeRepository(context)
            }

        }

        fun get() :CrimeRepository{
            return INSTANCE ?:
            throw IllegalStateException("CrimeRepository must be initialized ")
        }
    }
}