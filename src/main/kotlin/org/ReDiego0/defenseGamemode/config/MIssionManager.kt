package org.ReDiego0.defenseGamemode.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File

object MissionManager {

    private val missions = mutableMapOf<String, MissionConfig>()

    fun loadMissions(plugin: Plugin) {
        missions.clear()
        val missionsFolder = File(plugin.dataFolder, "missions")

        if (!missionsFolder.exists()) {
            missionsFolder.mkdirs()
            createDefaultMission(missionsFolder)
        }

        missionsFolder.listFiles()?.filter { it.extension == "yml" }?.forEach { file ->
            val config = YamlConfiguration.loadConfiguration(file)
            val id = file.nameWithoutExtension

            val templateName = config.getString("template-name") ?: return@forEach
            val maxPlayers = config.getInt("max-players", 4)

            val spawnX = config.getDouble("spawn.x")
            val spawnY = config.getDouble("spawn.y")
            val spawnZ = config.getDouble("spawn.z")
            val spawnYaw = config.getDouble("spawn.yaw", 0.0).toFloat()
            val spawnPitch = config.getDouble("spawn.pitch", 0.0).toFloat()
            val spawnRadius = config.getDouble("spawn.radius", 2.0)

            val targetX = config.getDouble("target.x")
            val targetY = config.getDouble("target.y")
            val targetZ = config.getDouble("target.z")

            val mission = MissionConfig(
                id = id,
                templateName = templateName,
                maxPlayers = maxPlayers,
                spawnLocation = CustomLocation(spawnX, spawnY, spawnZ, spawnYaw, spawnPitch),
                spawnRadius = spawnRadius,
                targetLocation = CustomLocation(targetX, targetY, targetZ)
            )

            missions[id] = mission
        }
    }

    fun getMission(id: String): MissionConfig? = missions[id]

    fun getAllMissions(): List<MissionConfig> = missions.values.toList()

    private fun createDefaultMission(folder: File) {
        val defaultFile = File(folder, "ejemplo_mapa.yml")
        val config = YamlConfiguration.loadConfiguration(defaultFile)

        config.set("template-name", "mapa_base")
        config.set("max-players", 4)
        config.set("spawn.x", 0.0)
        config.set("spawn.y", 64.0)
        config.set("spawn.z", 0.0)
        config.set("spawn.yaw", 0.0)
        config.set("spawn.pitch", 0.0)
        config.set("spawn.radius", 3.0)
        config.set("target.x", 10.0)
        config.set("target.y", 64.0)
        config.set("target.z", 10.0)

        config.save(defaultFile)
    }
}