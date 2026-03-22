package org.ReDiego0.defenseGamemode.game

import org.bukkit.entity.Damageable
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent

class MatchListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        val match = GameManager.getMatch(entity.world.name) ?: return

        if (match.state != MatchState.ACTIVE_WAVE && entity is Player) {
            event.isCancelled = true
            return
        }

        if (entity is Player) {
            if (entity.health - event.finalDamage <= 0) {
                event.isCancelled = true
                match.handlePlayerDeath(entity)
            }
        } else {
            if (match.objective?.entity == entity) {
                val damageable = entity as? Damageable ?: return
                if (damageable.health - event.finalDamage <= 0) {
                    event.isCancelled = true
                    match.handleObjectiveDeath()
                } else {
                    org.bukkit.Bukkit.getScheduler().runTaskLater(org.ReDiego0.defenseGamemode.DefenseGamemode.instance, Runnable {
                        match.objective?.updateHealthDisplay()
                    }, 1L)
                }
            } else if (match.state == MatchState.ACTIVE_WAVE && entity is org.bukkit.entity.Mob) {
                if (event is org.bukkit.event.entity.EntityDamageByEntityEvent) {
                    var damager = event.damager
                    if (damager is org.bukkit.entity.Projectile && damager.shooter is Player) {
                        damager = damager.shooter as Player
                    }
                    if (damager is Player) {
                        match.waveManager?.setPlayerAggro(entity, damager)
                    }
                }
            }
        }
    }

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity is Player) return

        val match = GameManager.getMatch(entity.world.name) ?: return

        if (match.state == MatchState.ACTIVE_WAVE) {
            if (match.objective?.entity != entity) {
                match.waveManager?.handleMobDeath()
                event.drops.clear()
                event.droppedExp = 0
            }
        }
    }
}