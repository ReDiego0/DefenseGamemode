package org.ReDiego0.defenseGamemode.combat.equipment

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

data class ArmorSet(
    val id: String,
    val helmet: Material,
    val chestplate: Material,
    val leggings: Material,
    val boots: Material
)

object ArmorManager {
    private val armors = mutableMapOf<String, ArmorSet>()

    init {
        armors["set_hierro"] = ArmorSet(
            "set_hierro",
            Material.IRON_HELMET,
            Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,
            Material.IRON_BOOTS
        )
    }

    fun getArmorSet(id: String): ArmorSet? = armors[id]

    fun buildPiece(material: Material): ItemStack {
        return ItemStack(material).apply {
            val meta = itemMeta
            meta?.isUnbreakable = true
            itemMeta = meta
        }
    }
}