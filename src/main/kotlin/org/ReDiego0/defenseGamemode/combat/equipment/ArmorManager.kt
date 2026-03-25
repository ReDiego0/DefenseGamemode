package org.ReDiego0.defenseGamemode.combat.equipment

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

data class ArmorSet(
    val id: String,
    val displayName: String,
    val iconMaterial: Material,
    val helmet: Material,
    val chestplate: Material,
    val leggings: Material,
    val boots: Material,
    val requiredLevel: Int = 1,
    val requiredMissions: Int = 0,
    val isExotic: Boolean = false
)

object ArmorManager {
    private val armors = mutableMapOf<String, ArmorSet>()

    init {
        armors["set_hierro"] = ArmorSet(
            "set_hierro",
            "§fArmadura de Hierro",
            Material.IRON_CHESTPLATE,
            Material.IRON_HELMET,
            Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,
            Material.IRON_BOOTS,
            1,
            0,
            false
        )
    }

    fun getArmorSet(id: String): ArmorSet? = armors[id]

    fun getAllArmors(): List<ArmorSet> = armors.values.toList()

    fun buildPiece(material: Material): ItemStack {
        return ItemStack(material).apply {
            val meta = itemMeta
            meta?.isUnbreakable = true
            itemMeta = meta
        }
    }
}