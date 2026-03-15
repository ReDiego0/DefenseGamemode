package org.ReDiego0.defenseGamemode.game

import org.bukkit.entity.Player
import org.ReDiego0.defenseGamemode.world.LocalWorldService

object MatchmakingManager {

    fun joinMap(player: Player, mapName: String) {
        val currentMatch = GameManager.getMatchByPlayer(player.uniqueId)
        if (currentMatch != null) {
            player.sendMessage("§cYa estás en una partida.")
            return
        }

        val availableMatch = GameManager.getAvailableMatch(mapName)
        if (availableMatch != null) {
            joinExistingMatch(player, availableMatch)
            return
        }

        player.sendMessage("§eBuscando partidas... Creando una nueva instancia de §b$mapName§e.")

        LocalWorldService.createInstanceAsync(mapName).thenAccept { world ->
            if (world == null) {
                player.sendMessage("§cError: El mapa '$mapName' no está configurado o no existe.")
                return@thenAccept
            }

            val newMatch = Match(
                matchId = world.name,
                mapName = mapName,
                world = world,
                maxPlayers = 4
            )

            GameManager.registerMatch(newMatch)
            joinExistingMatch(player, newMatch)
        }.exceptionally { ex ->
            player.sendMessage("§cOcurrió un error al crear la instancia.")
            ex.printStackTrace()
            null
        }
    }

    private fun joinExistingMatch(player: Player, match: Match) {
        if (match.addPlayer(player)) {
            player.teleportAsync(match.world.spawnLocation).thenAccept {
                player.sendMessage("§a¡Te has unido a la partida en §b${match.mapName}§a!")
            }
        } else {
            player.sendMessage("§cLa partida ya está llena o en progreso.")
        }
    }
}