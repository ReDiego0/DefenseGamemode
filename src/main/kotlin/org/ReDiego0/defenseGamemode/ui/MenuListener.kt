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
            title.contains("Categorías Exóticas") || title.contains("Armas Exóticas")) {

            event.isCancelled = true

            val clickedItem = event.currentItem ?: return
            if (!clickedItem.hasItemMeta() || clickedItem.itemMeta.displayName.isBlank()) return

            val itemName = clickedItem.itemMeta.displayName

            if (title.contains("Seleccionar Clase")) {
                val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
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
                    val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
                    data.currentClass = targetClass.id
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
                }
            } else if (title.contains("Seleccionar Arma")) {
                val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return

                if (clickedItem.type == org.bukkit.Material.BARRIER) {
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    return
                }

                val lore = clickedItem.itemMeta.lore ?: return
                val idLine = lore.find { it.startsWith("§8ID: ") } ?: return
                val weaponId = idLine.substringAfter("§8ID: ")
                val slotIndex = title.substringAfter("(").substringBefore(")").toIntOrNull() ?: 0

                data.equippedWeapons[slotIndex] = weaponId
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