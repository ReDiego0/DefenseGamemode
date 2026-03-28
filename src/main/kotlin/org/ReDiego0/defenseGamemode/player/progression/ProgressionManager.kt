package org.ReDiego0.defenseGamemode.player.progression

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File

enum class RewardType {
    WEAPON_UNLOCK,
    ARMOR_UNLOCK,
    CONSUMABLE_UNLOCK,
    SKILL_UPGRADE,
    COMMAND,
    MONEY
}

data class ProgressionReward(
    val type: RewardType,
    val id: String?,
    val command: String?,
    val amount: Double?,
    val icon: Material,
    val displayName: String,
    val lore: List<String>
)

object ProgressionManager {

    private val classProgressions = mutableMapOf<String, Map<String, Map<Int, ProgressionReward>>>()

    fun loadProgressions(plugin: Plugin) {
        classProgressions.clear()

        val file = File(plugin.dataFolder, "class_progression.yml")
        if (!file.exists()) {
            plugin.saveResource("class_progression.yml", false)
        }

        val config = YamlConfiguration.loadConfiguration(file)

        for (classId in config.getKeys(false)) {
            val classSection = config.getConfigurationSection(classId) ?: continue
            val branchesMap = mutableMapOf<String, Map<Int, ProgressionReward>>()

            for (branch in classSection.getKeys(false)) {
                val branchSection = classSection.getConfigurationSection(branch) ?: continue
                val levelsMap = mutableMapOf<Int, ProgressionReward>()

                for (levelStr in branchSection.getKeys(false)) {
                    val level = levelStr.toIntOrNull() ?: continue
                    val rewardSection = branchSection.getConfigurationSection(levelStr) ?: continue

                    val typeStr = rewardSection.getString("type", "COMMAND") ?: "COMMAND"
                    val type = runCatching { RewardType.valueOf(typeStr.uppercase()) }.getOrDefault(RewardType.COMMAND)
                    val iconStr = rewardSection.getString("icon", "BARRIER") ?: "BARRIER"

                    val reward = ProgressionReward(
                        type = type,
                        id = rewardSection.getString("id"),
                        command = rewardSection.getString("command"),
                        amount = rewardSection.getDouble("amount", 0.0),
                        icon = Material.matchMaterial(iconStr) ?: Material.BARRIER,
                        displayName = rewardSection.getString("display-name", "Recompensa Desconocida") ?: "",
                        lore = rewardSection.getStringList("lore").map { it.replace("&", "§") }
                    )
                    levelsMap[level] = reward
                }
                branchesMap[branch.lowercase()] = levelsMap
            }
            classProgressions[classId.lowercase()] = branchesMap
        }
    }

    fun getReward(classId: String, branch: String, level: Int): ProgressionReward? {
        return classProgressions[classId.lowercase()]?.get(branch.lowercase())?.get(level)
    }

    fun getBranchRewards(classId: String, branch: String): Map<Int, ProgressionReward> {
        return classProgressions[classId.lowercase()]?.get(branch.lowercase()) ?: emptyMap()
    }
}