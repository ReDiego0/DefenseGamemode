package org.ReDiego0.defenseGamemode

import org.ReDiego0.defenseGamemode.command.DefenseCommand
import org.ReDiego0.defenseGamemode.config.MissionManager
import org.ReDiego0.defenseGamemode.config.MobManager
import org.ReDiego0.defenseGamemode.game.GameManager
import org.ReDiego0.defenseGamemode.game.MatchListener
import org.ReDiego0.defenseGamemode.player.PlayerDataListener
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.ReDiego0.defenseGamemode.setup.SetupListener
import org.ReDiego0.defenseGamemode.world.LocalWorldService
import org.bukkit.plugin.java.JavaPlugin

class DefenseGamemode : JavaPlugin() {

    companion object {
        lateinit var instance: DefenseGamemode
    }

    override fun onEnable() {
        logger.info("Iniciando DefenseGamemode...")
        instance = this
        LocalWorldService.initialize(this)
        PlayerDataManager.initialize(this)

        loadManagers()
        loadEvents()
        loadCommands()

        logger.info("DefenseGamemode habilitado correctamente!")
    }

    override fun onDisable() {
        PlayerDataManager.saveAll()
        GameManager.shutdownAllMatches()
        LocalWorldService.shutdownAllInstances()
        logger.info("DefenseGamemode deshabilitado correctamente.")
    }

    fun loadCommands() {
        getCommand("defense")?.setExecutor(DefenseCommand())
        getCommand("defense")?.setTabCompleter(DefenseCommand())
    }

    fun loadEvents() {
        server.pluginManager.registerEvents(SetupListener(), this)
        server.pluginManager.registerEvents(MatchListener(), this)
        server.pluginManager.registerEvents(PlayerDataListener(), this)
    }

    fun loadManagers() {
        MissionManager.loadMissions(this)
        MobManager.loadMobs(this)
    }
}