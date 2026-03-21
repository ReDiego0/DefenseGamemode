package org.ReDiego0.defenseGamemode.game

enum class DifficultyProfile {
    ALWAYS_EASY,
    ALWAYS_HARD,
    PROGRESSIVE_CAPPED,
    PROGRESSIVE_UNCAPPED;

    fun calculateEffectiveWeight(baseWeight: Double, maxBaseWeight: Double, wave: Int): Double {
        return when (this) {
            ALWAYS_EASY -> baseWeight
            ALWAYS_HARD -> Math.max(1.0, maxBaseWeight - baseWeight + 10.0)
            PROGRESSIVE_CAPPED -> {
                if (baseWeight < maxBaseWeight) {
                    Math.min(maxBaseWeight, baseWeight + (wave * 5.0))
                } else {
                    baseWeight
                }
            }
            PROGRESSIVE_UNCAPPED -> {
                if (baseWeight < maxBaseWeight) {
                    baseWeight + (wave * 8.0)
                } else {
                    baseWeight
                }
            }
        }
    }
}