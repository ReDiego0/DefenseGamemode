package org.ReDiego0.defenseGamemode.setup

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.config.MissionManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SetupManager {
    private val sessions = ConcurrentHashMap<UUID, SetupSession>()

    fun startSetup(player: Player, mapName: String) {
        if (sessions.containsKey(player.uniqueId)) {
            player.sendMessage("§cYa estás en modo setup.")
            return
        }

        val session = SetupSession(
            mapName,
            player.inventory.contents.clone(),
            player.inventory.armorContents.clone()
        )
        sessions[player.uniqueId] = session

        player.inventory.clear()
        giveSetupItems(player)
        player.sendMessage("§aModo setup iniciado para el mapa §b$mapName§a.")
    }

    fun endSetup(player: Player, save: Boolean = false) {
        val session = sessions.remove(player.uniqueId) ?: return

        player.inventory.contents = session.originalInventory
        player.inventory.armorContents = session.originalArmor

        if (save) {
            MissionManager.saveSetupSession(DefenseGamemode.instance, session)
            player.sendMessage("§a¡Setup validado y guardado correctamente! Misión lista.")
        } else {
            player.sendMessage("§cSetup cancelado. Inventario restaurado.")
        }
    }

    fun getSession(playerUuid: UUID): SetupSession? = sessions[playerUuid]

    private fun giveSetupItems(player: Player) {
        player.inventory.setItem(0, createItem(Material.COMPASS, "§aFijar Spawn Jugadores", "§7Click D: §eFijar", "§7Shift + Click D: §cBorrar"))
        player.inventory.setItem(1, createItem(Material.BEACON, "§bFijar Spawn Objetivo", "§7Click D: §eFijar", "§7Shift + Click D: §cBorrar"))
        player.inventory.setItem(2, createItem(Material.ZOMBIE_SPAWN_EGG, "§cFijar Spawn Mobs", "§7Click D: §eAñadir (Mín 2, Máx 6)", "§7Shift + Click D: §cBorrar todos"))
        player.inventory.setItem(4, createItem(Material.TNT, "§cBorrar Todos los Puntos", "§7Click D: §eReiniciar setup"))
        player.inventory.setItem(7, createItem(Material.EMERALD, "§aGuardar y Finalizar", "§7Click D: §eCompletar setup"))
        player.inventory.setItem(8, createItem(Material.BARRIER, "§cCancelar Setup", "§7Click D: §eAbortar"))
    }

    private fun createItem(material: Material, name: String, vararg lore: String): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false))
        meta.lore(lore.map { Component.text(it).decoration(TextDecoration.ITALIC, false) })
        item.itemMeta = meta
        return item
    }
}