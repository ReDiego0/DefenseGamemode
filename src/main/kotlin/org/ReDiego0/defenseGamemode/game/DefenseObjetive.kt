package org.ReDiego0.defenseGamemode.game

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.config.MissionConfig
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.scheduler.BukkitTask
import kotlin.random.Random

class DefenseObjective(
    private val match: Match,
    private val config: MissionConfig,
    private val baseMaxHealth: Double = 500.0,
    private val healPercentagePerWave: Double = 0.10
) {
    var entity: Mob? = null
        private set

    private var aiTask: BukkitTask? = null

    fun spawn() {
        val loc = config.targetLocation.toBukkitLocation(match.world)

        val spawned = loc.world.spawnEntity(loc, EntityType.VILLAGER) as? Mob ?: return
        entity = spawned

        setupStats(spawned)
        setupAI(spawned)
        setupHologram(spawned)
    }

    private fun setupStats(mob: Mob) {
        val diff = config.baseDifficulty
        val healthPenalty = (diff - 1) * 60.0
        val finalHealth = (baseMaxHealth - healthPenalty).coerceAtLeast(50.0)

        mob.getAttribute(Attribute.MAX_HEALTH)?.baseValue = finalHealth
        mob.health = finalHealth
        mob.removeWhenFarAway = false
    }

    private fun setupAI(mob: Mob) {
        val diff = config.baseDifficulty

        when {
            diff in 1..4 -> {
                mob.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.0
                mob.isAware = false
            }
            diff in 5..6 -> {
                mob.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.25
                startRadiusWanderTask(mob)
            }
            diff >= 7 -> {
                mob.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = 0.35
            }
        }
    }

    private fun startRadiusWanderTask(mob: Mob) {
        aiTask = Bukkit.getScheduler().runTaskTimer(DefenseGamemode.instance, Runnable {
            if (mob.isDead || !mob.isValid) {
                aiTask?.cancel()
                return@Runnable
            }

            val center = config.targetLocation.toBukkitLocation(match.world)
            if (mob.location.distanceSquared(center) > 100.0) {
                mob.pathfinder.moveTo(center)
            } else if (Random.nextBoolean()) {
                val offsetX = (Random.nextDouble() * 10) - 5
                val offsetZ = (Random.nextDouble() * 10) - 5
                val randomLoc = mob.location.clone().add(offsetX, 0.0, offsetZ)
                mob.pathfinder.moveTo(randomLoc)
            }
        }, 0L, 60L)
    }

    private fun setupHologram(mob: Mob) {
        val customName = Component.text()
            .append(Component.text("✦ OBJETIVO ✦", NamedTextColor.AQUA, TextDecoration.BOLD))
            .build()
        mob.customName(customName)
        mob.isCustomNameVisible = true
    }

    fun healEndOfWave() {
        val currentMob = entity ?: return
        if (currentMob.isDead) return

        val maxHealth = currentMob.getAttribute(Attribute.MAX_HEALTH)?.value ?: return
        val healAmount = maxHealth * healPercentagePerWave

        currentMob.health = (currentMob.health + healAmount).coerceAtMost(maxHealth)
    }

    fun cleanUp() {
        aiTask?.cancel()
        entity?.remove()
    }
}