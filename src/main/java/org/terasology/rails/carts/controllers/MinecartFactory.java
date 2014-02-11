package org.terasology.rails.carts.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.asset.Assets;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.LocationComponent;
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

    public EntityRef createMinecart(Vector3f position, MinecartComponent.MinecartTType type) {
        EntityRef entity = null;
        switch (type) {
            case minecart: {
                entity = entityManager.create( "rails:minecart", position);
                break;
            }
        }
        if (entity == null)
            return null;

        LocationComponent loc = entity.getComponent(LocationComponent.class);
        if (loc != null) {

            MinecartComponent dbc = entity.getComponent(MinecartComponent.class);
            dbc.isCreated = true;

            /*if (type.equals(DynamicBlockComponent.DynamicType.Minecart)){
                dbc.vehicleFront = entityManager.create("dynamicBlocks:vehicle");
                dbc.vehicleBack  = entityManager.create("dynamicBlocks:vehicle");
                LocationComponent locationVehicleBack  = dbc.vehicleBack.getComponent(LocationComponent.class);
                locationVehicleBack.setLocalPosition(new Vector3f(0f, 0f, 1f));
                dbc.vehicleBack.saveComponent(locationVehicleBack);
                loc.addChild(dbc.vehicleFront, entity);
                loc.addChild(dbc.vehicleBack, entity);
            }     */

            entity.saveComponent(dbc);
        }

        return entity;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
