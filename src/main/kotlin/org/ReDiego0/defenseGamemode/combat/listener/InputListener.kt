package org.ReDiego0.defenseGamemode.combat.listener

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.combat.manager.CombatManager
import org.ReDiego0.defenseGamemode.combat.weapons.WeaponManager
import org.ReDiego0.defenseGamemode.game.GameManager
import org.ReDiego0.defenseGamemode.game.MatchState
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType

class InputListener(private val combatManager: CombatManager) : Listener {

    @EventHandler
    fun onSneak(event: PlayerToggleSneakEvent) {
        val player = event.player
        if (!event.isSneaking) return

        val match = GameManager.getMatchByPlayer(player.uniqueId)
        if (match != null && match.state == MatchState.ACTIVE_WAVE) {
            combatManager.handleDirectionalDash(player)
        }
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        val player = event.player
        val match = GameManager.getMatchByPlayer(player.uniqueId)

        if (match != null && match.state == MatchState.ACTIVE_WAVE) {
            event.isCancelled = true
            combatManager.handleSkill1(player)
        }
    }

    @EventHandler
    fun onSwapHand(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        val match = GameManager.getMatchByPlayer(player.uniqueId)

        if (match != null && match.state == MatchState.ACTIVE_WAVE) {
            event.isCancelled = true
            combatManager.handleSkill2(player)
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player

        if (event.hand != EquipmentSlot.HAND) return

        val match = GameManager.getMatchByPlayer(player.uniqueId)
        if (match != null && match.state == MatchState.ACTIVE_WAVE) {

            if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK ||
                event.action == Action.LEFT_CLICK_AIR || event.action == Action.LEFT_CLICK_BLOCK) {

                val item = player.inventory.itemInMainHand
                if (item.type.isAir) return
                val meta = item.itemMeta ?: return

                val consKey = NamespacedKey(DefenseGamemode.instance, "consumable_id")
                if (meta.persistentDataContainer.has(consKey, PersistentDataType.STRING)) {
                    event.isCancelled = true
                    val consumableId = meta.persistentDataContainer.get(consKey, PersistentDataType.STRING) ?: return

                    item.amount -= 1
                    combatManager.handleConsumable(player, consumableId)
                    return
                }

                val wpKey = NamespacedKey(DefenseGamemode.instance, "weapon_id")
                val weaponId = meta.persistentDataContainer.get(wpKey, PersistentDataType.STRING) ?: return

                val weapon = WeaponManager.getWeapon(weaponId) ?: return

                if (weapon.isExotic && weapon.skillId != null) {
                    combatManager.handleMythicWeapon(player, weapon.skillId)
                }
            }
        }
    }
}