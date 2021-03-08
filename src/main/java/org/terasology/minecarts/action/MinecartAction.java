// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

package org.terasology.minecarts.action;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.minecarts.blocks.RailComponent;
import org.terasology.minecarts.components.CartDefinitionComponent;
import org.terasology.registry.In;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

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

    @ReceiveEvent(components = {CartDefinitionComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {

        EntityRef targetEntity = event.getTarget();
        if (!targetEntity.hasComponent(RailComponent.class)) {
            return;
        }

        CartDefinitionComponent cartDefinition = item.getComponent(CartDefinitionComponent.class);

        Vector3f placementPos = new Vector3f(targetEntity.getComponent(BlockComponent.class).getPosition());
        placementPos.y += 0.2f;

        logger.info("Created vehicle at {}", placementPos);

        entityManager.create(cartDefinition.prefab, new Vector3f(placementPos));
        event.consume();
    }

}
