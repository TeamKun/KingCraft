package com.bun133.king

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import kotlin.reflect.KClass

interface Order<E> {
//    fun isDone(p: Player): Boolean
    fun getAll(): ActionStore<E>
    fun getDisplayName(): String
    fun onStart()
    fun getNoobs(): MutableList<Player>
}

class Orders {
    /**
     * 指定されたブロック掘るやつ
     */
    class Dig(var material: Material) : Order<BlockBreakEvent> {
        override fun getDisplayName(): String = "掘れ!"
        override fun onStart() {
            Observer.instance.dig = ActionStore(Observer.store_size)
        }

        override fun getAll(): ActionStore<BlockBreakEvent> = Observer.instance.dig
        override fun getNoobs(): MutableList<Player> {
            val list = mutableListOf<Player>()
            list.addAll(Bukkit.getOnlinePlayers())
            getAll().actions.forEach {
                if (it.block.type === material) {
                    list.remove(it.player)
                }
            }
            return list
        }
    }

    /**
     * Moveといいつつ、動いちゃダメな奴
     */
    class Move():Order<PlayerMoveEvent>{
        override fun getAll(): ActionStore<PlayerMoveEvent> = Observer.instance.move
        override fun getDisplayName(): String = "動くな!"
        override fun onStart() {
            Observer.instance.move = ActionStore(Observer.store_size * 3)
        }
        override fun getNoobs(): MutableList<Player> {
            val list = mutableListOf<Player>()
            getAll().actions.forEach {
                list.add(it.player)
            }
            return list
        }
    }

    /**
     * 死んじゃいけないやつ
     */
    class NotDeath():Order<PlayerDeathEvent>{
        override fun getAll(): ActionStore<PlayerDeathEvent> = Observer.instance.death
        override fun getDisplayName(): String = "死ぬな!"
        override fun onStart() {
            Observer.instance.death = ActionStore(Observer.store_size)
        }
        override fun getNoobs(): MutableList<Player> {
            val list = mutableListOf<Player>()
            getAll().actions.forEach {
                list.add(it.entity)
            }
            return list
        }
    }

    /**
     * 死なないといけないやつ
     */
    class Death():Order<PlayerDeathEvent>{
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
}