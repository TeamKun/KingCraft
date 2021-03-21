package com.bun133.king

import com.bun133.king.flylib.Events
import com.bun133.king.flylib.displayName
import com.destroystokyo.paper.Title
import org.bukkit.*
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.NotNull

interface Order<E> {
    fun getAll(): ActionStore<E>
    fun getDisplayName(): String
    fun onStart()
    fun getNoobs(): MutableList<Player>
    fun getPros(): MutableList<Player>
    fun killAll()
    fun onTick(): Boolean
    fun setTimer(i: Int)
    fun getTimer(): Int
    fun getDefaultTime(): Int
}

/**
 * Pending - 判定中
 * FAILURE - 失敗
 * SUCCESS - 成功
 * FINAL_SUCCESS - 最終Tickにおいて成功(それ以外のTickにおいてはPendingと同じ)
 * FINAL_FAILURE - 最終Tickにおいて失敗(それ以外のTickにおいてはPendingと同じ)
 */
enum class OrderResult {
    PENDING, FAILURE, SUCCESS, UNDEFINED, FINAL_SUCCESS, FINAL_FAILURE
}

abstract class OrderBase<E>(val defTime: Int) : Order<E> {
    init {
        Events.PlayerDeathEvent.register(::onDeath)
    }


    companion object {
        //        const val TimeUP = -1114
        const val forceEnd = -55484
    }

    var time: Int = 0
    var isTimerMoving = false

    override fun getTimer() = time

    private val failureNoticed = mutableListOf<Player>()
    private val successNoticed = mutableListOf<Player>()

    fun updateNotice() {
        if (time <= forceEnd) {
            //ForceEnd
            return
        }

        val failure = getNoobs()
        if (failure.size != failureNoticed.size) {
            failure.removeAll(failureNoticed)
            failure.forEach {
                it.sendTitle(Title("" + ChatColor.RED + "✘" + ChatColor.RESET + "命令に反した"))
                failureNoticed.add(it)
            }
        }

        val success = getPros()
        if (success.size != successNoticed.size) {
            success.removeAll(successNoticed)
            success.forEach {
                it.sendTitle(Title("" + ChatColor.GREEN + "✓" + ChatColor.RESET + "命令に従った"))
                successNoticed.add(it)
            }
        }

        if (time <= 0) {
            val finalSuccess = getFinalPros().filter { !successNoticed.contains(it) && !failureNoticed.contains(it) }
            finalSuccess.forEach {
                it.sendTitle(Title("" + ChatColor.GREEN + "✓" + ChatColor.RESET + "命令を完遂した"))
            }

            val finalNoobs = getFinalNoobs().filter { !successNoticed.contains(it) && !failureNoticed.contains(it) }
            finalNoobs.forEach {
                it.sendTitle(Title("" + ChatColor.RED + "✘" + ChatColor.RESET + "命令に反した"))
                killedPlayers.add(it)
                it.health = 0.0
                println("${it.displayName}:Final_Failure")
            }
        }
    }

    var result: MutableMap<Player, OrderResult> = mutableMapOf()
    fun updateResults() {
        result = getResults(time <= 0)
//        result.forEach { (player, r) ->
//                println("${player}:${r}")
//            }
    }

    override fun onTick(): Boolean {
        updateResults()
        updateNotice()
        return mainTick()
    }

    fun mainTick(): Boolean {
        if (isTimerMoving) {
            if (time <= 0) {
                println("時間切れ!!!!")
                killAll()
                isTimerMoving = false
                time = 0
                return true
            }
            time -= 1
            return false
        } else {
            return true
        }
    }

    override fun setTimer(i: Int) {
        time = i
        isTimerMoving = i >= 0
    }

    override fun killAll() {
        getNoobs().forEach {
            killedPlayers.add(it)
            it.health = 0.0
        }
    }

    private val killedPlayers = mutableListOf<Player>()

    fun onDeath(e: Event) {
        if (e is PlayerDeathEvent) {
            if (killedPlayers.contains(e.entity)) {
                e.deathMessage = "${e.entity.displayName}は${getDisplayName()}という命令を守らなかったので死んでしまった"
                killedPlayers.remove(e.entity)
            }
        }
    }

    override fun getNoobs(): MutableList<Player> {
        return result.filter { it.value === OrderResult.FAILURE }.map { it.key }.toMutableList()
    }

    override fun getPros(): MutableList<Player> {
        return result.filter { it.value === OrderResult.SUCCESS }.map { it.key }.toMutableList()
    }

    fun getFinalPros(): MutableList<Player> {
        return result.filter { it.value === OrderResult.FINAL_SUCCESS }.map { it.key }.toMutableList()
    }

    fun getFinalNoobs(): MutableList<Player> {
        return result.filter { it.value === OrderResult.FINAL_FAILURE }.map { it.key }.toMutableList()
    }

    fun isPlayerDone(p: Player): OrderResult {
        return result.getOrDefault(p, OrderResult.UNDEFINED)
    }

    abstract fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult>
    override fun getDefaultTime(): Int = defTime
}

class AbstractOrders {
    /**
     * 指定されたブロック掘るやつ
     */
    class Dig(var material: Material, var amount: Int, defTime: Int) : OrderBase<Pair<Player, ItemStack>>(defTime) {
        override fun getDisplayName(): String = "${material.key.key}を${amount}個掘れ!"
        override fun onStart() {
            Observer.instance.dig = ActionStore(Observer.store_size)
        }

        override fun getAll(): ActionStore<Pair<Player, ItemStack>> = Observer.instance.dig
        override fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult> {
            val list = mutableMapOf<Player, OrderResult>()
            Bukkit.getOnlinePlayers()
                .filter { Observer.isJoined(it) }
                .forEach { list[it] = OrderResult.FINAL_FAILURE }

            val map = mutableMapOf<Player, Int>()

            getAll().actions.forEach {
                if (it.second.type === material) {
                    if (map[it.first] == null) map[it.first] = 0
                    map[it.first] = it.second.amount + map[it.first]!!
                }
            }

            map.forEach { (player, amount) ->
                if (amount >= this.amount) {
                    list[player] = OrderResult.SUCCESS
                } else {
                    list[player] = OrderResult.PENDING
                }
            }

            return list
        }
    }

    /**
     * Moveといいつつ、動いちゃダメな奴
     */
    class Move(defTime: Int) : OrderBase<PlayerMoveEvent>(defTime) {
        override fun getAll(): ActionStore<PlayerMoveEvent> = Observer.instance.move
        override fun getDisplayName(): String = "動くな!"
        override fun onStart() {
            Observer.instance.move = ActionStore(Observer.store_size * 3)
        }

        override fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult> {
            val list = mutableMapOf<Player, OrderResult>()
            getAll().actions
                .filter {
                    it.from.x != it.to.x ||
                            it.from.y != it.to.y ||
                            it.from.z != it.to.z
                }
                .map {
                    it.player
                }
                .distinct()
                .forEach {
                    list[it] = OrderResult.FAILURE
                }
            Bukkit.getOnlinePlayers()
                .filter { Observer.isJoined(it) }
                .filter { !list.containsKey(it) }
                .forEach { list[it] = OrderResult.FINAL_SUCCESS }
            return list
        }
    }

    /**
     * 死んじゃいけないやつ
     */
    class NotDeath(defTime: Int) : OrderBase<PlayerDeathEvent>(defTime) {
        override fun getAll(): ActionStore<PlayerDeathEvent> = Observer.instance.death
        override fun getDisplayName(): String = "死ぬな!"
        override fun onStart() {
            Observer.instance.death = ActionStore(Observer.store_size)
        }

        override fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult> {
            val list = mutableMapOf<Player, OrderResult>()
            getAll().actions
                .map { it.entity }
                .distinct()
                .forEach {
                    list[it] = OrderResult.FAILURE
                }
            Bukkit.getOnlinePlayers()
                .filter { Observer.isJoined(it) }
                .filter { !list.containsKey(it) }
                .forEach { list[it] = OrderResult.FINAL_SUCCESS }
            return list
        }
    }

    /**
     * 死なないといけないやつ
     */
    class Death(defTime: Int) : OrderBase<PlayerDeathEvent>(defTime) {
        override fun getAll(): ActionStore<PlayerDeathEvent> = Observer.instance.death
        override fun getDisplayName(): String = "(これ考え中)!"
        override fun onStart() {
            Observer.instance.death = ActionStore(Observer.store_size)
        }

        override fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult> {
            val map = mutableMapOf<Player, OrderResult>()
            getAll().actions
                .forEach { map[it.entity] = OrderResult.SUCCESS }
            Bukkit.getOnlinePlayers()
                .filter { Observer.isJoined(it) }
                .filter { !map.containsKey(it) }
                .forEach { map[it] = OrderResult.PENDING }
            return map
        }
    }

    /**
     * 特定ディメンションにいないといけないやつ
     */
    class BeDim(var dimention: World.Environment, defTime: Int) : OrderBase<Empty>(defTime) {
        override fun getAll(): ActionStore<Empty> = Observer.instance.empty
        override fun getDisplayName(): String = "${dimention.displayName()}に行け!"
        override fun onStart() {
            Observer.instance.death = ActionStore(1)
        }

        override fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult> {
            val map = mutableMapOf<Player, OrderResult>()
            Bukkit.getOnlinePlayers()
                .filter { Observer.isJoined(it) }
                .forEach {
                    if (it.world.environment == dimention) {
                        map[it] = OrderResult.SUCCESS
                    } else map[it] = OrderResult.FINAL_FAILURE
                }
            return map
        }
    }

    /**
     * 特定の場所に行かなきゃいけないやつ
     */
    class Come(var loc: Location, defTime: Int) : OrderBase<PlayerMoveEvent>(defTime) {
        override fun getAll(): ActionStore<PlayerMoveEvent> = Observer.instance.move
        override fun getDisplayName(): String = "X:${loc.blockX} Y:${loc.blockY} Z:${loc.blockZ}まで行け!"
        override fun onStart() {
            Observer.instance.move = ActionStore(Observer.store_size * 3)
        }

        override fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult> {
            // TODO
            return mutableMapOf()
        }
    }

    class PlaceChooseYou(val material: Material, defTime: Int) : OrderBase<PlayerMoveEvent>(defTime) {
        private val blockList = mutableMapOf<Player, Boolean>()
        override fun getAll(): ActionStore<PlayerMoveEvent> = Observer.instance.move
        override fun getDisplayName(): String = "${material.name}に乗れ"
        override fun onStart() {
            Observer.instance.move = ActionStore(Observer.store_size * 3)
        }

        override fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult> {
            val list = mutableMapOf<Player, OrderResult>()
            Bukkit.getOnlinePlayers().filter { Observer.isJoined(it) }.forEach { list[it] = OrderResult.FINAL_FAILURE }
            getAll().actions.forEach { if (getMaterial(it) === material) list[it.player] = OrderResult.SUCCESS }
            // 負の遺産
//            val eventlist = mutableMapOf<Player,MutableList<Material>>()
//            getAll().actions
//                .filter { Observer.isJoined(it.player) }
//                .forEach {
//                    if (!eventlist.containsKey(it.player)) eventlist[it.player] = mutableListOf()
//                    eventlist[it.player]!!.add(getMaterial(it))
//                }
//
//            eventlist.forEach { (t, u) -> if (u.any { it === material }) blockList[t] = true }
//
//            blockList.forEach { (t, u) -> if(u) list[t] = OrderResult.SUCCESS }
//            println("Actions:${getAll().actions.size}")
//            list.forEach { (t, u) ->
//                println("${t.displayName}:$u")
//            }
            return list
        }

        fun getMaterial(e: PlayerMoveEvent): Material {
//            val loc = Location(e.to.world, e.to.blockX.toDouble(), (e.to.blockY - 1).toDouble(), e.to.blockZ.toDouble())
            val loc = e.player.location.add(0.0, -0.75, 0.0)
            val b = loc.block.type === material
            return loc.block.type
        }
    }

    class Kill(val entityType: EntityType, val amount: Int, defTime: Int) : OrderBase<EntityDeathEvent>(defTime) {
        override fun getAll(): ActionStore<EntityDeathEvent> = Observer.instance.kill
        override fun getDisplayName(): String = "${entityType.name}を${amount}体殺せ!"
        override fun onStart() {
            Observer.instance.kill = ActionStore(Observer.store_size * 2)
        }

        override fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult> {
            val list = mutableMapOf<Player, OrderResult>()
            val count = mutableMapOf<Player, Int>()
            Bukkit.getOnlinePlayers().filter { Observer.isJoined(it) }.forEach { list[it] = OrderResult.FINAL_FAILURE }
            getAll().actions.forEach {
                if (it.entityType === entityType) {
                    if(!count.containsKey(it.entity.killer!!)) count[it.entity.killer!!] = 0
                    count[it.entity.killer!!] = count[it.entity.killer!!]!! + 1
                }
            }

            count.forEach { (player, c) ->
                if(c>=amount){
                    list[player] = OrderResult.SUCCESS
                }else{
                    list[player] = OrderResult.FINAL_FAILURE
                }
            }

            return list
        }
    }
}
