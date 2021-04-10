package com.bun133.king

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

class TimeSkipper(king:King) : Listener {
    init {
        king.server.pluginManager.registerEvents(this,king)
    }
    @EventHandler
    fun onClick(e:PlayerInteractEvent){
        if(King.kingPlayers.contains(e.player)){
            if(e.player.inventory.itemInMainHand.type === Material.CLOCK){
                King.king!!.goOn.forEach {
                    it.setTimer(1)
                }
            }
        }
    }
}