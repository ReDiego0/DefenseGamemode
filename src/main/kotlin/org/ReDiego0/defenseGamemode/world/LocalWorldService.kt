package org.ReDiego0.defenseGamemode.world

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object LocalWorldService {

    private lateinit var plugin: Plugin
    private val activeInstances = ConcurrentHashMap<String, String>()
    private lateinit var templatesFolder: File

    fun initialize(pluginInstance: Plugin) {
        plugin = pluginInstance
        templatesFolder = File(plugin.dataFolder, "templates")
        if (!templatesFolder.exists()) {
            templatesFolder.mkdirs()
        }
    }

    fun createInstanceAsync(templateName: String): CompletableFuture<World?> {
        val future = CompletableFuture<World?>()
        val instanceId = "defense_${templateName}_${UUID.randomUUID().toString().substring(0, 6)}"

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val templateFolder = File(templatesFolder, templateName)
            val serverFolder = plugin.server.worldContainer
            val instanceFolder = File(serverFolder, instanceId)

            if (!templateFolder.exists() || !templateFolder.isDirectory) {
                Bukkit.getScheduler().runTask(plugin, Runnable { future.complete(null) })
                return@Runnable
            }

            copyWorldFolder(templateFolder, instanceFolder)

            Bukkit.getScheduler().runTask(plugin, Runnable {
                val creator = WorldCreator(instanceId)
                val world = Bukkit.createWorld(creator)

                if (world != null) {
                    world.isAutoSave = false
                    activeInstances[instanceId] = templateName
                }

                future.complete(world)
            })
        })

        return future
    }

    fun loadSetupWorldAsync(templateName: String): CompletableFuture<World?> {
        val future = CompletableFuture<World?>()
        val setupInstanceId = "setup_$templateName"

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val templateFolder = File(templatesFolder, templateName)
            val serverFolder = plugin.server.worldContainer
            val instanceFolder = File(serverFolder, setupInstanceId)

            if (!templateFolder.exists() || !templateFolder.isDirectory) {
                Bukkit.getScheduler().runTask(plugin, Runnable { future.complete(null) })
                return@Runnable
            }

            if (instanceFolder.exists()) {
                deleteWorldFolderSafe(instanceFolder)
            }

            copyWorldFolder(templateFolder, instanceFolder)

            Bukkit.getScheduler().runTask(plugin, Runnable {
                val creator = WorldCreator(setupInstanceId)
                val world = Bukkit.createWorld(creator)

                if (world != null) {
                    world.isAutoSave = false
                }

                future.complete(world)
            })
        })

        return future
    }

    fun unloadSetupWorld(templateName: String) {
        val setupInstanceId = "setup_$templateName"
        val world = Bukkit.getWorld(setupInstanceId) ?: return

        val fallbackWorld = Bukkit.getWorlds().first()
        world.players.forEach { it.teleportAsync(fallbackWorld.spawnLocation) }

        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.unloadWorld(world, false)

            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val worldFolder = File(plugin.server.worldContainer, setupInstanceId)
                deleteWorldFolderSafe(worldFolder)
            })
        })
    }

    private fun copyWorldFolder(source: File, target: File) {
        val ignore = listOf("session.lock", "uid.dat", "level.dat_old")
        if (ignore.contains(source.name)) return

        if (source.isDirectory) {
            if (!target.exists()) target.mkdirs()
            source.listFiles()?.forEach { file ->
                val dest = File(target, file.name)
                copyWorldFolder(file, dest)
            }
        } else {
            try {
                source.copyTo(target, overwrite = true)
            } catch (e: Exception) {
            }
        }
    }

    fun deleteInstance(instanceId: String) {
        val world = Bukkit.getWorld(instanceId) ?: return
        val fallbackWorld = Bukkit.getWorlds().first()

        world.players.forEach { it.teleportAsync(fallbackWorld.spawnLocation) }

        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.unloadWorld(world, false)
            activeInstances.remove(instanceId)

            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                val worldFolder = File(plugin.server.worldContainer, instanceId)
                deleteWorldFolderSafe(worldFolder)
            })
        })
    }

    private fun deleteWorldFolderSafe(folder: File) {
        if (folder.isDirectory) {
            folder.listFiles()?.forEach { deleteWorldFolderSafe(it) }
        }
        try {
            folder.delete()
        } catch (e: Exception) {
        }
    }

    fun shutdownAllInstances() {
        val instancesCopy = activeInstances.keys().toList()
        instancesCopy.forEach { instanceId ->
            deleteInstance(instanceId)
        }
    }
}