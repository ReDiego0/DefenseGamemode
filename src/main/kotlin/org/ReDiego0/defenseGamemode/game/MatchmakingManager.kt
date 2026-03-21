package org.ReDiego0.defenseGamemode.game

import org.ReDiego0.defenseGamemode.config.MissionConfig
import org.ReDiego0.defenseGamemode.config.MissionManager
import org.ReDiego0.defenseGamemode.player.Party
import org.ReDiego0.defenseGamemode.player.PartyManager
import org.ReDiego0.defenseGamemode.world.LocalWorldService
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object MatchmakingManager {

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

        if (party.members.size == missionConfig.maxPlayers) {
            player.sendMessage("§eParty completa. Saltando matchmaking y creando partida privada de §b$mapName§e...")
            createAndJoinInstance(party, mapName, missionConfig)
            return
        }

        val availableMatch = GameManager.getAvailableMatch(mapName, party.members.size)
        if (availableMatch != null) {
            joinExistingMatch(party, availableMatch, missionConfig)
            return
        }

        player.sendMessage("§eBuscando partidas... Creando una nueva instancia de §b$mapName§e para tu party.")
        createAndJoinInstance(party, mapName, missionConfig)
    }

    private fun createAndJoinInstance(party: Party, mapName: String, config: MissionConfig) {
        LocalWorldService.createInstanceAsync(config.templateName).thenAccept { world ->
            if (world == null) {
                party.members.mapNotNull { Bukkit.getPlayer(it) }.forEach {
                    it.sendMessage("§cError crítico: La plantilla '${config.templateName}' no existe.")
                }
                return@thenAccept
            }

            val newMatch = Match(
                matchId = world.name,
                mapName = mapName,
                world = world,
                maxPlayers = config.maxPlayers
            )

            GameManager.registerMatch(newMatch)
            joinExistingMatch(party, newMatch, config)
        }.exceptionally { ex ->
            ex.printStackTrace()
            null
        }
    }

    private fun joinExistingMatch(party: Party, match: Match, config: MissionConfig) {
        party.members.mapNotNull { Bukkit.getPlayer(it) }.forEach { member ->
            if (match.addPlayer(member)) {
                val spawnLoc = config.spawnLocation.toRandomizedBukkitLocation(match.world, config.spawnRadius)
                member.teleportAsync(spawnLoc).thenAccept {
                    member.sendMessage("§a¡Te has unido a la partida en §b${match.mapName}§a!")
                }
            }
        }
    }
}