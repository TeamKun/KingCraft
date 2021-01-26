package com.bun133.king

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent

class Observer : Listener {
    companion object {
        fun isJoined(p: Player) = p.gameMode === GameMode.SURVIVAL
        const val store_size: Int = 1000
        val instance = Observer()
    }

    var dig:ActionStore<BlockBreakEvent> = ActionStore(store_size)
    @EventHandler
    fun onDig(e: BlockBreakEvent) {
        if(!King.isGoingOn) return
        if(isJoined(e.player)){
            dig.add(e)
        }
    }

    var move:ActionStore<PlayerMoveEvent> = ActionStore(store_size * 3)
    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        if(!King.isGoingOn) return
        if(isJoined(e.player)){
            move.add(e)
        }
    }

    var death:ActionStore<PlayerDeathEvent> = ActionStore(store_size / 2)
    @EventHandler
    fun onMove(e: PlayerDeathEvent) {
        if(!King.isGoingOn) return
        if(isJoined(e.entity)){
            death.add(e)
        }
    }
}

class ActionStore<E>(val size:Int){
    var actions = mutableListOf<E>()
        private set

    private var index = 0

    fun add(entry:E){
//        actions[index] = entry
//        index++
//        if(index >= size - 1){
//            index = 0
//        }
        actions.add(entry)
        if(actions.size > size) actions.removeAt(0)
    }

    fun get(index:Int):E?{
        return actions.getOrNull(index)
    }

    fun getIterator() = actions.iterator()
}
