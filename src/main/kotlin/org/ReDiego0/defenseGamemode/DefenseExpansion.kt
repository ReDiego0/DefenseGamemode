package org.ReDiego0.defenseGamemode

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.ReDiego0.defenseGamemode.combat.PlayerClass
import org.ReDiego0.defenseGamemode.game.GameManager
import org.ReDiego0.defenseGamemode.player.PartyManager
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.math.roundToInt

class DefenseExpansion : PlaceholderExpansion() {

    override fun getIdentifier(): String {
        return "defense"
    }

    override fun getAuthor(): String {
        return "ReDiego0"
    }

    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun persist(): Boolean {
        return true
    }

    override fun canRegister(): Boolean {
        return true
    }

    override fun onPlaceholderRequest(player: Player?, params: String): String {
        if (player == null) return ""

        val data = PlayerDataManager.getPlayerData(player.uniqueId)
        val match = GameManager.getMatchByPlayer(player.uniqueId)
        val party = PartyManager.getParty(player.uniqueId)

        return when (params.lowercase()) {
            "level" -> data?.level?.toString() ?: "1"
            "exp" -> data?.experience?.roundToInt()?.toString() ?: "0"
            "exp_required" -> {
                val req = (data?.level ?: 1) * 1000.0
                req.roundToInt().toString()
            }
            "exp_progress_bar" -> {
                val currentExp = data?.experience ?: 0.0
                val reqExp = (data?.level ?: 1) * 1000.0
                createProgressBar(currentExp, reqExp, 10)
            }
            "kills" -> data?.totalKills?.toString() ?: "0"
            "missions_completed" -> data?.getTotalMissionsCompleted()?.toString() ?: "0"

            "class" -> {
                val classId = data?.currentClass ?: "iniciado"
                PlayerClass.fromId(classId).displayName
            }
            "class_level" -> "1" // futuro sistema de Árbol de Habilidades y niveles por clase
            "unlocked_classes" -> data?.unlockedClasses?.size?.toString() ?: "1"

            "in_match" -> if (match != null) "Sí" else "No"
            "match_map" -> match?.mapName ?: "Ninguno"
            "match_wave" -> match?.currentWave?.toString() ?: "0"
            "match_state" -> match?.state?.name ?: "N/A"
            "match_players" -> match?.players?.size?.toString() ?: "0"
            "match_max_players" -> match?.maxPlayers?.toString() ?: "0"

            "party_size" -> party.members.size.toString()
            "party_leader" -> Bukkit.getPlayer(party.leader)?.name ?: "Ninguno"
            "is_party_leader" -> if (party.isLeader(player.uniqueId)) "Sí" else "No"

            else -> ""
        }
    }

    private fun createProgressBar(current: Double, max: Double, bars: Int): String {
        val percentage = (current / max).coerceIn(0.0, 1.0)
        val filledBars = (percentage * bars).toInt()
        val emptyBars = bars - filledBars

        val filledColor = "§a"
        val emptyColor = "§7"
        val symbol = "■"

        return filledColor + symbol.repeat(filledBars) + emptyColor + symbol.repeat(emptyBars)
    }
}