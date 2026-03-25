package org.ReDiego0.defenseGamemode.combat.weapons

import org.bukkit.Material

data class WeaponConfig(
    val id: String,
    val displayName: String,
    val material: Material,
    val customModelData: Int,
    val requiredLevel: Int,
    val requiredMissions: Int,
    val classRequirement: String?,
    val skillId: String?,
    val isExotic: Boolean
)