package org.ReDiego0.defenseGamemode.config

data class MobEntry(
    val id: String,
    val tier: Int,
    val baseWeight: Double
)

data class MobPool(
    val id: String,
    val mobs: List<MobEntry>
) {
    fun getMobsForTier(maxTier: Int): List<MobEntry> {
        return mobs.filter { it.tier <= maxTier }
    }
}