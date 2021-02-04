package com.bun133.king.flylib

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class TabPart {
    companion object {
        val selectors = TabObject(arrayOf("@a", "@r", "@s", "@e"))
        val playerSelector = PlayerSelector()
    }

    class PlayerSelector() : TabObject() {
        override fun getAsList(): MutableList<String> = Bukkit.getOnlinePlayers().map { it.displayName }.toMutableList()
    }
}

class SmartTabCompleter(val chains: MutableList<TabChain>) : TabCompleter {
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        val strings = args.toMutableList().map { it as String }.toTypedArray()
        val list = mutableListOf<String>()
        chains
            .filter { it.isExist(strings) }
            .forEach { chain ->
                println("Chain:${chain.toString()}")
                chain.getMatched(strings)!!.getAsList()
                    .forEach { list.add(it) }
            }
        println("Result:${list.toString()}")
        if(list.isEmpty()) list.add("")
        return list
    }
}

/**
 * The one way of command,
 * like,(TabObject("kill","heal","give"),TabObject("@a","@r","@s","@e"))
 */
class TabChain(val tabObjects: Array<TabObject>) {

    fun getMatched(args: Array<String>): TabObject? {
        println("tabObjects.lastIndex:${tabObjects.lastIndex}")
        println("args.lastIndex:${args.lastIndex}")
        return try{
            tabObjects[args.lastIndex]
        }catch (e:Exception){
            null
        }
//        if (tabObjects.lastIndex >= args.lastIndex + 1) {
//            return tabObjects[args.lastIndex]
//        }
//        return null
    }

    fun isExist(args: Array<String>) = getMatched(args) != null

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("(")
        for (obj in tabObjects) {
            builder.append("{${obj.toString()}},")
        }
        builder.append(")")
        return builder.toString()
    }
}

/**
 * Part Of Tab Chain,
 * like ("@a","@r","@s","@e")
 * this will provide the player selector.
 */
open class TabObject {
    private val strings: MutableList<String> = mutableListOf()
    private val objects: MutableList<TabObject> = mutableListOf()

    constructor(args: Array<String>) {
        strings.addAll(args)
    }

    constructor(objs: Array<TabObject>) {
        objects.addAll(objs)
    }

    // Only For Override
    constructor()

    open fun getAsList(): MutableList<String> {
        val list = mutableListOf<String>()
        list.addAll(strings)
        objects.map { it.getAsList() }.forEach { obj -> obj.forEach { list.add(it) } }
        return list
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("[")
        getAsList().forEach { builder.append("$it,") }
        builder.append("]")
        return builder.toString()
    }
}