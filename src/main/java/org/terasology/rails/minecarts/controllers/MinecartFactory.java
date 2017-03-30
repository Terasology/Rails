/*
 * Copyright 2015 MovingBlocks
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
package org.terasology.rails.minecarts.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.rails.minecarts.components.RailVehicleComponent;
import org.terasology.rails.minecarts.components.WheelComponent;


public class MinecartFactory {
    private EntityManager entityManager;

    private final Logger logger = LoggerFactory.getLogger(MinecartFactory.class);

    public EntityRef create(Vector3f position, RailVehicleComponent.Types type) {
        EntityRef entity = null;
        switch (type) {
            case minecart: {
                entity = createMinecart(position);
                break;
            }

            case locomotive: {
                entity = createLocomotive(position);
                break;
            }
        }

        return entity;
    }

    private EntityRef createMinecart(Vector3f position) {
        EntityRef entity = entityManager.create("rails:minecartVehicle", position);


        if (entity == null) {
            return null;
        }
        LocationComponent minecartLocation = entity.getComponent(LocationComponent.class);
        if (minecartLocation != null) {
            RailVehicleComponent railVehicle = entity.getComponent(RailVehicleComponent.class);
            railVehicle.isCreated = true;
            //railVehicle.drive = 0;
            //railVehicle.pathDirection = new Vector3f();
            //railVehicle.direction = new Vector3f(1f, 1f, 1f);
            attachVehicle(entity, new Vector3f(-0.125f, -1.5f, 0.55f), 1f);
            attachVehicle(entity, new Vector3f(-0.125f, -1.5f, -0.55f), 1f);
            entity.saveComponent(railVehicle);
        }
        return entity;
    }

    private EntityRef createLocomotive(Vector3f position) {
        EntityRef entity = entityManager.create("rails:locoVehicle", position);

        if (entity == null) {
            return null;
        }
        LocationComponent minecartLocation = entity.getComponent(LocationComponent.class);
        if (minecartLocation != null) {
            RailVehicleComponent railVehicle = entity.getComponent(RailVehicleComponent.class);
            railVehicle.isCreated = true;
            //railVehicle.drive = 0;
           // railVehicle.pathDirection = new Vector3f();
            //railVehicle.direction = new Vector3f(1f, 1f, 1f);
            attachVehicle(entity, new Vector3f(-0.125f, -1.2f, 0.55f), 0.75f);
            attachVehicle(entity, new Vector3f(-0.125f, -1.2f, 0), 0.75f);
            attachVehicle(entity, new Vector3f(-0.125f, -1.2f, -0.55f), 0.75f);

            //add pipe
            EntityRef pipeEnity = entityManager.create("rails:pipe", position);
            Location.attachChild(entity, pipeEnity, new Vector3f(0.5f, 1.5f, 0), new Quat4f());
            railVehicle.pipe = pipeEnity;
            entity.saveComponent(railVehicle);
        }
        return entity;
    }

    public void attachVehicle(EntityRef railVehicleEntity, Vector3f position, float scale) {
        RailVehicleComponent railVehicle = railVehicleEntity.getComponent(RailVehicleComponent.class);
        EntityRef wheel = entityManager.create("rails:wheel");
        Location.attachChild(railVehicleEntity, wheel, position, new Quat4f());
        if (scale != 1) {
            LocationComponent locationComponent = wheel.getComponent(LocationComponent.class);
            locationComponent.setLocalScale(scale);
            wheel.saveComponent(locationComponent);
        }
        WheelComponent wheelComponent = wheel.getComponent(WheelComponent.class);
        wheelComponent.parent = railVehicleEntity;
        wheelComponent.position = position;
        wheelComponent.scale = scale;
        wheel.saveComponent(wheelComponent);
        railVehicle.vehicles.add(wheel);
        railVehicleEntity.saveComponent(railVehicle);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
