package com.bun133.king

import com.bun133.king.flylib.*
import com.destroystokyo.paper.Title
import com.flylib.util.NaturalNumber
import org.bukkit.Bukkit
import org.bukkit.ChatColor
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
        val kingPlayers = mutableListOf<Player>()
    }

    lateinit var configManager: KingConfig

    override fun onEnable() {
        FlyLib(this)
        // Plugin startup logic
        server.pluginManager.registerEvents(Observer.instance, this)
        saveDefaultConfig()
        configManager = KingConfig(this)
        king = KingCommand(this)
        getCommand("king")!!.setExecutor(king)
        getCommand("king")!!.tabCompleter = KingTab.gen()
        server.scheduler.runTaskTimer(this, Runnable {
            king!!.checkGoOn()
        }, 10, 1)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}

class KingTab {
    companion object {
        fun gen(): SmartTabCompleter {
            return SmartTabCompleter(
                TabChain(
                    TabObject(
                        "s", "start"
                    )
                ),
                TabChain(
                    TabObject(
                        "e", "end"
                    )
                ),
                TabChain(
                    TabObject(
                        "c", "choice"
                    ),
                    TabObject(
                        TabPart.selectors,
                        TabPart.playerSelector
                    )
                ),
                TabChain(
                    TabObject("set"),
                    TabObject(TabPart.selectors, TabPart.playerSelector)
                )
            )
        }
    }
}

class KingConfig(plugin: King) {

//    private fun getPlayer(s: String): Player? {
//        println("Players:${Bukkit.getOnlinePlayers().toTypedArray().contentToString()}")
//        return Bukkit.getOnlinePlayers().filter { it.displayName === s }.getOrNull(0)
//    }
//
//    init {
//        val s = plugin.config.getStringList("DefaultKing")
//        s.forEach { println("DefaultKingString:${it}") }
//        val p = s.map { getPlayer(it) }
//        println("Matched:${p.toTypedArray().contentToString()}")
//        p.filterNotNull()
//            .forEach { King.kingPlayers.add(it); println("DefaultKing:${it.displayName}")}
//    }

    val digTime = plugin.config.getInt("Orders.Dig.Time")
    val moveTime = plugin.config.getInt("Orders.Move.Time")
    val noDeathTime = plugin.config.getInt("Orders.NoDeath.Time")
    val comeTime = plugin.config.getInt("Orders.Come.Time")
    val placeChooseTime = plugin.config.getInt("Orders.PlaceChooseYou.Time")
}

class KingCommand(val plugin: King) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            if (sender.isOp) {
                return run(sender, command, label, args)
            }
        }
        return false
    }

    fun run(sender: Player, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.size == 1) {
            when (args[0]) {
                "s", "start" -> {
                    King.isGoingOn = true
                    Bukkit.getOnlinePlayers().forEach { it.sendTitle(Title("ゲーム開始", "言いなりにならねば殺される")) }
                }
                "e", "end" -> {
                    King.isGoingOn = false
                    goOn.forEach { it.setTimer(OrderBase.forceEnd) }
                    Bukkit.getOnlinePlayers().forEach { it.sendTitle(Title("ゲーム終了")) }
                }
                "c", "choice" -> {
                    if (King.kingPlayers.contains(sender)) {
                        ChoiceInventory(sender, plugin).open()
                    } else {
                        sender.sendMessage("You are not King!")
                    }
                }
                "set" -> {
                    if (sender.isOp) {
                        if (King.kingPlayers.contains(sender)) {
                            sender.sendMessage("You are no longer King!")
                            King.kingPlayers.remove(sender)
                            return true
                        } else {
                            sender.sendMessage("You became a King!")
                            Bukkit.getOnlinePlayers().forEach {
                                it.sendTitle(
                                    Title(
                                        "新しい王様だ!",
                                        "" + ChatColor.GOLD + sender.displayName + ChatColor.RESET + "が新しい王様だ"
                                    )
                                )
                            }
                            King.kingPlayers.add(sender)
                            return true
                        }
                    } else {
                        sender.sendMessage("You don't have enough perm!")
                        return true
                    }
                }
                else -> {
                    return false
                }
            }
            return true
        } else if (args.size == 2) {
            when (args[0]) {
                "c", "choice" -> {
                    val p = Bukkit.selectEntities(sender, args[1])
                    return if (p.isEmpty() || p[0] !is Player) {
                        sender.sendMessage("Player NotFound!")
                        false
                    } else {
                        if (King.kingPlayers.contains(p[0])) {
                            ChoiceInventory(p[0] as Player, plugin).open()
                            return true
                        } else {
                            sender.sendMessage("${(p[0] as Player).displayName} isn't King!")
                            return false
                        }
                    }
                }
                "set" -> {
                    val p = Bukkit.selectEntities(sender, args[1])
                    if (p.isEmpty() || p[0] !is Player) {
                        sender.sendMessage("Player NotFound!")
                        return false
                    } else {
                        val player: Player = p[0] as Player
                        if (King.kingPlayers.contains(player)) {
                            sender.sendMessage("${player.displayName} are no longer King!")
                            player.sendMessage("You are no longer King!")
                            King.kingPlayers.remove(player)
                            return true
                        } else {
                            sender.sendMessage("${player.displayName} became a King!")
                            player.sendMessage("You became a King!")
                            Bukkit.getOnlinePlayers()
                                .forEach {
                                    it.sendTitle(
                                        Title(
                                            "新しい王様だ!",
                                            "" + ChatColor.GOLD + player.displayName + ChatColor.RESET + "が新しい王様だ"
                                        )
                                    )
                                }
                            King.kingPlayers.add(player)
                            return true
                        }
                    }
                }
            }
        }

        return false
    }

    private val goOn = mutableListOf<Order<*>>()

    fun addGoOn(o: Order<*>) {
        o.setTimer(o.getDefaultTime())
        o.onStart()
        goOn.add(o)
    }

    fun removeGoOn(o: Order<*>) {
        goOn.remove(o)
    }

    fun checkGoOn() {
        val list = mutableListOf<Order<*>>()
        goOn.forEach {
            val b = it.onTick()
            if (b) list.add(it)
        }

        //強引
        list.forEach { removeGoOn(it) }

        if (goOn.size >= 1) {
            Bukkit.getOnlinePlayers().forEach {
                it.sendActionBar("${goOn[0].getDisplayName()} 残り時間:${goOn[0].getTimer() / 20}秒")
            }
        }
    }
}

class ChoiceInventory(p: Player, val plugin: King) {
    init {
        // 自動的に開始されるように
        if (!King.isGoingOn) {
            King.isGoingOn = true
            Bukkit.broadcastMessage("Auto Enabled!")
        }
    }


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

        gui.addGUIObject(
            GUIObject(
                NaturalNumber(5), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "乗れ!")
            ).addCallBack(::choosePlace)
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
            King.king!!.addGoOn(AbstractOrders.Dig(it.type, it.amount, plugin.configManager.digTime))
        }
    }

    fun move(e: InventoryClickEvent) {
        King.king!!.addGoOn(AbstractOrders.Move(plugin.configManager.moveTime))
        (e.whoClicked as Player).closeInventory()
    }

    fun noDeath(e: InventoryClickEvent) {
        King.king!!.addGoOn(AbstractOrders.NotDeath(plugin.configManager.noDeathTime))
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

    fun addCome(e: InventoryClickEvent) {
        if (e.currentItem == null) return
        when (e.currentItem!!.type) {
            Material.GRASS_BLOCK -> King.king!!.addGoOn(
                AbstractOrders.BeDim(
                    World.Environment.NORMAL,
                    plugin.configManager.comeTime
                )
            )
            Material.NETHERRACK -> King.king!!.addGoOn(
                AbstractOrders.BeDim(
                    World.Environment.NETHER,
                    plugin.configManager.comeTime
                )
            )
            Material.END_STONE -> King.king!!.addGoOn(
                AbstractOrders.BeDim(
                    World.Environment.THE_END,
                    plugin.configManager.comeTime
                )
            )
            else -> return
        }
        (e.whoClicked as Player).closeInventory()
    }

    fun choosePlace(e: InventoryClickEvent) {
        val place_gui = DropChestGUI("乗らせるブロック選択", e.whoClicked as Player)
        (e.whoClicked as Player).closeInventory()
        place_gui.register(::placeChooseYou).open()
    }

    fun placeChooseYou(stack: MutableList<ItemStack>) {
        stack.filter { it.type.isBlock }
            .forEach {
                King.king!!.addGoOn(AbstractOrders.PlaceChooseYou(it.type, plugin.configManager.placeChooseTime))
            }
    }
}