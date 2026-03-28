package org.ReDiego0.defenseGamemode.ui

import net.kyori.adventure.text.Component
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.ReDiego0.defenseGamemode.player.progression.ProgressionManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object ProgressionTreeMenu {

    val SNAKE_SLOTS = intArrayOf(
        1, 4, 7,      // Nivel 1, 2, 3
        16, 13, 10,   // Nivel 4, 5, 6
        19, 22, 25,   // Nivel 7, 8, 9
        34, 31, 28,   // Nivel 10, 11, 12
        37, 40, 43,   // Nivel 13, 14, 15
        52, 49, 46    // Nivel 16, 17, 18
    )

    fun openTree(player: Player, branch: String) {
        val title = if (branch.lowercase() == "equipamiento") "§8Árbol de Equipamiento" else "§8Árbol de Habilidades"
        val inv: Inventory = Bukkit.createInventory(null, 54, Component.text(title))
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return

        val classId = data.currentClass
        val currentLevel = data.getClassLevel(classId)
        val rewards = ProgressionManager.getBranchRewards(classId, branch)
        val claimedKey = "${classId}_${branch.lowercase()}"
        val claimedSet = data.claimedClassRewards.getOrDefault(claimedKey, emptySet())

        val bg = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            val meta = itemMeta; meta.setDisplayName(" "); itemMeta = meta
        }
        for (i in 0 until 54) inv.setItem(i, bg)

        for (i in 0 until 18) {
            val level = i + 1
            val slot = SNAKE_SLOTS[i]
            val reward = rewards[level]

            val isUnlocked = currentLevel >= level
            val isClaimed = claimedSet.contains(level)

            val item = if (reward != null) {
                ItemStack(if (isClaimed) Material.MINECART else if (isUnlocked) reward.icon else Material.GRAY_DYE).apply {
                    val meta = itemMeta
                    meta.setDisplayName(if (isClaimed) "§a§m${reward.displayName.replace("§", "&")}" else reward.displayName)

                    val lore = mutableListOf<String>()
                    lore.addAll(reward.lore)
                    lore.add("")

                    if (isClaimed) {
                        lore.add("§a✔ Ya reclamado")
                    } else if (isUnlocked) {
                        lore.add("§e¡Haz click para desbloquear!")
                    } else {
                        lore.add("§cBloqueado")
                        lore.add("§cRequiere nivel de clase: $level")
                    }

                    meta.lore = lore
                    itemMeta = meta
                }
            } else {
                ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE).apply {
                    val meta = itemMeta
                    meta.setDisplayName("§7Nivel $level")
                    meta.lore = listOf("§8Sin recompensa")
                    itemMeta = meta
                }
            }

            inv.setItem(slot, item)
        }

        player.openInventory(inv)
    }
}