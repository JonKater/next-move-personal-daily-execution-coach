package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.NextMoveDatabase
import com.example.data.NextMoveRepository

class NextMoveApplication : Application() {
    val database by lazy { 
        Room.databaseBuilder(this, NextMoveDatabase::class.java, "next_move_db")
            .addMigrations(NextMoveDatabase.MIGRATION_1_2)
            .build()
    }
    val repository by lazy { NextMoveRepository(database.dao()) }
}
