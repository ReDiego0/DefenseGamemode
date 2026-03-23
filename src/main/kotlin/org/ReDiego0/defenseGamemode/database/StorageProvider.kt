package org.ReDiego0.defenseGamemode.database

import org.ReDiego0.defenseGamemode.player.PlayerData
import java.util.UUID

interface StorageProvider {
    fun init()
    fun loadPlayer(uuid: UUID): PlayerData
    fun savePlayer(playerData: PlayerData)
    fun close()
}