package org.ReDiego0.defenseGamemode.ui

import net.kyori.adventure.text.Component
import org.ReDiego0.defenseGamemode.combat.equipment.ConsumableManager
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object ConsumableSelectMenu {

    fun open(player: Player, slotIndex: Int) {
        val title = "§8Seleccionar Consumible ($slotIndex)"
        val inv: Inventory = Bukkit.createInventory(null, 54, Component.text(title))
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return

        val availableConsumables = ConsumableManager.getAllConsumables().filter { consumable ->
            val levelReqMet = data.level >= consumable.requiredLevel
            val missionsReqMet = data.getTotalMissionsCompleted() >= consumable.requiredMissions
            !consumable.isExotic || (levelReqMet && missionsReqMet)
        }

        availableConsumables.forEachIndexed { index, consumable ->
            if (index >= 54) return@forEachIndexed

            val levelReqMet = data.level >= consumable.requiredLevel
            val missionsReqMet = data.getTotalMissionsCompleted() >= consumable.requiredMissions
            val isUnlocked = levelReqMet && missionsReqMet

            val item = ItemStack(if (isUnlocked) consumable.material else Material.BARRIER).apply {
                val meta = itemMeta
                meta.setDisplayName(consumable.displayName)

                val lore = mutableListOf<String>()
                if (consumable.isExotic) lore.add("§d§lMÍTICO") else lore.add("§7Suministro Táctico")
                lore.add("§7Cantidad por partida: §e${consumable.matchAmount}")
                lore.add("")

                if (isUnlocked) {
                    lore.add("§aClick para equipar")
                    lore.add("§8ID: ${consumable.id}")
                } else {
                    lore.add("§cBloqueado")
                    if (!levelReqMet) lore.add("§c- Nivel requerido: ${consumable.requiredLevel}")
                    if (!missionsReqMet) lore.add("§c- Misiones requeridas: ${consumable.requiredMissions}")
                }

                meta.lore = lore
                itemMeta = meta
            }
            inv.setItem(index, item)
        }

        player.openInventory(inv)
    }
}