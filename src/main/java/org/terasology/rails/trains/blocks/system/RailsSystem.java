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
package org.terasology.rails.trains.blocks.system;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.Vector3i;
import org.terasology.rails.minecarts.blocks.ConnectsToRailsComponent;
import org.terasology.rails.minecarts.components.WrenchComponent;
import org.terasology.rails.trains.blocks.system.Builder.Builder;
import org.terasology.rails.trains.components.DebugTrainComponent;
import org.terasology.registry.In;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import javax.vecmath.Vector3f;

@RegisterSystem
public class RailsSystem extends BaseComponentSystem {
    @In
    private BlockManager blockManager;
    @In
    private EntityManager entityManager;

    private final Logger logger = LoggerFactory.getLogger(RailsSystem.class);
    private Builder railBuilder;

    public void initialise() {
        railBuilder = new Builder(entityManager);
    }

    @ReceiveEvent(components = {DebugTrainComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {

        if (item.hasComponent(WrenchComponent.class)) {
            return;
        }

        EntityRef targetEntity = event.getTarget();
        BlockComponent blockComponent = targetEntity.getComponent(BlockComponent.class);
        ConnectsToRailsComponent connectsToRailsComponent = targetEntity.getComponent(ConnectsToRailsComponent.class);

        if (blockComponent == null) {
            return;
        }

        Vector3f placementPos = new Vector3i(event.getTarget().getComponent(BlockComponent.class).getPosition()).toVector3f();
        placementPos.y += 0.6f;
        logger.info("Created debug rail at {}", placementPos);
        //entityManager.create("rails:railBlock", placementPos);
        railBuilder.buildLeft(placementPos);
        event.consume();
    }
}
