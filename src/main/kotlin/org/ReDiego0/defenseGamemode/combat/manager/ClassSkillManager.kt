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
    private val activeCasts = mutableSetOf<UUID>()

    fun executeSkillQ(player: Player) {
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
        val playerClass = PlayerClass.fromId(data.currentClass)
        val match = GameManager.getMatchByPlayer(player.uniqueId) ?: return

        if (activeCasts.contains(player.uniqueId)) return
        if (isOnCooldown(player, "Q", getCooldownFor(playerClass, "Q"))) return

        when (playerClass) {
            PlayerClass.CABALLERO -> caballeroSkillQ(player, match)
            PlayerClass.MAGO -> magoSkillQ(player, match)
            PlayerClass.GUARDIAN -> guardianSkillQ(player, match)
            PlayerClass.INICIADO -> player.sendMessage("§cLos Iniciados no tienen habilidades.")
        }
    }

    fun executeSkillF(player: Player) {
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
        val playerClass = PlayerClass.fromId(data.currentClass)
        val match = GameManager.getMatchByPlayer(player.uniqueId) ?: return

        if (activeCasts.contains(player.uniqueId)) return
        if (isOnCooldown(player, "F", getCooldownFor(playerClass, "F"))) return

        when (playerClass) {
            PlayerClass.CABALLERO -> caballeroSkillF(player, match)
            PlayerClass.MAGO -> magoSkillF(player, match)
            PlayerClass.GUARDIAN -> guardianSkillF(player, match)
            PlayerClass.INICIADO -> player.sendMessage("§cLos Iniciados no tienen habilidades.")
        }
    }

    private fun castSkill(player: Player, ticks: Int, onTick: (Int) -> Unit, onFinish: () -> Unit) {
        activeCasts.add(player.uniqueId)
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, ticks, 4, false, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, ticks, 250, false, false, false))

        object : BukkitRunnable() {
            var currentTick = 0
            override fun run() {
                if (!player.isOnline || player.isDead) {
                    activeCasts.remove(player.uniqueId)
                    cancel()
                    return
                }

                if (currentTick >= ticks) {
                    activeCasts.remove(player.uniqueId)
                    onFinish()
                    cancel()
                    return
                }

                onTick(currentTick)
                currentTick++
            }
        }.runTaskTimer(DefenseGamemode.instance, 0L, 1L)
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
        castSkill(player, 10, { tick ->
            val pitch = 0.5f + (tick / 10f)
            player.world.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.5f, pitch)
            player.world.spawnParticle(Particle.CRIT, player.location.add(0.0, 1.0, 0.0), 2, 0.5, 0.5, 0.5, 0.0)
        }, {
            player.world.playSound(player.location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f)
            player.world.playSound(player.location, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.5f, 1.5f)

            val damage = 12.0 * getDamageMultiplier(match)
            val loc = player.location
            val direction = loc.direction.normalize()

            player.world.spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(direction).add(0.0, 1.0, 0.0), 5, 1.0, 0.2, 1.0, 0.0)

            for (entity in player.world.getNearbyEntities(loc, 4.0, 2.0, 4.0)) {
                if (entity is LivingEntity && entity !is Player && entity != match.objective?.entity) {
                    val toEntity = entity.location.toVector().subtract(loc.toVector())
                    if (toEntity.dot(direction) > 0 && toEntity.length() <= 3.0) {
                        entity.damage(damage, player)
                        entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 120, 1))
                        entity.world.spawnParticle(Particle.BLOCK, entity.location.add(0.0, 1.0, 0.0), 10, 0.2, 0.2, 0.2, 0.0, org.bukkit.Material.REDSTONE_BLOCK.createBlockData())
                    }
                }
            }
        })
    }

    private fun caballeroSkillF(player: Player, match: Match) {
        castSkill(player, 20, { tick ->
            val pitch = 0.5f + (tick / 20f)
            player.world.playSound(player.location, Sound.BLOCK_GRINDSTONE_USE, 0.5f, pitch)
            player.world.spawnParticle(Particle.LARGE_SMOKE, player.location.add(0.0, 2.0, 0.0), 2, 0.2, 0.2, 0.2, 0.0)
        }, {
            player.world.playSound(player.location, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.8f)
            player.world.playSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f)

            val damage = 35.0 * getDamageMultiplier(match)
            val loc = player.location
            val direction = loc.direction.normalize()
            val impactLoc = loc.add(direction.clone().multiply(2.0)).add(0.0, 0.5, 0.0)

            player.world.spawnParticle(Particle.EXPLOSION, impactLoc, 1)
            player.world.spawnParticle(Particle.LAVA, impactLoc, 15, 0.5, 0.5, 0.5, 0.1)

            for (entity in player.world.getNearbyEntities(impactLoc, 3.0, 3.0, 3.0)) {
                if (entity is LivingEntity && entity !is Player && entity != match.objective?.entity) {
                    entity.damage(damage, player)
                    entity.velocity = entity.location.toVector().subtract(impactLoc.toVector()).normalize().multiply(0.8).setY(0.4)
                }
            }
        })
    }

    private fun magoSkillQ(player: Player, match: Match) {
        castSkill(player, 15, { tick ->
            val angle = tick * 0.5
            val x = kotlin.math.cos(angle) * 1.5
            val z = kotlin.math.sin(angle) * 1.5
            player.world.spawnParticle(Particle.HAPPY_VILLAGER, player.location.clone().add(x, 1.0, z), 1)
            player.world.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.0f + (tick * 0.05f))
        }, {
            player.world.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f)
            player.world.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f)
            player.world.spawnParticle(Particle.END_ROD, player.location.add(0.0, 1.0, 0.0), 50, 4.0, 1.0, 4.0, 0.1)

            for (ally in player.world.getNearbyPlayers(player.location, 8.0)) {
                ally.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 600, 1))
                val maxHp = ally.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0
                ally.health = (ally.health + 8.0).coerceAtMost(maxHp)
                ally.sendMessage("§a¡Has sido bendecido por el Mago ${player.name}!")
                ally.world.spawnParticle(Particle.HEART, ally.location.add(0.0, 2.0, 0.0), 5, 0.5, 0.5, 0.5)
            }
        })
    }

    private fun magoSkillF(player: Player, match: Match) {
        castSkill(player, 25, { tick ->
            player.world.spawnParticle(Particle.FLAME, player.location.add(0.0, 1.0, 0.0), 3, 1.0, 1.0, 1.0, 0.0)
            player.world.playSound(player.location, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 1.5f)
        }, {
            player.world.playSound(player.location, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f)
            player.world.playSound(player.location, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.5f)
            val loc = player.location

            for (i in 0..360 step 15) {
                val rad = Math.toRadians(i.toDouble())
                val x = Math.cos(rad) * 6.0
                val z = Math.sin(rad) * 6.0
                player.world.spawnParticle(Particle.FLAME, loc.clone().add(x, 0.5, z), 3, 0.0, 0.0, 0.0, 0.1)
                player.world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(x, 0.5, z), 1, 0.0, 0.0, 0.0, 0.0)
            }

            for (entity in player.world.getNearbyEntities(loc, 6.0, 3.0, 6.0)) {
                if (entity is LivingEntity && entity !is Player && entity != match.objective?.entity) {
                    entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 100, 4))
                    entity.fireTicks = 160
                    entity.damage(5.0, player)
                }
            }
        })
    }

    private fun guardianSkillQ(player: Player, match: Match) {
        castSkill(player, 15, { tick ->
            player.world.spawnParticle(Particle.ANGRY_VILLAGER, player.location.add(0.0, 2.0, 0.0), 1, 0.5, 0.5, 0.5, 0.0)
            player.world.playSound(player.location, Sound.ENTITY_WOLF_GROWL, 0.5f, 0.8f)
        }, {
            player.world.playSound(player.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.2f)
            player.world.spawnParticle(Particle.SONIC_BOOM, player.location.add(0.0, 1.0, 0.0), 1)

            player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 200, 1))
            player.sendMessage("§6¡Has provocado a los enemigos cercanos!")

            for (entity in player.world.getNearbyEntities(player.location, 10.0, 4.0, 10.0)) {
                if (entity is Mob && entity != match.objective?.entity) {
                    match.waveManager?.setPlayerAggro(entity, player)
                    entity.world.spawnParticle(Particle.DAMAGE_INDICATOR, entity.location.add(0.0, 2.0, 0.0), 3, 0.2, 0.2, 0.2, 0.0)
                }
            }
        })
    }

    private fun guardianSkillF(player: Player, match: Match) {
        castSkill(player, 10, { tick ->
            player.world.spawnParticle(Particle.CLOUD, player.location.add(0.0, 0.5, 0.0), 2, 0.5, 0.1, 0.5, 0.0)
        }, {
            player.world.playSound(player.location, Sound.ENTITY_BAT_TAKEOFF, 1f, 0.8f)
            val direction = player.location.direction.normalize().setY(0.2).multiply(1.8)
            player.velocity = direction

            val damage = 20.0 * getDamageMultiplier(match)

            object : BukkitRunnable() {
                var ticks = 0
                val hitEntities = mutableSetOf<UUID>()

                override fun run() {
                    if (ticks >= 15 || !player.isOnline) {
                        this.cancel()
                        return
                    }

                    player.world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.location.add(0.0, 1.0, 0.0), 3, 0.2, 0.2, 0.2, 0.0)

                    for (entity in player.world.getNearbyEntities(player.location, 2.0, 2.0, 2.0)) {
                        if (entity is LivingEntity && entity !is Player && entity != match.objective?.entity) {
                            if (hitEntities.add(entity.uniqueId)) {
                                player.world.playSound(entity.location, Sound.ITEM_SHIELD_BLOCK, 1f, 0.8f)
                                entity.damage(damage, player)
                                entity.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 60, 2))
                                val push = player.location.direction.normalize().multiply(0.8).setY(0.3)
                                entity.velocity = push
                            }
                        }
                    }
                    ticks++
                }
            }.runTaskTimer(DefenseGamemode.instance, 0L, 1L)
        })
    }
}