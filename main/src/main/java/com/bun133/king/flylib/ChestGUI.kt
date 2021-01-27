package com.bun133.king.flylib

import com.flylib.util.NaturalNumber
import com.flylib.util.SizedFlatList
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KFunction1

class ChestGUI(var p: Player, var col: NaturalNumber, name: String) {
    var guis: SizedFlatList<GUIObject> = SizedFlatList(NaturalNumber(9), col)
        private set
    var inventory: Inventory
        private set
    var isOpening: Boolean = false
        private set

    init {
        if (col > 6 || col < 1) {
            throw IllegalArgumentException("ChestGUI col size is Illegal")
        }
        inventory = Bukkit.createInventory(p, col * 9, name)
    }

    fun open() {
        inventorySync()
        p.openInventory(inventory)
    }

    /**
     * @param x left is 1,right is max
     * @param y top is 1,bottom is max
     */
    fun addGUIObject(obj: GUIObject) {
        guis.set(obj.x, obj.y, obj)
        inventorySync()
        if (isOpening) {
            //ReOpen
            open()
        }
    }

    /**
     * This method is syncer between FlatList and Inventory
     */
    fun inventorySync() {
        inventory.contents = getItemsArray()
    }

    fun getGUIsArray(): Array<GUIObject> {
        val array: Array<GUIObject> =
            Array(9 * col.toInt()) {
                GUIObject(
                    NaturalNumber(it % 9 + 1),
                    NaturalNumber(it / 9 + 1),
                    ItemStack(Material.AIR)
                )
            }
        for (x in 1..9) {
            for (y in 1..col.toInt()) {
                val t = guis.get(NaturalNumber(x), NaturalNumber(y))
                if (t == null) {
                    array[(y - 1) * 9 + (x - 1)] =
                        GUIObject(NaturalNumber(x), NaturalNumber(y), ItemStack(Material.AIR))
                } else {
                    array[(y - 1) * 9 + (x - 1)] = t.t
                }
            }
        }
        return array
    }

    fun getItemsArray(): Array<ItemStack> {
        val array = getGUIsArray()
        val copy: Array<ItemStack> = Array<ItemStack>(array.size) { ItemStack(Material.AIR) }
        for (i in array.indices) {
            copy[i] = array[i].getStack()
        }
        return copy
    }
}

/**
 * @param stack The ItemStack that will be showed in ChestGUI
 */
class GUIObject(val x: NaturalNumber, val y: NaturalNumber, real_stack: ItemStack) {
    val id: ByteArray = GUIObjectByteManager.instance.getNew()
    private val handler = GUIObjectEventHandler(this, real_stack)
    fun getStack() = handler.getStack()
    fun addCallBack(f: KFunction1<InventoryClickEvent, Unit>): GUIObject {
        handler.callbacks.add(f)
        return this
    }
}

class GUIObjectEventHandler(
    var obj: GUIObject,
    stack: ItemStack,
    var callbacks: ArrayList<KFunction1<InventoryClickEvent, Unit>> = arrayListOf()
) {

    companion object {
        val nameKey = NamespacedKey(FlyLib.get()!!.plugin, "FlyLib")
    }

    private var copy: ItemStack = stack.clone()

    init {
        val meta = copy.itemMeta
        if (meta !== null) {
            Events.ClickEvent.register(::onClick)
            meta.persistentDataContainer.set(
                nameKey,
                PersistentDataType.BYTE_ARRAY,
                obj.id
            )
            copy.itemMeta = meta
        }
    }

    fun getStack() = copy

    fun onClick(e: Event) {
        if (e is InventoryClickEvent) {
            if (callbacks.isEmpty()) {
                return
            }
            if (e.currentItem!!.hasItemMeta()) {
                val meta = e.currentItem!!.itemMeta
                if (meta.persistentDataContainer.has(
                        nameKey,
                        PersistentDataType.BYTE_ARRAY
                    )
                ) {
                    println("Data Found!")
                    if (obj.id === meta.persistentDataContainer.get(
                            nameKey,
                            PersistentDataType.BYTE_ARRAY
                        )
                    ) {
                        println("Data Got!")
                        for (callback in callbacks) {
                            callback.invoke(e)
                        }
                        e.isCancelled = true
                    }
                } else {
                    println("Data not Found!")
                }
            }
        }
    }
}

class GUIObjectByteManager() {
    companion object {
        val instance = GUIObjectByteManager()
    }

    private val list: ArrayList<Byte> = arrayListOf()
    fun isUsed(b: Byte): Boolean {
        return list.contains(b)
    }

    fun register(b: Byte) {
        if (isUsed(b)) throw GUIObjectByteException(b)
        list.add(b)
    }

    fun getNew(): ByteArray {
        return UUIDByteConverter.getBytesFromUUID(UUID.randomUUID())
    }

    fun remove(b: Byte) {
        list.remove(b)
    }
}

class GUIObjectByteException(b: Byte) : Exception("$b is already used byte")