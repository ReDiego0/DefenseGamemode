package org.ReDiego0.defenseGamemode.database

import org.ReDiego0.defenseGamemode.player.PlayerData
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
                    "unlockedClasses TEXT NOT NULL)"
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

            return PlayerData(uuid, level, exp, kills, currentClass, unlockedClasses)
        }
        return PlayerData(uuid)
    }

    override fun savePlayer(playerData: PlayerData) {
        val statement = connection?.prepareStatement(
            "INSERT INTO player_data (uuid, level, experience, totalKills, currentClass, unlockedClasses) VALUES (?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE level = ?, experience = ?, totalKills = ?, currentClass = ?, unlockedClasses = ?"
        )
        statement?.setString(1, playerData.uuid.toString())
        statement?.setInt(2, playerData.level)
        statement?.setDouble(3, playerData.experience)
        statement?.setInt(4, playerData.totalKills)
        statement?.setString(5, playerData.currentClass)
        statement?.setString(6, playerData.unlockedClasses.joinToString(","))

        statement?.setInt(7, playerData.level)
        statement?.setDouble(8, playerData.experience)
        statement?.setInt(9, playerData.totalKills)
        statement?.setString(10, playerData.currentClass)
        statement?.setString(11, playerData.unlockedClasses.joinToString(","))

        statement?.executeUpdate()
    }

    override fun close() {
        connection?.close()
    }
}