package org.ReDiego0.defenseGamemode.command

import org.ReDiego0.defenseGamemode.DefenseGamemode
import org.ReDiego0.defenseGamemode.game.MatchmakingManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class DefenseCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty() || args[0].lowercase() != "join") {
            sender.sendMessage("§cUso: /defense join [jugador] <mapa>")
            return true
        }

        if (args.size == 2) {
            if (sender !is Player) {
                sender.sendMessage("§cLa consola debe especificar un jugador: /defense join <jugador> <mapa>")
                return true
            }
            MatchmakingManager.joinMap(sender, args[1])
            return true
        }

        if (args.size == 3) {
            val target = Bukkit.getPlayer(args[1])
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado.")
                return true
            }
            MatchmakingManager.joinMap(target, args[2])
            return true
        }

        sender.sendMessage("§cUso: /defense join [jugador] <mapa>")
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            if ("join".startsWith(args[0].lowercase())) {
                completions.add("join")
            }
        } else if (args.size == 2 && args[0].lowercase() == "join") {
            Bukkit.getOnlinePlayers().forEach { player ->
                if (player.name.lowercase().startsWith(args[1].lowercase())) {
                    completions.add(player.name)
                }
            }
            completions.add("<mapa>")
        } else if (args.size == 3 && args[0].lowercase() == "join") {
            completions.add("<mapa>")
        }

        return completions
    }
}