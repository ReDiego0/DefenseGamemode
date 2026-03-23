package org.ReDiego0.defenseGamemode.database

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.player.PlayerData
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

class YamlStorage(private val plugin: DefenseGamemode) : StorageProvider {
    private val dataFolder = File(plugin.dataFolder, "playerdata")

    override fun init() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
    }

    override fun loadPlayer(uuid: UUID): PlayerData {
        val file = File(dataFolder, "$uuid.yml")
        if (!file.exists()) {
            return PlayerData(uuid)
        }

        val config = YamlConfiguration.loadConfiguration(file)
        val level = config.getInt("level", 1)
        val exp = config.getDouble("experience", 0.0)
        val kills = config.getInt("totalKills", 0)
        val currentClass = config.getString("currentClass", "iniciado") ?: "iniciado"

        val unlockedClasses = config.getStringList("unlockedClasses").toMutableSet()
        if (unlockedClasses.isEmpty()) unlockedClasses.add("iniciado")

        return PlayerData(uuid, level, exp, kills, currentClass, unlockedClasses)
    }

    override fun savePlayer(playerData: PlayerData) {
        val file = File(dataFolder, "${playerData.uuid}.yml")
        val config = YamlConfiguration.loadConfiguration(file)

        config.set("level", playerData.level)
        config.set("experience", playerData.experience)
        config.set("totalKills", playerData.totalKills)
        config.set("currentClass", playerData.currentClass)
        config.set("unlockedClasses", playerData.unlockedClasses.toList())

        config.save(file)
    }

    override fun close() {
    }
}