package org.ReDiego0.defenseGamemode.player

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PartyManager {

    private val parties = ConcurrentHashMap<UUID, Party>()
    private val pendingInvites = ConcurrentHashMap<UUID, UUID>()

    fun getParty(playerUuid: UUID): Party {
        return parties.values.firstOrNull { it.members.contains(playerUuid) }
            ?: Party(playerUuid).also { parties[playerUuid] = it }
    }

    fun invitePlayer(sender: Player, target: Player) {
        val senderParty = getParty(sender.uniqueId)

        if (!senderParty.isLeader(sender.uniqueId)) {
            sender.sendMessage("§cSolo el líder de la party puede invitar.")
            return
        }
        if (senderParty.members.size >= 4) {
            sender.sendMessage("§cTu party ya está llena.")
            return
        }
        if (getParty(target.uniqueId).members.size > 1) {
            sender.sendMessage("§cEse jugador ya pertenece a otra party.")
            return
        }

        pendingInvites[target.uniqueId] = sender.uniqueId

        val mm = MiniMessage.miniMessage()
        val inviteText = "<yellow>Has sido invitado a la party de <aqua>${sender.name}</aqua>. <green><click:run_command:'/defense party accept'><b>[¡HAZ CLIC PARA UNIRTE!]</b></click></green>"

        target.sendMessage(mm.deserialize(inviteText))
        sender.sendMessage("§aInvitación enviada a §b${target.name}§a.")
    }

    fun acceptInvite(player: Player) {
        val leaderUuid = pendingInvites[player.uniqueId]
        if (leaderUuid == null) {
            player.sendMessage("§cNo tienes invitaciones pendientes.")
            return
        }

        val leader = Bukkit.getPlayer(leaderUuid)
        if (leader == null) {
            player.sendMessage("§cEl jugador que te invitó ya no está en línea.")
            pendingInvites.remove(player.uniqueId)
            return
        }

        val targetParty = getParty(leaderUuid)
        if (targetParty.members.size >= 4) {
            player.sendMessage("§cLa party a la que intentas unirte ya está llena.")
            pendingInvites.remove(player.uniqueId)
            return
        }

        val currentParty = getParty(player.uniqueId)
        if (currentParty.members.size > 1) {
            player.sendMessage("§cPrimero debes salir de tu party actual.")
            return
        }

        parties.remove(player.uniqueId)
        targetParty.addMember(player.uniqueId)
        pendingInvites.remove(player.uniqueId)

        targetParty.members.mapNotNull { Bukkit.getPlayer(it) }.forEach { member ->
            member.sendMessage("§b${player.name} §ase ha unido a la party.")
        }
    }

    fun leaveParty(player: Player) {
        val party = getParty(player.uniqueId)

        if (party.members.size == 1) {
            player.sendMessage("§cNo estás en ninguna party.")
            return
        }

        if (party.isLeader(player.uniqueId)) {
            party.members.mapNotNull { Bukkit.getPlayer(it) }.forEach { member ->
                if (member.uniqueId != player.uniqueId) {
                    member.sendMessage("§cEl líder ha disuelto la party.")
                    parties[member.uniqueId] = Party(member.uniqueId)
                }
            }
            parties.remove(party.leader)
            player.sendMessage("§aHas disuelto la party correctamente.")
        } else {
            party.removeMember(player.uniqueId)
            parties[player.uniqueId] = Party(player.uniqueId)

            party.members.mapNotNull { Bukkit.getPlayer(it) }.forEach { member ->
                member.sendMessage("§b${player.name} §cha abandonado la party.")
            }
            player.sendMessage("§aHas salido de la party.")
        }
    }

    fun kickPlayer(leader: Player, targetName: String) {
        val party = getParty(leader.uniqueId)

        if (!party.isLeader(leader.uniqueId)) {
            leader.sendMessage("§cSolo el líder puede expulsar miembros.")
            return
        }

        val targetUuid = party.members.firstOrNull { Bukkit.getOfflinePlayer(it).name.equals(targetName, true) }
        if (targetUuid == null || targetUuid == leader.uniqueId) {
            leader.sendMessage("§cEse jugador no está en tu party.")
            return
        }

        party.removeMember(targetUuid)
        parties[targetUuid] = Party(targetUuid)

        val targetPlayer = Bukkit.getPlayer(targetUuid)
        targetPlayer?.sendMessage("§cHas sido expulsado de la party.")

        party.members.mapNotNull { Bukkit.getPlayer(it) }.forEach { member ->
            member.sendMessage("§b${targetPlayer?.name ?: targetName} §cha sido expulsado.")
        }
    }
}