package org.ReDiego0.defenseGamemode

import org.ReDiego0.defenseGamemode.world.LocalWorldService
import org.bukkit.plugin.java.JavaPlugin

class DefenseGamemode : JavaPlugin() {

    // TODO: Strings en un yml.
    override fun onEnable() {
        logger.info("Iniciando DefenseGamemode...")

        LocalWorldService.initialize(this)

        logger.info("DefenseGamemode habilitado correctamente!")
    }

    override fun onDisable() {
        LocalWorldService.shutdownAllInstances()
        logger.info("DefenseGamemode deshabilitado correctamente.")
    }
}