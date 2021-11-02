package com.example.criminalintent.database

import android.app.Application

class CriminalIntentApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        CrimeRepository.initialize(this)
    }
}