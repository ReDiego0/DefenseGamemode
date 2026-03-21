package org.ReDiego0.defenseGamemode.setup

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class SetupListener : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val session = SetupManager.getSession(player.uniqueId) ?: return

        event.isCancelled = true

        if (session.chatPhase != ChatPhase.NONE) {
            player.sendMessage("§cPor favor, responde en el chat para continuar con el guardado.")
            return
        }

        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        val meta = item.itemMeta ?: return

        val plainName = PlainTextComponentSerializer.plainText().serialize(meta.displayName() ?: return)
        val isShift = player.isSneaking

        when {
            plainName.contains("Spawn Jugadores") -> handlePlayerSpawn(player, session, isShift)
            plainName.contains("Spawn Objetivo") -> handleTargetSpawn(player, session, isShift)
            plainName.contains("Spawn Mobs") -> handleMobSpawn(player, session, isShift)
            plainName.contains("Borrar Todos") -> handleClearAll(player, session)
            plainName.contains("Guardar y Finalizar") -> handleFinish(player, session)
            plainName.contains("Cancelar Setup") -> handleCancel(player)
        }
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        if (SetupManager.getSession(event.player.uniqueId) != null) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val session = SetupManager.getSession(player.uniqueId) ?: return

        if (session.chatPhase == ChatPhase.NONE) return

        event.isCancelled = true
        val input = PlainTextComponentSerializer.plainText().serialize(event.message())

        if (input.lowercase() == "cancelar") {
            Bukkit.getScheduler().runTask(DefenseGamemode.instance, Runnable {
                SetupManager.endSetup(player, false)
            })
            return
        }

        Bukkit.getScheduler().runTask(DefenseGamemode.instance, Runnable {
            when (session.chatPhase) {
                ChatPhase.AWAITING_NAME -> {
                    session.missionDisplayName = input
                    session.chatPhase = ChatPhase.AWAITING_DIFFICULTY
                    player.sendMessage("§aNombre de misión fijado como: §b$input")
                    player.sendMessage("§eAhora, escribe la §cDificultad Base§e de la misión (Un número del 1 al 6):")
                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                }
                ChatPhase.AWAITING_DIFFICULTY -> {
                    val diff = input.toIntOrNull()
                    if (diff == null || diff !in 1..6) {
                        player.sendMessage("§cPor favor, introduce un número válido entre 1 y 6.")
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                        return@Runnable
                    }
                    session.baseDifficulty = diff
                    SetupManager.endSetup(player, true)
                }
                else -> {}
            }
        })
    }

    private fun handlePlayerSpawn(player: Player, session: SetupSession, isShift: Boolean) {
        if (isShift) {
            session.playerSpawn = null
            player.sendMessage("§cSpawn de jugadores eliminado.")
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
        } else {
            session.playerSpawn = player.location
            player.sendMessage("§aSpawn de jugadores fijado.")
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        }
    }

    private fun handleTargetSpawn(player: Player, session: SetupSession, isShift: Boolean) {
        if (isShift) {
            session.targetSpawn = null
            player.sendMessage("§cSpawn del objetivo eliminado.")
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
        } else {
            session.targetSpawn = player.location
            player.sendMessage("§aSpawn del objetivo fijado.")
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        }
    }

    private fun handleMobSpawn(player: Player, session: SetupSession, isShift: Boolean) {
        if (isShift) {
            session.mobSpawns.clear()
            player.sendMessage("§cTodos los spawns de mobs han sido eliminados.")
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
        } else {
            if (session.mobSpawns.size >= 6) {
                player.sendMessage("§cYa has alcanzado el límite máximo de 6 spawns para mobs.")
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                return
            }
            session.mobSpawns.add(player.location)
            player.sendMessage("§aSpawn de mob añadido (${session.mobSpawns.size}/6).")
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        }
    }

    private fun handleClearAll(player: Player, session: SetupSession) {
        session.clearAll()
        player.sendMessage("§cSe han reiniciado todos los puntos del mapa.")
        player.playSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)
    }

    private fun handleFinish(player: Player, session: SetupSession) {
        if (session.playerSpawn == null) {
            player.sendMessage("§cFalta fijar el spawn de jugadores.")
            return
        }
        if (session.targetSpawn == null) {
            player.sendMessage("§cFalta fijar el spawn del objetivo.")
            return
        }
        if (session.mobSpawns.size < 2) {
            player.sendMessage("§cDebes fijar al menos 2 spawns para los mobs (Actual: ${session.mobSpawns.size}).")
            return
        }

        session.chatPhase = ChatPhase.AWAITING_NAME
        player.sendMessage("§e---------------------------------------")
        player.sendMessage("§a¡Puntos de spawn configurados correctamente!")
        player.sendMessage("§eEscribe en el chat el §bNombre de la Misión§e (Ej: Defensa del Valle):")
        player.sendMessage("§7(O escribe 'cancelar' para abortar)")
        player.sendMessage("§e---------------------------------------")
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f)
    }

    private fun handleCancel(player: Player) {
        SetupManager.endSetup(player, false)
    }
}