package org.ReDiego0.defenseGamemode.ui

import net.kyori.adventure.text.Component
import org.ReDiego0.defenseGamemode.combat.weapons.WeaponManager
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object ExoticMenu {

    const val MAIN_TITLE = "§8Categorías Exóticas"
    const val WEAPONS_TITLE = "§8Armas Exóticas"

    fun openMainMenu(player: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 27, Component.text(MAIN_TITLE))

        val bg = ItemStack(Material.BLACK_STAINED_GLASS_PANE).apply {
            val meta = itemMeta; meta.setDisplayName(" "); itemMeta = meta
        }
        for (i in 0 until 27) inv.setItem(i, bg)

        val weaponIcon = createIcon("§c§lArmas Exóticas", Material.NETHERITE_SWORD, "§7Click para ver las armas míticas.")
        val armorIcon = createIcon("§b§lArmaduras Exóticas", Material.NETHERITE_CHESTPLATE, "§7Click para ver las armaduras míticas.", "§c(Próximamente)")
        val potionIcon = createIcon("§d§lConsumibles Exóticos", Material.DRAGON_BREATH, "§7Click para ver los consumibles míticos.", "§c(Próximamente)")

        inv.setItem(11, weaponIcon)
        inv.setItem(13, armorIcon)
        inv.setItem(15, potionIcon)

        player.openInventory(inv)
    }

    fun openWeaponCategory(player: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 54, Component.text(WEAPONS_TITLE))
        val data = PlayerDataManager.getPlayerData(player.uniqueId) ?: return

        val exoticWeapons = WeaponManager.getWeaponsForClass("").filter { it.isExotic }

        exoticWeapons.forEachIndexed { index, weapon ->
            if (index >= 54) return@forEachIndexed

            val levelReqMet = data.level >= weapon.requiredLevel
            val missionsReqMet = data.getTotalMissionsCompleted() >= weapon.requiredMissions
            val isUnlocked = levelReqMet && missionsReqMet

            val item = ItemStack(if (isUnlocked) weapon.material else Material.GRAY_DYE).apply {
                val meta = itemMeta
                meta.setDisplayName(if (isUnlocked) weapon.displayName.replace("&", "§") else "§8???")

                if (isUnlocked && meta.hasCustomModelData()) {
                    meta.setCustomModelData(weapon.customModelData)
                }

                val lore = mutableListOf<String>()
                lore.add("§d§lMÍTICA")
                lore.add("")

                if (isUnlocked) {
                    lore.add("§a¡Desbloqueada!")
                    lore.add("§7Puedes equiparla desde tu Loadout.")
                } else {
                    lore.add("§cRequisitos para desbloquear:")
                    val levelColor = if (levelReqMet) "§a" else "§c"
                    val missionColor = if (missionsReqMet) "§a" else "§c"
                    lore.add("$levelColor- Nivel: ${data.level} / ${weapon.requiredLevel}")
                    lore.add("$missionColor- Misiones: ${data.getTotalMissionsCompleted()} / ${weapon.requiredMissions}")
                }

                meta.lore = lore
                itemMeta = meta
            }
            inv.setItem(index, item)
        }

        val backButton = createIcon("§cVolver", Material.ARROW, "§7Regresar a las categorías.")
        inv.setItem(49, backButton)

        player.openInventory(inv)
    }

    private fun createIcon(name: String, material: Material, vararg loreLines: String): ItemStack {
        return ItemStack(material).apply {
            val meta = itemMeta
            meta.setDisplayName(name)
            meta.lore = loreLines.toList()
            itemMeta = meta
        }
    }
}