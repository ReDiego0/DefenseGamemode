package org.ReDiego0.defenseGamemode.database

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.player.PlayerData
import org.ReDiego0.defenseGamemode.player.WeaponData
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

        val missionsCompleted = mutableMapOf<Int, Int>()
        val missionsSection = config.getConfigurationSection("missionsCompleted")
        if (missionsSection != null) {
            for (key in missionsSection.getKeys(false)) {
                val levelKey = key.toIntOrNull()
                if (levelKey != null) {
                    missionsCompleted[levelKey] = missionsSection.getInt(key)
                }
            }
        }

        val classLevels = mutableMapOf<String, Int>()
        val classLevelsSec = config.getConfigurationSection("classLevels")
        if (classLevelsSec != null) {
            for (key in classLevelsSec.getKeys(false)) {
                classLevels[key] = classLevelsSec.getInt(key)
            }
        }

        val classExperience = mutableMapOf<String, Double>()
        val classExpSec = config.getConfigurationSection("classExperience")
        if (classExpSec != null) {
            for (key in classExpSec.getKeys(false)) {
                classExperience[key] = classExpSec.getDouble(key)
            }
        }

        val unlockedWeapons = mutableMapOf<String, WeaponData>()
        val weaponsSection = config.getConfigurationSection("unlockedWeapons")
        if (weaponsSection != null) {
            for (key in weaponsSection.getKeys(false)) {
                val wLevel = weaponsSection.getInt("$key.level", 1)
                val wRarity = weaponsSection.getString("$key.rarity", "COMUN") ?: "COMUN"
                unlockedWeapons[key] = WeaponData(key, wLevel, wRarity)
            }
        }

        val equippedWeapons = config.getStringList("equippedWeapons").toMutableList()
        if (equippedWeapons.isEmpty()) equippedWeapons.addAll(listOf("", "", ""))

        val equippedArmor = config.getStringList("equippedArmor").toMutableList()
        if (equippedArmor.isEmpty()) equippedArmor.addAll(listOf("", "", "", ""))

        val equippedConsumables = config.getStringList("equippedConsumables").toMutableList()
        if (equippedConsumables.isEmpty()) equippedConsumables.addAll(listOf("", ""))

        return PlayerData(uuid, level, exp, kills, missionsCompleted, currentClass, classLevels, classExperience, unlockedClasses, unlockedWeapons, equippedWeapons, equippedArmor, equippedConsumables)
    }

    override fun savePlayer(playerData: PlayerData) {
        val file = File(dataFolder, "${playerData.uuid}.yml")
        val config = YamlConfiguration.loadConfiguration(file)

        config.set("level", playerData.level)
        config.set("experience", playerData.experience)
        config.set("totalKills", playerData.totalKills)

        val missionsStringKeys = playerData.missionsCompleted.mapKeys { it.key.toString() }
        config.set("missionsCompleted", missionsStringKeys)

        config.set("classLevels", playerData.classLevels)
        config.set("classExperience", playerData.classExperience)

        config.set("currentClass", playerData.currentClass)
        config.set("unlockedClasses", playerData.unlockedClasses.toList())

        val weaponsMap = playerData.unlockedWeapons.mapValues {
            mapOf("level" to it.value.level, "rarity" to it.value.rarity)
        }
        config.set("unlockedWeapons", weaponsMap)
        config.set("equippedWeapons", playerData.equippedWeapons)
        config.set("equippedArmor", playerData.equippedArmor)
        config.set("equippedConsumables", playerData.equippedConsumables)

        config.save(file)
    }

    override fun close() {
    }
}