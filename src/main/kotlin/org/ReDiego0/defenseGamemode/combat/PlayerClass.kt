package org.ReDiego0.defenseGamemode.combat

enum class PlayerClass(
    val id: String,
    val displayName: String,
    val description: String
) {
    INICIADO(
        "iniciado",
        "Iniciado",
        "Clase básica. Aprende los fundamentos del combate."
    ),
    TANQUE(
        "tanque",
        "Tanque",
        "Especialista en mitigar daño, atraer la atención y aguantar golpes."
    ),
    SOPORTE(
        "soporte",
        "Soporte",
        "Ayuda a sus aliados con curaciones y debilita a las hordas enemigas."
    ),
    DANO(
        "dano",
        "Daño",
        "Especialista ágil enfocado en infligir daño masivo en área."
    );

    companion object {
        fun fromId(id: String): PlayerClass {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: INICIADO
        }
    }
}