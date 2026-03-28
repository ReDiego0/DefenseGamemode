package org.ReDiego0.defenseGamemode

import org.ReDiego0.defenseGamemode.combat.CooldownManager
import org.ReDiego0.defenseGamemode.combat.listener.InputListener
import org.ReDiego0.defenseGamemode.combat.manager.CombatManager
import org.ReDiego0.defenseGamemode.command.DefenseCommand
import org.ReDiego0.defenseGamemode.config.MissionManager
import org.ReDiego0.defenseGamemode.config.MobManager
import org.ReDiego0.defenseGamemode.game.GameManager
import org.ReDiego0.defenseGamemode.game.MatchListener
import org.ReDiego0.defenseGamemode.player.PlayerDataListener
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.ReDiego0.defenseGamemode.setup.SetupListener
import org.ReDiego0.defenseGamemode.ui.MenuListener
import org.ReDiego0.defenseGamemode.utils.EconomyManager
import org.ReDiego0.defenseGamemode.world.LocalWorldService
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class DefenseGamemode : JavaPlugin() {

    companion object {
        lateinit var instance: DefenseGamemode
    }

    lateinit var cooldownManager: CooldownManager
        private set
    lateinit var combatManager: CombatManager
        private set

    override fun onEnable() {
        logger.info("Iniciando DefenseGamemode...")
        instance = this

        cooldownManager = CooldownManager()
        combatManager = CombatManager(this)
        EconomyManager.setupEconomy()

        LocalWorldService.initialize(this)
        PlayerDataManager.initialize(this)

        loadManagers()
        loadEvents()
        loadCommands()

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            DefenseExpansion().register()
            logger.info("¡PlaceholderAPI conectado exitosamente!")
        }

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
        server.pluginManager.registerEvents(InputListener(combatManager), this)
        server.pluginManager.registerEvents(MenuListener(), this)
    }

    fun loadManagers() {
        MissionManager.loadMissions(this)
        MobManager.loadMobs(this)
        org.ReDiego0.defenseGamemode.combat.weapons.WeaponManager.loadWeapons(this)
    }
}