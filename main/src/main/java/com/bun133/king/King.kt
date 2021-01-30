package com.bun133.king

import com.bun133.king.flylib.*
import com.flylib.util.NaturalNumber
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
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
        var king: KingCommand? = null
        const val time = 20 * 20
    }


    override fun onEnable() {
        FlyLib(this)
        // Plugin startup logic
        server.pluginManager.registerEvents(Observer.instance, this)
        king = KingCommand()
        getCommand("king")!!.setExecutor(king)
        server.scheduler.runTaskTimer(this, Runnable {
            king!!.checkGoOn()
        }, 10, 1)
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

    private val goOn = mutableListOf<Order<*>>()

    fun addGoOn(o: Order<*>, time: Int) {
        o.setTimer(time)
        o.onStart()
        goOn.add(o)
    }

    fun removeGoOn(o: Order<*>) {
        goOn.remove(o)
    }

    fun checkGoOn() {
        val list = mutableListOf<Order<*>>()
        goOn.forEach {
            if (it.onTick()) list.add(it)
        }

        list.forEach { removeGoOn(it) }
        //強引

        if (goOn.size >= 1) {
            Bukkit.getOnlinePlayers().forEach {
                it.sendActionBar("${goOn[0].getDisplayName()} LeftTime:${goOn[0].getTimer()}")
            }
        }
    }
}

class ChoiceInventory(p: Player) {
    val gui = ChestGUI(p, NaturalNumber(4), "命令一覧")

    init {
        gui.addGUIObject(
            GUIObject(
                NaturalNumber(1), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "掘れ!")
            ).addCallBack(::dig)
        )

        gui.addGUIObject(
            GUIObject(
                NaturalNumber(2), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "動くな!")
            ).addCallBack(::move)
        )

        gui.addGUIObject(
            GUIObject(
                NaturalNumber(3), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "死ぬな!")
            ).addCallBack(::noDeath)
        )

        gui.addGUIObject(
            GUIObject(
                NaturalNumber(4), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "来い!")
            ).addCallBack(::come)
        )
    }

    fun open() {
        gui.open()
    }

    fun dig(e: InventoryClickEvent) {
        val dig_gui = DropChestGUI("掘らせるブロック選択", e.whoClicked as Player)
        (e.whoClicked as Player).closeInventory()
        dig_gui.register(::chooseDig).open()
    }

    fun chooseDig(stack: MutableList<ItemStack>) {
        stack.filter { it.type.isBlock }.forEach {
            King.king!!.addGoOn(AbstractOrders.Dig(it.type, it.amount), King.time)
        }

        King.isGoingOn = true
    }

    fun move(e: InventoryClickEvent) {
        King.king!!.addGoOn(AbstractOrders.Move(), King.time)
        (e.whoClicked as Player).closeInventory()
    }

    fun noDeath(e: InventoryClickEvent) {
        King.king!!.addGoOn(AbstractOrders.NotDeath(), King.time)
        (e.whoClicked as Player).closeInventory()
    }

    fun come(e: InventoryClickEvent) {
        val dim_gui = ChestGUI(e.whoClicked as Player, NaturalNumber(3), "来させる場所選択")
        (e.whoClicked as Player).closeInventory()
        dim_gui.addGUIObject(
            GUIObject(
                NaturalNumber(1), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.GRASS_BLOCK, "オーバーワールド")
            ).addCallBack(::addCome)
        )

        dim_gui.addGUIObject(
            GUIObject(
                NaturalNumber(2), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.NETHERRACK, "ネザー")
            ).addCallBack(::addCome)

        )

        dim_gui.addGUIObject(
            GUIObject(
                NaturalNumber(3), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.END_STONE, "エンド")
            ).addCallBack(::addCome)

        )

        dim_gui.open()
    }

    fun addCome(e:InventoryClickEvent){
        if(e.currentItem == null) return
        when(e.currentItem!!.type){
            Material.GRASS_BLOCK -> King.king!!.addGoOn(AbstractOrders.BeDim(World.Environment.NORMAL), King.time * 2)
            Material.NETHERRACK -> King.king!!.addGoOn(AbstractOrders.BeDim(World.Environment.NETHER), King.time * 2)
            Material.END_STONE -> King.king!!.addGoOn(AbstractOrders.BeDim(World.Environment.THE_END), King.time * 2)
            else -> return
        }
        (e.whoClicked as Player).closeInventory()
    }
}