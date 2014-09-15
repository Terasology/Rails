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
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.rails.trains.blocks.components.DebugTrainComponent;
import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.registry.In;

import javax.vecmath.Vector3f;


public class VehicleSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    @In
    private EntityManager entityManager;

    private final Logger logger = LoggerFactory.getLogger(VehicleSystem.class);
    @In
    private Physics physics;

    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(DebugTrainComponent.class)) {
            DebugTrainComponent railVehicleComponent = railVehicle.getComponent(DebugTrainComponent.class);
            LocationComponent location = railVehicle.getComponent(LocationComponent.class);
            HitResult hit = physics.rayTrace(location.getWorldPosition(), new Vector3f(0,-1,0), 5f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);

            EntityRef rail = hit.getEntity();

            if (rail == null || !rail.hasComponent(TrainRailComponent.class)) {
                logger.info("somethink wrong");
                return;
            }

            TrainRailComponent railComponent = rail.getComponent(TrainRailComponent.class);

            EntityRef nextRail = railComponent.nextTrack;

            if (nextRail == null) {
                logger.info("nextRail is empty");
                return;
            }

            //LocationComponent fromRail = rail.getComponent(LocationComponent.class);
            LocationComponent toRail = nextRail.getComponent(LocationComponent.class);

            Vector3f dir = toRail.getWorldPosition();
            dir.sub(location.getWorldPosition());

            dir.normalize();
            dir.scale(0.1f);
            logger.info("dir is " + dir);

            Vector3f position = location.getWorldPosition();
            position.add(dir);

            location.setWorldPosition(position);
            railVehicle.saveComponent(location);

        }
    }
}
