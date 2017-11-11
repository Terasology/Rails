/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import org.terasology.logic.players.event.OnPlayerSpawnedEvent;
import org.terasology.math.geom.Vector3i;
import org.terasology.minecarts.blocks.RailComponent;
import org.terasology.minecarts.components.CartDefinitionComponent;
import org.terasology.registry.In;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.items.BlockItemFactory;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MinecartAction extends BaseComponentSystem {
    @In
    private EntityManager entityManager;
    @In
    private InventoryManager inventoryManager;
    @In
    private BlockManager blockManager;


    private final Logger logger = LoggerFactory.getLogger(MinecartAction.class);

    @Override
    public void initialise() {
    }


    @ReceiveEvent
    public void onPlayerSpawn(OnPlayerSpawnedEvent event, EntityRef player, InventoryComponent inventory) {
        BlockItemFactory blockFactory = new BlockItemFactory(entityManager);
        inventoryManager.giveItem(player, player, entityManager.create("Rails:minecart"));
        inventoryManager.giveItem(player, player, entityManager.create("Rails:wrench"));
        inventoryManager.giveItem(player, player, blockFactory.newInstance(blockManager.getBlockFamily("Rails:rails"), 99));
    }

    @ReceiveEvent(components = {CartDefinitionComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {

        EntityRef targetEntity = event.getTarget();
        if (!targetEntity.hasComponent(RailComponent.class))
            return;

        CartDefinitionComponent cartDefinition = item.getComponent(CartDefinitionComponent.class);

        Vector3i placementPos = new Vector3i(targetEntity.getComponent(BlockComponent.class).getPosition());
        placementPos.y += 0.2f;

        logger.info("Created vehicle at {}", placementPos);

        entityManager.create(cartDefinition.prefab, placementPos.toVector3f());
        event.consume();
    }

}
