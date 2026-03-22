package org.ReDiego0.defenseGamemode.game

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.ReDiego0.defenseGamemode.config.MobEntry
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import kotlin.random.Random

class WaveManager(
    private val match: Match,
    private val baseDifficulty: Int,
    private val mobPoolId: String,
    private val difficultyProfile: DifficultyProfile
) {
    private var currentWave = 0
    private var aliveMobs = 0

    fun startNextWave() {
        currentWave++
        match.currentWave = currentWave

        val currentLevel = currentWave
        val currentTier = baseDifficulty + (currentWave / 5)

        spawnWave(currentLevel, currentTier)
    }

    private fun spawnWave(level: Int, maxTier: Int) {
        val pool = org.ReDiego0.defenseGamemode.config.MobManager.getPool(mobPoolId) ?: return
        val validMobs = pool.getMobsForTier(maxTier)

        if (validMobs.isEmpty()) return

        val maxBaseWeight = validMobs.maxOfOrNull { it.baseWeight } ?: 100.0
        val weightedMobs = validMobs.map { mob ->
            val effectiveWeight = difficultyProfile.calculateEffectiveWeight(mob.baseWeight, maxBaseWeight, currentWave)
            Pair(mob, effectiveWeight)
        }

        val mobsToSpawn = 10 + (currentWave * 2) + (match.players.size * 5)
        val spawns = match.getMobSpawns()
        if (spawns.isEmpty()) return

        for (i in 0 until mobsToSpawn) {
            val selectedMob = getRandomWeightedMob(weightedMobs) ?: continue
            val spawnLoc = spawns.random()

            spawnEntity(selectedMob, spawnLoc, level)
        }
    }

    private fun getRandomWeightedMob(weightedMobs: List<Pair<MobEntry, Double>>): MobEntry? {
        val totalWeight = weightedMobs.sumOf { it.second }
        if (totalWeight <= 0.0) return null

        var randomValue = Random.nextDouble() * totalWeight
        for (item in weightedMobs) {
            randomValue -= item.second
            if (randomValue <= 0.0) {
                return item.first
            }
        }
        return weightedMobs.lastOrNull()?.first
    }

    private fun spawnEntity(mobEntry: MobEntry, location: Location, level: Int) {
        try {
            val entityType = EntityType.valueOf(mobEntry.id.uppercase())
            val entity = location.world.spawnEntity(location, entityType) as? LivingEntity ?: return

            applyLevelScaling(entity, level)
            setupHologram(entity, mobEntry.id, level)

            aliveMobs++
        } catch (e: IllegalArgumentException) {
            println("Pendiente: Integrar MythicMobs/LevelledMobs para el id ${mobEntry.id}")
        }
    }

    private fun applyLevelScaling(entity: LivingEntity, level: Int) {
        val baseHealth = entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue ?: 20.0
        val newHealth = baseHealth + (level * 2.0)

        entity.getAttribute(Attribute.MAX_HEALTH)?.baseValue = newHealth
        entity.health = newHealth

        val dmgAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE)
        if (dmgAttr != null) {
            dmgAttr.baseValue += (level * 0.5)
        }
    }

    private fun setupHologram(entity: LivingEntity, name: String, level: Int) {
        val formattedName = name.replace("_", " ").replaceFirstChar { it.uppercase() }

        val customName = Component.text()
            .append(Component.text("[Lv. $level] ", NamedTextColor.YELLOW, TextDecoration.BOLD))
            .append(Component.text(formattedName, NamedTextColor.RED))
            .build()

        entity.customName(customName)
        entity.isCustomNameVisible = true
    }

    fun handleMobDeath() {
        aliveMobs--
        if (aliveMobs <= 0) {
            val wavesPerRot = match.config?.wavesPerRotation ?: 5

            if (currentWave % wavesPerRot == 0) {
                match.changeState(MatchState.VOTING)
            } else {
                org.bukkit.Bukkit.getScheduler().runTaskLater(org.ReDiego0.defenseGamemode.DefenseGamemode.instance, Runnable {
                    if (match.state == MatchState.ACTIVE_WAVE) {
                        match.changeState(MatchState.ACTIVE_WAVE)
                    }
                }, 60L)
            }
        } else if (aliveMobs <= 3) {
            highlightRemainingMobs()
        }
    }

    private fun highlightRemainingMobs() {
        match.world.livingEntities.forEach { entity ->
            if (entity !is Player && entity != match.objective?.entity) {
                entity.isGlowing = true
            }
        }
    }
}