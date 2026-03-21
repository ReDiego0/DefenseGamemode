package org.ReDiego0.defenseGamemode

import org.ReDiego0.defenseGamemode.command.DefenseCommand
import org.ReDiego0.defenseGamemode.config.MissionManager
import org.ReDiego0.defenseGamemode.world.LocalWorldService
import org.bukkit.plugin.java.JavaPlugin

class DefenseGamemode : JavaPlugin() {

    companion object {
        lateinit var instance: DefenseGamemode
    }

    override fun onEnable() {
        logger.info("Iniciando DefenseGamemode...")

        loadManagers()
        loadCommands()

        LocalWorldService.initialize(this)

        logger.info("DefenseGamemode habilitado correctamente!")
    }

    override fun onDisable() {
        LocalWorldService.shutdownAllInstances()
        logger.info("DefenseGamemode deshabilitado correctamente.")
    }

    fun loadCommands() {
        getCommand("defense")?.setExecutor(DefenseCommand())
        getCommand("defense")?.setTabCompleter(DefenseCommand())
    }

    fun loadEvents() {
        null
    }

    fun loadManagers() {
        MissionManager.loadMissions(this)
    }
}