package com.lambda.client.manager.managers.activity.activities.interaction

import com.lambda.client.event.SafeClientEvent
import com.lambda.client.manager.managers.activity.types.InstantActivity
import com.lambda.client.util.EntityUtils.flooredPosition
import com.lambda.client.util.math.Direction
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos

class PlaceBlockAHeadActivity : InstantActivity() {
    override fun SafeClientEvent.onInitialize() {
        subActivities.add(
            PlaceBlockActivity(
                BlockPos(player.flooredPosition.add(Direction.fromEntity(player).directionVec)),
                Blocks.OBSIDIAN
            )
        )
    }
}