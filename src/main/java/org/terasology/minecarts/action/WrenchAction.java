// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.action;

import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.minecarts.blocks.RailBlockFamily;
import org.terasology.minecarts.blocks.RailComponent;
import org.terasology.minecarts.components.CartJointComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.minecarts.components.WrenchComponent;
import org.terasology.minecarts.controllers.CartJointSystem;

/**
 * Created by michaelpollind on 4/1/17.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class WrenchAction extends BaseComponentSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(WrenchAction.class);

    @In
    WorldProvider worldProvider;
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    BlockManager blockManager;
    @In
    CartJointSystem cartJointSystem;

    @ReceiveEvent(components = {WrenchComponent.class})
    public void onCartJoinAction(ActivateEvent event, EntityRef item, WrenchComponent wrenchComponent) {
        EntityRef targetVehicle = event.getTarget();

        if (!targetVehicle.hasComponent(RailVehicleComponent.class) && !targetVehicle.hasComponent(CartJointComponent.class)) {
            return;
        }

        if (wrenchComponent.lastSelectedCart != null) {
            cartJointSystem.joinVehicles(targetVehicle, wrenchComponent.lastSelectedCart);
            wrenchComponent.lastSelectedCart = null;
            LOGGER.debug("Carts Joined");
        }

        wrenchComponent.lastSelectedCart = targetVehicle;

    }

    @ReceiveEvent(components = {WrenchComponent.class})
    public void onRailFlipAction(ActivateEvent event, EntityRef item) {
        EntityRef targetEntity = event.getTarget();
        if (!targetEntity.hasComponent(RailComponent.class)) {
            return;
        }

        Vector3i position = targetEntity.getComponent(BlockComponent.class).getPosition(new Vector3i());

        RailBlockFamily railFamily = (RailBlockFamily) blockManager.getBlockFamily("Rails:rails");
        RailBlockFamily invertFamily = (RailBlockFamily) blockManager.getBlockFamily("railsTBlockInverted");

        Block block = worldProvider.getBlock(position);

        byte connections = Byte.parseByte(block.getURI().getIdentifier().toString());

        if (SideBitFlag.getSides(connections).size() == 3) {
            if (block.getBlockFamily() == railFamily) {
                blockEntityRegistry.setBlockForceUpdateEntity(position, invertFamily.getBlockByConnection(connections));
            } else if (block.getBlockFamily() == invertFamily) {

                blockEntityRegistry.setBlockForceUpdateEntity(position, railFamily.getBlockByConnection(connections));
            }
        }
    }
}
