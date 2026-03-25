package org.ReDiego0.defenseGamemode.ui

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.ReDiego0.defenseGamemode.combat.PlayerClass
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class MenuListener : Listener {

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val titleComponent = event.view.title()
        val title = PlainTextComponentSerializer.plainText().serialize(titleComponent)

        if (title.contains("Seleccionar Clase") || title.contains("Detalles de Clase") ||
            title.contains("Inventario de Expedición") || title.contains("Seleccionar Arma") ||
            title.contains("Seleccionar Armadura") || title.contains("Seleccionar Consumible") ||
            title.contains("Categorías Exóticas") || title.contains("Armas Exóticas")) {

            event.isCancelled = true

            val clickedItem = event.currentItem ?: return
            if (!clickedItem.hasItemMeta() || clickedItem.itemMeta.displayName.isBlank()) return

            val itemName = clickedItem.itemMeta.displayName
            val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return

            if (title.contains("Seleccionar Clase")) {
                val selectedClass = PlayerClass.entries.find { itemName.contains(it.displayName) } ?: return

                if (data.unlockedClasses.contains(selectedClass.id)) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    ClassMenu.openClassDetails(player, selectedClass.id)
                } else {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    player.sendMessage("§cEsta clase está bloqueada.")
                }
            } else if (title.contains("Detalles de Clase")) {
                val classDisplayName = title.substringAfter(": ").trim()
                val targetClass = PlayerClass.entries.find { it.displayName == classDisplayName } ?: return

                if (itemName.contains("Equipar Clase")) {
                    data.currentClass = targetClass.id
                    data.validateLoadoutOnClassChange()
                    PlayerDataManager.savePlayerAsync(player.uniqueId)

                    player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f)
                    player.sendMessage("§a¡Has equipado la clase ${targetClass.displayName}!")
                    player.closeInventory()
                } else if (itemName.contains("Árbol de Habilidades")) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    player.sendMessage("§eAbriendo el Árbol de Habilidades... (Próximamente)")
                } else if (itemName.contains("Volver")) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    ClassMenu.openMainMenu(player)
                }
            } else if (title.contains("Inventario de Expedición")) {
                val slot = event.rawSlot
                if (slot in 10..12) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    WeaponSelectMenu.open(player, slot - 10)
                } else if (slot == 20) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    ArmorSelectMenu.open(player)
                } else if (slot == 16) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    ConsumableSelectMenu.open(player, 0)
                } else if (slot == 25) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    ConsumableSelectMenu.open(player, 1)
                }
            } else if (title.contains("Seleccionar Armadura")) {
                if (clickedItem.type == org.bukkit.Material.BARRIER) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    return
                }

                val lore = clickedItem.itemMeta.lore ?: return
                val idLine = lore.find { it.startsWith("§8ID: ") } ?: return
                val armorId = idLine.substringAfter("§8ID: ")

                data.equippedArmor[0] = armorId
                PlayerDataManager.savePlayerAsync(player.uniqueId)

                player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f)
                player.sendMessage("§aArmadura equipada.")
                LoadoutMenu.openLoadout(player)
            } else if (title.contains("Seleccionar Consumible")) {
                if (clickedItem.type == org.bukkit.Material.BARRIER) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    return
                }

                val lore = clickedItem.itemMeta.lore ?: return
                val idLine = lore.find { it.startsWith("§8ID: ") } ?: return
                val consumableId = idLine.substringAfter("§8ID: ")
                val slotIndex = title.substringAfter("(").substringBefore(")").toIntOrNull() ?: 0

                data.equippedConsumables[slotIndex] = consumableId
                PlayerDataManager.savePlayerAsync(player.uniqueId)

                player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1f)
                player.sendMessage("§aConsumible equipado en la ranura ${slotIndex + 1}.")
                LoadoutMenu.openLoadout(player)
            } else if (title.contains("Seleccionar Arma")) {
                if (clickedItem.type == org.bukkit.Material.BARRIER) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    return
                }

                val lore = clickedItem.itemMeta.lore ?: return
                val idLine = lore.find { it.startsWith("§8ID: ") } ?: return
                val weaponId = idLine.substringAfter("§8ID: ")
                val slotIndex = title.substringAfter("(").substringBefore(")").toIntOrNull() ?: 0

                data.equippedWeapons[slotIndex] = weaponId
                PlayerDataManager.savePlayerAsync(player.uniqueId)

                player.playSound(player.location, Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f)
                player.sendMessage("§aArma equipada en la ranura ${slotIndex + 1}.")
                LoadoutMenu.openLoadout(player)
            } else if (title.contains("Categorías Exóticas")) {
                if (itemName.contains("Armas Exóticas")) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    ExoticMenu.openWeaponCategory(player)
                }
            } else if (title.contains("Armas Exóticas")) {
                if (itemName.contains("Volver")) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    ExoticMenu.openMainMenu(player)
                }
            }
        }
    }
}