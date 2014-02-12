package org.terasology.rails.carts.controllers;

import com.bulletphysics.linearmath.QuaternionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.Location;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Rotation;
import org.terasology.math.Yaw;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.rails.carts.components.MinecartComponent;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Created with IntelliJ IDEA.
 * User: Pencilcheck
 * Date: 12/22/12
 * Time: 6:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class MinecartFactory {

    private static final Logger logger = LoggerFactory.getLogger(MinecartFactory.class);

    private EntityManager entityManager;

    public EntityRef create(Vector3f position, MinecartComponent.MinecartTType type) {
        EntityRef entity = null;
        switch (type) {
            case minecart: {
                entity = createMinecart(position);
                break;
            }
        }

        return entity;
    }

    private EntityRef createMinecart(Vector3f position){
        EntityRef entity = null;
        entity = entityManager.create( "rails:minecart", position);

        if (entity == null)
            return null;

        LocationComponent minecartLocation = entity.getComponent(LocationComponent.class);
        if (minecartLocation != null) {
            MinecartComponent minecart = entity.getComponent(MinecartComponent.class);
            minecart.isCreated = true;

            attachVehicle(entity, minecart, new Vector3f(-0.2f, -1.2f, 0f));
            attachVehicle(entity, minecart, new Vector3f(1.3f, -1.2f, 0f));

            entity.saveComponent(minecart);
        }

        return entity;
    }
    
    private void attachVehicle( EntityRef minecartEntity, MinecartComponent minecart, Vector3f position ){
        EntityRef entity = entityManager.create("rails:vehicle");
        LocationComponent locationVehicle   = entity.getComponent(LocationComponent.class);
        Location.attachChild(minecartEntity, entity);
        locationVehicle.setLocalRotation(Rotation.rotate(Yaw.CLOCKWISE_90).getQuat4f());
        locationVehicle.setLocalPosition(position);
        entity.saveComponent(locationVehicle);
        minecart.vehicles.add(entity);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
