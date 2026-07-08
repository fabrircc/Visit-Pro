package com.example.data.repository

import com.example.data.local.ClientDao
import com.example.data.local.VisitDao
import com.example.data.local.OrderDao
import com.example.data.model.Client
import com.example.data.model.Visit
import com.example.data.model.Order
import kotlinx.coroutines.flow.Flow

class ClientRepository(
    private val clientDao: ClientDao,
    private val visitDao: VisitDao,
    private val orderDao: OrderDao
) {
    val allClients: Flow<List<Client>> = clientDao.getAllClients()
    val allVisits: Flow<List<Visit>> = visitDao.getAllVisits()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()

    suspend fun getClientById(id: Int): Client? = clientDao.getClientById(id)
    suspend fun insertClient(client: Client): Long = clientDao.insertClient(client)
    suspend fun updateClient(client: Client) = clientDao.updateClient(client)
    suspend fun deleteClient(client: Client) = clientDao.deleteClient(client)
    suspend fun insertClients(clients: List<Client>) = clientDao.insertClients(clients)
    suspend fun deleteAllClients() = clientDao.deleteAllClients()

    suspend fun getVisitById(id: Int): Visit? = visitDao.getVisitById(id)
    suspend fun insertVisit(visit: Visit): Long = visitDao.insertVisit(visit)
    suspend fun updateVisit(visit: Visit) = visitDao.updateVisit(visit)
    suspend fun deleteVisit(visit: Visit) = visitDao.deleteVisit(visit)
    suspend fun deleteAllVisits() = visitDao.deleteAllVisits()

    suspend fun getOrderById(id: Int): Order? = orderDao.getOrderById(id)
    suspend fun insertOrder(order: Order): Long = orderDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)
    suspend fun deleteOrder(order: Order) = orderDao.deleteOrder(order)
    suspend fun deleteAllOrders() = orderDao.deleteAllOrders()
}
