package org.terasology.rails.carts.componentsystem.entityfactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.rails.carts.components.DynamicBlockComponent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.location.LocationComponent;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 * Created with IntelliJ IDEA.
 * User: Pencilcheck
 * Date: 12/22/12
 * Time: 6:14 AM
 * To change this template use File | Settings | File Templates.
 */
public class DynamicFactory {

    private static final Logger logger = LoggerFactory.getLogger(DynamicFactory.class);

    private EntityManager entityManager;

    public EntityRef generateDynamicBlock(Vector3f position, DynamicBlockComponent.DynamicType type) {
        EntityRef entity = null;
        switch (type) {
            case Minecart: {
                entity = entityManager.create("rails:train");
                break;
            }
            case Boat: {
                entity = entityManager.create("rails:boat");
                break;
            }
            default:
                entity = entityManager.create("rails:train");
        }
        if (entity == null)
            return null;

        LocationComponent loc = entity.getComponent(LocationComponent.class);
        if (loc != null) {

            DynamicBlockComponent dbc = entity.getComponent(DynamicBlockComponent.class);
            dbc.isCreated = true;

            if (type.equals(DynamicBlockComponent.DynamicType.Minecart)){
                dbc.vehicleFront = entityManager.create("rails:vehicle");
                dbc.vehicleBack  = entityManager.create("rails:vehicle");
                LocationComponent locationVehicleBack  = dbc.vehicleBack.getComponent(LocationComponent.class);
                locationVehicleBack.setLocalPosition(new Vector3f(0f, 0f, 1f));
                dbc.vehicleBack.saveComponent(locationVehicleBack);

                loc.getChildren().add(dbc.vehicleFront);
                loc.getChildren().add(dbc.vehicleBack);
            }

            entity.saveComponent(dbc);

            loc.setWorldPosition(position);
            loc.setWorldRotation(new Quat4f(0, 0, 0, 1));
            entity.saveComponent(loc);
        }

        return entity;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
