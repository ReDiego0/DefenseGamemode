package org.ReDiego0.defenseGamemode.setup

import org.bukkit.Location
import org.bukkit.inventory.ItemStack

enum class ChatPhase {
    NONE,
    AWAITING_NAME,
    AWAITING_DIFFICULTY
}

data class SetupSession(
    val mapName: String,
    val originalInventory: Array<ItemStack?>,
    val originalArmor: Array<ItemStack?>
) {
    var playerSpawn: Location? = null
    var targetSpawn: Location? = null
    val mobSpawns = mutableListOf<Location>()

    var chatPhase: ChatPhase = ChatPhase.NONE
    var missionDisplayName: String = ""
    var baseDifficulty: Int = 1

    fun clearAll() {
        playerSpawn = null
        targetSpawn = null
        mobSpawns.clear()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SetupSession

        if (mapName != other.mapName) return false
        if (!originalInventory.contentEquals(other.originalInventory)) return false
        if (!originalArmor.contentEquals(other.originalArmor)) return false
        if (playerSpawn != other.playerSpawn) return false
        if (targetSpawn != other.targetSpawn) return false
        if (mobSpawns != other.mobSpawns) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mapName.hashCode()
        result = 31 * result + originalInventory.contentHashCode()
        result = 31 * result + originalArmor.contentHashCode()
        result = 31 * result + (playerSpawn?.hashCode() ?: 0)
        result = 31 * result + (targetSpawn?.hashCode() ?: 0)
        result = 31 * result + mobSpawns.hashCode()
        return result
    }
}