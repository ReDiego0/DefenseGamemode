package org.ReDiego0.defenseGamemode.combat.manager

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.combat.PlayerClass
import org.ReDiego0.defenseGamemode.game.GameManager
import org.ReDiego0.defenseGamemode.game.Match
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

class ClassSkillManager {

    private val cooldowns = mutableMapOf<UUID, MutableMap<String, Long>>()

    fun executeSkillQ(player: Player) {
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
        val playerClass = PlayerClass.fromId(data.currentClass)
        val match = GameManager.getMatchByPlayer(player.uniqueId) ?: return

        if (isOnCooldown(player, "Q", getCooldownFor(playerClass, "Q"))) return

        when (playerClass) {
            PlayerClass.CABALLERO -> caballeroSkillQ(player, match)
            PlayerClass.MAGO -> magoSkillQ(player)
            PlayerClass.GUARDIAN -> guardianSkillQ(player, match)
            PlayerClass.INICIADO -> player.sendMessage("§cLos Iniciados no tienen habilidades.")
        }
    }

    fun executeSkillF(player: Player) {
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
        val playerClass = PlayerClass.fromId(data.currentClass)
        val match = GameManager.getMatchByPlayer(player.uniqueId) ?: return

        if (isOnCooldown(player, "F", getCooldownFor(playerClass, "F"))) return

        when (playerClass) {
            PlayerClass.CABALLERO -> caballeroSkillF(player, match)
            PlayerClass.MAGO -> magoSkillF(player)
            PlayerClass.GUARDIAN -> guardianSkillF(player, match)
            PlayerClass.INICIADO -> player.sendMessage("§cLos Iniciados no tienen habilidades.")
        }
    }

    private fun getDamageMultiplier(match: Match): Double {
        val diff = match.config?.baseDifficulty ?: 1
        val wave = match.currentWave
        return 1.0 + (diff * 0.2) + (wave * 0.15)
    }

    private fun getCooldownFor(playerClass: PlayerClass, key: String): Long {
        return when (playerClass) {
            PlayerClass.CABALLERO -> if (key == "Q") 8000L else 12000L
            PlayerClass.MAGO -> if (key == "Q") 25000L else 15000L
            PlayerClass.GUARDIAN -> if (key == "Q") 20000L else 10000L
            else -> 0L
        }
    }

    private fun isOnCooldown(player: Player, skill: String, cdTime: Long): Boolean {
        val playerCds = cooldowns.getOrPut(player.uniqueId) { mutableMapOf() }
        val expireTime = playerCds[skill] ?: 0L
        val now = System.currentTimeMillis()

        if (now < expireTime) {
            val left = (expireTime - now) / 1000
            player.sendMessage("§cHabilidad en enfriamiento. Espera ${left}s.")
            return true
        }
        playerCds[skill] = now + cdTime
        return false
    }

    private fun caballeroSkillQ(player: Player, match: Match) {
        player.world.playSound(player.location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f)
        val damage = 12.0 * getDamageMultiplier(match)
        val loc = player.location
        val direction = loc.direction.normalize()

        for (entity in player.world.getNearbyEntities(loc, 4.0, 2.0, 4.0)) {
            if (entity is LivingEntity && entity !is Player && entity != match.objective?.entity) {
                val toEntity = entity.location.toVector().subtract(loc.toVector())
                if (toEntity.dot(direction) > 0 && toEntity.length() <= 3.0) {
                    entity.damage(damage, player)
                    entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 120, 1))
                    entity.world.spawnParticle(Particle.SWEEP_ATTACK, entity.location.add(0.0, 1.0, 0.0), 1)
                }
            }
        }
    }

    private fun caballeroSkillF(player: Player, match: Match) {
        player.world.playSound(player.location, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.8f)
        val damage = 35.0 * getDamageMultiplier(match)
        val loc = player.location
        val direction = loc.direction.normalize()

        player.world.spawnParticle(Particle.CRIT, loc.add(direction.clone().multiply(2.0)).add(0.0, 1.0, 0.0), 30, 0.2, 1.5, 0.2, 0.1)

        for (entity in player.world.getNearbyEntities(loc, 2.0, 3.0, 2.0)) {
            if (entity is LivingEntity && entity !is Player && entity != match.objective?.entity) {
                val toEntity = entity.location.toVector().subtract(player.location.toVector())
                if (toEntity.dot(direction) > 0.7) {
                    entity.damage(damage, player)
                }
            }
        }
    }

    private fun magoSkillQ(player: Player) {
        player.world.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f)
        player.world.spawnParticle(Particle.END_ROD, player.location, 50, 4.0, 1.0, 4.0, 0.0)

        for (ally in player.world.getNearbyPlayers(player.location, 8.0)) {
            ally.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 600, 1))
            val maxHp = ally.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0
            ally.health = (ally.health + 8.0).coerceAtMost(maxHp)
            ally.sendMessage("§a¡Has sido bendecido por el Mago ${player.name}!")
            ally.world.spawnParticle(Particle.HEART, ally.location.add(0.0, 2.0, 0.0), 3, 0.5, 0.5, 0.5)
        }
    }

    private fun magoSkillF(player: Player) {
        player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f)
        val loc = player.location

        for (i in 0..360 step 20) {
            val rad = Math.toRadians(i.toDouble())
            val x = Math.cos(rad) * 6.0
            val z = Math.sin(rad) * 6.0
            player.world.spawnParticle(Particle.FLAME, loc.clone().add(x, 0.5, z), 2, 0.0, 0.0, 0.0, 0.05)
        }

        for (entity in player.world.getNearbyEntities(loc, 6.0, 3.0, 6.0)) {
            if (entity is LivingEntity && entity !is Player) {
                entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 100, 4))
                entity.fireTicks = 100
            }
        }
    }

    private fun guardianSkillQ(player: Player, match: Match) {
        player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f)
        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 200, 0))
        player.sendMessage("§6¡Has provocado a los enemigos cercanos!")

        object : BukkitRunnable() {
            var ticks = 0
            override fun run() {
                if (ticks >= 20 || !player.isOnline || player.isDead) {
                    this.cancel()
                    return
                }
                player.world.spawnParticle(Particle.ANGRY_VILLAGER, player.location.add(0.0, 2.5, 0.0), 1)

                for (entity in player.world.getNearbyEntities(player.location, 6.0, 4.0, 6.0)) {
                    if (entity is Mob && entity != match.objective?.entity) {
                        val pullDir = player.location.toVector().subtract(entity.location.toVector()).normalize().multiply(0.2)
                        entity.velocity = entity.velocity.add(pullDir)
                    }
                }
                ticks++
            }
        }.runTaskTimer(DefenseGamemode.instance, 0L, 10L)
    }

    private fun guardianSkillF(player: Player, match: Match) {
        player.world.playSound(player.location, Sound.ENTITY_BAT_TAKEOFF, 1f, 0.8f)
        val direction = player.location.direction.normalize().setY(0.1).multiply(1.5)
        player.velocity = direction

        val damage = 20.0 * getDamageMultiplier(match)

        object : BukkitRunnable() {
            var ticks = 0
            val hitEntities = mutableSetOf<UUID>()

            override fun run() {
                if (ticks >= 10 || !player.isOnline) {
                    this.cancel()
                    return
                }

                player.world.spawnParticle(Particle.CLOUD, player.location, 5, 0.2, 0.2, 0.2, 0.0)

                for (entity in player.world.getNearbyEntities(player.location, 1.5, 1.5, 1.5)) {
                    if (entity is LivingEntity && entity !is Player && entity != match.objective?.entity) {
                        if (hitEntities.add(entity.uniqueId)) {
                            entity.damage(damage, player)
                            entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 60, 2))
                            val push = player.location.direction.normalize().multiply(0.8).setY(0.2)
                            entity.velocity = push
                        }
                    }
                }
                ticks++
            }
        }.runTaskTimer(DefenseGamemode.instance, 0L, 1L)
    }
}