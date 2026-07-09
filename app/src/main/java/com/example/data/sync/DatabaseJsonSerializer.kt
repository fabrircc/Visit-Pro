package com.example.data.sync

import com.example.data.model.Client
import com.example.data.model.Order
import com.example.data.model.Visit
import org.json.JSONArray
import org.json.JSONObject

object DatabaseJsonSerializer {
    fun serialize(clients: List<Client>, visits: List<Visit>, orders: List<Order>, email: String, backupTime: Long): String {
        val root = JSONObject()
        root.put("email", email)
        root.put("backupTime", backupTime)
        
        val clientsArray = JSONArray()
        for (c in clients) {
            val jobj = JSONObject()
            jobj.put("id", c.id)
            jobj.put("seq", c.seq)
            jobj.put("name", c.name)
            jobj.put("address", c.address)
            jobj.put("neighborhood", c.neighborhood)
            jobj.put("contact", c.contact)
            jobj.put("phone", c.phone)
            jobj.put("paymentTerm", c.paymentTerm)
            jobj.put("segment", c.segment)
            jobj.put("latitude", c.latitude)
            jobj.put("longitude", c.longitude)
            clientsArray.put(jobj)
        }
        root.put("clients", clientsArray)

        val visitsArray = JSONArray()
        for (v in visits) {
            val jobj = JSONObject()
            jobj.put("id", v.id)
            jobj.put("clientId", v.clientId)
            jobj.put("clientName", v.clientName)
            jobj.put("address", v.address)
            jobj.put("neighborhood", v.neighborhood)
            jobj.put("date", v.date)
            jobj.put("period", v.period)
            jobj.put("status", v.status)
            jobj.put("notes", v.notes)
            jobj.put("kmsDriven", v.kmsDriven)
            visitsArray.put(jobj)
        }
        root.put("visits", visitsArray)

        val ordersArray = JSONArray()
        for (o in orders) {
            val jobj = JSONObject()
            jobj.put("id", o.id)
            jobj.put("visitId", o.visitId ?: -1)
            jobj.put("clientId", o.clientId)
            jobj.put("clientName", o.clientName)
            jobj.put("date", o.date)
            jobj.put("paymentTerm", o.paymentTerm)
            jobj.put("notes", o.notes)
            jobj.put("totalValue", o.totalValue)
            jobj.put("status", o.status)
            ordersArray.put(jobj)
        }
        root.put("orders", ordersArray)

        return root.toString(2)
    }

    fun deserializeClients(jsonStr: String): List<Client> {
        val list = mutableListOf<Client>()
        try {
            val root = JSONObject(jsonStr)
            val array = root.optJSONArray("clients") ?: return emptyList()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Client(
                        id = o.optInt("id", 0),
                        seq = o.optString("seq", ""),
                        name = o.optString("name", ""),
                        address = o.optString("address", ""),
                        neighborhood = o.optString("neighborhood", ""),
                        contact = o.optString("contact", ""),
                        phone = o.optString("phone", ""),
                        paymentTerm = o.optString("paymentTerm", ""),
                        segment = o.optString("segment", "Geral"),
                        latitude = o.optDouble("latitude", 0.0),
                        longitude = o.optDouble("longitude", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun deserializeVisits(jsonStr: String): List<Visit> {
        val list = mutableListOf<Visit>()
        try {
            val root = JSONObject(jsonStr)
            val array = root.optJSONArray("visits") ?: return emptyList()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    Visit(
                        id = o.optInt("id", 0),
                        clientId = o.optInt("clientId", 0),
                        clientName = o.optString("clientName", ""),
                        address = o.optString("address", ""),
                        neighborhood = o.optString("neighborhood", ""),
                        date = o.optLong("date", 0L),
                        period = o.optString("period", "Manhã"),
                        status = o.optString("status", "A Realizar"),
                        notes = o.optString("notes", ""),
                        kmsDriven = o.optDouble("kmsDriven", 0.0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun deserializeOrders(jsonStr: String): List<Order> {
        val list = mutableListOf<Order>()
        try {
            val root = JSONObject(jsonStr)
            val array = root.optJSONArray("orders") ?: return emptyList()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val vIdVal = o.optInt("visitId", -1)
                val visitId = if (vIdVal == -1) null else vIdVal
                list.add(
                    Order(
                        id = o.optInt("id", 0),
                        visitId = visitId,
                        clientId = o.optInt("clientId", 0),
                        clientName = o.optString("clientName", ""),
                        date = o.optLong("date", 0L),
                        paymentTerm = o.optString("paymentTerm", ""),
                        notes = o.optString("notes", ""),
                        totalValue = o.optDouble("totalValue", 0.0),
                        status = o.optString("status", "Realizado")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
