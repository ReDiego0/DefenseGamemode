package org.ReDiego0.defenseGamemode.game

import org.bukkit.World
import org.bukkit.entity.Player
import org.ReDiego0.defenseGamemode.world.LocalWorldService
import java.util.UUID

class Match(
    val matchId: String,
    val mapName: String,
    val world: World,
    val maxPlayers: Int
) {
    var state: MatchState = MatchState.WAITING
        private set

    val players = mutableSetOf<UUID>()
    val deadPlayers = mutableSetOf<UUID>()
    var currentWave = 0

    fun changeState(newState: MatchState) {
        state = newState
        handleStateTransition()
    }

    private fun handleStateTransition() {
        when (state) {
            MatchState.WAITING -> { }
            MatchState.PREPARATION -> { }
            MatchState.ACTIVE_WAVE -> { }
            MatchState.VOTING -> { }
            MatchState.ENDING -> {
                LocalWorldService.deleteInstance(world.name)
                GameManager.removeMatch(matchId)
            }
        }
    }

    fun getAvailableSlots(): Int = maxPlayers - players.size

    fun addPlayer(player: Player): Boolean {
        if (state != MatchState.WAITING || players.size >= maxPlayers) return false
        players.add(player.uniqueId)
        return true
    }

    fun removePlayer(player: Player) {
        players.remove(player.uniqueId)
        deadPlayers.remove(player.uniqueId)
        if (players.isEmpty() && state != MatchState.ENDING) {
            changeState(MatchState.ENDING)
        }
    }
}