package org.ReDiego0.defenseGamemode.config

import org.ReDiego0.defenseGamemode.game.DifficultyProfile
import org.ReDiego0.defenseGamemode.setup.SetupSession
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
            val displayName = config.getString("display-name", id) ?: id
            val baseDifficulty = config.getInt("base-difficulty", 1)

            val profileString = config.getString("difficulty-profile", "PROGRESSIVE_CAPPED")?.uppercase() ?: "PROGRESSIVE_CAPPED"
            val difficultyProfile = try {
                DifficultyProfile.valueOf(profileString)
            } catch (e: IllegalArgumentException) {
                DifficultyProfile.PROGRESSIVE_CAPPED
            }

            val spawnX = config.getDouble("spawn.x")
            val spawnY = config.getDouble("spawn.y")
            val spawnZ = config.getDouble("spawn.z")
            val spawnYaw = config.getDouble("spawn.yaw", 0.0).toFloat()
            val spawnPitch = config.getDouble("spawn.pitch", 0.0).toFloat()
            val spawnRadius = config.getDouble("spawn.radius", 2.0)

            val targetX = config.getDouble("target.x")
            val targetY = config.getDouble("target.y")
            val targetZ = config.getDouble("target.z")

            val mobSpawnsList = config.getMapList("mob-spawns").mapNotNull { map ->
                val mx = (map["x"] as? Number)?.toDouble() ?: return@mapNotNull null
                val my = (map["y"] as? Number)?.toDouble() ?: return@mapNotNull null
                val mz = (map["z"] as? Number)?.toDouble() ?: return@mapNotNull null
                CustomLocation(mx, my, mz)
            }

            val mission = MissionConfig(
                id = id,
                displayName = displayName,
                templateName = templateName,
                maxPlayers = maxPlayers,
                baseDifficulty = baseDifficulty,
                difficultyProfile = difficultyProfile,
                spawnLocation = CustomLocation(spawnX, spawnY, spawnZ, spawnYaw, spawnPitch),
                spawnRadius = spawnRadius,
                targetLocation = CustomLocation(targetX, targetY, targetZ),
                mobSpawns = mobSpawnsList
            )

            missions[id] = mission
        }
    }

    fun getMission(id: String): MissionConfig? = missions[id]

    fun getAllMissions(): List<MissionConfig> = missions.values.toList()

    private fun createDefaultMission(folder: File) {
        val defaultFile = File(folder, "ejemplo_mapa.yml")
        val config = YamlConfiguration.loadConfiguration(defaultFile)

        config.set("display-name", "Defensa del Castillo")
        config.set("template-name", "mapa_base")
        config.set("max-players", 4)
        config.set("base-difficulty", 1)
        config.set("difficulty-profile", "PROGRESSIVE_CAPPED")
        config.set("spawn.x", 0.0)
        config.set("spawn.y", 64.0)
        config.set("spawn.z", 0.0)
        config.set("spawn.yaw", 0.0)
        config.set("spawn.pitch", 0.0)
        config.set("spawn.radius", 3.0)
        config.set("target.x", 10.0)
        config.set("target.y", 64.0)
        config.set("target.z", 10.0)

        val defaultMobSpawns = listOf(
            mapOf("x" to 20.0, "y" to 64.0, "z" to 20.0),
            mapOf("x" to -20.0, "y" to 64.0, "z" to -20.0)
        )
        config.set("mob-spawns", defaultMobSpawns)

        config.save(defaultFile)
    }

    fun saveSetupSession(plugin: Plugin, session: SetupSession) {
        val missionsFolder = File(plugin.dataFolder, "missions")
        if (!missionsFolder.exists()) missionsFolder.mkdirs()

        val file = File(missionsFolder, "${session.mapName}.yml")
        val config = YamlConfiguration.loadConfiguration(file)

        config.set("display-name", session.missionDisplayName)
        config.set("template-name", session.mapName)
        config.set("max-players", 4)
        config.set("base-difficulty", session.baseDifficulty)
        config.set("difficulty-profile", "PROGRESSIVE_CAPPED")

        session.playerSpawn?.let { loc ->
            config.set("spawn.x", loc.x)
            config.set("spawn.y", loc.y)
            config.set("spawn.z", loc.z)
            config.set("spawn.yaw", loc.yaw.toDouble())
            config.set("spawn.pitch", loc.pitch.toDouble())
            config.set("spawn.radius", 3.0)
        }

        session.targetSpawn?.let { loc ->
            config.set("target.x", loc.x)
            config.set("target.y", loc.y)
            config.set("target.z", loc.z)
        }

        val mobSpawnsList = session.mobSpawns.map { loc ->
            mapOf("x" to loc.x, "y" to loc.y, "z" to loc.z)
        }
        config.set("mob-spawns", mobSpawnsList)

        config.save(file)
        loadMissions(plugin)
    }
}