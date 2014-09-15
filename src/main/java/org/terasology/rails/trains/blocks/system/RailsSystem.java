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
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
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
import java.util.Map;

@RegisterSystem
public class RailsSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    @In
    private BlockManager blockManager;
    @In
    private EntityManager entityManager;

    private final Logger logger = LoggerFactory.getLogger(RailsSystem.class);
    private Builder railBuilder;

    public void postBegin() {
        logger.info("Loading railway...");
        railBuilder = new Builder(entityManager);
        Map<EntityRef, Track> tracks = railBuilder.getTracks();

        int countBlocks = 0;
        for (EntityRef railBlock : entityManager.getEntitiesWith(TrainRailComponent.class)) {
            tracks.put(railBlock, new Track(railBlock));
            countBlocks++;
        }
        logger.info("Loaded " + countBlocks + " railway blocks.");
    }

    @ReceiveEvent(components = {RailBuilderComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {

        Track selectedTrack = null;
        float yaw = 0;
        Vector3f placementPos = null;
        boolean reverse = false;

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
            selectedTrack = onSelectRail(event, item);

            if (selectedTrack == null) {
                return;
            }

            Vector3f  hitPosition = event.getHitPosition();
            if (hitPosition != null) {
                logger.info("GO!");
                Vector3f startPosition = new Vector3f(selectedTrack.getStartPosition());
                Vector3f endPosition = new Vector3f(selectedTrack.getEndPosition());
                startPosition.sub(hitPosition);
                endPosition.sub(hitPosition);
                float distFromStart = startPosition.lengthSquared();
                float distFromend = endPosition.lengthSquared();

                logger.info("from start:" + distFromStart);
                logger.info("from end:" + distFromend);
                if ( distFromStart > distFromend && selectedTrack.getPrevTrack() == null) {
                    reverse = true;
                }
            }
        }


        RailBuilderComponent railBuilderComponent = item.getComponent(RailBuilderComponent.class);

        if (selectedTrack == null) {
            placementPos = new Vector3i(event.getTarget().getComponent(BlockComponent.class).getPosition()).toVector3f();
            placementPos.y += 0.6f;

            Vector3f direction = event.getDirection();
            direction.y = 0;
            Direction dir = Direction.inDirection(direction);


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
                    yaw = 0;
                    logger.info("FORWARD");
                    break;
                case BACKWARD:
                    logger.info("BACKWARD");
                    yaw = 180;
                    break;
            }
        }else{
            logger.info("Track is selected!");
        }

        switch (railBuilderComponent.type) {
            case LEFT:
                railBuilder.buildLeft(placementPos, selectedTrack, new Orientation(yaw, 0, 0), reverse);
                break;
            case RIGHT:
                railBuilder.buildRight(placementPos, selectedTrack, new Orientation(yaw, 0, 0), reverse);
                break;
            case UP:
                railBuilder.buildUp(placementPos, selectedTrack, new Orientation(yaw, 0, 0), reverse);
                break;
            case DOWN:
                railBuilder.buildDown(placementPos, selectedTrack, new Orientation(yaw, 0, 0), reverse);
                break;
            case STRAIGHT:
                railBuilder.buildStraight(placementPos, selectedTrack, new Orientation(yaw, 0, 0), reverse);
                break;
        }

        event.consume();
    }

    private Track onSelectRail(ActivateEvent event, EntityRef item) {
        EntityRef target = event.getTarget();
        //EntityRef player = event.getInstigator();

        if (!target.hasComponent(TrainRailComponent.class)) {
            return null;
        }

        return railBuilder.getTracks().get(target);
    }

    @Override
    public void update(float delta) {

    }
}
