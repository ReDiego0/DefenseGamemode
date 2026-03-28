package org.ReDiego0.defenseGamemode.game

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.config.MissionConfig
import org.ReDiego0.defenseGamemode.config.MissionManager
import org.ReDiego0.defenseGamemode.player.Party
import org.ReDiego0.defenseGamemode.player.PartyManager
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.ReDiego0.defenseGamemode.utils.EconomyManager
import org.ReDiego0.defenseGamemode.world.LocalWorldService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

object MatchmakingManager {

    private val activeQueues = mutableMapOf<String, LobbyQueue>()

    fun isPlayerInQueue(uuid: UUID): Boolean {
        return activeQueues.values.any { it.waitingPlayers.contains(uuid) }
    }

    fun joinMap(player: Player, mapName: String) {
        val missionConfig = MissionManager.getMission(mapName)
        if (missionConfig == null) {
            player.sendMessage("§cError: La misión '$mapName' no existe.")
            return
        }

        val party = PartyManager.getParty(player.uniqueId)

        if (!party.isLeader(player.uniqueId)) {
            player.sendMessage("§cSolo el líder de la party puede iniciar el matchmaking.")
            return
        }

        if (party.members.size > missionConfig.maxPlayers) {
            player.sendMessage("§cTu party tiene más jugadores que el máximo permitido (${missionConfig.maxPlayers}).")
            return
        }

        val memberInMatch = party.members.firstOrNull { GameManager.getMatchByPlayer(it) != null }
        if (memberInMatch != null) {
            val memberName = Bukkit.getPlayer(memberInMatch)?.name ?: "Alguien"
            player.sendMessage("§c$memberName ya está en una partida. Espera a que termine.")
            return
        }

        val requirements = missionConfig.requirements
        if (requirements != null) {
            for (memberUuid in party.members) {
                val memberData = PlayerDataManager.getPlayerData(memberUuid) ?: continue
                val memberName = Bukkit.getPlayer(memberUuid)?.name ?: "Un miembro"

                if (memberData.level < requirements.minLevel) {
                    player.sendMessage("§c$memberName no cumple con el nivel mínimo (${requirements.minLevel}).")
                    return
                }

                if (requirements.requiredClass != null && !memberData.currentClass.equals(requirements.requiredClass, true)) {
                    player.sendMessage("§c$memberName debe tener la clase ${requirements.requiredClass} equipada.")
                    return
                }

                if (requirements.maxWeaponSlots != null) {
                    val equippedCount = memberData.equippedWeapons.count { it.isNotBlank() }
                    if (equippedCount > requirements.maxWeaponSlots) {
                        player.sendMessage("§c$memberName supera el límite de armas equipadas (${requirements.maxWeaponSlots}).")
                        return
                    }
                }
            }

            val leaderData = PlayerDataManager.getPlayerData(player.uniqueId) ?: return
            val taxiMultiplier = 1.0 + ((party.members.size - 1) * 0.15)

            val finalVaultCost = requirements.vaultCost * taxiMultiplier
            if (finalVaultCost > 0.0) {
                if (!EconomyManager.hasEnough(player, finalVaultCost)) {
                    player.sendMessage("§cNo tienes fondos suficientes. Requieres ${finalVaultCost.toInt()} monedas (incluye tasa de grupo).")
                    return
                }
            }

            for ((itemId, amount) in requirements.itemCosts) {
                val finalItemCost = (amount * taxiMultiplier).toInt()
                val currentAmount = leaderData.bodega.getOrDefault(itemId, 0)
                if (currentAmount < finalItemCost) {
                    player.sendMessage("§cNo tienes suficientes materiales. Requieres $finalItemCost de $itemId en tu bodega.")
                    return
                }
            }

            if (finalVaultCost > 0.0) {
                EconomyManager.withdraw(player, finalVaultCost)
            }

            for ((itemId, amount) in requirements.itemCosts) {
                val finalItemCost = (amount * taxiMultiplier).toInt()
                val currentAmount = leaderData.bodega.getOrDefault(itemId, 0)
                leaderData.bodega[itemId] = currentAmount - finalItemCost
            }
            PlayerDataManager.savePlayerAsync(player.uniqueId)
        }

        val queue = activeQueues.getOrPut(mapName) { LobbyQueue(mapName, missionConfig) }

        if (queue.isFull()) {
            player.sendMessage("§cLa sala de espera para este mapa está llena. Intenta de nuevo en unos segundos.")
            return
        }

        queue.addParty(party)
    }

    fun forceStartQueue(mapName: String, player: Player) {
        val queue = activeQueues[mapName]
        if (queue == null) {
            player.sendMessage("§cLa cola para este mapa ya no existe o ya ha iniciado.")
            return
        }

        if (!queue.waitingPlayers.contains(player.uniqueId)) {
            player.sendMessage("§cNo estás en la sala de espera de este mapa.")
            return
        }

        queue.forceStart()
    }

    class LobbyQueue(val mapName: String, val config: MissionConfig) {
        val waitingPlayers = mutableSetOf<UUID>()
        private var task: BukkitTask? = null
        private var countdown = 30
        private var isStarting = false

        fun isFull(): Boolean = waitingPlayers.size >= config.maxPlayers

        fun addParty(party: Party) {
            waitingPlayers.addAll(party.members)

            val mm = MiniMessage.miniMessage()
            val messageString = "<yellow>${party.members.size} jugador(es) se unieron a la cola para <aqua>$mapName</aqua>. (${waitingPlayers.size}/${config.maxPlayers}) <hover:show_text:'<green>Haz clic para saltar la espera'><click:run_command:'/defense forcestart $mapName'><green><bold>[▶ EMPEZAR AHORA]</bold></green></click></hover>"
            broadcast(mm.deserialize(messageString))

            if (waitingPlayers.size >= config.maxPlayers) {
                countdown = 5
                broadcast("§a¡Sala llena! Empezando en breve...")
            }

            if (task == null) {
                startTimer()
            }
        }

        fun forceStart() {
            if (isStarting) return
            isStarting = true
            task?.cancel()
            activeQueues.remove(mapName)
            broadcast("§a¡Inicio forzado! Preparando misión...")
            createAndJoinInstance(waitingPlayers.toList())
        }

        private fun startTimer() {
            task = Bukkit.getScheduler().runTaskTimer(DefenseGamemode.instance, Runnable {
                if (isStarting) return@Runnable
                countdown--

                if (countdown == 15 || countdown == 10 || (countdown <= 5 && countdown > 0)) {
                    broadcast("§eIniciando misión en §b$countdown§e segundos...")
                }

                if (countdown <= 0) {
                    isStarting = true
                    task?.cancel()
                    activeQueues.remove(mapName)
                    createAndJoinInstance(waitingPlayers.toList())
                }
            }, 0L, 20L)
        }

        private fun broadcast(message: String) {
            waitingPlayers.mapNotNull { Bukkit.getPlayer(it) }.forEach { it.sendMessage(message) }
        }

        private fun broadcast(component: Component) {
            waitingPlayers.mapNotNull { Bukkit.getPlayer(it) }.forEach { it.sendMessage(component) }
        }

        private fun createAndJoinInstance(playersToJoin: List<UUID>) {
            broadcast("§aCreando el mundo... Serás teletransportado en un instante.")

            LocalWorldService.createInstanceAsync(config.templateName).thenAccept { world ->
                if (world == null) {
                    playersToJoin.mapNotNull { Bukkit.getPlayer(it) }.forEach {
                        it.sendMessage("§cError crítico: La plantilla '${config.templateName}' no existe.")
                    }
                    return@thenAccept
                }

                val newMatch = Match(
                    matchId = world.name,
                    mapName = mapName,
                    world = world,
                    maxPlayers = config.maxPlayers,
                    initialPlayers = playersToJoin
                )

                GameManager.registerMatch(newMatch)

                playersToJoin.mapNotNull { Bukkit.getPlayer(it) }.forEach { member ->
                    val spawnLoc = config.spawnLocation.toRandomizedBukkitLocation(world, config.spawnRadius)
                    member.teleportAsync(spawnLoc).thenAccept {
                        member.sendMessage("§a¡Misión iniciada!")
                    }
                }
            }.exceptionally { ex ->
                ex.printStackTrace()
                null
            }
        }
    }
}