package org.ReDiego0.defenseGamemode.combat.manager

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.combat.CooldownManager
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.util.Vector

class CombatManager(private val plugin: DefenseGamemode) {

    private val skillManager = SkillManager(plugin)
    private val classSkillManager = ClassSkillManager()

    fun handleSkill1(player: Player) {
        if (plugin.cooldownManager.checkAndNotify(player, CooldownManager.CooldownType.SKILL_1)) return

        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
        val playerClass = data.currentClass

        player.sendMessage("§b[SISTEMA] Has usado la Habilidad 1 de la clase: §e${playerClass.uppercase()}")
        plugin.cooldownManager.setCooldown(player.uniqueId, CooldownManager.CooldownType.SKILL_1, 5.0)
    }

    fun handleSkill2(player: Player) {
        if (plugin.cooldownManager.checkAndNotify(player, CooldownManager.CooldownType.SKILL_2)) return

        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
        val playerClass = data.currentClass

        player.sendMessage("§b[SISTEMA] Has usado la Habilidad 2 de la clase: §e${playerClass.uppercase()}")
        plugin.cooldownManager.setCooldown(player.uniqueId, CooldownManager.CooldownType.SKILL_2, 15.0)
    }

    fun handleMythicWeapon(player: Player, skillId: String) {
        if (plugin.cooldownManager.checkAndNotify(player, CooldownManager.CooldownType.MYTHIC_WEAPON)) return

        skillManager.executeWeaponSkill(player, skillId)
        plugin.cooldownManager.setCooldown(player.uniqueId, CooldownManager.CooldownType.MYTHIC_WEAPON, 12.0)
    }

    fun handleDirectionalDash(player: Player) {
        if (plugin.cooldownManager.isOnCooldown(player.uniqueId, CooldownManager.CooldownType.DASH)) {
            return
        }

        val velocity = player.velocity
        val horizontalVelocity = velocity.clone().setY(0)

        if (horizontalVelocity.lengthSquared() < 0.01) {
            val backDir = player.location.direction.clone().setY(0).normalize().multiply(-1)
            performDash(player, backDir, "§7Evasión Atrás", 1.0)
            return
        }

        val targetDir = horizontalVelocity.normalize()
        val playerDir = player.location.direction.clone().setY(0).normalize()
        val dot = targetDir.dot(playerDir)

        var dashType = "Dash"
        var power = 1.5

        if (dot > 0.5) {
            dashType = "§bDash Frontal"
            power = 1.8
        } else if (dot < -0.5) {
            dashType = "§7Retirada"
            power = 1.4
        } else {
            val crossY = (playerDir.x * targetDir.z) - (playerDir.z * targetDir.x)
            if (crossY > 0) {
                dashType = "§eDash Derecha"
            } else {
                dashType = "§eDash Izquierda"
            }
            power = 1.6
        }

        performDash(player, targetDir, dashType, power)
    }

    private fun performDash(player: Player, direction: Vector, name: String, power: Double) {
        plugin.cooldownManager.setCooldown(player.uniqueId, CooldownManager.CooldownType.DASH, 2.0)
        player.velocity = direction.multiply(power).setY(0.4)

        player.noDamageTicks = 20

        player.world.playSound(player.location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f)
        player.world.spawnParticle(Particle.CLOUD, player.location, 5, 0.2, 0.1, 0.2, 0.05)
        player.sendActionBar(net.kyori.adventure.text.Component.text(name))
    }

    fun handleConsumable(player: Player, consumableId: String) {
        when (consumableId) {
            "pocion_curacion" -> {
                val maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)?.value ?: 20.0
                player.health = (player.health + 8.0).coerceAtMost(maxHealth)
                player.world.spawnParticle(org.bukkit.Particle.HEART, player.location.add(0.0, 1.0, 0.0), 5, 0.5, 0.5, 0.5, 0.0)
                player.playSound(player.location, org.bukkit.Sound.ENTITY_WITCH_DRINK, 1f, 1f)
            }
            "bomba_rango_bajo" -> {
                val snowball = player.launchProjectile(org.bukkit.entity.Snowball::class.java)
                val key = org.bukkit.NamespacedKey(plugin, "bomb_projectile")
                snowball.persistentDataContainer.set(key, org.bukkit.persistence.PersistentDataType.BYTE, 1)
                snowball.item = org.bukkit.inventory.ItemStack(org.bukkit.Material.TNT)
                player.playSound(player.location, org.bukkit.Sound.ENTITY_SNOWBALL_THROW, 1f, 0.5f)
            }
        }
    }

    fun executeSkillQ(player: org.bukkit.entity.Player) {
        classSkillManager.executeSkillQ(player)
    }

    fun executeSkillF(player: org.bukkit.entity.Player) {
        classSkillManager.executeSkillF(player)
    }
}