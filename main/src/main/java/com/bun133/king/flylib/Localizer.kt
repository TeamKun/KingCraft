package com.bun133.king.flylib

import org.bukkit.World

class DimensionLocalizer(){
    companion object{
        fun get(dimension:World.Environment):String{
            return when(dimension){
                World.Environment.NORMAL -> "オーバーワールド"
                World.Environment.NETHER -> "ネザー"
                World.Environment.THE_END -> "エンド"
            }
        }
    }
}

fun World.Environment.displayName(): String = DimensionLocalizer.get(this)