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
import org.terasology.input.Mouse;
import org.terasology.input.cameraTarget.CameraTargetChangedEvent;
import org.terasology.input.cameraTarget.CameraTargetSystem;
import org.terasology.input.events.MouseWheelEvent;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.events.InventorySlotChangedEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.Direction;
import org.terasology.math.Vector3i;
import org.terasology.network.ClientComponent;
import org.terasology.physics.Physics;
import org.terasology.rails.minecarts.components.RailVehicleComponent;
import org.terasology.rails.minecarts.components.WrenchComponent;
import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.rails.trains.blocks.system.Builder.Builder;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.components.RailBuilderComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.registry.In;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.BlockManager;

import javax.vecmath.Vector3f;

@RegisterSystem
public class RailsSystem extends BaseComponentSystem {
    @In
    private BlockManager blockManager;
    @In
    private EntityManager entityManager;
    @In
    private LocalPlayer localPlayer;

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

    @ReceiveEvent(components = ClientComponent.class, priority = EventPriority.PRIORITY_NORMAL)
    public void mouseWheelEvent(MouseWheelEvent event, EntityRef entity) {
        ClientComponent clientComponent = entity.getComponent(ClientComponent.class);
        EntityRef player = clientComponent.character;
        CharacterComponent characterComponent = player.getComponent(CharacterComponent.class);
        CameraTargetSystem ct = CoreRegistry.get(CameraTargetSystem.class);
        Railway.getInstance().removeChunk(Railway.GHOST_KEY);
        EntityRef heldItem = InventoryUtils.getItemAt(player, characterComponent.selectedItem);

        if (!heldItem.hasComponent(RailBuilderComponent.class)) {
            return;
        }

        RailBuilderComponent railBuilderComponent = heldItem.getComponent(RailBuilderComponent.class);
        createRail(ct.getTarget(), characterComponent.getLookDirection(), railBuilderComponent.type, true);
    }

    @ReceiveEvent(components = {LocationComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onCamTargetChanged(CameraTargetChangedEvent event, EntityRef entity) {
        EntityRef newTarget = event.getNewTarget();
        CharacterComponent characterComponent = entity.getComponent(CharacterComponent.class);
        EntityRef heldItem = InventoryUtils.getItemAt(entity, characterComponent.selectedItem);
        Railway.getInstance().removeChunk(Railway.GHOST_KEY);
        if (!heldItem.hasComponent(RailBuilderComponent.class)) {
            return;
        }

        RailBuilderComponent railBuilderComponent = heldItem.getComponent(RailBuilderComponent.class);
        createRail(newTarget, characterComponent.getLookDirection(), railBuilderComponent.type, true);
    }

    private void createRail(EntityRef target, Vector3f direction, RailBuilderComponent.RailType type, boolean preview) {
        float yaw = 0;
        Vector3f placementPos = new Vector3f();
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
        } else {
            placementPos = new Vector3i(target.getComponent(BlockComponent.class).getPosition()).toVector3f();
        }

        if (selectedTrack.hasComponent(TrainRailComponent.class)) {
            TrainRailComponent trainRailComponent = selectedTrack.getComponent(TrainRailComponent.class);
            if (trainRailComponent.chunkKey.equals(Railway.GHOST_KEY)) {
                return;
            }
        }


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

        switch (type) {
            case LEFT:
                railBuilder.buildLeft(placementPos, selectedTrack, new Orientation(yaw, 0, 0), preview);
                break;
            case RIGHT:
                railBuilder.buildRight(placementPos, selectedTrack, new Orientation(yaw, 0, 0), preview);
                break;
            case UP:
                railBuilder.buildUp(placementPos, selectedTrack, new Orientation(yaw, 0, 0), preview);
                break;
            case DOWN:
                railBuilder.buildDown(placementPos, selectedTrack, new Orientation(yaw, 0, 0), preview);
                break;
            case STRAIGHT:
                railBuilder.buildStraight(placementPos, selectedTrack, new Orientation(yaw, 0, 0), preview);
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
