package com.bun133.king.flylib

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class EasyItemBuilder {
    companion object{
        fun genItem(material:Material): ItemStack {
            return ItemStack(material)
        }

        fun genItem(material:Material,stack:Int): ItemStack {
            return ItemStack(material,stack)
        }

        fun genItem(material:Material,display_name:String): ItemStack {
            val i = ItemStack(material)
            val meta = i.itemMeta
            meta.setDisplayName(display_name)
            i.itemMeta = meta
            return i
        }

        fun genItem(material:Material,display_name:String,lores:ArrayList<String>): ItemStack {
            val i = ItemStack(material)
            val meta = i.itemMeta
            meta.setDisplayName(display_name)
            meta.lore = lores
            i.itemMeta = meta
            return i
        }

        fun genItem(material:Material,stack:Int,name:String):ItemStack{
            val i = ItemStack(material,stack)
            val meta = i.itemMeta
            meta.setDisplayName(name)
            i.itemMeta = meta
            return i
        }
    }
}