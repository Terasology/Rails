// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.blocks;

import com.google.common.collect.Sets;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.health.DoDestroyEvent;
import org.terasology.engine.logic.health.EngineDamageTypes;
import org.terasology.module.health.events.DoDamageEvent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.OnChangedBlock;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.entity.neighbourUpdate.LargeBlockUpdateFinished;
import org.terasology.engine.world.block.entity.neighbourUpdate.LargeBlockUpdateStarting;
import org.terasology.engine.world.block.family.BlockPlacementData;
import org.terasology.engine.world.block.items.BlockItemComponent;
import org.terasology.engine.world.block.items.OnBlockItemPlaced;

import java.util.Set;

@RegisterSystem(RegisterMode.AUTHORITY)
public class RailsBlockFamilyUpdateSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    private static final Logger logger = LoggerFactory.getLogger(RailsBlockFamilyUpdateSystem.class);

    @In
    private WorldProvider worldProvider;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private BlockManager blockManager;

    private int largeBlockUpdateCount;
    private Set<Vector3i> blocksUpdatedInLargeBlockUpdate = Sets.newHashSet();
    private int[] checkOnHeight = {-1, 0, 1};

    @ReceiveEvent
    public void largeBlockUpdateStarting(LargeBlockUpdateStarting event, EntityRef entity) {
        largeBlockUpdateCount++;
    }

    @ReceiveEvent
    public void largeBlockUpdateFinished(LargeBlockUpdateFinished event, EntityRef entity) {
        largeBlockUpdateCount--;
        if (largeBlockUpdateCount < 0) {
            largeBlockUpdateCount = 0;
            throw new IllegalStateException("LargeBlockUpdateFinished invoked too many times");
        }

        if (largeBlockUpdateCount == 0) {
            notifyNeighboursOfChangedBlocks();
        }
    }

    @ReceiveEvent()
    public void doDestroy(DoDestroyEvent event, EntityRef entity, BlockComponent blockComponent) {
        Vector3i upBlock = blockComponent.getPosition().add(0, 1, 0, new Vector3i());
        Block block = worldProvider.getBlock(upBlock);

        if (block.getBlockFamily() instanceof RailBlockFamily) {
            blockEntityRegistry.getEntityAt(upBlock).send(new DoDamageEvent(1000, EngineDamageTypes.DIRECT.get()));
        }
    }

    //prevents rails from being stacked on top of each other.
    @ReceiveEvent(components = {BlockItemComponent.class, ItemComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onBlockActivated(ActivateEvent event, EntityRef item) {
        BlockComponent blockComponent = event.getTarget().getComponent(BlockComponent.class);
        if (blockComponent == null) {
            return;
        }

        Block centerBlock = worldProvider.getBlock(blockComponent.getPosition());

        if (centerBlock.getBlockFamily() instanceof RailBlockFamily) {
            event.consume();
        }
    }

    @ReceiveEvent(components = {BlockItemComponent.class, ItemComponent.class})
    public void onPlaceBlock(OnBlockItemPlaced event, EntityRef entity) {
        BlockComponent blockComponent = event.getPlacedBlock().getComponent(BlockComponent.class);
        if (blockComponent == null) {
            return;
        }

        Vector3ic targetBlock = blockComponent.getPosition();
        Block centerBlock = worldProvider.getBlock(targetBlock);

        if (centerBlock.getBlockFamily() instanceof RailBlockFamily) {
            processUpdateForBlockLocation(targetBlock);
        }

    }

    private void notifyNeighboursOfChangedBlocks() {
        // Invoke the updates in another large block change for this class only
        largeBlockUpdateCount++;
        while (!blocksUpdatedInLargeBlockUpdate.isEmpty()) {
            Set<Vector3i> blocksToUpdate = blocksUpdatedInLargeBlockUpdate;

            // Setup new collection for blocks changed in this pass
            blocksUpdatedInLargeBlockUpdate = Sets.newHashSet();

            for (Vector3i blockLocation : blocksToUpdate) {
                processUpdateForBlockLocation(blockLocation);
            }
        }
        largeBlockUpdateCount--;
    }

    @ReceiveEvent(components = {BlockComponent.class})
    public void blockUpdate(OnChangedBlock event, EntityRef blockEntity) {
        if (largeBlockUpdateCount > 0) {
            blocksUpdatedInLargeBlockUpdate.add(event.getBlockPosition());
        } else {
            Vector3i blockLocation = event.getBlockPosition();
            processUpdateForBlockLocation(blockLocation);
        }
    }

    private void processUpdateForBlockLocation(Vector3ic blockLocation) {
        for (int height : checkOnHeight) {
            for (Side side : Side.horizontalSides()) {
                Vector3i neighborLocation = new Vector3i(blockLocation);
                neighborLocation.add(side.direction());
                neighborLocation.y += height;
                Block neighborBlock = worldProvider.getBlock(neighborLocation);
                EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(neighborLocation);
                if (blockEntity.hasComponent(RailComponent.class)) {
                    RailBlockFamily railsFamily = (RailBlockFamily) neighborBlock.getBlockFamily();
                    Block neighborBlockAfterUpdate =
                            railsFamily.getBlockForPlacement(new BlockPlacementData(neighborLocation, Side.FRONT,
                                    new Vector3f()));
                    if (neighborBlock != neighborBlockAfterUpdate && neighborBlockAfterUpdate != null) {
                        byte connections = Byte.parseByte(neighborBlock.getURI().getIdentifier().toString());
                        //only add segment with two connections
                        if (SideBitFlag.getSides(connections).size() <= 1) {
                            worldProvider.setBlock(neighborLocation, neighborBlockAfterUpdate);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void update(float delta) {
        if (largeBlockUpdateCount > 0) {
            logger.error("Unmatched LargeBlockUpdateStarted - LargeBlockUpdateFinished not invoked enough times");
        }
        largeBlockUpdateCount = 0;
    }
}
