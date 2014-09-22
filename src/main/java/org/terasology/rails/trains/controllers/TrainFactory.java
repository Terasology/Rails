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

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.rails.minecarts.components.RailVehicleComponent;
import org.terasology.rails.minecarts.components.WheelComponent;
import org.terasology.rails.trains.blocks.components.TrainComponent;
import org.terasology.rails.trains.blocks.components.TrainCreaterComponent;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Created by adeon on 22.09.14.
 */
public class TrainFactory {
    private EntityManager entityManager;

    public EntityRef create(Vector3f position, TrainCreaterComponent.Trains train) {
        EntityRef entity = null;
        switch (train) {
            case CHEREPANOV_LOCOMOTIVE: {
                entity = createCherepanov(position);
                break;
            }
        }

        return entity;
    }

    private EntityRef createCherepanov(Vector3f position) {
        EntityRef entity = null;
        entity = entityManager.create("rails:cherepanov", position);

        if (entity == null) {
            return null;
        }

        LocationComponent trainLocation = entity.getComponent(LocationComponent.class);
        if (trainLocation != null) {
            TrainComponent trainComponent = entity.getComponent(TrainComponent.class);
            attachVehicle("rails:cherepanov_wheel", entity, new Vector3f(-0.125f, -1.5f, 0.55f), 1f);
            attachVehicle("rails:cherepanov_wheel", entity, new Vector3f(-0.125f, -1.5f, -0.55f), 1f);
            entity.saveComponent(trainComponent);
        }

        return entity;
    }

    public void attachVehicle(String prefabName, EntityRef railVehicleEntity, Vector3f position, float scale) {
        RailVehicleComponent railVehicle = railVehicleEntity.getComponent(RailVehicleComponent.class);
        EntityRef wheel = entityManager.create(prefabName);
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
