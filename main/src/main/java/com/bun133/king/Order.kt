package com.bun133.king

import com.bun133.king.flylib.displayName
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack

interface Order<E> {
    fun getAll(): ActionStore<E>
    fun getDisplayName(): String
    fun onStart()
    fun getNoobs(): MutableList<Player>
    fun killAll()
    fun onTick(): Boolean
    fun setTimer(i: Int)
    fun getTimer(): Int
}

abstract class OrderBase<E> : Order<E> {
    companion object {
        const val TimeUP = -1114
    }

    var time: Int = 0
    var isTimerMoving = false

    override fun getTimer() = time
    override fun onTick(): Boolean {
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
            it.health = 0.0
        }
    }
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
        override fun getNoobs(): MutableList<Player> {
            val list = mutableListOf<Player>()
            list.addAll(Bukkit.getOnlinePlayers())
            val map = mutableMapOf<Player, Int>()
            getAll().actions.forEach {
                if (it.second.type === material) {
                    if (map[it.first] == null) map[it.first] = 0
                    map[it.first] = it.second.amount + map[it.first]!!
                }
            }

            map.forEach { (t, u) ->
                if (u >= amount) {
                    list.remove(t)
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

        override fun getNoobs(): MutableList<Player> {
            val list = mutableListOf<Player>()
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
                    list.add(it)
                }
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

        override fun getNoobs(): MutableList<Player> {
            val list = mutableListOf<Player>()
            getAll().actions
                .map { it.entity }
                .distinct()
                .forEach {
                    list.add(it)
                }
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

        override fun getNoobs(): MutableList<Player> {
            val list = mutableListOf<Player>()
            list.addAll(Bukkit.getOnlinePlayers())
            getAll().actions.forEach {
                list.remove(it.entity)
            }
            return list
        }
    }

    class BeDim(var dimention: World.Environment) : OrderBase<Empty>() {
        override fun getAll(): ActionStore<Empty> = Observer.instance.empty
        override fun getDisplayName(): String = "${dimention.displayName()}に行け!"
        override fun onStart() {
            Observer.instance.death = ActionStore(1)
        }

        override fun getNoobs(): MutableList<Player> {
            val list = mutableListOf<Player>()
            val li = mutableListOf<Player>()
            list.addAll(Bukkit.getOnlinePlayers())
            list.filter {
                it.world.environment != dimention
            }.forEach { li.add(it) }
            return li
        }
    }
}
