package org.ReDiego0.defenseGamemode.game

import org.ReDiego0.defenseGamemode.config.MissionConfig
import org.ReDiego0.defenseGamemode.config.MissionManager
import org.ReDiego0.defenseGamemode.world.LocalWorldService
import org.bukkit.entity.Player

object MatchmakingManager {

    fun joinMap(player: Player, mapName: String) {
        val missionConfig = MissionManager.getMission(mapName)
        if (missionConfig == null) {
            player.sendMessage("§cError: La misión '$mapName' no existe en la configuración.")
            return
        }

        val currentMatch = GameManager.getMatchByPlayer(player.uniqueId)
        if (currentMatch != null) {
            player.sendMessage("§cYa estás en una partida.")
            return
        }

        val availableMatch = GameManager.getAvailableMatch(mapName)
        if (availableMatch != null) {
            joinExistingMatch(player, availableMatch, missionConfig)
            return
        }

        player.sendMessage("§eBuscando partidas... Creando una nueva instancia de §b$mapName§e.")

        LocalWorldService.createInstanceAsync(missionConfig.templateName).thenAccept { world ->
            if (world == null) {
                player.sendMessage("§cError: La plantilla '${missionConfig.templateName}' no existe en el servidor.")
                return@thenAccept
            }

            val newMatch = Match(
                matchId = world.name,
                mapName = mapName,
                world = world,
                maxPlayers = missionConfig.maxPlayers
            )

            GameManager.registerMatch(newMatch)
            joinExistingMatch(player, newMatch, missionConfig)
        }.exceptionally { ex ->
            player.sendMessage("§cOcurrió un error al crear la instancia.")
            ex.printStackTrace()
            null
        }
    }

    private fun joinExistingMatch(player: Player, match: Match, config: MissionConfig) {
        if (match.addPlayer(player)) {
            val spawnLoc = config.spawnLocation.toRandomizedBukkitLocation(match.world, config.spawnRadius)

            player.teleportAsync(spawnLoc).thenAccept {
                player.sendMessage("§a¡Te has unido a la partida en §b${match.mapName}§a!")
            }
        } else {
            player.sendMessage("§cLa partida ya está llena o en progreso.")
        }
    }
}