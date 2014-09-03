/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rails.minecarts.controllers;

import com.bulletphysics.linearmath.QuaternionUtil;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.asset.Assets;
import org.terasology.audio.events.PlaySoundEvent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.*;
import org.terasology.input.binds.movement.ForwardsMovementAxis;
import org.terasology.input.binds.movement.VerticalMovementAxis;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.particles.BlockParticleEffectComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.Side;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.network.ClientComponent;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.events.ChangeVelocityEvent;
import org.terasology.rails.minecarts.blocks.ConnectsToRailsComponent;
import org.terasology.rails.minecarts.components.RailVehicleComponent;
import org.terasology.rails.minecarts.utils.MinecartHelper;
import org.terasology.registry.In;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.Map;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MinecartSystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private Physics physics;
    @In
    private LocalPlayer localPlayer;
    @In
    private InventoryManager inventoryManager;
    @In
    private org.terasology.engine.Time time;


    private MoveDescriptor moveDescriptor;
    private Map<EntityRef, Long> soundStack = Maps.newHashMap();
    private Map<EntityRef, MotionState> moveStates = Maps.newHashMap();
    private final Logger logger = LoggerFactory.getLogger(MinecartSystem.class);
    private static final Vector3f FREE_MOTION   = new Vector3f(1f, 1f, 1f);
    private static final Vector3f LOCKED_MOTION = new Vector3f(0f, 0f, 0f);
    private static final Vector3f UNDER_MINECART_DIRECTION = new Vector3f(0f, -1f, 0f);

    @Override
    public void initialise() {
        moveDescriptor = new MoveDescriptor();
    }

    @Override
    public void update(float delta) {
        for (EntityRef railVehicle : entityManager.getEntitiesWith(RailVehicleComponent.class)) {
            RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);
            if (railVehicleComponent.isCreated) {
                moveRailVehicle(railVehicle);
            }
        }
    }

    private MotionState getCurrentState(EntityRef railVehicle) {
        MotionState motionState;
        if (!moveStates.containsKey(railVehicle)) {
            motionState = new MotionState();
            motionState.railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);
            moveStates.put(railVehicle, motionState);
        } else {
            motionState = moveStates.get(railVehicle);
        }
        return motionState;
    }

    private void moveRailVehicle(EntityRef railVehicle) {
        LocationComponent location = railVehicle.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBody = railVehicle.getComponent(RigidBodyComponent.class);
        RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);

        Vector3f position = location.getWorldPosition();
        Vector3f velocity = new Vector3f(rigidBody.velocity);
        int slopeFactor = 0;
        MotionState motionState = getCurrentState(railVehicle);
        motionState.angularFactor.set(rigidBody.angularFactor);

        if (!railVehicleComponent.pathDirection.equals(FREE_MOTION) || !railVehicleComponent.pathDirection.equals(LOCKED_MOTION)) {
            BlockInfo blockPossibleSlope = getBlockInDirection(position, new Vector3f(railVehicleComponent.direction.x , -1f, railVehicleComponent.direction.z), 1.2f);
            if (blockPossibleSlope.isRails() && blockPossibleSlope.isSlope()) {
                slopeFactor = 1;
                motionState.nextBlockIsSlope = true;
            }
        }

        BlockInfo currentBlock = getBlockInDirection(position, UNDER_MINECART_DIRECTION, 3.3f);

        if (!currentBlock.isEmptyBlock()) {
            if (slopeFactor == 0 && currentBlock.isRails() && currentBlock.isSlope()) {
                Vector3f distance = new Vector3f(currentBlock.getBlockPosition().x, currentBlock.getBlockPosition().y, currentBlock.getBlockPosition().z);
                distance.sub(motionState.prevBlockPosition);
                if (distance.y < 0) {
                    slopeFactor = -1;
                } else {
                    slopeFactor = 1;
                    motionState.nextBlockIsSlope = false;
                }
            }

            if (slopeFactor == 0) {
                motionState.nextBlockIsSlope = false;
            }

            if (currentBlock.isRails()) {
                boolean isSameBlock = currentBlock.isSameBlock(motionState.currentBlockPosition);
                if (isSameBlock && !isLowSpeed(railVehicleComponent.drive, velocity.length()) && slopeFactor == 0 && !currentBlock.isCorner() && railVehicleComponent.parentNode == null) {
                    motionState.setCurrentState(railVehicleComponent.pathDirection, railVehicleComponent.direction, motionState.angularFactor, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_PATH);
                } else {
                    if (!isSameBlock) {
                        motionState.yawSign = 0;
                        railVehicleComponent.prevYaw = -1;
                    }

                    motionState.setCurrentBlockPosition(currentBlock.getBlockPosition().toVector3f());
                    moveDescriptor.calculateDirection(velocity, currentBlock, railVehicleComponent, motionState, position, slopeFactor);
                    motionState.setCurrentState(railVehicleComponent.pathDirection, railVehicleComponent.direction, LOCKED_MOTION, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_PATH);
                    if (motionState.prevBlockPosition.length() > 0) {
                        Vector3i prevBlockPostion = new Vector3i(motionState.prevBlockPosition);

                        if (velocity.y > 0 && slopeFactor < 1) {
                            Block prevblock = worldProvider.getBlock(prevBlockPostion);
                            EntityRef prevBlockEntity = prevblock.getEntity();
                            ConnectsToRailsComponent prevBlockRailsComponent = prevBlockEntity.getComponent(ConnectsToRailsComponent.class);

                            if (prevBlockRailsComponent != null && prevBlockRailsComponent.type == ConnectsToRailsComponent.RAILS.SLOPE) {
                                velocity.y *= -1;
                            }
                        }
                    }
                    if (!isSameBlock) {
                        //playSound(railVehicle, velocity, railVehicleComponent.drive);
                        showSmoke(railVehicleComponent);
                    }
                }

                correctPositionAndRotation(railVehicle, currentBlock);
                railVehicle.send(new ChangeVelocityEvent(velocity));
            } else {
                railVehicleComponent.direction.y=0;
                motionState.setCurrentState(FREE_MOTION, railVehicleComponent.direction, FREE_MOTION, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_GROUND);
            }
        } else {
            railVehicleComponent.direction.y=0;
            motionState.setCurrentState(FREE_MOTION, railVehicleComponent.direction, FREE_MOTION, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_AIR);
        }

        setAngularAndLinearFactors(railVehicle, rigidBody, railVehicleComponent.pathDirection, motionState.angularFactor);
        railVehicle.saveComponent(railVehicleComponent);
    }

    private void playSound(EntityRef railVehicle, Vector3f velocity, float drive) {

        long currentTime = time.getGameTimeInMs();
        if(!soundStack.containsKey(railVehicle)) {
            soundStack.put(railVehicle, currentTime);
        }

        long soundProgress = currentTime - soundStack.get(railVehicle);

        if ((velocity.z > 0.1 || velocity.x > 0.1) && (soundProgress == 0f||soundProgress>1000)) {
            Vector3f tv = new Vector3f(velocity);
            tv.y = 0;
            float p = drive * 0.01f;
            //float volume = ((tv.length() / p) * 0.01f)/10f;
           // logger.info("timr: " + soundProgress);
           // logger.info("volume: " + volume);
            //audioManager.
            railVehicle.send(new PlaySoundEvent(railVehicle, Assets.getSound("rails:vehicle"), 0.2f));
            soundStack.put(railVehicle, currentTime);
        }
    }

    private void correctPositionAndRotation(EntityRef entity, BlockInfo blockInfo) {
        RailVehicleComponent railVehicleComponent = entity.getComponent(RailVehicleComponent.class);
        MeshComponent mesh = entity.getComponent(MeshComponent.class);
        LocationComponent location = entity.getComponent(LocationComponent.class);
        MotionState motionState = getCurrentState(entity);
        Vector3f position = location.getWorldPosition();
        RigidBodyComponent rb = entity.getComponent(RigidBodyComponent.class);

        if (motionState == null) {
            return;
        }

        if (railVehicleComponent.isCreated && motionState.currentPositionStatus == MotionState.PositionStatus.ON_THE_PATH) {
            if (blockInfo.isIntersection()) {
                blockInfo.getBlock().setDirection(railVehicleComponent.pathDirection.x != 0 ? Side.LEFT : Side.FRONT);
            }

            position = setPositionOnTheRail(railVehicleComponent.pathDirection, blockInfo, position, mesh.mesh.getAABB().getMax(), motionState);

            Vector3f distance = new Vector3f(position);
            distance.sub(motionState.prevPosition);
            Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
            moveDescriptor.setYawOnPath(railVehicleComponent, motionState, blockInfo, distance);
            moveDescriptor.setPitchOnPath(railVehicleComponent, position, motionState, blockInfo);

            QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * railVehicleComponent.yaw, TeraMath.DEG_TO_RAD * railVehicleComponent.pitch, 0);

            motionState.prevPosition.set(position);

            location.setWorldPosition(position);
            location.setWorldRotation(yawPitch);

            entity.saveComponent(railVehicleComponent);
            entity.saveComponent(location);
        }
        rotateVehicles(rb.velocity, railVehicleComponent);
     }

    @ReceiveEvent(components = {ClientComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void updateVerticalMovement(VerticalMovementAxis event, EntityRef entity) {
        ClientComponent clientComponent = entity.getComponent(ClientComponent.class);
        LocationComponent location = clientComponent.character.getComponent(LocationComponent.class);
        if (!location.getParent().equals(EntityRef.NULL)&&location.getParent().hasComponent(RailVehicleComponent.class)) {
            EntityRef railVehicle = location.getParent();
            railVehicle.send(new ActivateEvent(railVehicle,clientComponent.character));
            event.consume();
        }
    }

    @ReceiveEvent(components = {ClientComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void updateForwardsMovement(ForwardsMovementAxis event, EntityRef entity) {
        ClientComponent clientComponent = entity.getComponent(ClientComponent.class);
        LocationComponent location = clientComponent.character.getComponent(LocationComponent.class);
        if (!location.getParent().equals(EntityRef.NULL)&&location.getParent().hasComponent(RailVehicleComponent.class)) {

            EntityRef railVehicle = location.getParent();
            RailVehicleComponent railVehicleComponent = railVehicle.getComponent(RailVehicleComponent.class);

            MotionState motionState = moveStates.get(railVehicle);

            if (motionState == null || motionState.currentBlockPosition.lengthSquared() == 0) {
                return;
            }
            Block block = worldProvider.getBlock(motionState.currentBlockPosition);
            if (block == null) {
                return;
            }
            EntityRef blockEntity = block.getEntity();
            if (blockEntity == null || !blockEntity.hasComponent(ConnectsToRailsComponent.class)) {
                return;
            }

            ConnectsToRailsComponent connectsToRailsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);

            if (!connectsToRailsComponent.type.equals(ConnectsToRailsComponent.RAILS.PLANE)) {
                return;
            }

            int value = (int) event.getValue();
            Vector3f viewDirection = localPlayer.getViewDirection();

            if (Math.abs(viewDirection.x) > Math.abs(viewDirection.z)) {
                viewDirection.z = 0;
            } else {
                viewDirection.x = 0;
            }
            railVehicleComponent.drive += value * railVehicleComponent.changeDriveByStep;

            if (railVehicleComponent.drive > railVehicleComponent.maxDrive)  {
                railVehicleComponent.drive = railVehicleComponent.maxDrive;
            } else if(railVehicleComponent.drive < 0) {
                railVehicleComponent.drive = 0;
            }

            if (railVehicleComponent.drive > 0) {
                Vector3f velocity = new Vector3f(railVehicleComponent.drive, 0, railVehicleComponent.drive);
                velocity.x *= railVehicleComponent.direction.x;
                velocity.z *= railVehicleComponent.direction.z;
                if ( velocity.length() > 0 ) {
                    railVehicle.send(new ChangeVelocityEvent(velocity));
                }
            }
            railVehicle.saveComponent(railVehicleComponent);
            event.consume();
        }
    }

    private boolean isLowSpeed(float drive, float velocitySpeed) {
        float driveSpeed = drive / 100;
        return (velocitySpeed / driveSpeed) < 90;
    }

    private BlockInfo getBlockInDirection(Vector3f from, Vector3f to, float length) {
        HitResult hit = physics.rayTrace(from, to, length, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
        Vector3i blockPosition = hit.getBlockPosition();
        Block block = null;
        EntityRef blockEntity = null;
        ConnectsToRailsComponent railsComponent = null;

        if (blockPosition != null) {
            block = worldProvider.getBlock(blockPosition);
            blockEntity = block.getEntity();
            railsComponent = blockEntity.getComponent(ConnectsToRailsComponent.class);
        }
        return new BlockInfo(block, blockPosition, blockEntity, railsComponent, hit.getHitPoint());
    }

    private void setAngularAndLinearFactors (EntityRef entity, RigidBodyComponent rigidBodyComponent, Vector3f linearFactor, Vector3f angularFactor) {
        boolean needSave = false;
        if (!linearFactor.equals(rigidBodyComponent.linearFactor)) {
            rigidBodyComponent.linearFactor.set(linearFactor);
            needSave = true;
        }
        if (!angularFactor.equals(rigidBodyComponent.angularFactor)) {
            rigidBodyComponent.angularFactor.set(angularFactor);
            needSave = true;
        }

        if (needSave) {
            entity.saveComponent(rigidBodyComponent);
        }
    }

    private void showSmoke(RailVehicleComponent railVehicleComponent) {
        if (railVehicleComponent.pipe != null) {
            BlockParticleEffectComponent particleEffectComponent = railVehicleComponent.pipe.getComponent(BlockParticleEffectComponent.class);
            if (railVehicleComponent.drive != 0) {
                particleEffectComponent.spawnCount = 20;
                particleEffectComponent.targetVelocity.set(railVehicleComponent.direction);
                particleEffectComponent.targetVelocity.negate();
                particleEffectComponent.targetVelocity.y = 0.7f;
                particleEffectComponent.acceleration.set(railVehicleComponent.pathDirection);
                particleEffectComponent.acceleration.y = 0.7f;
                particleEffectComponent.targetVelocity.scale(4f);
                particleEffectComponent.acceleration.scale(2.5f);
                railVehicleComponent.pipe.saveComponent(particleEffectComponent);
            } else {
                particleEffectComponent.targetVelocity.set(0,0,0);
            }
        }
    }

    private void rotateVehicles(Vector3f velocity, RailVehicleComponent railVehicleComponent) {
        velocity.y = 0;
        if (velocity.length() == 0) {
            return;
        }
        for (EntityRef vehicle : railVehicleComponent.vehicles) {
            LocationComponent locationComponent = vehicle.getComponent(LocationComponent.class);
            if (locationComponent == null) {
                continue;
            }
            Quat4f rotate = new Quat4f(0, 0, 0, 1);

            float yawSide = Math.round(railVehicleComponent.yaw/90f);
            float reverseSign = 1;
            if (yawSide > 1) {
                reverseSign = -1;
            }

            float angleSign = velocity.x >= 0 && velocity.z >= 0 ? 1 : -1;
            float angle = reverseSign*angleSign*(velocity.length()/MinecartHelper.TWO_PI) + QuaternionUtil.getAngle(locationComponent.getLocalRotation());
            if (angle > MinecartHelper.TWO_PI) {
                angle = 0;
            } else if ( angle < 0 ) {
                angle = MinecartHelper.TWO_PI;
            }

            QuaternionUtil.setRotation(rotate, new Vector3f(1, 0, 0), angle);
            locationComponent.setLocalRotation(rotate);
            vehicle.saveComponent(locationComponent);
        }
    }

    private Vector3f setPositionOnTheRail(Vector3f direction, BlockInfo block, Vector3f position, Vector3f railVehicleMaxExtends, MotionState motionState) {
        Vector3f fixedPosition = new Vector3f(position);
        if (direction.x == 0 || direction.z == 0) {
            if (direction.z != 0) {
                fixedPosition.x = block.getBlockPosition().x;
            } else {
                fixedPosition.z = block.getBlockPosition().z;
            }
        }

        fixedPosition.y = block.hitPoint().y + railVehicleMaxExtends.y/2 + 0.05f;

       // }
        /*if (!block.isSlope() && !motionState.nextBlockIsSlope) {
            float halfHeight = minecartMaxExtends.y/2;
            float maxBlockHeight = block.getBlockPosition().y + block.getBlock().getCollisionOffset().y;

           // if ((maxBlockHeight + halfHeight)>position.y) {
                fixedPosition.y = maxBlockHeight + halfHeight   ;
         //   }
        }*/

        return fixedPosition;
    }
}
