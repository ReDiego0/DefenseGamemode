package org.ReDiego0.defenseGamemode.ui

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object LoadoutMenu {

    const val TITLE = "§8Inventario de Expedición"

    fun openLoadout(player: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 36, Component.text(TITLE))

        val bg = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            val meta = itemMeta; meta.setDisplayName(" "); itemMeta = meta
        }
        for (i in 0 until 36) inv.setItem(i, bg)

        val weaponSlot = createSlotItem("§c§lArma", "§7Click para seleccionar un arma.")
        inv.setItem(10, weaponSlot)
        inv.setItem(11, weaponSlot)
        inv.setItem(12, weaponSlot)

        val armorSlot = createSlotItem("§b§lArmadura", "§7Click para equipar armadura.")
        inv.setItem(19, armorSlot)
        inv.setItem(20, armorSlot)
        inv.setItem(21, armorSlot)
        inv.setItem(22, armorSlot)

        val potionSlot = createSlotItem("§d§lConsumible", "§7Click para equipar pociones.")
        inv.setItem(16, potionSlot)
        inv.setItem(25, potionSlot)

        player.openInventory(inv)
    }

    private fun createSlotItem(name: String, loreLine: String): ItemStack {
        return ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE).apply {
            val meta = itemMeta
            meta.setDisplayName(name)
            meta.lore = listOf(loreLine)
            itemMeta = meta
        }
    }
}