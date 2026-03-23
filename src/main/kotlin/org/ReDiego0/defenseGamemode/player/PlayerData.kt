package org.ReDiego0.defenseGamemode.player

import java.util.UUID

data class PlayerData(
    val uuid: UUID,
    var level: Int = 1,
    var experience: Double = 0.0,
    var totalKills: Int = 0,
    var currentClass: String = "iniciado",
    val unlockedClasses: MutableSet<String> = mutableSetOf("iniciado")
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

    private fun checkClassUnlocks() {
        if (level >= 5) {
            unlockedClasses.add("tanque")
            unlockedClasses.add("soporte")
            unlockedClasses.add("dano")
        }
    }
}