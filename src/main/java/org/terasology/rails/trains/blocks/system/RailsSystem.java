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
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.input.cameraTarget.CameraTargetChangedEvent;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Direction;
import org.terasology.math.Vector3i;
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

@RegisterSystem
public class RailsSystem extends BaseComponentSystem {
    public static final float TRACK_LENGTH = 1f;
    public static final float STANDARD_ANGLE_CHANGE = 7.5f;
    public static final float STANDARD_PITCH_ANGLE_CHANGE = 7.5f;

    @In
    private BlockManager blockManager;
    @In
    private EntityManager entityManager;

    private final Logger logger = LoggerFactory.getLogger(RailsSystem.class);
    private Builder railBuilder;

    @ReceiveEvent(components = {RailBuilderComponent.class, ItemComponent.class})
    public void onPlaceFunctional(ActivateEvent event, EntityRef item) {

        if (!item.hasComponent(RailBuilderComponent.class)) {
            return;
        }

        RailBuilderComponent railBuilderComponent = item.getComponent(RailBuilderComponent.class);

        createRail(event.getTarget(), event.getDirection(), railBuilderComponent.type, false);
        event.consume();
    }

    @ReceiveEvent(components = {LocationComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onCamTargetChanged(CameraTargetChangedEvent event, EntityRef entity) {
        //EntityRef oldTarget = event.getOldTarget();
        EntityRef newTarget = event.getNewTarget();
        CharacterComponent characterComponent = entity.getComponent(CharacterComponent.class);
        EntityRef heldItem = InventoryUtils.getItemAt(entity, characterComponent.selectedItem);

        if (!heldItem.hasComponent(RailBuilderComponent.class)) {
            return;
        }

        RailBuilderComponent railBuilderComponent = heldItem.getComponent(RailBuilderComponent.class);
        createRail(newTarget, characterComponent.getLookDirection(), railBuilderComponent.type, true);

/*        if (oldTarget.hasComponent(RailVehicleComponent.class)) {
            RailVehicleComponent railVehicleComponent = oldTarget.getComponent(RailVehicleComponent.class);
            if (railVehicleComponent.type.equals(RailVehicleComponent.Types.minecart)) {
                setSelectMaterial(oldTarget, "rails:minecart");
            }
        }

        if (newTarget.hasComponent(RailVehicleComponent.class)) {
            RailVehicleComponent railVehicleComponent = newTarget.getComponent(RailVehicleComponent.class);
            if (railVehicleComponent.type.equals(RailVehicleComponent.Types.minecart)) {
                LocationComponent location = newTarget.getComponent(LocationComponent.class);
                if (railVehicleComponent.parentNode != null) {
                    setSelectMaterial(newTarget, "rails:minecart-unjoin");
                }else if (checkMineCartJoin(railVehicleComponent, location.getWorldPosition()) != null) {
                    setSelectMaterial(newTarget, "rails:minecart-join");
                }
            }
        }  */
    }

    private void createRail(EntityRef target, Vector3f direction, RailBuilderComponent.RailType type, boolean ghost) {
        float yaw = 0;
        Vector3f placementPos = null;
        boolean reverse = false;
        EntityRef selectedTrack = EntityRef.NULL;

        if (railBuilder == null) {
            railBuilder = new Builder(entityManager);
        }

        BlockComponent blockComponent = target.getComponent(BlockComponent.class);
        if (blockComponent == null) {
            if (!checkSelectRail(target)) {
                return;
            }
            selectedTrack = target;
        }


        if (selectedTrack.equals(EntityRef.NULL)) {
            placementPos = new Vector3i(target.getComponent(BlockComponent.class).getPosition()).toVector3f();
            placementPos.y += 0.65f;

            direction.y = 0;
            Direction dir = Direction.inDirection(direction);


            switch (dir) {
                case LEFT:
                    yaw = 90;
                    placementPos.x -= 0.5f;
                    break;
                case RIGHT:
                    yaw = 270;
                    placementPos.x += 0.5f;
                    break;
                case FORWARD:
                    yaw = 0;
                    placementPos.z -= 0.5f;
                    break;
                case BACKWARD:
                    placementPos.z += 0.5f;
                    yaw = 180;
                    break;
            }
        }

        switch (type) {
            case LEFT:
                railBuilder.buildLeft(placementPos, selectedTrack, new Orientation(yaw, 0, 0), ghost);
                break;
            case RIGHT:
                railBuilder.buildRight(placementPos, selectedTrack, new Orientation(yaw, 0, 0), ghost);
                break;
            case UP:
                railBuilder.buildUp(placementPos, selectedTrack, new Orientation(yaw, 0, 0), ghost);
                break;
            case DOWN:
                railBuilder.buildDown(placementPos, selectedTrack, new Orientation(yaw, 0, 0), ghost);
                break;
            case STRAIGHT:
                railBuilder.buildStraight(placementPos, selectedTrack, new Orientation(yaw, 0, 0), ghost);
                break;
        }
    }

    private boolean checkSelectRail(EntityRef target) {
        if (!target.hasComponent(TrainRailComponent.class)) {
            return false;
        }

        return true;
    }
}
