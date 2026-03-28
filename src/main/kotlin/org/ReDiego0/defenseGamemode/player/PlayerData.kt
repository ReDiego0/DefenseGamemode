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
    val classLevels: MutableMap<String, Int> = mutableMapOf(),
    val classExperience: MutableMap<String, Double> = mutableMapOf(),
    val unlockedClasses: MutableSet<String> = mutableSetOf("iniciado"),
    val unlockedWeapons: MutableMap<String, WeaponData> = mutableMapOf(),
    var equippedWeapons: MutableList<String> = mutableListOf("", "", ""),
    var equippedArmor: MutableList<String> = mutableListOf("", "", "", ""),
    var equippedConsumables: MutableList<String> = mutableListOf("", ""),
    val claimedClassRewards: MutableMap<String, MutableSet<Int>> = mutableMapOf(),
    val bodega: MutableMap<String, Int> = mutableMapOf()
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

    fun addClassExperience(classId: String, amount: Double): Boolean {
        val currentExp = classExperience.getOrDefault(classId, 0.0) + amount
        classExperience[classId] = currentExp
        return checkClassLevelUp(classId)
    }

    private fun checkClassLevelUp(classId: String): Boolean {
        var leveledUp = false
        var currentLevel = classLevels.getOrDefault(classId, 1)
        var requiredExp = getClassRequiredExp(currentLevel)

        while (classExperience.getOrDefault(classId, 0.0) >= requiredExp && currentLevel < 18) {
            classExperience[classId] = classExperience.getOrDefault(classId, 0.0) - requiredExp
            currentLevel++
            classLevels[classId] = currentLevel
            requiredExp = getClassRequiredExp(currentLevel)
            leveledUp = true
        }
        return leveledUp
    }

    fun getClassLevel(classId: String): Int {
        return classLevels.getOrDefault(classId, 1)
    }

    fun getClassRequiredExp(currentLevel: Int): Double {
        return 5000.0 * currentLevel + (currentLevel * currentLevel * 800.0)
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