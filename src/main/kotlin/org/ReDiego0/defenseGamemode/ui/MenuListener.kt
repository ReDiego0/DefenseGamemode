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

        if (title.contains("Seleccionar Clase") || title.contains("Detalles de Clase") || title.contains("Inventario de Expedición")) {
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
            }

            else if (title.contains("Detalles de Clase")) {
                val classDisplayName = title.substringAfter(": ").trim()
                val targetClass = PlayerClass.entries.find { it.displayName == classDisplayName } ?: return

                if (itemName.contains("Equipar Clase")) {
                    val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
                    data.currentClass = targetClass.id
                    player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f)
                    player.sendMessage("§a¡Has equipado la clase ${targetClass.displayName}!")
                    player.closeInventory()
                }
                else if (itemName.contains("Árbol de Habilidades")) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    player.sendMessage("§eAbriendo el Árbol de Habilidades... (Próximamente)")
                }
                else if (itemName.contains("Volver")) {
                    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                    ClassMenu.openMainMenu(player)
                }
            }

            else if (title.contains("Inventario de Expedición")) {
                player.playSound(player.location, Sound.UI_BUTTON_CLICK, 1f, 1f)
                player.sendMessage("§eAbriendo lista de ítems disponibles... (Próximamente)")
            }
        }
    }
}