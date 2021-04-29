package com.bun133.king

import com.bun133.king.flylib.*
import com.bun133.king.flylib.ChestGUICollections
import com.destroystokyo.paper.Title
import com.flylib.util.NaturalNumber
import com.github.bun133.flyframe.*
import com.github.bun133.langmodule.LangModule
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.inventory.meta.SpawnEggMeta
import org.jetbrains.annotations.Nullable

class King : FlyModulePlugin() {
    companion object {
        var isGoingOn = false
        var king: KingCommand? = null
        val kingPlayers = mutableListOf<Player>()
        var plugin: King? = null
    }

    val worker = KingWorkerModule(this)
    lateinit var configManager: KingConfig
    var langModule: LangModule? = null
    lateinit var skipper: TimeSkipper

    override fun onEnable() {
        FlyLib(this)
        // Plugin startup logic
        plugin = this
        server.pluginManager.registerEvents(Observer.instance, this)
        saveDefaultConfig()
        configManager = KingConfig(this)
        king = KingCommand(this)
        skipper = TimeSkipper(this)
//        king = KingCommand(this)
//        getCommand("king")!!.setExecutor(king)
//        getCommand("king")!!.tabCompleter = KingTab.gen()
        server.scheduler.runTaskTimer(this, Runnable {
            king!!.checkGoOn()
        }, 10, 1)
    }

    override fun getCommands(): MutableList<FlyCommandProxy> {
        return mutableListOf(FlyCommandProxy(this, "king", king!!, KingTab.gen()))
    }

    override fun getModule(): Module = worker

    override fun onDisable() {
        // Plugin shutdown logic
    }
}


class KingWorkerModule(val plugin: King) : Module {
    override var authorName: String = "Bun133"
    override var moduleName: String = "KingWorkerModule"
    override var version: String = "1.0"

    override fun onEvent(e: ModuleEvent) {
        when (e) {
            ModuleEvent.LOADED_ALL_MODULE -> {
                println("[KingWorkerModule] ALL Module Loaded!")
                println("[KingWorkerModule] Loading Lang Module...")
                val flyframe = plugin.server.pluginManager.getPlugin("Flyframe") as FlyFrame
                val lang = flyframe.requireModule("LangModule")
                if (lang != null) {
                    plugin.langModule = lang as LangModule
                } else {
                    println("[KingWorkerModule]Something went wrong while require!")
                }
            }
        }
    }

    override fun onModuleDisable() {
    }

    override fun onModuleEnable() {
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
                        TabObject("@r"),
                        TabPart.playerSelector
                    )
                ),
//                TabChain(
//                    TabObject("set"),
//                    TabObject(TabObject("@r"), TabPart.playerSelector)
//                )
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
    val killTime = plugin.config.getInt("Orders.Kill.Time")
    val gotTime = plugin.config.getInt("Orders.Got.Time")
    val knockTime = plugin.config.getInt("Orders.Knock.Time")
}

class KingCommand(val plugin: King) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            if (sender.isOp) {
                return run(sender, command, label, args)
            }
        }
        return serverRun(sender, command, label, args)
    }

    private fun serverRun(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (args.size) {
//            2 -> {
//                when (args[0]) {
//                    "set" -> {
//                        val ps = Bukkit.selectEntities(sender, args[1])
//                        if (ps.isNotEmpty()) {
//                            val p = ps[0]
//                            if (p !is Player) return false
//                            if (King.kingPlayers.contains(p)) {
//                                p.sendMessage("You are no longer King!")
//                                King.kingPlayers.remove(p)
//                                return true
//                            } else {
//                                p.sendMessage("You became a King!")
//                                Bukkit.getOnlinePlayers().forEach {
//                                    it.sendTitle(
//                                        Title(
//                                            "新しい王様だ!",
//                                            "" + ChatColor.GOLD + p.displayName + ChatColor.RESET + "が新しい王様だ"
//                                        )
//                                    )
//                                }
//                                King.kingPlayers.add(p)
//                                return true
//                            }
//                        }
//                    }
//                }
//            }
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
                        sender.inventory.addItem(EasyItemBuilder.genItem(Material.CLOCK))
                    } else {
                        if (sender.isOp) {

                            Bukkit.getOnlinePlayers().forEach {
                                it.sendTitle(
                                    Title(
                                        "新しい王様だ!",
                                        "" + ChatColor.GOLD + sender.displayName + ChatColor.RESET + "が新しい王様だ"
                                    )
                                )
                            }

                            King.kingPlayers.add(sender)

                            ChoiceInventory(sender, plugin).open()
                            sender.inventory.addItem(EasyItemBuilder.genItem(Material.CLOCK))
                            return true
                        }
                        sender.sendMessage("You are not King!")
                    }
                }
                "debug" -> {
                    println("kingPlayers:")
                    King.kingPlayers.forEach { println(it.displayName) }
                    println("Configs:")
                    println("comeTime:${King.plugin!!.configManager.comeTime}")
                    println("digTime:${King.plugin!!.configManager.digTime}")
                    println("killTime:${King.plugin!!.configManager.killTime}")
                    println("knockTime:${King.plugin!!.configManager.knockTime}")
                    println("noDeathTime:${King.plugin!!.configManager.noDeathTime}")
                    println("placeChooseTime:${King.plugin!!.configManager.placeChooseTime}")
                    println("moveTime:${King.plugin!!.configManager.moveTime}")
                }
//                "set" -> {
//                    if (sender.isOp) {
//                        if (King.kingPlayers.contains(sender)) {
//                            sender.sendMessage("You are no longer King!")
//                            King.kingPlayers.remove(sender)
//                            return true
//                        } else {
//                            sender.sendMessage("You became a King!")
//                            Bukkit.getOnlinePlayers().forEach {
//                                it.sendTitle(
//                                    Title(
//                                        "新しい王様だ!",
//                                        "" + ChatColor.GOLD + sender.displayName + ChatColor.RESET + "が新しい王様だ"
//                                    )
//                                )
//                            }
//                            King.kingPlayers.add(sender)
//                            return true
//                        }
//                    } else {
//                        sender.sendMessage("You don't have enough perm!")
//                        return true
//                    }
//                }
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
                            (p[0] as Player).inventory.addItem(EasyItemBuilder.genItem(Material.CLOCK))
                            return true
                        } else {
                            if(sender.isOp){
                                Bukkit.getOnlinePlayers().forEach {
                                    it.sendTitle(
                                        Title(
                                            "新しい王様だ!",
                                            "" + ChatColor.GOLD + (p[0] as Player).displayName + ChatColor.RESET + "が新しい王様だ"
                                        )
                                    )
                                }
                                King.kingPlayers.add(p[0] as Player)
                                ChoiceInventory(p[0] as Player, plugin).open()
                                (p[0] as Player).inventory.addItem(EasyItemBuilder.genItem(Material.CLOCK))
                                return true
                            }
                            sender.sendMessage("${(p[0] as Player).displayName} isn't King!")
                            return false
                        }
                    }
                }
//                "set" -> {
//                    val p = Bukkit.selectEntities(sender, args[1])
//                    if (p.isEmpty() || p[0] !is Player) {
//                        sender.sendMessage("Player NotFound!")
//                        return false
//                    } else {
//                        val player: Player = p[0] as Player
//                        if (King.kingPlayers.contains(player)) {
//                            sender.sendMessage("${player.displayName} are no longer King!")
//                            player.sendMessage("You are no longer King!")
//                            King.kingPlayers.remove(player)
//                            return true
//                        } else {
//                            sender.sendMessage("${player.displayName} became a King!")
//                            player.sendMessage("You became a King!")
//                            Bukkit.getOnlinePlayers()
//                                .forEach {
//                                    it.sendTitle(
//                                        Title(
//                                            "新しい王様だ!",
//                                            "" + ChatColor.GOLD + player.displayName + ChatColor.RESET + "が新しい王様だ"
//                                        )
//                                    )
//                                }
//                            King.kingPlayers.add(player)
//                            return true
//                        }
//                    }
//                }
            }
        }

        return false
    }

    val goOn = mutableListOf<Order<*>>()

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

class ChoiceInventory(p: Player, val plugin: King) : Listener{
    init {
        // 自動的に開始されるように
        if (!King.isGoingOn) {
            King.isGoingOn = true
            Bukkit.broadcastMessage("Auto Enabled!")
        }

        plugin.server.pluginManager.registerEvents(this,plugin)
    }

    @EventHandler
    fun onClose(e:InventoryCloseEvent){
        if(e.inventory == gui.inventory){
            if(isChosen){
                isChosen = false
                return
            }
            println("Removed!")
            King.kingPlayers.remove(e.player)
        }
    }

    // 選んだ時にTrueになる
    var isChosen = false
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

        // ただ負荷がかかるだけのクソモードなので削除
        /*
        gui.addGUIObject(
            GUIObject(
                NaturalNumber(3), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "死ぬな!")
            ).addCallBack(::noDeath)
        )
        */

        gui.addGUIObject(
            GUIObject(
                NaturalNumber(3), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "倒せ!")
            ).addCallBack(::kill)
        )


        gui.addGUIObject(
            GUIObject(
                NaturalNumber(4), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "行け!")
            ).addCallBack(::come)
        )

        gui.addGUIObject(
            GUIObject(
                NaturalNumber(5), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "乗れ!")
            ).addCallBack(::choosePlace)
        )

        gui.addGUIObject(
            GUIObject(
                NaturalNumber(6), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "ゲットしろ!")
            ).addCallBack(::got)
        )

        gui.addGUIObject(
            GUIObject(
                NaturalNumber(7), NaturalNumber(1),
                EasyItemBuilder.genItem(Material.CHEST, "殴れ!")
            ).addCallBack(::knock)
        )
    }

    fun open() {
        gui.open()
    }

    fun dig(e: InventoryClickEvent) {
//        val dig_gui = DropChestGUI("掘らせるブロック選択", e.whoClicked as Player)
        val dig_gui = ChestGUICollections.genMaterial(
            (e.whoClicked as Player),
            { EasyItemBuilder.genItem(it).type.isOccluding },
            "掘らせるブロック選択"
        )
        isChosen = true
        (e.whoClicked as Player).closeInventory()
        dig_gui.callbacks.add { page, stack -> (e.whoClicked as Player).closeInventory();chooseDig(stack) }
        dig_gui.open()
    }

    fun chooseDig(s: ItemStack) {
        King.king!!.addGoOn(AbstractOrders.Dig(s.type, s.amount, plugin.configManager.digTime))
    }

    fun move(e: InventoryClickEvent) {
        King.king!!.addGoOn(AbstractOrders.Move(plugin.configManager.moveTime))
        isChosen = true
        (e.whoClicked as Player).closeInventory()
    }

    fun noDeath(e: InventoryClickEvent) {
        King.king!!.addGoOn(AbstractOrders.NotDeath(plugin.configManager.noDeathTime))
        isChosen = true
        (e.whoClicked as Player).closeInventory()
    }

    fun come(e: InventoryClickEvent) {
        val dim_gui = ChestGUI(e.whoClicked as Player, NaturalNumber(3), "行かせる場所選択")
        isChosen = true
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
//        val place_gui = DropChestGUI("乗らせるブロック選択", e.whoClicked as Player)
//        (e.whoClicked as Player).closeInventory()
//        place_gui.register(::placeChooseYou).open()

        val place_gui = ChestGUICollections.genMaterial(
            e.whoClicked as Player,
            { EasyItemBuilder.genItem(it).type.isOccluding },
            "乗らせるブロック選択"
        )
        isChosen = true
        (e.whoClicked as Player).closeInventory()
        place_gui.callbacks.add { page, stack -> (e.whoClicked as Player).closeInventory();placeChooseYou(stack) }
        place_gui.open()
    }

    fun placeChooseYou(stack: ItemStack) {
        King.king!!.addGoOn(AbstractOrders.PlaceChooseYou(stack.type, plugin.configManager.placeChooseTime))
    }

    fun kill(e: InventoryClickEvent) {
//        val egg_gui = DropChestGUI("殺させるMOB選択(卵をいれる)", e.whoClicked as Player)
        isChosen = true
        (e.whoClicked as Player).closeInventory()
        val egg_gui = ChestGUICollections.genMaterial(
            (e.whoClicked as Player),
            { isEgg(EasyItemBuilder.genItem(it)) },
            "殺させるMOB選択"
        )
        egg_gui.callbacks.add { page, stack -> (e.whoClicked as Player).closeInventory();addKill(stack) }
        egg_gui.open()
    }

    fun addKill(stack: ItemStack) {
        if (isEgg(stack)) {
            if (getEntityType(stack) != null) {
                King.king!!.addGoOn(
                    AbstractOrders.Kill(
                        getEntityType(stack)!!,
                        stack.amount,
                        plugin.configManager.killTime
                    )
                )
            }
        }
//        stack
//            .filter { println("isEgg:${isEgg(it)}");isEgg(it) }
//            .map { Pair(it.amount, getEntityType(it)) }
//            .filter { it.second != null }
//            .forEach { King.king!!.addGoOn(AbstractOrders.Kill(it.second!!, it.first, plugin.configManager.killTime)) }
    }

    fun got(e: InventoryClickEvent) {
        isChosen = true
        (e.whoClicked as Player).closeInventory()
        val got_gui = ChestGUICollections.genMaterial((e.whoClicked as Player), { it.isItem }, "とらせるもの選択")
        got_gui.callbacks.add { page, stack -> (e.whoClicked as Player).closeInventory();addGot(stack) }
        got_gui.open()
    }

    fun addGot(stack: ItemStack) {
        King.king!!.addGoOn(AbstractOrders.Got(stack.type, stack.amount, plugin.configManager.gotTime))
    }

    fun knock(e: InventoryClickEvent) {
        isChosen = true
        (e.whoClicked as Player).closeInventory()
        val knock_gui = ChestGUICollections.genPlayerHead((e.whoClicked as Player), { Observer.isJoined(it) }, "殴らせる人")
        knock_gui.addCallBack { page, stack -> (e.whoClicked as Player).closeInventory(); addKnock((stack.itemMeta as SkullMeta).owningPlayer!!.player) }
        knock_gui.open()
    }

    fun addKnock(p: Player?) {
        if (p == null) {
            println("[King][Knock] The Player IS ALREADY OFFLINE!!!!")
            return
        }
        King.king!!.addGoOn(AbstractOrders.Knock(p, plugin.configManager.knockTime))
    }

    fun isEgg(stack: ItemStack): Boolean {
        return stack.itemMeta is SpawnEggMeta
    }

    fun getEntityType(stack: ItemStack): EntityType? {
        val mob_name = stack.type.name.replace("_SPAWN_EGG", "").toLowerCase()
        val entityType = EntityType.fromName(mob_name)
        return entityType
    }
}