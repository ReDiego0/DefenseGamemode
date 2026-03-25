package org.ReDiego0.defenseGamemode.combat.weapons

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.player.WeaponData
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.io.File

object WeaponManager {
    private val weapons = mutableMapOf<String, WeaponConfig>()

    fun loadWeapons(plugin: Plugin) {
        weapons.clear()
        val folder = File(plugin.dataFolder, "weapons")
        if (!folder.exists()) {
            folder.mkdirs()
            createDefaultFiles(folder)
        }

        loadFromFile(File(folder, "class_weapons.yml"), isExotic = false)
        loadFromFile(File(folder, "exotic_weapons.yml"), isExotic = true)
    }

    private fun loadFromFile(file: File, isExotic: Boolean) {
        if (!file.exists()) return
        val config = YamlConfiguration.loadConfiguration(file)

        for (key in config.getKeys(false)) {
            val section = config.getConfigurationSection(key) ?: continue
            val materialStr = section.getString("material", "IRON_SWORD") ?: "IRON_SWORD"

            val weapon = WeaponConfig(
                id = key,
                displayName = section.getString("display-name", key) ?: key,
                material = Material.matchMaterial(materialStr) ?: Material.IRON_SWORD,
                customModelData = section.getInt("custom-model-data", 0),
                requiredLevel = section.getInt("requirements.level", 1),
                requiredMissions = section.getInt("requirements.missions", 0),
                classRequirement = section.getString("class-requirement"),
                skillId = section.getString("skill-id"),
                isExotic = isExotic
            )
            weapons[key] = weapon
        }
    }

    fun getWeapon(id: String): WeaponConfig? = weapons[id]

    fun getWeaponsForClass(className: String): List<WeaponConfig> {
        return weapons.values.filter { it.isExotic || it.classRequirement == className }
    }

    fun buildWeaponItem(weaponId: String, weaponData: WeaponData?): ItemStack? {
        val config = getWeapon(weaponId) ?: return null
        val item = ItemStack(config.material)
        val meta = item.itemMeta ?: return null

        val rarityColor = when(weaponData?.rarity?.uppercase()) {
            "COMUN" -> "§f"
            "RARO" -> "§9"
            "LEGENDARIO" -> "§6"
            "MITICO" -> "§d"
            else -> "§f"
        }

        meta.setDisplayName("$rarityColor${config.displayName.replace("&", "§")} §7[Lv. ${weaponData?.level ?: 1}]")

        if (config.customModelData > 0) {
            meta.setCustomModelData(config.customModelData)
        }

        val lore = mutableListOf<String>()
        if (config.isExotic) {
            lore.add("§d§lArma Mítica")
        } else {
            lore.add("§7Clase: ${config.classRequirement?.uppercase() ?: "TODAS"}")
        }
        lore.add("§8ID: ${config.id}")

        meta.lore = lore

        val key = NamespacedKey(DefenseGamemode.instance, "weapon_id")
        meta.persistentDataContainer.set(key, PersistentDataType.STRING, config.id)

        item.itemMeta = meta
        return item
    }

    private fun createDefaultFiles(folder: File) {
        val classWeapons = File(folder, "class_weapons.yml")
        val classConfig = YamlConfiguration.loadConfiguration(classWeapons)
        classConfig.set("espada_iniciado.display-name", "&fEspada de Iniciado")
        classConfig.set("espada_iniciado.material", "WOODEN_SWORD")
        classConfig.set("espada_iniciado.custom-model-data", 1)
        classConfig.set("espada_iniciado.requirements.level", 1)
        classConfig.set("espada_iniciado.requirements.missions", 0)
        classConfig.set("espada_iniciado.class-requirement", "iniciado")
        classConfig.save(classWeapons)

        val exoticWeapons = File(folder, "exotic_weapons.yml")
        val exoticConfig = YamlConfiguration.loadConfiguration(exoticWeapons)
        exoticConfig.set("katana_vacio.display-name", "&5Katana del Vacío")
        exoticConfig.set("katana_vacio.material", "IRON_SWORD")
        exoticConfig.set("katana_vacio.custom-model-data", 100)
        exoticConfig.set("katana_vacio.requirements.level", 10)
        exoticConfig.set("katana_vacio.requirements.missions", 5)
        exoticConfig.set("katana_vacio.skill-id", "corte_vacio")
        exoticConfig.save(exoticWeapons)
    }
}