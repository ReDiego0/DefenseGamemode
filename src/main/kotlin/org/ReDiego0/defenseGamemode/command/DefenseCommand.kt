package org.ReDiego0.defenseGamemode.command

import org.ReDiego0.defenseGamemode.game.MatchmakingManager
import org.ReDiego0.defenseGamemode.player.PartyManager
import org.ReDiego0.defenseGamemode.player.PlayerDataManager
import org.ReDiego0.defenseGamemode.setup.SetupManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class DefenseCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        val subCommand = args[0].lowercase()

        when (subCommand) {
            "join" -> handleJoin(sender, args)
            "party" -> handleParty(sender, args)
            "setup" -> handleSetup(sender, args)
            "class" -> handleClassMenu(sender)
            "loadout" -> handleLoadoutMenu(sender)
            "exotics" -> handleExoticsMenu(sender)
            "level" -> handleLevel(sender, args)
            else -> sendHelp(sender)
        }
        return true
    }

    private fun handleExoticsMenu(sender: CommandSender) {
        if (sender !is Player) return
        org.ReDiego0.defenseGamemode.ui.ExoticMenu.openMainMenu(sender)
    }

    private fun handleClassMenu(sender: CommandSender) {
        if (sender !is Player) return
        org.ReDiego0.defenseGamemode.ui.ClassMenu.openMainMenu(sender)
    }

    private fun handleLoadoutMenu(sender: CommandSender) {
        if (sender !is Player) return
        org.ReDiego0.defenseGamemode.ui.LoadoutMenu.openLoadout(sender)
    }

    private fun handleSetup(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("§cSolo los jugadores pueden usar el modo setup.")
            return
        }

        if (!sender.hasPermission("defense.setup")) {
            sender.sendMessage("§cNo tienes permisos para usar esto.")
            return
        }

        if (args.size < 3 || args[1].lowercase() != "start") {
            sender.sendMessage("§cUso: /defense setup start <mapa>")
            return
        }

        SetupManager.startSetup(sender, args[2])
    }

    private fun handleJoin(sender: CommandSender, args: Array<out String>) {
        if (args.size == 2) {
            if (sender !is Player) {
                sender.sendMessage("§cLa consola debe especificar un jugador: /defense join <jugador> <mapa>")
                return
            }
            MatchmakingManager.joinMap(sender, args[1])
            return
        }

        if (args.size == 3) {
            val target = Bukkit.getPlayer(args[1])
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado.")
                return
            }
            MatchmakingManager.joinMap(target, args[2])
            return
        }
        sender.sendMessage("§cUso: /defense join [jugador] <mapa>")
    }

    private fun handleParty(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage("§cLos comandos de party son solo para jugadores.")
            return
        }

        if (args.size < 2) {
            sender.sendMessage("§cUso: /defense party <invite|accept|leave|kick>")
            return
        }

        val action = args[1].lowercase()

        when (action) {
            "invite" -> {
                if (args.size < 3) {
                    sender.sendMessage("§cUso: /defense party invite <jugador>")
                    return
                }
                val target = Bukkit.getPlayer(args[2])
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado.")
                    return
                }
                PartyManager.invitePlayer(sender, target)
            }
            "accept" -> PartyManager.acceptInvite(sender)
            "leave" -> PartyManager.leaveParty(sender)
            "kick" -> {
                if (args.size < 3) {
                    sender.sendMessage("§cUso: /defense party kick <jugador>")
                    return
                }
                PartyManager.kickPlayer(sender, args[2])
            }
            else -> sender.sendMessage("§cUso: /defense party <invite|accept|leave|kick>")
        }
    }

    private fun handleLevel(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("defense.admin")) {
            sender.sendMessage("§cNo tienes permisos para usar esto.")
            return
        }

        if (args.size < 4) {
            sender.sendMessage("§cUso: /defense level <add|set> <jugador> <cantidad>")
            return
        }

        val action = args[1].lowercase()
        val target = Bukkit.getPlayer(args[2])
        val amount = args[3].toIntOrNull()

        if (target == null) {
            sender.sendMessage("§cJugador no encontrado.")
            return
        }

        if (amount == null || amount < 0) {
            sender.sendMessage("§cLa cantidad debe ser un número entero positivo.")
            return
        }

        val data = PlayerDataManager.getPlayerData(target.uniqueId)

        if (data == null) {
            sender.sendMessage("§cNo se encontraron datos para el jugador.")
            return
        }

        when (action) {
            "add" -> {
                data.level += amount
                data.checkClassUnlocks()
                sender.sendMessage("§aSe han añadido $amount niveles a ${target.name}. Nivel actual: ${data.level}")
            }
            "set" -> {
                data.level = amount
                data.checkClassUnlocks()
                sender.sendMessage("§aEl nivel de ${target.name} se ha establecido en $amount.")
            }
            else -> sender.sendMessage("§cUso: /defense level <add|set> <jugador> <cantidad>")
        }
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§e--- Comandos de Defense ---")
        sender.sendMessage("§a/defense join [jugador] <mapa>")
        sender.sendMessage("§a/defense party invite <jugador>")
        sender.sendMessage("§a/defense party accept")
        sender.sendMessage("§a/defense party leave")
        sender.sendMessage("§a/defense party kick <jugador>")
        sender.sendMessage("§a/defense class")
        sender.sendMessage("§a/defense loadout")
        sender.sendMessage("§a/defense exotics")
        if (sender.hasPermission("defense.admin")) {
            sender.sendMessage("§c/defense level <add|set> <jugador> <cantidad>")
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            val options = mutableListOf("join", "party", "class", "loadout", "exotics")
            if (sender.hasPermission("defense.setup")) options.add("setup")
            if (sender.hasPermission("defense.admin")) options.add("level")
            options.filter { it.startsWith(args[0].lowercase()) }.forEach { completions.add(it) }
        } else if (args.size >= 2) {
            val subCommand = args[0].lowercase()

            when (subCommand) {
                "join" -> {
                    if (args.size == 2) {
                        Bukkit.getOnlinePlayers().forEach { player ->
                            if (player.name.lowercase().startsWith(args[1].lowercase())) completions.add(player.name)
                        }
                        completions.add("<mapa>")
                    } else if (args.size == 3) {
                        completions.add("<mapa>")
                    }
                }
                "party" -> {
                    if (args.size == 2) {
                        listOf("invite", "accept", "leave", "kick").filter { it.startsWith(args[1].lowercase()) }.forEach { completions.add(it) }
                    } else if (args.size == 3 && (args[1].lowercase() == "invite" || args[1].lowercase() == "kick")) {
                        Bukkit.getOnlinePlayers().forEach { player ->
                            if (player.name.lowercase().startsWith(args[2].lowercase())) completions.add(player.name)
                        }
                    }
                }
                "setup" -> {
                    if (sender.hasPermission("defense.setup")) {
                        if (args.size == 2) {
                            if ("start".startsWith(args[1].lowercase())) completions.add("start")
                        } else if (args.size == 3 && args[1].lowercase() == "start") {
                            completions.add("<mapa>")
                        }
                    }
                }
                "level" -> {
                    if (sender.hasPermission("defense.admin")) {
                        if (args.size == 2) {
                            listOf("add", "set").filter { it.startsWith(args[1].lowercase()) }.forEach { completions.add(it) }
                        } else if (args.size == 3) {
                            Bukkit.getOnlinePlayers().forEach { player ->
                                if (player.name.lowercase().startsWith(args[2].lowercase())) completions.add(player.name)
                            }
                        } else if (args.size == 4) {
                            completions.add("<cantidad>")
                        }
                    }
                }
            }
        }
        return completions
    }
}