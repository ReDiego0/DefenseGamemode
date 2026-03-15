package org.ReDiego0.defenseGamemode.world

import org.bukkit.Bukkit
import org.bukkit.GameRule
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

    fun initialize(pluginInstance: Plugin) {
        plugin = pluginInstance
    }

    fun createInstanceAsync(templateName: String): CompletableFuture<World?> {
        val future = CompletableFuture<World?>()
        val instanceId = "defense_${templateName}_${UUID.randomUUID().toString().substring(0, 6)}"

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val serverFolder = plugin.server.worldContainer
            val templateFolder = File(serverFolder, templateName)
            val instanceFolder = File(serverFolder, instanceId)

            if (!templateFolder.exists() || !templateFolder.isDirectory) {
                future.complete(null)
                return@Runnable
            }

            templateFolder.copyRecursively(instanceFolder, overwrite = true)
            File(instanceFolder, "uid.dat").delete()
            File(instanceFolder, "session.lock").delete()

            Bukkit.getScheduler().runTask(plugin, Runnable {
                val creator = WorldCreator(instanceId)
                val world = Bukkit.createWorld(creator)

                if (world != null) {
                    world.isAutoSave = false
                    world.setChunkForceLoaded(0, 0, false)
                    activeInstances[instanceId] = templateName
                }

                future.complete(world)
            })
        })

        return future
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
                worldFolder.deleteRecursively()
            })
        })
    }

    fun shutdownAllInstances() {
        val instancesCopy = activeInstances.keys().toList()
        instancesCopy.forEach { instanceId ->
            deleteInstance(instanceId)
        }
    }
}