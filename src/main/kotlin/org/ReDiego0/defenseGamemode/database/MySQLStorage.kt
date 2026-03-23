package org.ReDiego0.defenseGamemode.database

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.ReDiego0.defenseGamemode.player.PlayerData
import org.ReDiego0.defenseGamemode.player.WeaponData
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class MySQLStorage(
    private val host: String,
    private val port: Int,
    private val database: String,
    private val user: String,
    private val pass: String
) : StorageProvider {
    private var connection: Connection? = null
    private val gson = Gson()

    override fun init() {
        connection = DriverManager.getConnection(
            "jdbc:mysql://$host:$port/$database?autoReconnect=true&useSSL=false", user, pass
        )

        connection?.prepareStatement(
            "CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "level INT NOT NULL," +
                    "experience DOUBLE NOT NULL," +
                    "totalKills INT NOT NULL," +
                    "currentClass VARCHAR(32) NOT NULL DEFAULT 'iniciado'," +
                    "unlockedClasses TEXT NOT NULL," +
                    "unlockedWeapons TEXT," +
                    "equippedWeapons TEXT," +
                    "equippedArmor TEXT," +
                    "equippedConsumables TEXT)"
        )?.executeUpdate()
    }

    override fun loadPlayer(uuid: UUID): PlayerData {
        val statement = connection?.prepareStatement("SELECT * FROM player_data WHERE uuid = ?")
        statement?.setString(1, uuid.toString())
        val rs = statement?.executeQuery()

        if (rs != null && rs.next()) {
            val level = rs.getInt("level")
            val exp = rs.getDouble("experience")
            val kills = rs.getInt("totalKills")
            val currentClass = rs.getString("currentClass") ?: "iniciado"
            val unlockedStr = rs.getString("unlockedClasses") ?: "iniciado"

            val unlockedClasses = unlockedStr.split(",").filter { it.isNotEmpty() }.toMutableSet()
            if (unlockedClasses.isEmpty()) unlockedClasses.add("iniciado")

            val mapType = object : TypeToken<MutableMap<String, WeaponData>>() {}.type
            val listType = object : TypeToken<MutableList<String>>() {}.type

            val unlockedWeapons: MutableMap<String, WeaponData> = gson.fromJson(rs.getString("unlockedWeapons") ?: "{}", mapType) ?: mutableMapOf()
            val equippedWeapons: MutableList<String> = gson.fromJson(rs.getString("equippedWeapons") ?: "[\"\",\"\",\"\"]", listType) ?: mutableListOf("", "", "")
            val equippedArmor: MutableList<String> = gson.fromJson(rs.getString("equippedArmor") ?: "[\"\",\"\",\"\",\"\"]", listType) ?: mutableListOf("", "", "", "")
            val equippedConsumables: MutableList<String> = gson.fromJson(rs.getString("equippedConsumables") ?: "[\"\",\"\"]", listType) ?: mutableListOf("", "")

            return PlayerData(uuid, level, exp, kills, currentClass, unlockedClasses, unlockedWeapons, equippedWeapons, equippedArmor, equippedConsumables)
        }
        return PlayerData(uuid)
    }

    override fun savePlayer(playerData: PlayerData) {
        val statement = connection?.prepareStatement(
            "INSERT INTO player_data (uuid, level, experience, totalKills, currentClass, unlockedClasses, unlockedWeapons, equippedWeapons, equippedArmor, equippedConsumables) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE level = ?, experience = ?, totalKills = ?, currentClass = ?, unlockedClasses = ?, unlockedWeapons = ?, equippedWeapons = ?, equippedArmor = ?, equippedConsumables = ?"
        )
        val weaponsJson = gson.toJson(playerData.unlockedWeapons)
        val eqWeaponsJson = gson.toJson(playerData.equippedWeapons)
        val eqArmorJson = gson.toJson(playerData.equippedArmor)
        val eqConsumablesJson = gson.toJson(playerData.equippedConsumables)

        statement?.setString(1, playerData.uuid.toString())
        statement?.setInt(2, playerData.level)
        statement?.setDouble(3, playerData.experience)
        statement?.setInt(4, playerData.totalKills)
        statement?.setString(5, playerData.currentClass)
        statement?.setString(6, playerData.unlockedClasses.joinToString(","))
        statement?.setString(7, weaponsJson)
        statement?.setString(8, eqWeaponsJson)
        statement?.setString(9, eqArmorJson)
        statement?.setString(10, eqConsumablesJson)

        statement?.setInt(11, playerData.level)
        statement?.setDouble(12, playerData.experience)
        statement?.setInt(13, playerData.totalKills)
        statement?.setString(14, playerData.currentClass)
        statement?.setString(15, playerData.unlockedClasses.joinToString(","))
        statement?.setString(16, weaponsJson)
        statement?.setString(17, eqWeaponsJson)
        statement?.setString(18, eqArmorJson)
        statement?.setString(19, eqConsumablesJson)

        statement?.executeUpdate()
    }

    override fun close() {
        connection?.close()
    }
}