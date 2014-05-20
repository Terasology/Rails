package org.terasology.rails.carts.controllers;

import com.bulletphysics.linearmath.QuaternionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Rotation;
import org.terasology.math.TeraMath;
import org.terasology.math.Yaw;
import org.terasology.rails.carts.components.MinecartComponent;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

public class MinecartFactory {

    private final Logger logger = LoggerFactory.getLogger(MinecartFactory.class);

    private EntityManager entityManager;

    public EntityRef create(Vector3f position, MinecartComponent.Types type) {
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
            MinecartComponent minecart = entity.getComponent(MinecartComponent.class);
            minecart.isCreated = true;
            minecart.drive = 0;
            minecart.pathDirection = new Vector3f();
            minecart.direction = new Vector3f(1f, 1f, 1f);
            attachVehicle(entity, minecart, new Vector3f(-0.125f, -1.5f, 0.55f), 1f);
            attachVehicle(entity, minecart, new Vector3f(-0.125f, -1.5f, -0.55f), 1f);
            entity.saveComponent(minecart);
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
            MinecartComponent minecart = entity.getComponent(MinecartComponent.class);
            minecart.isCreated = true;
            minecart.drive = 0;
            minecart.pathDirection = new Vector3f();
            minecart.direction = new Vector3f(1f, 1f, 1f);
            attachVehicle(entity, minecart, new Vector3f(-0.125f, -1.2f, 0.55f), 0.75f);
            attachVehicle(entity, minecart, new Vector3f(-0.125f, -1.2f, 0), 0.75f);
            attachVehicle(entity, minecart, new Vector3f(-0.125f, -1.2f, -0.55f), 0.75f);

            //add pipe
            EntityRef pipeEnity = entityManager.create("rails:pipe", position);
            Location.attachChild(entity, pipeEnity, new Vector3f(0,1,0), new Quat4f());
            minecart.pipe = pipeEnity;
            entity.saveComponent(minecart);
        }

        return entity;
    }

    private void attachVehicle(EntityRef minecartEntity, MinecartComponent minecart, Vector3f position, float scale) {
        EntityRef vehicle = entityManager.create("rails:vehicle");
        Location.attachChild(minecartEntity, vehicle, position, new Quat4f());
        if (scale != 1) {
            LocationComponent locationComponent = vehicle.getComponent(LocationComponent.class);
            locationComponent.setLocalScale(scale);
            vehicle.saveComponent(locationComponent);
        }
        minecart.vehicles.add(vehicle);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
