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
import org.terasology.math.Direction;
import org.terasology.math.Side;
import org.terasology.math.Vector3i;
import org.terasology.rails.minecarts.blocks.ConnectsToRailsComponent;
import org.terasology.rails.minecarts.components.RailVehicleComponent;
import org.terasology.rails.minecarts.components.WrenchComponent;
import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.rails.trains.blocks.system.Builder.Builder;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.components.RailBuilderComponent;
import org.terasology.registry.In;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import javax.vecmath.Vector3f;
import java.util.ArrayList;

@RegisterSystem
public class RailsSystem extends BaseComponentSystem {
    @In
    private BlockManager blockManager;
    @In
    private EntityManager entityManager;

    private final Logger logger = LoggerFactory.getLogger(RailsSystem.class);
    private Builder railBuilder;

    public void initialise() {
        logger.info("Loading railway...");
        railBuilder = new Builder(entityManager);
        ArrayList<Track> tracks = railBuilder.getTracks();

        int countBlocks = 0;
        for (EntityRef railBlock : entityManager.getEntitiesWith(TrainRailComponent.class)) {
            tracks.add(new Track(railBlock));
            countBlocks++;
        }
        logger.info("Loaded " + countBlocks + " railway blocks.");
    }

    @ReceiveEvent(components = {RailBuilderComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {

        if (railBuilder == null) {
            railBuilder = new Builder(entityManager);
            return;
        }

        if (item.hasComponent(WrenchComponent.class)) {
            return;
        }

        EntityRef targetEntity = event.getTarget();
        BlockComponent blockComponent = targetEntity.getComponent(BlockComponent.class);

        if (blockComponent == null) {
            onSelectRail(event, item);
            return;
        }

        Vector3f placementPos = new Vector3i(event.getTarget().getComponent(BlockComponent.class).getPosition()).toVector3f();
        placementPos.y += 0.6f;

        RailBuilderComponent railBuilderComponent = item.getComponent(RailBuilderComponent.class);

        Vector3f direction = event.getDirection();
        direction.y = 0;
        Direction dir = Direction.inDirection(direction);
        float yaw = 0;

        switch (dir) {
            case LEFT:
                yaw = 90;
                logger.info("LEFT");
                break;
            case RIGHT:
                yaw = 270;
                logger.info("RIGHT");
                break;
            case FORWARD:
                yaw = 180;
                logger.info("FORWARD");
                break;
            case BACKWARD:
                logger.info("BACKWARD");
                yaw = 0;
                break;
        }

        switch (railBuilderComponent.type) {
            case LEFT:
                railBuilder.buildLeft(placementPos, new Orientation(yaw, 0, 0));
                break;
            case RIGHT:
                railBuilder.buildRight(placementPos, new Orientation(yaw, 0, 0));
                break;
            case UP:
                railBuilder.buildUp(placementPos, new Orientation(yaw, 0, 0));
                break;
            case DOWN:
                railBuilder.buildDown(placementPos, new Orientation(yaw, 0, 0));
                break;
            case STRAIGHT:
                railBuilder.buildStraight(placementPos, new Orientation(yaw, 0, 0));
                break;
        }

        event.consume();
    }

    private void onSelectRail(ActivateEvent event, EntityRef item) {
        EntityRef target = event.getTarget();
        EntityRef player = event.getInstigator();

        if (!target.hasComponent(RailBuilderComponent.class)) {
            return;
        }

    }
}
