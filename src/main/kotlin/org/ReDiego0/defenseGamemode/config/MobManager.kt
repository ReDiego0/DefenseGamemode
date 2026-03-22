package org.ReDiego0.defenseGamemode.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File

object MobManager {
    private val mobPools = mutableMapOf<String, MobPool>()

    fun loadMobs(plugin: Plugin) {
        mobPools.clear()
        val file = File(plugin.dataFolder, "mobs.yml")

        if (!file.exists()) {
            plugin.dataFolder.mkdirs()
            createDefaultMobsFile(file)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val poolsSection = config.getConfigurationSection("pools") ?: return

        poolsSection.getKeys(false).forEach { poolId ->
            val rawList = poolsSection.getStringList(poolId)
            val parsedMobs = rawList.mapNotNull { entry ->
                val parts = entry.split(";")
                if (parts.size >= 2) {
                    val id = parts[0]
                    val tier = parts[1].toIntOrNull() ?: 1
                    val weight = parts.getOrNull(2)?.toDoubleOrNull() ?: 50.0
                    MobEntry(id, tier, weight)
                } else null
            }
            mobPools[poolId] = MobPool(poolId, parsedMobs)
        }
    }

    fun getPool(id: String): MobPool? = mobPools[id]

    private fun createDefaultMobsFile(file: File) {
        val config = YamlConfiguration.loadConfiguration(file)
        config.set("pools.vanilla", listOf(
            "zombie;1;100",
            "skeleton;1;80",
            "spider;1;70",
            "creeper;2;40",
            "wither_skeleton;3;20"
        ))
        config.set("pools.medieval", listOf(
            "ia_soldado;1;100",
            "ia_arquero;1;80",
            "ia_caballero;2;40",
            "mm_rey_maldito;4;10"
        ))
        config.save(file)
    }
}