package com.noque.svampeatlas.daos

import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.services.RoomService

class HostsDaoInterface(private val dao: HostsDao) {
    suspend fun saveHosts(hosts: List<Host>) {
        dao.save(*hosts.toTypedArray())
    }

    suspend fun getAll(): Result<List<Host>, RoomService.Error> {
        dao.getHosts().also {
            return if(it.isEmpty()) {
                Result.Error(RoomService.Error.NoData(RoomService.Error.DataType.HOST))
            } else {
                Result.Success(it.toList())
            }
        }
    }

    fun getHostsWithIds(ids: List<Int>): Result<List<Host>, RoomService.Error> {
        val hosts = mutableListOf<Host>()

        ids.forEach {
            dao.getHostWithID(it)?.let {
                hosts.add(it)
            }
        }
        return if (hosts.isNotEmpty()) Result.Success(hosts) else Result.Error(
            RoomService.Error.NoData(
                RoomService.Error.DataType.HOST))
    }
}