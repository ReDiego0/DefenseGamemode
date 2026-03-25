package org.ReDiego0.defenseGamemode.ui

import net.kyori.adventure.text.Component
import org.ReDiego0.defenseGamemode.combat.weapons.WeaponManager
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object WeaponSelectMenu {

    fun open(player: Player, slotIndex: Int) {
        val title = "§8Seleccionar Arma ($slotIndex)"
        val inv: Inventory = Bukkit.createInventory(null, 54, Component.text(title))
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return

        val availableWeapons = WeaponManager.getWeaponsForClass(data.currentClass)

        availableWeapons.forEachIndexed { index, weapon ->
            if (index >= 54) return@forEachIndexed

            val levelReqMet = data.level >= weapon.requiredLevel
            val missionsReqMet = data.missionsCompleted >= weapon.requiredMissions
            val isUnlocked = levelReqMet && missionsReqMet

            val item = ItemStack(if (isUnlocked) weapon.material else Material.BARRIER).apply {
                val meta = itemMeta
                meta.setDisplayName(weapon.displayName.replace("&", "§"))

                if (meta.hasCustomModelData()) {
                    meta.setCustomModelData(weapon.customModelData)
                }

                val lore = mutableListOf<String>()
                if (weapon.isExotic) lore.add("§d§lMÍTICA") else lore.add("§7Arma de Clase")
                lore.add("")

                if (isUnlocked) {
                    lore.add("§aClick para equipar")
                    lore.add("§8ID: ${weapon.id}")
                } else {
                    lore.add("§cBloqueada")
                    if (!levelReqMet) lore.add("§c- Nivel requerido: ${weapon.requiredLevel}")
                    if (!missionsReqMet) lore.add("§c- Misiones requeridas: ${weapon.requiredMissions}")
                }

                meta.lore = lore
                itemMeta = meta
            }
            inv.setItem(index, item)
        }

        player.openInventory(inv)
    }
}