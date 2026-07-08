package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visits")
data class Visit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val clientName: String,
    val address: String = "",
    val neighborhood: String = "",
    val date: Long, // timestamp
    val period: String, // "Manhã", "Tarde", "Noite"
    val status: String, // "A Realizar", "Realizada", "Cancelada"
    val notes: String = "",
    val kmsDriven: Double = 0.0
)
