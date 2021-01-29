package com.bun133.king

import com.bun133.king.flylib.*
import com.flylib.util.NaturalNumber
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class King : JavaPlugin() {
    companion object {
        var isGoingOn = false
    }

    override fun onEnable() {
        FlyLib(this)
        // Plugin startup logic
        server.pluginManager.registerEvents(Observer.instance, this)
        getCommand("king")!!.setExecutor(KingCommand())
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}

class KingCommand() : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            if (sender.isOp) {
                return run(sender, command, label, args)
            }
        }
        return false
    }

    fun run(sender: Player, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.size != 1) return false
        when (args[0]) {
            "s", "start" -> King.isGoingOn = true
            "e", "end" -> King.isGoingOn = false
            "c", "choice" -> {
                ChoiceInventory(sender).open()
            }
            else -> {
                return false
            }
        }
        return true
    }
}

class ChoiceInventory(p: Player) {
    val gui = ChestGUI(p, NaturalNumber(4), "命令一覧")

    init {
//        gui.addGUIObject(
//            GUIObject(
//                NaturalNumber(1),NaturalNumber(1),
//                ItemStack(Material.CHEST,1)
//            ).addCallBack(::test)
//        )
        gui.addGUIObject(
            GUIObject(
                NaturalNumber(1), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST,"掘れ!")
            ).addCallBack(::dig)
        )
    }

    fun open() {
        gui.open()
    }

    fun dig(e: InventoryClickEvent) {
        val dig_gui = DropChestGUI("掘らせるブロック選択",e.whoClicked as Player)
        dig_gui.register(::chooseDig).open()
    }

    fun chooseDig(stack:MutableList<ItemStack>){
        stack.forEach {
            Bukkit.broadcastMessage("${it.type.key} is Chosen")
        }
    }
}