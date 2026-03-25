package org.ReDiego0.defenseGamemode.ui

import net.kyori.adventure.text.Component
import org.ReDiego0.defenseGamemode.combat.equipment.ArmorManager
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object ArmorSelectMenu {

    fun open(player: Player) {
        val title = "§8Seleccionar Armadura"
        val inv: Inventory = Bukkit.createInventory(null, 54, Component.text(title))
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return

        val availableArmors = ArmorManager.getAllArmors().filter { armor ->
            val levelReqMet = data.level >= armor.requiredLevel
            val missionsReqMet = data.missionsCompleted >= armor.requiredMissions
            !armor.isExotic || (levelReqMet && missionsReqMet)
        }

        availableArmors.forEachIndexed { index, armor ->
            if (index >= 54) return@forEachIndexed

            val levelReqMet = data.level >= armor.requiredLevel
            val missionsReqMet = data.missionsCompleted >= armor.requiredMissions
            val isUnlocked = levelReqMet && missionsReqMet

            val item = ItemStack(if (isUnlocked) armor.iconMaterial else Material.BARRIER).apply {
                val meta = itemMeta
                meta.setDisplayName(armor.displayName)

                val lore = mutableListOf<String>()
                if (armor.isExotic) lore.add("§d§lMÍTICO") else lore.add("§7Set de Armadura")
                lore.add("")

                if (isUnlocked) {
                    lore.add("§aClick para equipar")
                    lore.add("§8ID: ${armor.id}")
                } else {
                    lore.add("§cBloqueado")
                    if (!levelReqMet) lore.add("§c- Nivel requerido: ${armor.requiredLevel}")
                    if (!missionsReqMet) lore.add("§c- Misiones requeridas: ${armor.requiredMissions}")
                }

                meta.lore = lore
                itemMeta = meta
            }
            inv.setItem(index, item)
        }

        player.openInventory(inv)
    }
}