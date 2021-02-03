package com.bun133.king

import com.bun133.king.flylib.Events
import com.bun133.king.flylib.displayName
import com.destroystokyo.paper.Title
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack

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
}

/**
 * Pending - 判定中
 * FAILURE - 失敗
 * SUCCESS - 成功
 * FINAL_SUCCESS - 最終Tickにおいて成功(それ以外のTickにおいてはPendingと同じ)
 */
enum class OrderResult {
    PENDING, FAILURE, SUCCESS, UNDEFINED, FINAL_SUCCESS
}

abstract class OrderBase<E> : Order<E> {
    init {
        Events.PlayerDeathEvent.register(::onDeath)
    }


    companion object {
        const val TimeUP = -1114
    }

    var time: Int = 0
    var isTimerMoving = false

    override fun getTimer() = time

    val noticedPlayer = mutableListOf<Player>()

    fun updateNotice() {
        val list = getNoobs()
        if (list.size != noticedPlayer.size) {
            list.removeAll(noticedPlayer)
            list.forEach {
                it.sendTitle(Title("" + ChatColor.RED + "✘" + ChatColor.RESET + "命令に反した"))
                noticedPlayer.add(it)
            }
        }
    }

    var result: MutableMap<Player, OrderResult> = mutableMapOf()
    fun updateResults() {
        result = getResults(time <= 0)
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
                return true
            }
            time -= 1
        }
        return false
    }

    override fun setTimer(i: Int) {
        time = i
        isTimerMoving = true
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
                e.deathMessage = "${e.entity.displayName}は${getDisplayName()}という命令を守らなかってので死んでしまった"
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

    fun isPlayerDone(p: Player): OrderResult {
        return result.getOrDefault(p, OrderResult.UNDEFINED)
    }

    abstract fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult>
}

class AbstractOrders {
    /**
     * 指定されたブロック掘るやつ
     */
    class Dig(var material: Material, var amount: Int) : OrderBase<Pair<Player, ItemStack>>() {
        override fun getDisplayName(): String = "${material.key.key}を${amount}個掘れ!"
        override fun onStart() {
            Observer.instance.dig = ActionStore(Observer.store_size)
        }

        override fun getAll(): ActionStore<Pair<Player, ItemStack>> = Observer.instance.dig
        override fun onTick(): Boolean {
            return super.mainTick()
        }

        override fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult> {
            val list = mutableMapOf<Player, OrderResult>()
            Bukkit.getOnlinePlayers()
                .filter { Observer.isJoined(it) }
                .forEach { list[it] = OrderResult.UNDEFINED }

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
                    list[player] = OrderResult.FINAL_SUCCESS
                }
            }

            return list
        }
    }

    /**
     * Moveといいつつ、動いちゃダメな奴
     */
    class Move() : OrderBase<PlayerMoveEvent>() {
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
    class NotDeath() : OrderBase<PlayerDeathEvent>() {
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
    class Death() : OrderBase<PlayerDeathEvent>() {
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
    class BeDim(var dimention: World.Environment) : OrderBase<Empty>() {
        override fun getAll(): ActionStore<Empty> = Observer.instance.empty
        override fun getDisplayName(): String = "${dimention.displayName()}に行け!"
        override fun onStart() {
            Observer.instance.death = ActionStore(1)
        }

        override fun onTick(): Boolean {
            return super.mainTick()
        }

        override fun getResults(isFinalTick: Boolean): MutableMap<Player, OrderResult> {
            val map = mutableMapOf<Player,OrderResult>()
            Bukkit.getOnlinePlayers()
                .filter { Observer.isJoined(it) }
                .forEach {
                    if(it.world.environment == dimention){
                        map[it] = OrderResult.SUCCESS
                    }else map[it] = OrderResult.PENDING
                }
            return map
        }
    }

    /**
     * 特定の場所に行かなきゃいけないやつ
     */
    class Come(var loc: Location) : OrderBase<PlayerMoveEvent>() {
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
}
