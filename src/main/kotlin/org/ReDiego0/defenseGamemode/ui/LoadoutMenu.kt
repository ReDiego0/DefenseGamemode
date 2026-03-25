package org.ReDiego0.defenseGamemode.ui

import net.kyori.adventure.text.Component
import org.ReDiego0.defenseGamemode.combat.equipment.ArmorManager
import org.ReDiego0.defenseGamemode.combat.equipment.ConsumableManager
import org.ReDiego0.defenseGamemode.combat.weapons.WeaponManager
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object LoadoutMenu {

    const val TITLE = "§8Inventario de Expedición"

    fun openLoadout(player: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 36, Component.text(TITLE))
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return

        val bg = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            val meta = itemMeta; meta.setDisplayName(" "); itemMeta = meta
        }
        for (i in 0 until 36) inv.setItem(i, bg)

        for (i in 0..2) {
            val slotIndex = 10 + i
            val weaponId = data.equippedWeapons.getOrNull(i)

            if (!weaponId.isNullOrEmpty() && weaponId.isNotBlank()) {
                val weaponData = data.unlockedWeapons[weaponId]
                val item = WeaponManager.buildWeaponItem(weaponId, weaponData)
                if (item != null) {
                    val meta = item.itemMeta
                    val lore = meta.lore ?: mutableListOf()
                    lore.add("")
                    lore.add("§aClick para cambiar de arma.")
                    meta.lore = lore
                    item.itemMeta = meta
                    inv.setItem(slotIndex, item)
                    continue
                }
            }
            val weaponSlot = createSlotItem("§c§lArma", "§7Click para seleccionar un arma.")
            inv.setItem(slotIndex, weaponSlot)
        }

        val armorId = data.equippedArmor.firstOrNull { it.isNotBlank() }
        if (!armorId.isNullOrEmpty()) {
            val armorSet = ArmorManager.getArmorSet(armorId)
            if (armorSet != null) {
                val item = ItemStack(armorSet.iconMaterial).apply {
                    val meta = itemMeta
                    meta.setDisplayName(armorSet.displayName)
                    meta.lore = listOf("", "§aClick para cambiar armadura.")
                    itemMeta = meta
                }
                inv.setItem(20, item)
            } else {
                inv.setItem(20, createSlotItem("§b§lArmadura", "§7Click para equipar armadura."))
            }
        } else {
            inv.setItem(20, createSlotItem("§b§lArmadura", "§7Click para equipar armadura."))
        }

        val consumableSlots = listOf(16, 25)
        for (i in 0..1) {
            val slotIndex = consumableSlots[i]
            val consumableId = data.equippedConsumables.getOrNull(i)

            if (!consumableId.isNullOrEmpty() && consumableId.isNotBlank()) {
                val item = ConsumableManager.buildConsumableIcon(consumableId)
                if (item != null) {
                    val meta = item.itemMeta
                    val lore = meta.lore ?: mutableListOf()
                    lore.add("")
                    lore.add("§aClick para cambiar consumible.")
                    meta.lore = lore
                    item.itemMeta = meta
                    inv.setItem(slotIndex, item)
                    continue
                }
            }
            inv.setItem(slotIndex, createSlotItem("§d§lConsumible", "§7Click para equipar suministros."))
        }

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