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

import com.bulletphysics.linearmath.QuaternionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.TeraMath;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.rails.trains.blocks.components.TrainComponent;
import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.registry.In;

import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
public class VehicleSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    @In
    private EntityManager entityManager;

    private final Logger logger = LoggerFactory.getLogger(VehicleSystem.class);
    @In
    private Physics physics;

    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(TrainComponent.class)) {

            if (false) {
                break;
            }
            TrainComponent railVehicleComponent = railVehicle.getComponent(TrainComponent.class);
            LocationComponent location = railVehicle.getComponent(LocationComponent.class);
            HitResult hit = physics.rayTrace(location.getWorldPosition(), new Vector3f(0,-1,0), 5f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);

            EntityRef rail = hit.getEntity();

            if (rail == null) {
                return;
            }

            if(!rail.hasComponent(TrainRailComponent.class)) {
                rail = railVehicleComponent.currentRailBlock;
            } else {
                if (railVehicleComponent.currentRailBlock == null) {
                    railVehicleComponent.currentRailBlock = rail;
                } else {
                    TrainRailComponent railComponent2 = railVehicleComponent.currentRailBlock.getComponent(TrainRailComponent.class);
                  /*  if (rail.equals(railComponent2.nextTrack)) {
                        railVehicleComponent.currentRailBlock = rail;
                    }*/
                }
            }

            TrainRailComponent railComponent = rail.getComponent(TrainRailComponent.class);

            EntityRef nextRail = null;//railComponent.nextTrack;


            if (nextRail == null) {
                logger.info("nextRail is empty");
                railVehicle.destroy();
                return;
            }

            LocationComponent toRail = nextRail.getComponent(LocationComponent.class);
            LocationComponent fromRail = rail.getComponent(LocationComponent.class);

            Vector3f dir = toRail.getWorldPosition();
            dir.sub(fromRail.getWorldPosition());

            dir.normalize();
            dir.scale(0.05f);
            logger.info("dir is " + dir);

            Vector3f position = location.getWorldPosition();
            //dir.y=0;
            position.add(dir);

            Quat4f yawPitch = new Quat4f(TeraMath.DEG_TO_RAD * (railComponent.yaw + 90),TeraMath.DEG_TO_RAD * (railComponent.pitch!=0?180 - railComponent.pitch:0),0);

            location.setWorldPosition(position);
            location.setWorldRotation(yawPitch);
            railVehicle.saveComponent(location);

        }
    }
}
