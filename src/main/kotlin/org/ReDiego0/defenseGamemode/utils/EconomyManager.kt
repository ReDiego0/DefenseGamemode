package org.ReDiego0.defenseGamemode.utils

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object EconomyManager {

    private var economy: Economy? = null

    fun setupEconomy(): Boolean {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false
        val rsp = Bukkit.getServicesManager().getRegistration(Economy::class.java) ?: return false
        economy = rsp.provider
        return economy != null
    }

    fun hasEnough(player: Player, amount: Double): Boolean {
        return economy?.has(player, amount) ?: false
    }

    fun withdraw(player: Player, amount: Double): Boolean {
        return economy?.withdrawPlayer(player, amount)?.transactionSuccess() ?: false
    }
}