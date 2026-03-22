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
    private var targetKills = 0
    private var currentKills = 0
    private var waveBossBar: org.bukkit.boss.BossBar? = null

    private var aiTask: org.bukkit.scheduler.BukkitTask? = null
    private val aggroTimers = mutableMapOf<java.util.UUID, Long>()

    fun startNextWave() {
        currentWave++
        match.currentWave = currentWave

        val currentLevel = currentWave
        val currentTier = baseDifficulty + (currentWave / 5)

        if (waveBossBar == null) {
            waveBossBar = org.bukkit.Bukkit.createBossBar(
                "§c⚔ Oleada $currentWave ⚔",
                org.bukkit.boss.BarColor.RED,
                org.bukkit.boss.BarStyle.SOLID
            )
        } else {
            waveBossBar?.setTitle("§c⚔ Oleada $currentWave ⚔")
        }

        match.players.mapNotNull { org.bukkit.Bukkit.getPlayer(it) }.forEach { player ->
            waveBossBar?.addPlayer(player)
        }
        waveBossBar?.progress = 1.0

        spawnWave(currentLevel, currentTier)
        if (aiTask == null) startSwarmAI()
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

        targetKills = 10 + (currentWave * 2) + (match.players.size * 5)

        val mobsToSpawn = (targetKills * 1.3).toInt()
        currentKills = 0

        val spawns = match.getMobSpawns()
        if (spawns.isEmpty()) return

        for (i in 0 until mobsToSpawn) {
            val selectedMob = getRandomWeightedMob(weightedMobs) ?: continue
            val baseLoc = spawns.random()

            val offsetX = Random.nextDouble(-2.0, 2.0)
            val offsetZ = Random.nextDouble(-2.0, 2.0)
            val spawnLoc = baseLoc.clone().add(offsetX, 0.0, offsetZ)

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

            val mob = entity as? org.bukkit.entity.Mob
            if (mob != null) {
                mob.getAttribute(Attribute.FOLLOW_RANGE)?.baseValue = 150.0
                mob.target = match.objective?.entity
            }

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
        currentKills++
        updateBossBar()

        val remaining = targetKills - currentKills

        if (remaining <= 0) {
            finishWave()
        } else if (remaining <= 3) {
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
            progress > 0.5 -> waveBossBar?.color = org.bukkit.boss.BarColor.RED
            progress > 0.2 -> waveBossBar?.color = org.bukkit.boss.BarColor.YELLOW
            else -> waveBossBar?.color = org.bukkit.boss.BarColor.GREEN
        }
    }

    fun hideBossBar() {
        waveBossBar?.removeAll()
    }

    private fun finishWave() {
        hideBossBar()
        clearRemainingMobs()

        aiTask?.cancel()
        aiTask = null
        aggroTimers.clear()

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
    }

    private fun clearRemainingMobs() {
        match.world.livingEntities.forEach { entity ->
            if (entity !is Player && entity != match.objective?.entity) {
                entity.world.spawnParticle(org.bukkit.Particle.CLOUD, entity.location, 10, 0.5, 0.5, 0.5, 0.1)
                entity.remove()
            }
        }
    }

    fun setPlayerAggro(mob: org.bukkit.entity.Mob, player: Player) {
        mob.target = player
        aggroTimers[mob.uniqueId] = System.currentTimeMillis() + 5000L
    }

    private fun startSwarmAI() {
        aiTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(org.ReDiego0.defenseGamemode.DefenseGamemode.instance, Runnable {
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