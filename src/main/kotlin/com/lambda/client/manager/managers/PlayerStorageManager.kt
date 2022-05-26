package com.lambda.client.manager.managers

import com.lambda.client.LambdaMod
import com.lambda.client.manager.Manager
import com.lambda.client.util.items.inventorySlots
import com.lambda.client.util.items.item
import com.lambda.client.util.items.swapToBlockOrMove
import com.lambda.client.util.items.swapToItemOrMove
import com.lambda.client.util.threads.runSafe
import com.lambda.client.util.threads.safeListener
import com.lambda.client.util.world.isBlacklisted
import net.minecraft.block.BlockHardenedClay
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.ItemStackHelper
import net.minecraft.inventory.Slot
import net.minecraft.item.*
import net.minecraft.util.NonNullList
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.concurrent.ConcurrentSkipListSet

object PlayerStorageManager: Manager {
    private val requestedSet = ConcurrentSkipListSet(itemComparator())

    private val currentStage = Stage.IDLE

    init {
        safeListener<TickEvent.ClientTickEvent> { event ->
            if (event.phase != TickEvent.Phase.START) return@safeListener

            when (currentStage) {

            }

            if (PlayerInventoryManager.isDone()) {
                requestedSet.firstOrNull()?.let { set ->
                    if (swapToItemOrMove(set.item, predicateItem = { it.metadata == set.meta })) {
                        LambdaMod.LOG.info("Material request executed for Block: ${set.item.registryName}")
                        requestedSet.remove(set)
                    } else {
                        getShulkerWith(player.inventorySlots, set.item, predicate = { it.metadata == set.meta })?.let { shulker ->
                            LambdaMod.LOG.info("${set.item.registryName} meta: ${set.meta} found in slot: ${shulker.slotNumber}")
                        } ?: run {
                            LambdaMod.LOG.info("${set.item.registryName} meta: ${set.meta} not found in any shulker")
                        }
                    }
                }
            }
        }
    }

//    fun requestMaterial(vararg request: MaterialRequest) {
//        request.forEach {
//            LambdaMod.LOG.info("Material request registered for Item: ${it.item.registryName}")
//            requestQueue.add(it)
//        }
//    }

    fun requestAll(desired: List<IBlockState>) {
        desired.firstOrNull()?.let {
            val request = convertBlockStateToRequest(it)
            if (requestedSet.add(request)) {
                LambdaMod.LOG.info("Material request registered for Block: $request")
            }
        }
    }

    private fun convertBlockStateToRequest(state: IBlockState): MaterialRequest {
        val item = state.block.item
        var color: EnumDyeColor? = null

        state.properties.filterKeys { key -> key.name == "color" && key.valueClass == EnumDyeColor::class.java }.forEach { prop ->
            color = prop.value as EnumDyeColor
        }

        return MaterialRequest(item, color?.ordinal ?: 0)
    }

    private fun getShulkerWith(slots: List<Slot>, item: Item, predicate: (ItemStack) -> Boolean = { true }): Slot? {
        return slots.filter {
            it.stack.item is ItemShulkerBox && getShulkerData(it.stack, item, predicate) > 0
        }.minByOrNull {
            getShulkerData(it.stack, item, predicate)
        }
    }

    private fun getShulkerData(stack: ItemStack, item: Item, predicate: (ItemStack) -> Boolean = { true }): Int {
        val tagCompound = if (stack.item is ItemShulkerBox) stack.tagCompound else return 0

        tagCompound?.let { compound ->
            if (compound.hasKey("BlockEntityTag", 10)) {
                val blockEntityTag = compound.getCompoundTag("BlockEntityTag")
                if (blockEntityTag.hasKey("Items", 9)) {
                    val shulkerInventory = NonNullList.withSize(27, ItemStack.EMPTY)
                    ItemStackHelper.loadAllItems(blockEntityTag, shulkerInventory)
                    return shulkerInventory.count { it.item == item && predicate(it) }
                }
            }
        }

        return 0
    }

    private fun itemComparator() = compareBy<MaterialRequest> {
        it.item.registryName
    }.thenBy {
        it.meta
    }

    fun isDone() = requestedSet.isEmpty()

    data class MaterialRequest(val item: Item, val meta: Int) {
        override fun toString(): String {
            return "(item=${item.registryName}, meta=$meta)"
        }
    }

    private enum class Stage {
        IDLE,
        PLACE_SHULKER,
        OPEN_SHULKER,
        MOVE_TO_SHULKER,
        BREAK_SHULKER
    }
}