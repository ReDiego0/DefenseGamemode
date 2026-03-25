package org.ReDiego0.defenseGamemode.game

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.config.MissionConfig
import org.ReDiego0.defenseGamemode.config.MissionManager
import org.ReDiego0.defenseGamemode.player.Party
import org.ReDiego0.defenseGamemode.player.PartyManager
import org.ReDiego0.defenseGamemode.world.LocalWorldService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

object MatchmakingManager {

    private val activeQueues = mutableMapOf<String, LobbyQueue>()

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