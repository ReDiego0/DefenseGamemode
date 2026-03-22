package org.ReDiego0.defenseGamemode.ui

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import net.kyori.adventure.text.Component
import org.ReDiego0.defenseGamemode.game.Match
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object VoteMenu {

    fun open(player: Player, match: Match) {
        val gui = ChestGui(3, "§8¿Extraer o Continuar?")
        gui.setOnGlobalClick { event -> event.isCancelled = true }

        val pane = StaticPane(0, 0, 9, 3)

        val stayItem = ItemStack(Material.EMERALD_BLOCK)
        val stayMeta = stayItem.itemMeta
        stayMeta.displayName(Component.text("§a§lQUEDARSE Y CONTINUAR"))
        stayMeta.lore(listOf(Component.text("§7Seguir luchando para obtener"), Component.text("§7mejores recompensas.")))
        stayItem.itemMeta = stayMeta

        val stayGuiItem = GuiItem(stayItem) { _ ->
            match.votes[player.uniqueId] = true
            player.sendMessage("§aHas votado por: §lQUEDARSE")
            player.closeInventory()
        }

        val leaveItem = ItemStack(Material.REDSTONE_BLOCK)
        val leaveMeta = leaveItem.itemMeta
        leaveMeta.displayName(Component.text("§c§lEXTRAER Y SALIR"))
        leaveMeta.lore(listOf(Component.text("§7Asegurar el botín actual"), Component.text("§7y volver al lobby.")))
        leaveItem.itemMeta = leaveMeta

        val leaveGuiItem = GuiItem(leaveItem) { _ ->
            match.votes[player.uniqueId] = false
            player.sendMessage("§cHas votado por: §lEXTRAER")
            player.closeInventory()
        }

        pane.addItem(stayGuiItem, 2, 1)
        pane.addItem(leaveGuiItem, 6, 1)

        gui.addPane(pane)
        gui.show(player)
    }
}