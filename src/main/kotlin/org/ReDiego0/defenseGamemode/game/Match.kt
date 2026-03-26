package org.ReDiego0.defenseGamemode.game

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.combat.weapons.WeaponManager
import org.ReDiego0.defenseGamemode.config.LivesType
import org.ReDiego0.defenseGamemode.config.MissionManager
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.ReDiego0.defenseGamemode.world.LocalWorldService
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class Match(
    val matchId: String,
    val mapName: String,
    val world: World,
    val maxPlayers: Int,
    initialPlayers: List<UUID>
) {
    var state: MatchState = MatchState.WAITING
        private set

    val players = mutableSetOf<UUID>()
    val deadPlayers = mutableSetOf<UUID>()
    var currentWave = 0

    val config = MissionManager.getMission(mapName)
    val votes = mutableMapOf<UUID, Boolean>()

    var objective: DefenseObjective? = null
        private set

    var waveManager: WaveManager? = null
        private set

    private var matchTask: BukkitTask? = null
    private var countdown = 0

    private var groupLives = config?.livesCount ?: 3
    private val individualLives = mutableMapOf<UUID, Int>()

    init {
        initialPlayers.forEach { uuid ->
            players.add(uuid)
            individualLives[uuid] = config?.livesCount ?: 3
        }
        changeState(MatchState.PREPARATION)
    }

    fun changeState(newState: MatchState) {
        if (state == MatchState.ENDING) return
        state = newState
        handleStateTransition()
    }

    private fun handleStateTransition() {
        matchTask?.cancel()

        when (state) {
            MatchState.WAITING -> { }
            MatchState.PREPARATION -> {
                if (config == null) {
                    changeState(MatchState.ENDING)
                    return
                }

                objective = DefenseObjective(this, config)
                objective?.spawn()

                players.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
                    giveLoadout(player)
                }

                waveManager = WaveManager(this, config.baseDifficulty, config.mobPool, config.difficultyProfile)
                countdown = 15
                broadcast("§e¡La partida comienza en $countdown segundos! Protege el objetivo.")

                matchTask = Bukkit.getScheduler().runTaskTimer(DefenseGamemode.instance, Runnable {
                    countdown--
                    if (countdown <= 0) {
                        changeState(MatchState.ACTIVE_WAVE)
                    } else if (countdown <= 5) {
                        broadcast("§eEmpezando en $countdown...")
                    }
                }, 0L, 20L)
            }
            MatchState.ACTIVE_WAVE -> {
                broadcast("§c¡Ronda ${currentWave + 1} iniciada! ¡Defiende el objetivo!")
                waveManager?.startNextWave()
            }
            MatchState.VOTING -> {
                objective?.healEndOfWave()
                broadcast("§a¡Ronda superada! El objetivo ha recuperado vida.")

                votes.clear()
                players.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
                    org.ReDiego0.defenseGamemode.ui.VoteMenu.open(player, this)
                }

                countdown = 15
                broadcast("§bFase de extracción. Tienes $countdown segundos para decidir.")

                matchTask = Bukkit.getScheduler().runTaskTimer(DefenseGamemode.instance, Runnable {
                    countdown--
                    if (countdown == 5) {
                        broadcast("§c¡Las elecciones se han bloqueado!")
                        players.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
                            player.closeInventory()
                        }
                    }
                    if (countdown <= 0) {
                        matchTask?.cancel()
                        processVotingResults()
                    }
                }, 0L, 20L)
            }
            MatchState.ENDING -> {
                broadcast("§cFinalizando partida...")
                waveManager?.hideBossBar()
                matchTask?.cancel()
                objective?.cleanUp()

                Bukkit.getScheduler().runTaskLater(DefenseGamemode.instance, Runnable {
                    players.mapNotNull { Bukkit.getPlayer(it) }.forEach {
                        it.inventory.clear()
                        it.gameMode = GameMode.SURVIVAL
                    }
                    LocalWorldService.deleteInstance(world.name)
                    GameManager.removeMatch(matchId)
                }, 100L)
            }
        }
    }

    private fun giveLoadout(player: Player) {
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
        player.inventory.clear()

        data.equippedWeapons.forEachIndexed { index, weaponId ->
            if (weaponId.isNotEmpty()) {
                val weaponData = data.unlockedWeapons[weaponId]
                val item = WeaponManager.buildWeaponItem(weaponId, weaponData)
                if (item != null) {
                    player.inventory.setItem(index, item)
                }
            }
        }

        val armorId = data.equippedArmor.firstOrNull { it.isNotEmpty() } ?: "set_hierro"
        val armorSet = org.ReDiego0.defenseGamemode.combat.equipment.ArmorManager.getArmorSet(armorId)

        if (armorSet != null) {
            player.inventory.helmet = org.ReDiego0.defenseGamemode.combat.equipment.ArmorManager.buildPiece(armorSet.helmet)
            player.inventory.chestplate = org.ReDiego0.defenseGamemode.combat.equipment.ArmorManager.buildPiece(armorSet.chestplate)
            player.inventory.leggings = org.ReDiego0.defenseGamemode.combat.equipment.ArmorManager.buildPiece(armorSet.leggings)
            player.inventory.boots = org.ReDiego0.defenseGamemode.combat.equipment.ArmorManager.buildPiece(armorSet.boots)
        }

        val hotbarSlots = listOf(3, 4)
        data.equippedConsumables.forEachIndexed { index, consumableId ->
            if (index < hotbarSlots.size && consumableId.isNotBlank()) {
                val item = org.ReDiego0.defenseGamemode.combat.equipment.ConsumableManager.buildConsumableItem(consumableId)
                if (item != null) {
                    player.inventory.setItem(hotbarSlots[index], item)
                }
            }
        }

        player.updateInventory()
    }

    private fun processVotingResults() {
        val toExtract = mutableListOf<Player>()

        players.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
            val voteStay = votes[player.uniqueId] ?: true
            if (!voteStay) {
                toExtract.add(player)
            } else {
                player.sendMessage("§aHas decidido continuar defendiendo el objetivo.")
            }
        }

        toExtract.forEach { player ->
            giveRewards(player, success = true)

            val data = PlayerDataManager.getPlayerData(player.uniqueId)
            val difficultyLevel = config?.baseDifficulty ?: 1
            if (data != null) {
                val currentCount = data.missionsCompleted.getOrDefault(difficultyLevel, 0)
                data.missionsCompleted[difficultyLevel] = currentCount + 1
                PlayerDataManager.savePlayerAsync(player.uniqueId)
            }

            player.sendMessage("§aTe has retirado con éxito. ¡Misión completada!")
            val fallbackWorld = Bukkit.getWorlds().first()
            player.inventory.clear()
            player.teleportAsync(fallbackWorld.spawnLocation)
            removePlayer(player)
        }

        if (state != MatchState.ENDING) {
            broadcast("§eSiguiente oleada en breve...")
            Bukkit.getScheduler().runTaskLater(DefenseGamemode.instance, Runnable {
                if (state != MatchState.ENDING) {
                    changeState(MatchState.ACTIVE_WAVE)
                }
            }, 60L)
        }
    }

    private fun giveRewards(player: Player, success: Boolean) {
        if (config == null || config.rewards.isEmpty()) return

        val penalty = if (success) 1.0 else 0.25
        val multiplier = currentWave * config.baseDifficulty * penalty

        for (reward in config.rewards) {
            val finalQuantity = (reward.baseValue * multiplier).toInt()

            if (reward.baseValue > 0 && finalQuantity <= 0) continue

            var parsedCommand = reward.command
                .replace("{user}", player.name)
                .replace("{quantity}", finalQuantity.toString())

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand)
        }
    }

    fun broadcast(message: String) {
        players.mapNotNull { Bukkit.getPlayer(it) }.forEach { it.sendMessage(message) }
    }

    fun removePlayer(player: Player) {
        players.remove(player.uniqueId)
        deadPlayers.remove(player.uniqueId)
        individualLives.remove(player.uniqueId)

        if (players.isEmpty() && state != MatchState.ENDING) {
            changeState(MatchState.ENDING)
        } else {
            checkGameOver()
        }
    }

    fun getMobSpawns(): List<org.bukkit.Location> {
        return config?.mobSpawns?.map { it.toBukkitLocation(world) } ?: emptyList()
    }

    fun handlePlayerDeath(player: Player) {
        val maxHealth = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        player.health = maxHealth
        player.gameMode = GameMode.SPECTATOR

        deadPlayers.add(player.uniqueId)

        var remainingLives = 0
        if (config?.livesType == LivesType.GROUP) {
            groupLives--
            remainingLives = groupLives
            broadcast("§c${player.name} ha muerto. Vidas del equipo restantes: $remainingLives")
        } else {
            val current = (individualLives[player.uniqueId] ?: 1) - 1
            individualLives[player.uniqueId] = current
            remainingLives = current
            broadcast("§c${player.name} ha muerto. Vidas restantes: $remainingLives")
        }

        if (checkGameOver()) return

        if (remainingLives > 0) {
            player.sendMessage("§eReapareciendo en 10 segundos...")
            Bukkit.getScheduler().runTaskLater(DefenseGamemode.instance, Runnable {
                if (state != MatchState.ENDING && players.contains(player.uniqueId)) {
                    val spawnLoc = config?.spawnLocation?.toRandomizedBukkitLocation(world, config.spawnRadius)
                    if (spawnLoc != null) player.teleportAsync(spawnLoc)
                    player.gameMode = GameMode.SURVIVAL
                    deadPlayers.remove(player.uniqueId)
                    giveLoadout(player)
                    player.sendMessage("§a¡Has reaparecido!")
                }
            }, 200L)
        } else {
            player.sendMessage("§cTe has quedado sin vidas. Estás fuera de la partida.")
        }
    }

    fun handleObjectiveDeath() {
        broadcast("§4¡EL OBJETIVO HA SIDO DESTRUIDO! MISIÓN FALLIDA.")

        players.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
            giveRewards(player, success = false)
            player.sendMessage("§cCalculando recompensas de consolación...")
        }

        changeState(MatchState.ENDING)
    }

    private fun checkGameOver(): Boolean {
        val allDead = players.all { deadPlayers.contains(it) }

        val noLivesLeft = if (config?.livesType == LivesType.GROUP) {
            groupLives <= 0
        } else {
            players.all { (individualLives[it] ?: 0) <= 0 }
        }

        if (allDead && noLivesLeft) {
            broadcast("§4¡TODOS HAN MUERTO! GAME OVER.")

            players.mapNotNull { Bukkit.getPlayer(it) }.forEach { player ->
                giveRewards(player, success = false)
                player.sendMessage("§cCalculando recompensas de consolación...")
            }

            changeState(MatchState.ENDING)
            return true
        }
        return false
    }
}