/*
 * Copyright 2014 MovingBlocks
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
package org.terasology.rails.trains.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.event.OnPlayerSpawnedEvent;
import org.terasology.math.Vector3i;
import org.terasology.physics.Physics;
import org.terasology.rails.trains.blocks.components.DebugTrainComponent;
import org.terasology.rails.trains.blocks.components.TrainCreaterComponent;
import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.items.BlockItemFactory;

import javax.vecmath.Vector3f;

@RegisterSystem(RegisterMode.AUTHORITY)
public class VehicleAction extends BaseComponentSystem {
    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private InventoryManager inventoryManager;
    @In
    private BlockManager blockManager;
    @In
    private Physics physics;

    private final Logger logger = LoggerFactory.getLogger(VehicleAction.class);


    @ReceiveEvent
    public void onPlayerSpawn(OnPlayerSpawnedEvent event, EntityRef player, InventoryComponent inventory) {
        BlockItemFactory blockFactory = new BlockItemFactory(entityManager);
        //inventoryManager.giveItem(player,player,entityManager.create("rails:minecart"));
        inventoryManager.giveItem(player,player,entityManager.create("rails:trainCreater"));
        inventoryManager.giveItem(player,player,entityManager.create("rails:railBlockTool"));
        inventoryManager.giveItem(player,player,entityManager.create("rails:railBlockTool-left"));
        inventoryManager.giveItem(player,player,entityManager.create("rails:railBlockTool-right"));
        inventoryManager.giveItem(player,player,entityManager.create("rails:railBlockTool-up"));
        inventoryManager.giveItem(player,player,entityManager.create("rails:railBlockTool-down"));
        //inventoryManager.giveItem(player,player,blockFactory.newInstance(blockManager.getBlockFamily("rails:Rails"), 99));
    }

    @ReceiveEvent(components = {TrainCreaterComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {


        logger.info("Go go go");

        EntityRef targetEntity = event.getTarget();
        TrainRailComponent trainRailComponent = targetEntity.getComponent(TrainRailComponent.class);

        if (trainRailComponent == null) {
            logger.info("cant find train component");
            return;
        }

        LocationComponent locationComponent = targetEntity.getComponent(LocationComponent.class);
        Vector3f placementPos = locationComponent.getWorldPosition();
        placementPos.y += 0.4f;

        logger.info("AAAA Created vehicle at {}", placementPos);

        EntityRef ent  = entityManager.create("rails:debugCube", placementPos);
        event.consume();
    }

}
