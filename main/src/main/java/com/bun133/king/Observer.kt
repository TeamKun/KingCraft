package com.bun133.king

import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.inventory.ItemStack

class Observer : Listener {
    companion object {
        fun isJoined(p: Player) = p.gameMode === GameMode.SURVIVAL && !King.kingPlayers.contains(p)
        const val store_size: Int = 1000
        val instance = Observer()
    }

    var dig: ActionStore<Pair<Player, ItemStack>> = ActionStore(store_size)

    @EventHandler
    fun onDig(e: BlockBreakEvent) {
        if (!King.isGoingOn) return
        if (isJoined(e.player)) {
            dig.add(Pair(e.player, ItemStack(e.block.type, 1)))
        }
    }

    var move: ActionStore<PlayerMoveEvent> = ActionStore(store_size * 3)

    @EventHandler
    fun onMove(e: PlayerMoveEvent) {
        if (!King.isGoingOn) return
        if (isJoined(e.player)) {
            move.add(e)
        }
    }

    var death: ActionStore<PlayerDeathEvent> = ActionStore(store_size / 2)

    @EventHandler
    fun onDeath(e: PlayerDeathEvent) {
        if (!King.isGoingOn) return
        if (isJoined(e.entity)) {
            death.add(e)
        }
    }

    var kill: ActionStore<EntityDeathEvent> = ActionStore(store_size * 2)

    @EventHandler
    fun onKill(e: EntityDeathEvent) {
        if (!King.isGoingOn) return
        if (e.entity.killer != null) {
            if (isJoined(e.entity.killer!!)) {
                kill.add(e)
            }
        }
    }

    var itemPick:ActionStore<EntityPickupItemEvent> = ActionStore(store_size * 2)

    @EventHandler
    fun onPickUp(e: EntityPickupItemEvent){
        if(!King.isGoingOn) return
        if(e.entity is Player){
            if(isJoined(e.entity as Player)){
                itemPick.add(e)
            }
        }
    }

    var knock:ActionStore<EntityDamageByEntityEvent> = ActionStore(store_size * 2)

    @EventHandler
    fun onKnock(e:EntityDamageByEntityEvent){
        if(!King.isGoingOn) return
        if(e.damager is Player && e.entity is Player && isJoined(e.damager as Player) && isJoined(e.entity as Player)){
            knock.add(e)
        }
    }

    var empty: ActionStore<Empty> = ActionStore(1)
}

class ActionStore<E>(val size: Int) {
    var actions = mutableListOf<E>()
        private set

    private var index = 0

    fun add(entry: E) {
        actions.add(entry)
//        if(actions.size > size) actions.removeAt(0)
    }

    fun get(index: Int): E? {
        return actions.getOrNull(index)
    }

    fun getIterator() = actions.iterator()
}
