package org.ReDiego0.defenseGamemode.combat.manager

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector

class SkillManager(private val plugin: DefenseGamemode) {

    fun executeWeaponSkill(player: Player, skillId: String) {
        when (skillId.lowercase()) {
            "corte_vacio" -> executeCorteVacio(player)
            else -> player.sendMessage("§c[SISTEMA] Habilidad desconocida: $skillId")
        }
    }

    private fun executeCorteVacio(player: Player) {
        player.sendMessage("§5Concentrando el vacío...")
        player.playSound(player.location, Sound.ITEM_TRIDENT_THUNDER, 0.5f, 2.0f)
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 20, 5, false, false))

        object : BukkitRunnable() {
            var chargeTicks = 0
            val chargeTime = 15

            override fun run() {
                if (!player.isOnline || player.isDead) {
                    this.cancel()
                    return
                }

                if (chargeTicks < chargeTime) {
                    val loc = player.location.add(0.0, 1.0, 0.0)
                    player.world.spawnParticle(Particle.PORTAL, loc, 5, 0.5, 0.5, 0.5, 1.0)
                    player.world.spawnParticle(Particle.CRIT, loc, 2, 0.2, 0.5, 0.2, 0.0)

                    chargeTicks++
                } else {
                    this.cancel()
                    fireVoidSlash(player)
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }

    private fun fireVoidSlash(player: Player) {
        player.playSound(player.location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f)
        player.playSound(player.location, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 2.0f)

        val startLoc = player.eyeLocation.subtract(0.0, 0.5, 0.0)
        val direction = player.location.direction.normalize()
        val rightVec = direction.clone().crossProduct(Vector(0, 1, 0)).normalize()

        object : BukkitRunnable() {
            var distance = 0.0
            val maxRange = 15.0
            val width = 2.5

            override fun run() {
                if (!player.isOnline || distance > maxRange) {
                    this.cancel()
                    return
                }

                distance += 1.5
                val currentCenter = startLoc.clone().add(direction.clone().multiply(distance))

                if (currentCenter.block.type.isSolid) {
                    player.world.spawnParticle(Particle.BLOCK_CRUMBLE, currentCenter, 10, 0.5, 0.5, 0.5, 0.0, currentCenter.block.blockData)
                    player.playSound(currentCenter, Sound.BLOCK_ANVIL_LAND, 0.5f, 2f)
                    this.cancel()
                    return
                }

                var i = -width
                while (i <= width) {
                    val curveOffset = 0.5 * Math.cos((i / width) * Math.PI)

                    val particleLoc = currentCenter.clone()
                        .add(rightVec.clone().multiply(i))
                        .add(direction.clone().multiply(curveOffset))

                    player.world.spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 0, direction.x, direction.y, direction.z)
                    player.world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 0, 0.0, 0.0, 0.0)

                    i += 0.5
                }

                val hitbox = width
                for (e in player.world.getNearbyEntities(currentCenter, hitbox, 2.0, hitbox)) {
                    if (e is LivingEntity && e != player) {
                        e.noDamageTicks = 0
                        e.damage(35.0, player)

                        player.world.spawnParticle(Particle.CRIT, e.location.add(0.0, 1.0, 0.0), 10)
                        player.playSound(e.location, Sound.ENTITY_PHANTOM_BITE, 1f, 1.5f)
                        e.velocity = e.velocity.multiply(0.0)
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }
}