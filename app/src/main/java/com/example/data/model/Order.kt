package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val visitId: Int? = null,
    val clientId: Int,
    val clientName: String,
    val date: Long,
    val paymentTerm: String = "",
    val notes: String = "", // details of order (items, quantities)
    val totalValue: Double = 0.0,
    val status: String = "Realizado" // "Realizado", "A Faturar", "Faturado", "Entregue"
)
