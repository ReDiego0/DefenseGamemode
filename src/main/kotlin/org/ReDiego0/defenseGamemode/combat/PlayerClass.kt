package org.ReDiego0.defenseGamemode.combat

enum class PlayerClass(
    val id: String,
    val displayName: String,
    val description: String
) {
    INICIADO(
        "iniciado",
        "§7Iniciado",
        "Clase básica. Aprende los fundamentos del combate y la supervivencia."
    ),
    GUARDIAN(
        "guardian",
        "§6Guardián",
        "Usa escudo y lanza. Especialista en atraer la atención y aguantar el daño."
    ),
    MAGO(
        "mago",
        "§bMago",
        "Usa grimorios. Aplica buffos vitales a aliados y debilita a las hordas."
    ),
    CABALLERO(
        "caballero",
        "§cCaballero",
        "Usa espadones. Enfocado en generar daño masivo de área y control."
    );

    companion object {
        fun fromId(id: String): PlayerClass {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: INICIADO
        }
    }
}