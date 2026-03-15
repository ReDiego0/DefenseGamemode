package org.ReDiego0.defenseGamemode.game

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val matches = ConcurrentHashMap<String, Match>()

    fun registerMatch(match: Match) {
        matches[match.matchId] = match
    }

    fun getMatch(matchId: String): Match? {
        return matches[matchId]
    }

    fun removeMatch(matchId: String) {
        matches.remove(matchId)
    }

    fun getMatchByPlayer(playerUuid: UUID): Match? {
        return matches.values.firstOrNull { it.players.contains(playerUuid) }
    }

    fun getAvailableMatch(mapName: String): Match? {
        return matches.values.firstOrNull {
            it.mapName == mapName &&
                    it.state == MatchState.WAITING &&
                    it.players.size < it.maxPlayers
        }
    }

    fun shutdownAllMatches() {
        matches.values.forEach { it.changeState(MatchState.ENDING) }
        matches.clear()
    }
}