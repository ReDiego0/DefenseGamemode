package org.ReDiego0.defenseGamemode.player

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerDataListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        PlayerDataManager.loadPlayerAsync(event.player.uniqueId)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        PlayerDataManager.savePlayerAsync(event.player.uniqueId, removeFromCache = true)
    }
}