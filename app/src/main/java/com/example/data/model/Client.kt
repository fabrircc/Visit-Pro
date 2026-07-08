package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val seq: String = "",
    val name: String,
    val address: String = "",
    val neighborhood: String = "",
    val contact: String = "",
    val phone: String = "",
    val paymentTerm: String = "",
    val segment: String = "Geral",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
