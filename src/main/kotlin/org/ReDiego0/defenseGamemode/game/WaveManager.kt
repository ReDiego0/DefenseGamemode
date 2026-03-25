package org.ReDiego0.defenseGamemode.game

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.config.MobEntry
import org.ReDiego0.defenseGamemode.config.MobManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.attribute.Attribute
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import kotlin.math.pow
import kotlin.random.Random

class WaveManager(
    private val match: Match,
    private val baseDifficulty: Int,
    private val mobPoolId: String,
    private val difficultyProfile: DifficultyProfile
) {
    private var currentWave = 0
    private var targetKills = 0
    private var currentKills = 0
    private var waveBossBar: BossBar? = null

    private var aiTask: BukkitTask? = null
    private val aggroTimers = mutableMapOf<UUID, Long>()

    private var mobsLeftToSpawn = 0
    private var activeSpawns = 0
    private var spawnTask: BukkitTask? = null

    fun startNextWave() {
        currentWave++
        match.currentWave = currentWave

        val currentLevel = currentWave
        val currentTier = baseDifficulty + (currentWave / 5)

        if (waveBossBar == null) {
            waveBossBar = Bukkit.createBossBar(
                "§c⚔ Oleada $currentWave ⚔",
                BarColor.RED,
                BarStyle.SOLID
            )
        } else {
            waveBossBar?.setTitle("§c⚔ Oleada $currentWave ⚔")
        }

        match.players.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
            waveBossBar?.addPlayer(player)
        }
        waveBossBar?.progress = 1.0

        spawnWave(currentLevel, currentTier)
        if (aiTask == null) startSwarmAI()
    }

    private fun spawnWave(level: Int, maxTier: Int) {
        val pool = MobManager.getPool(mobPoolId) ?: return
        val validMobs = pool.getMobsForTier(maxTier)

        if (validMobs.isEmpty()) return

        val maxBaseWeight = validMobs.maxOfOrNull { it.baseWeight } ?: 100.0
        val weightedMobs = validMobs.map { mob ->
            val effectiveWeight = difficultyProfile.calculateEffectiveWeight(mob.baseWeight, maxBaseWeight, currentWave)
            Pair(mob, effectiveWeight)
        }

        targetKills = 10 + (currentWave * 2) + (match.players.size * 5)
        mobsLeftToSpawn = (targetKills * 1.3).toInt()
        currentKills = 0
        activeSpawns = 0

        val spawns = match.getMobSpawns()
        if (spawns.isEmpty()) return

        val absoluteMaxConcurrent = 12 + (match.players.size * 3)
        var currentMaxConcurrent = 3

        spawnTask = Bukkit.getScheduler().runTaskTimer(DefenseGamemode.instance, Runnable {
            if (match.state != MatchState.ACTIVE_WAVE) {
                spawnTask?.cancel()
                return@Runnable
            }

            currentMaxConcurrent = minOf(absoluteMaxConcurrent, 3 + (currentKills / 2))

            if (mobsLeftToSpawn > 0 && activeSpawns < currentMaxConcurrent) {
                val amountToSpawn = minOf(3, mobsLeftToSpawn, currentMaxConcurrent - activeSpawns)

                for (i in 0 until amountToSpawn) {
                    val selectedMob = getRandomWeightedMob(weightedMobs) ?: continue
                    val baseLoc = spawns.random()

                    val offsetX = Random.nextDouble(-2.0, 2.0)
                    val offsetZ = Random.nextDouble(-2.0, 2.0)
                    val spawnLoc = baseLoc.clone().add(offsetX, 0.0, offsetZ)

                    spawnEntity(selectedMob, spawnLoc, level)
                    mobsLeftToSpawn--
                    activeSpawns++
                }
            }
        }, 0L, 20L)
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

            val pdc = entity.persistentDataContainer
            val levelKey = NamespacedKey(DefenseGamemode.instance, "mob_level")
            val nameKey = NamespacedKey(DefenseGamemode.instance, "mob_name")
            val formattedName = mobEntry.id.replace("_", " ").replaceFirstChar { it.uppercase() }

            pdc.set(levelKey, PersistentDataType.INTEGER, level)
            pdc.set(nameKey, PersistentDataType.STRING, formattedName)

            updateMobHologram(entity)

            val mob = entity as? org.bukkit.entity.Mob
            if (mob != null) {
                mob.getAttribute(Attribute.FOLLOW_RANGE)?.baseValue = 150.0
                mob.target = match.objective?.entity
            }

        } catch (e: IllegalArgumentException) {
        }
    }

    private fun applyLevelScaling(entity: LivingEntity, wave: Int) {
        val baseMissionMult = 0.5 + ((baseDifficulty - 1) * 0.2)
        val waveZeroIndexed = (wave - 1).toDouble().coerceAtLeast(0.0)
        val waveMult = 1.0 + (waveZeroIndexed * 0.03) + (waveZeroIndexed.pow(2.0) * 0.0015)
        val totalMultiplier = baseMissionMult * waveMult

        val healthAttr = entity.getAttribute(Attribute.MAX_HEALTH)
        if (healthAttr != null) {
            val newHealth = (healthAttr.baseValue * totalMultiplier).coerceAtLeast(1.0)
            healthAttr.baseValue = newHealth
            entity.health = newHealth
        }

        val dmgAttr = entity.getAttribute(Attribute.ATTACK_DAMAGE)
        if (dmgAttr != null) {
            val newDmg = (dmgAttr.baseValue * totalMultiplier).coerceAtLeast(1.0)
            dmgAttr.baseValue = newDmg
        }
    }

    fun updateMobHologram(entity: LivingEntity) {
        val pdc = entity.persistentDataContainer
        val levelKey = NamespacedKey(DefenseGamemode.instance, "mob_level")
        val nameKey = NamespacedKey(DefenseGamemode.instance, "mob_name")

        val level = pdc.get(levelKey, PersistentDataType.INTEGER) ?: return
        val name = pdc.get(nameKey, PersistentDataType.STRING) ?: return

        val maxHealth = entity.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        val currentHealth = entity.health.coerceAtLeast(0.0)

        val percentage = currentHealth / maxHealth
        val color = when {
            percentage > 0.66 -> NamedTextColor.GREEN
            percentage > 0.33 -> NamedTextColor.YELLOW
            else -> NamedTextColor.RED
        }

        val totalBars = 5
        val activeBars = (percentage * totalBars).toInt().coerceIn(0, totalBars)
        val barString = "█".repeat(activeBars) + "░".repeat(totalBars - activeBars)

        val customName = Component.text()
            .append(Component.text("[Lv. $level] ", NamedTextColor.GOLD, TextDecoration.BOLD))
            .append(Component.text(name, NamedTextColor.WHITE))
            .append(Component.text(" [", NamedTextColor.DARK_GRAY))
            .append(Component.text(barString, color))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY))
            .append(Component.text("${currentHealth.toInt()}", color))
            .append(Component.text("❤", NamedTextColor.RED))
            .build()

        entity.customName(customName)
        entity.isCustomNameVisible = true
    }

    fun handleMobDeath() {
        currentKills++
        activeSpawns--
        updateBossBar()

        val remaining = targetKills - currentKills

        if (remaining <= 0) {
            finishWave()
        } else if (remaining <= 3 && mobsLeftToSpawn <= 0) {
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

    private fun updateBossBar() {
        if (targetKills == 0) return
        val remaining = targetKills - currentKills

        val progress = (remaining.toDouble() / targetKills.toDouble()).coerceIn(0.0, 1.0)
        waveBossBar?.progress = progress

        when {
            progress > 0.5 -> waveBossBar?.color = BarColor.RED
            progress > 0.2 -> waveBossBar?.color = BarColor.YELLOW
            else -> waveBossBar?.color = BarColor.GREEN
        }
    }

    fun hideBossBar() {
        waveBossBar?.removeAll()
    }

    private fun finishWave() {
        hideBossBar()
        clearRemainingMobs()

        spawnTask?.cancel()
        spawnTask = null
        aiTask?.cancel()
        aiTask = null
        aggroTimers.clear()

        val wavesPerRot = match.config?.wavesPerRotation ?: 5

        if (currentWave % wavesPerRot == 0) {
            match.changeState(MatchState.VOTING)
        } else {
            Bukkit.getScheduler().runTaskLater(DefenseGamemode.instance, Runnable {
                if (match.state == MatchState.ACTIVE_WAVE) {
                    match.changeState(MatchState.ACTIVE_WAVE)
                }
            }, 60L)
        }
    }

    private fun clearRemainingMobs() {
        match.world.livingEntities.forEach { entity ->
            if (entity !is Player && entity != match.objective?.entity) {
                entity.world.spawnParticle(Particle.CLOUD, entity.location, 10, 0.5, 0.5, 0.5, 0.1)
                entity.remove()
            }
        }
    }

    fun setPlayerAggro(mob: org.bukkit.entity.Mob, player: Player) {
        mob.target = player
        aggroTimers[mob.uniqueId] = System.currentTimeMillis() + 5000L
    }

    private fun startSwarmAI() {
        aiTask = Bukkit.getScheduler().runTaskTimer(DefenseGamemode.instance, Runnable {
            val objectiveEntity = match.objective?.entity ?: return@Runnable
            val currentTime = System.currentTimeMillis()

            match.world.livingEntities.forEach { entity ->
                if (entity is org.bukkit.entity.Mob && entity != objectiveEntity) {
                    val aggroExpiry = aggroTimers[entity.uniqueId]

                    if (aggroExpiry != null) {
                        if (currentTime > aggroExpiry) {
                            aggroTimers.remove(entity.uniqueId)
                            entity.target = objectiveEntity
                        } else {
                            val currentTarget = entity.target
                            if (currentTarget is Player && currentTarget.location.distanceSquared(entity.location) > 400.0) {
                                aggroTimers.remove(entity.uniqueId)
                                entity.target = objectiveEntity
                            }
                        }
                    } else {
                        if (entity.target != objectiveEntity) {
                            entity.target = objectiveEntity
                        }

                        if (entity.ticksLived % 60 == 0) {
                            entity.pathfinder.moveTo(objectiveEntity)
                        }
                    }
                }
            }
        }, 0L, 20L)
    }
}