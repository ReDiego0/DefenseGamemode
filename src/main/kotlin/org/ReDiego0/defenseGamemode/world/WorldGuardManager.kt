package org.ReDiego0.defenseGamemode.world

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import com.sk89q.worldguard.protection.flags.Flags
import com.sk89q.worldguard.protection.flags.StateFlag
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion
import org.bukkit.World

object WorldGuardManager {
    fun applyInstanceRules(bukkitWorld: World) {
        try {
            val container = WorldGuard.getInstance().platform.regionContainer
            val wgWorld = BukkitAdapter.adapt(bukkitWorld)
            val regions = container.get(wgWorld) ?: return

            var globalRegion = regions.getRegion("__global__")
            if (globalRegion == null) {
                globalRegion = GlobalProtectedRegion("__global__")
                regions.addRegion(globalRegion)
            }

            globalRegion.setFlag(Flags.PVP, StateFlag.State.DENY)
            globalRegion.setFlag(Flags.FALL_DAMAGE, StateFlag.State.DENY)
            globalRegion.setFlag(Flags.BLOCK_PLACE, StateFlag.State.DENY)
            globalRegion.setFlag(Flags.BLOCK_BREAK, StateFlag.State.DENY)
            globalRegion.setFlag(Flags.INTERACT, StateFlag.State.ALLOW)

            globalRegion.setFlag(Flags.CREEPER_EXPLOSION, StateFlag.State.DENY)
            globalRegion.setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY)
            globalRegion.setFlag(Flags.LAVA_FIRE, StateFlag.State.DENY)
            globalRegion.setFlag(Flags.ENDERDRAGON_BLOCK_DAMAGE, StateFlag.State.DENY)

            regions.saveChanges()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}