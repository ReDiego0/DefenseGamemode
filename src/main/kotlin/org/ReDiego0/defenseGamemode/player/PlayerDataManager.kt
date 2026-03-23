package org.ReDiego0.defenseGamemode.player

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.database.StorageProvider
import org.ReDiego0.defenseGamemode.database.YamlStorage
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PlayerDataManager {
    private val cache = ConcurrentHashMap<UUID, PlayerData>()
    lateinit var storage: StorageProvider

    fun initialize(plugin: DefenseGamemode) {
        // Por ahora solo yml
        storage = YamlStorage(plugin)
        storage.init()
    }

    fun loadPlayerAsync(uuid: UUID) {
        Bukkit.getScheduler().runTaskAsynchronously(DefenseGamemode.instance, Runnable {
            val data = storage.loadPlayer(uuid)
            cache[uuid] = data
        })
    }

    fun savePlayerAsync(uuid: UUID, removeFromCache: Boolean = false) {
        val data = cache[uuid] ?: return
        Bukkit.getScheduler().runTaskAsynchronously(DefenseGamemode.instance, Runnable {
            storage.savePlayer(data)
            if (removeFromCache) {
                cache.remove(uuid)
            }
        })
    }

    fun getPlayerData(uuid: UUID): PlayerData? {
        return cache[uuid]
    }

    fun saveAll() {
        cache.values.forEach { storage.savePlayer(it) }
        storage.close()
    }
}