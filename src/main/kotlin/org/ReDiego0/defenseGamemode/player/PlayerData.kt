package org.ReDiego0.defenseGamemode.player

import org.ReDiego0.defenseGamemode.combat.weapons.WeaponManager
import java.util.UUID

data class WeaponData(
    val id: String,
    var level: Int = 1,
    var rarity: String = "COMUN"
)

data class PlayerData(
    val uuid: UUID,
    var level: Int = 1,
    var experience: Double = 0.0,
    var totalKills: Int = 0,
    val missionsCompleted: MutableMap<Int, Int> = mutableMapOf(),
    var currentClass: String = "iniciado",
    val unlockedClasses: MutableSet<String> = mutableSetOf("iniciado"),
    val unlockedWeapons: MutableMap<String, WeaponData> = mutableMapOf(),
    var equippedWeapons: MutableList<String> = mutableListOf("", "", ""),
    var equippedArmor: MutableList<String> = mutableListOf("", "", "", ""),
    var equippedConsumables: MutableList<String> = mutableListOf("", "")
) {
    fun addExperience(amount: Double): Boolean {
        experience += amount
        return checkLevelUp()
    }

    private fun checkLevelUp(): Boolean {
        var leveledUp = false
        var requiredExp = level * 1000.0

        while (experience >= requiredExp) {
            experience -= requiredExp
            level++
            requiredExp = level * 1000.0
            leveledUp = true
            checkClassUnlocks()
        }
        return leveledUp
    }

    fun checkClassUnlocks() {
        if (level >= 5) {
            unlockedClasses.add("guardian")
            unlockedClasses.add("mago")
            unlockedClasses.add("caballero")
        }
    }

    fun validateLoadoutOnClassChange() {
        equippedWeapons = equippedWeapons.map { weaponId ->
            if (weaponId.isNotBlank()) {
                val config = WeaponManager.getWeapon(weaponId)
                if (config != null && !config.isExotic && config.classRequirement != null) {
                    if (!config.classRequirement.equals(currentClass, ignoreCase = true)) {
                        return@map ""
                    }
                }
            }
            weaponId
        }.toMutableList()
    }

    fun getTotalMissionsCompleted(): Int {
        return missionsCompleted.values.sum()
    }
}