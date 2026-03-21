package org.ReDiego0.defenseGamemode.config

import org.bukkit.Location
import org.bukkit.World
import kotlin.random.Random

data class MissionConfig(
    val id: String,
    val templateName: String,
    val maxPlayers: Int,
    val spawnLocation: CustomLocation,
    val spawnRadius: Double,
    val targetLocation: CustomLocation
)

data class CustomLocation(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float = 0f,
    val pitch: Float = 0f
) {
    fun toBukkitLocation(world: World): Location {
        return Location(world, x, y, z, yaw, pitch)
    }

    fun toRandomizedBukkitLocation(world: World, radius: Double): Location {
        if (radius <= 0.0) return toBukkitLocation(world)

        val offsetX = (Random.nextDouble() * 2 - 1) * radius
        val offsetZ = (Random.nextDouble() * 2 - 1) * radius

        return Location(world, x + offsetX, y, z + offsetZ, yaw, pitch)
    }
}