package org.ReDiego0.defenseGamemode.ui

import net.kyori.adventure.text.Component
import org.ReDiego0.defenseGamemode.combat.PlayerClass
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object ClassMenu {

    const val MAIN_TITLE = "§8Seleccionar Clase"
    const val DETAILS_TITLE = "§8Detalles de Clase"

    fun openMainMenu(player: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 27, Component.text(MAIN_TITLE))
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return

        val border = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            val meta = itemMeta
            meta.setDisplayName(" ")
            itemMeta = meta
        }
        for (i in 0 until 27) inv.setItem(i, border)

        val classes = PlayerClass.entries
        val slots = listOf(10, 12, 14, 16)

        classes.forEachIndexed { index, playerClass ->
            if (index < slots.size) {
                val isUnlocked = data.unlockedClasses.contains(playerClass.id)
                val isEquipped = data.currentClass == playerClass.id

                val material = when {
                    isEquipped -> Material.NETHER_STAR
                    isUnlocked -> Material.EMERALD
                    else -> Material.REDSTONE
                }

                val item = ItemStack(material).apply {
                    val meta = itemMeta
                    meta.setDisplayName(if (isEquipped) "§a§l${playerClass.displayName} §7(Equipada)" else "§e§l${playerClass.displayName}")

                    val lore = mutableListOf<String>()
                    lore.add("§7${playerClass.description}")
                    lore.add("")
                    if (isUnlocked) lore.add("§aClick para ver opciones")
                    else lore.add("§cBloqueado (Requiere Nivel 5)")

                    meta.lore = lore
                    itemMeta = meta
                }
                inv.setItem(slots[index], item)
            }
        }
        player.openInventory(inv)
    }

    fun openClassDetails(player: Player, classId: String) {
        val playerClass = PlayerClass.fromId(classId)
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return

        val inv: Inventory = Bukkit.createInventory(null, 27, Component.text("$DETAILS_TITLE: ${playerClass.displayName}"))

        val icon = ItemStack(Material.BOOK).apply {
            val meta = itemMeta
            meta.setDisplayName("§6§l${playerClass.displayName}")
            meta.lore = listOf("§7${playerClass.description}")
            itemMeta = meta
        }
        inv.setItem(13, icon)

        val isEquipped = data.currentClass == playerClass.id
        val equipItem = ItemStack(if (isEquipped) Material.LIME_DYE else Material.GRAY_DYE).apply {
            val meta = itemMeta
            meta.setDisplayName(if (isEquipped) "§a§lEquipada" else "§e§lEquipar Clase")
            itemMeta = meta
        }
        inv.setItem(11, equipItem)

        val skillsItem = ItemStack(Material.ANVIL).apply {
            val meta = itemMeta
            meta.setDisplayName("§b§lÁrbol de Habilidades")
            meta.lore = listOf("§7Click para ver y mejorar", "§7las habilidades de esta clase.")
            itemMeta = meta
        }
        inv.setItem(15, skillsItem)

        val backItem = ItemStack(Material.ARROW).apply {
            val meta = itemMeta
            meta.setDisplayName("§cVolver")
            itemMeta = meta
        }
        inv.setItem(22, backItem)

        player.openInventory(inv)
    }
}