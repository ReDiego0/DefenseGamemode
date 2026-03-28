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
                    "missionsCompleted TEXT," +
                    "currentClass VARCHAR(32) NOT NULL DEFAULT 'iniciado'," +
                    "classLevels TEXT," +
                    "classExperience TEXT," +
                    "claimedClassRewards TEXT," +
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

            val missionsType = object : TypeToken<MutableMap<Int, Int>>() {}.type
            val classLevelsType = object : TypeToken<MutableMap<String, Int>>() {}.type
            val classExpType = object : TypeToken<MutableMap<String, Double>>() {}.type
            val claimedType = object : TypeToken<MutableMap<String, MutableSet<Int>>>() {}.type
            val mapType = object : TypeToken<MutableMap<String, WeaponData>>() {}.type
            val listType = object : TypeToken<MutableList<String>>() {}.type

            val missionsCompleted: MutableMap<Int, Int> = gson.fromJson(rs.getString("missionsCompleted") ?: "{}", missionsType) ?: mutableMapOf()
            val classLevels: MutableMap<String, Int> = gson.fromJson(rs.getString("classLevels") ?: "{}", classLevelsType) ?: mutableMapOf()
            val classExperience: MutableMap<String, Double> = gson.fromJson(rs.getString("classExperience") ?: "{}", classExpType) ?: mutableMapOf()
            val claimedClassRewards: MutableMap<String, MutableSet<Int>> = gson.fromJson(rs.getString("claimedClassRewards") ?: "{}", claimedType) ?: mutableMapOf()
            val unlockedWeapons: MutableMap<String, WeaponData> = gson.fromJson(rs.getString("unlockedWeapons") ?: "{}", mapType) ?: mutableMapOf()
            val equippedWeapons: MutableList<String> = gson.fromJson(rs.getString("equippedWeapons") ?: "[\"\",\"\",\"\"]", listType) ?: mutableListOf("", "", "")
            val equippedArmor: MutableList<String> = gson.fromJson(rs.getString("equippedArmor") ?: "[\"\",\"\",\"\",\"\"]", listType) ?: mutableListOf("", "", "", "")
            val equippedConsumables: MutableList<String> = gson.fromJson(rs.getString("equippedConsumables") ?: "[\"\",\"\"]", listType) ?: mutableListOf("", "")

            return PlayerData(uuid, level, exp, kills, missionsCompleted, currentClass, classLevels, classExperience, unlockedClasses, unlockedWeapons, equippedWeapons, equippedArmor, equippedConsumables, claimedClassRewards)
        }
        return PlayerData(uuid)
    }

    override fun savePlayer(playerData: PlayerData) {
        val statement = connection?.prepareStatement(
            "INSERT INTO player_data (uuid, level, experience, totalKills, missionsCompleted, currentClass, classLevels, classExperience, claimedClassRewards, unlockedClasses, unlockedWeapons, equippedWeapons, equippedArmor, equippedConsumables) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE level = ?, experience = ?, totalKills = ?, missionsCompleted = ?, currentClass = ?, classLevels = ?, classExperience = ?, claimedClassRewards = ?, unlockedClasses = ?, unlockedWeapons = ?, equippedWeapons = ?, equippedArmor = ?, equippedConsumables = ?"
        )
        val missionsJson = gson.toJson(playerData.missionsCompleted)
        val classLevelsJson = gson.toJson(playerData.classLevels)
        val classExpJson = gson.toJson(playerData.classExperience)
        val claimedRewardsJson = gson.toJson(playerData.claimedClassRewards)
        val weaponsJson = gson.toJson(playerData.unlockedWeapons)
        val eqWeaponsJson = gson.toJson(playerData.equippedWeapons)
        val eqArmorJson = gson.toJson(playerData.equippedArmor)
        val eqConsumablesJson = gson.toJson(playerData.equippedConsumables)

        statement?.setString(1, playerData.uuid.toString())
        statement?.setInt(2, playerData.level)
        statement?.setDouble(3, playerData.experience)
        statement?.setInt(4, playerData.totalKills)
        statement?.setString(5, missionsJson)
        statement?.setString(6, playerData.currentClass)
        statement?.setString(7, classLevelsJson)
        statement?.setString(8, classExpJson)
        statement?.setString(9, claimedRewardsJson)
        statement?.setString(10, playerData.unlockedClasses.joinToString(","))
        statement?.setString(11, weaponsJson)
        statement?.setString(12, eqWeaponsJson)
        statement?.setString(13, eqArmorJson)
        statement?.setString(14, eqConsumablesJson)

        statement?.setInt(15, playerData.level)
        statement?.setDouble(16, playerData.experience)
        statement?.setInt(17, playerData.totalKills)
        statement?.setString(18, missionsJson)
        statement?.setString(19, playerData.currentClass)
        statement?.setString(20, classLevelsJson)
        statement?.setString(21, classExpJson)
        statement?.setString(22, claimedRewardsJson)
        statement?.setString(23, playerData.unlockedClasses.joinToString(","))
        statement?.setString(24, weaponsJson)
        statement?.setString(25, eqWeaponsJson)
        statement?.setString(26, eqArmorJson)
        statement?.setString(27, eqConsumablesJson)

        statement?.executeUpdate()
    }

    override fun close() {
        connection?.close()
    }
}