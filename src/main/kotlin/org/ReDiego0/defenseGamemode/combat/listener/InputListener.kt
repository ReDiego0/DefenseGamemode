package org.ReDiego0.defenseGamemode.combat.listener

import org.ReDiego0.defenseGamemode.combat.manager.CombatManager
import org.ReDiego0.defenseGamemode.game.GameManager
import org.ReDiego0.defenseGamemode.game.MatchState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.EquipmentSlot

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
                // verificar si el ítem de la mano es Mítico y su habilidad
            }
        }
    }
}