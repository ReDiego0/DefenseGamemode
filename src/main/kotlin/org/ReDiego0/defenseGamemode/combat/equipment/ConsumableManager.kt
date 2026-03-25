package org.ReDiego0.defenseGamemode.combat.equipment

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

data class Consumable(
    val id: String,
    val displayName: String,
    val material: Material,
    val matchAmount: Int
)

object ConsumableManager {
    private val consumables = mutableMapOf<String, Consumable>()

    init {
        consumables["pocion_curacion"] = Consumable("pocion_curacion", "§aPoción de Curación", Material.HONEY_BOTTLE, 4)
        consumables["bomba_rango_bajo"] = Consumable("bomba_rango_bajo", "§cBomba de Rango Bajo", Material.SNOWBALL, 3)
    }

    fun buildConsumableItem(id: String): ItemStack? {
        val consumable = consumables[id] ?: return null
        val item = ItemStack(consumable.material, consumable.matchAmount)
        val meta = item.itemMeta ?: return null

        meta.setDisplayName(consumable.displayName)

        val key = NamespacedKey(DefenseGamemode.instance, "consumable_id")
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, id)

        item.itemMeta = meta
        return item
    }
}