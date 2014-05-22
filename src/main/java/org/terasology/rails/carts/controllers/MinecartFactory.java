package org.terasology.rails.carts.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.rails.carts.components.LocomotiveComponent;
import org.terasology.rails.carts.components.RailVehicleComponent;
import org.terasology.rails.carts.components.WheelComponent;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

public class MinecartFactory {

    private final Logger logger = LoggerFactory.getLogger(MinecartFactory.class);

    private EntityManager entityManager;

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
        EntityRef entity = null;
        entity = entityManager.create("rails:minecart", position);

        if (entity == null) {
            return null;
        }
        LocationComponent minecartLocation = entity.getComponent(LocationComponent.class);
        if (minecartLocation != null) {
            RailVehicleComponent railVehicle = entity.getComponent(RailVehicleComponent.class);
            railVehicle.isCreated = true;
            railVehicle.drive = 0;
            railVehicle.pathDirection = new Vector3f();
            railVehicle.direction = new Vector3f(1f, 1f, 1f);
            attachVehicle(entity, new Vector3f(-0.125f, -1.5f, 0.55f), 1f);
            attachVehicle(entity, new Vector3f(-0.125f, -1.5f, -0.55f), 1f);
            entity.saveComponent(railVehicle);
        }

        return entity;
    }

    private EntityRef createLocomotive(Vector3f position) {
        EntityRef entity = null;
        entity = entityManager.create("rails:loco", position);

        if (entity == null) {
            return null;
        }
        LocationComponent minecartLocation = entity.getComponent(LocationComponent.class);
        if (minecartLocation != null) {
            RailVehicleComponent railVehicle = entity.getComponent(RailVehicleComponent.class);
            railVehicle.isCreated = true;
            railVehicle.drive = 0;
            railVehicle.pathDirection = new Vector3f();
            railVehicle.direction = new Vector3f(1f, 1f, 1f);
            attachVehicle(entity, new Vector3f(-0.125f, -1.2f, 0.55f), 0.75f);
            attachVehicle(entity, new Vector3f(-0.125f, -1.2f, 0), 0.75f);
            attachVehicle(entity, new Vector3f(-0.125f, -1.2f, -0.55f), 0.75f);

            //add pipe
            EntityRef pipeEnity = entityManager.create("rails:pipe", position);
            Location.attachChild(entity, pipeEnity, new Vector3f(0.5f,1.5f,0), new Quat4f());
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
