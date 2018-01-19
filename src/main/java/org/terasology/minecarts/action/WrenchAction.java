/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.minecarts.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;
import org.terasology.minecarts.blocks.RailComponent;
import org.terasology.minecarts.blocks.RailsUpdateFamily;
import org.terasology.minecarts.components.CartJointComponent;
import org.terasology.minecarts.components.RailVehicleComponent;
import org.terasology.minecarts.components.WrenchComponent;
import org.terasology.minecarts.controllers.CartJointSystem;
import org.terasology.registry.In;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

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

        if (!targetVehicle.hasComponent(RailVehicleComponent.class) && !targetVehicle.hasComponent(CartJointComponent.class))
            return;

        if(wrenchComponent.lastSelectedCart != null){
            cartJointSystem.joinVehicles(targetVehicle,wrenchComponent.lastSelectedCart);
            wrenchComponent.lastSelectedCart = null;
            LOGGER.debug("Carts Joined");
        }

        wrenchComponent.lastSelectedCart = targetVehicle;

    }

    @ReceiveEvent(components = {WrenchComponent.class})
    public void onRailFlipAction(ActivateEvent event, EntityRef item) {
        EntityRef targetEntity = event.getTarget();
        if (!targetEntity.hasComponent(RailComponent.class))
            return;

        Vector3i position = targetEntity.getComponent(BlockComponent.class).getPosition();

        RailsUpdateFamily railFamily = (RailsUpdateFamily) blockManager.getBlockFamily("Rails:rails");
        RailsUpdateFamily invertFamily = (RailsUpdateFamily) blockManager.getBlockFamily("railsTBlockInverted");

        Block block = worldProvider.getBlock(targetEntity.getComponent(BlockComponent.class).getPosition());

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
