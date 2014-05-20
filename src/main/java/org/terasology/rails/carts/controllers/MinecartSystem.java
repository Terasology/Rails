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
package org.terasology.rails.carts.controllers;

import com.bulletphysics.linearmath.QuaternionUtil;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.asset.Assets;
import org.terasology.audio.AudioManager;
import org.terasology.audio.events.PlaySoundEvent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnChangedComponent;
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
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.rails.carts.components.MinecartComponent;
import org.terasology.rails.carts.utils.MinecartHelper;
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
        for (EntityRef minecart : entityManager.getEntitiesWith(MinecartComponent.class)) {
            MinecartComponent minecartComponent = minecart.getComponent(MinecartComponent.class);
            if (minecartComponent.isCreated) {
                moveMinecart(minecart);
            }
        }
    }

    private MotionState getCurrentState(EntityRef minecart) {
        MotionState motionState;
        if (!moveStates.containsKey(minecart)) {
            motionState = new MotionState();
            motionState.minecartComponent = minecart.getComponent(MinecartComponent.class);
            moveStates.put(minecart, motionState);
        } else {
            motionState = moveStates.get(minecart);
        }
        return motionState;
    }

    private void moveMinecart(EntityRef minecart) {
        LocationComponent location = minecart.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBody = minecart.getComponent(RigidBodyComponent.class);
        MinecartComponent minecartComponent = minecart.getComponent(MinecartComponent.class);

        Vector3f position = location.getWorldPosition();
        Vector3f velocity = new Vector3f(rigidBody.velocity);
        int slopeFactor = 0;
        MotionState motionState = getCurrentState(minecart);
        motionState.angularFactor.set(rigidBody.angularFactor);

        if (!minecartComponent.pathDirection.equals(FREE_MOTION) || !minecartComponent.pathDirection.equals(LOCKED_MOTION)) {
            BlockInfo blockPossibleSlope = getBlockInDirection(position, new Vector3f(minecartComponent.direction.x , -1f, minecartComponent.direction.z), 1.2f);
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
                if (isSameBlock && !isLowSpeed(minecartComponent.drive, velocity.length()) && slopeFactor == 0 && !currentBlock.isCorner()) {
                    motionState.setCurrentState(minecartComponent.pathDirection, minecartComponent.direction, motionState.angularFactor, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_PATH);
                } else {
                    if (!isSameBlock) {
                        motionState.yawSign = 0;
                        minecartComponent.prevYaw = -1;
                    }
                    motionState.setCurrentBlockPosition(currentBlock.getBlockPosition().toVector3f());
                    moveDescriptor.calculateDirection(velocity, currentBlock, minecartComponent, motionState, position, slopeFactor);
                    motionState.setCurrentState(minecartComponent.pathDirection, minecartComponent.direction, LOCKED_MOTION, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_PATH);
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
                        playSound(minecart, velocity, minecartComponent.drive);
                        showSmoke(minecartComponent);
                    }
                }

                correctPositionAndRotation(minecart, currentBlock);
                minecart.send(new ChangeVelocityEvent(velocity));
            } else {
                minecartComponent.direction.y=0;
                motionState.setCurrentState(FREE_MOTION, minecartComponent.direction, FREE_MOTION, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_GROUND);
            }
        } else {
            minecartComponent.direction.y=0;
            motionState.setCurrentState(FREE_MOTION, minecartComponent.direction, FREE_MOTION, currentBlock.getBlockPosition(), MotionState.PositionStatus.ON_THE_AIR);
        }

        setAngularAndLinearFactors(minecart, rigidBody, minecartComponent.pathDirection, motionState.angularFactor);
        minecart.saveComponent(minecartComponent);
    }

    private void playSound(EntityRef minecart, Vector3f velocity, float drive) {

        long currentTime = time.getGameTimeInMs();
        if(!soundStack.containsKey(minecart)) {
            soundStack.put(minecart, currentTime);
        }

        long soundProgress = currentTime - soundStack.get(minecart);

        if ((velocity.z > 0.1 || velocity.x > 0.1) && (soundProgress == 0f||soundProgress>1000)) {
            Vector3f tv = new Vector3f(velocity);
            tv.y = 0;
            float p = drive * 0.01f;
            //float volume = ((tv.length() / p) * 0.01f)/10f;
           // logger.info("timr: " + soundProgress);
           // logger.info("volume: " + volume);
            //audioManager.
            minecart.send(new PlaySoundEvent(minecart, Assets.getSound("rails:vehicle"), 0.2f));
            soundStack.put(minecart, currentTime);
        }
    }


    private void correctPositionAndRotation(EntityRef entity, BlockInfo blockInfo) {
        MinecartComponent minecartComponent = entity.getComponent(MinecartComponent.class);
        MeshComponent mesh = entity.getComponent(MeshComponent.class);
        LocationComponent location = entity.getComponent(LocationComponent.class);
        MotionState motionState = getCurrentState(entity);
        Vector3f position = location.getWorldPosition();
        RigidBodyComponent rb = entity.getComponent(RigidBodyComponent.class);

        if (motionState == null) {
            return;
        }

        if (minecartComponent.isCreated && motionState.currentPositionStatus == MotionState.PositionStatus.ON_THE_PATH) {
            if (blockInfo.isIntersection()) {
                blockInfo.getBlock().setDirection(minecartComponent.pathDirection.x != 0 ? Side.LEFT : Side.FRONT);
            }

            position = setPositionOnTheRail(minecartComponent.pathDirection, blockInfo, position, mesh.mesh.getAABB().getMax(), motionState);

            Vector3f distance = new Vector3f(position);
            distance.sub(motionState.prevPosition);
            Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
            moveDescriptor.setYawOnPath(minecartComponent, motionState, blockInfo, distance);
            moveDescriptor.setPitchOnPath(minecartComponent, position, motionState, blockInfo);

            QuaternionUtil.setEuler(yawPitch, TeraMath.DEG_TO_RAD * minecartComponent.yaw, TeraMath.DEG_TO_RAD * minecartComponent.pitch, 0);

            motionState.prevPosition.set(position);

            location.setWorldPosition(position);
            location.setWorldRotation(yawPitch);

            entity.saveComponent(minecartComponent);
            entity.saveComponent(location);
        }
        rotateVehicles(rb.velocity, minecartComponent);
     }

    @ReceiveEvent(components = {ClientComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void updateVerticalMovement(VerticalMovementAxis event, EntityRef entity) {
        ClientComponent clientComponent = entity.getComponent(ClientComponent.class);
        LocationComponent location = clientComponent.character.getComponent(LocationComponent.class);
        if (!location.getParent().equals(EntityRef.NULL)&&location.getParent().hasComponent(MinecartComponent.class)) {
            EntityRef minecart = location.getParent();
            minecart.send(new ActivateEvent(minecart,clientComponent.character));
            event.consume();
        }
    }

    @ReceiveEvent(components = {ClientComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void updateForwardsMovement(ForwardsMovementAxis event, EntityRef entity) {
        ClientComponent clientComponent = entity.getComponent(ClientComponent.class);
        LocationComponent location = clientComponent.character.getComponent(LocationComponent.class);
        if (!location.getParent().equals(EntityRef.NULL)&&location.getParent().hasComponent(MinecartComponent.class)) {

            EntityRef minecart = location.getParent();
            MinecartComponent minecartComponent = minecart.getComponent(MinecartComponent.class);

            MotionState motionState = moveStates.get(minecart);

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
            minecartComponent.drive += value * minecartComponent.changeDriveByStep;

            if (minecartComponent.drive > minecartComponent.maxDrive)  {
                minecartComponent.drive = minecartComponent.maxDrive;
            } else if(minecartComponent.drive < 0) {
                minecartComponent.drive = 0;
            }

            if (minecartComponent.drive > 0) {
                Vector3f velocity = new Vector3f(minecartComponent.drive, 0, minecartComponent.drive);
                velocity.x *= minecartComponent.direction.x;
                velocity.z *= minecartComponent.direction.z;
                if ( velocity.length() > 0 ) {
                    minecart.send(new ChangeVelocityEvent(velocity));
                }
            }
            minecart.saveComponent(minecartComponent);
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

    private void showSmoke(MinecartComponent minecartComponent) {
        if (minecartComponent.pipe != null) {
            BlockParticleEffectComponent particleEffectComponent = minecartComponent.pipe.getComponent(BlockParticleEffectComponent.class);
            if (minecartComponent.drive != 0) {
                particleEffectComponent.spawnCount = 20;
                particleEffectComponent.targetVelocity.set(minecartComponent.direction);
                particleEffectComponent.targetVelocity.negate();
                particleEffectComponent.targetVelocity.y = 0.7f;
                particleEffectComponent.acceleration.set(minecartComponent.pathDirection);
                particleEffectComponent.acceleration.y = 0.7f;
                particleEffectComponent.targetVelocity.scale(4f);
                particleEffectComponent.acceleration.scale(2.5f);
                minecartComponent.pipe.saveComponent(particleEffectComponent);
            } else {
                particleEffectComponent.targetVelocity.set(0,0,0);
            }
        }
    }

    private void rotateVehicles(Vector3f velocity, MinecartComponent minecartComponent) {
        velocity.y = 0;
        if (velocity.length() == 0) {
            return;
        }
        for (EntityRef vehicle : minecartComponent.vehicles) {
            LocationComponent locationComponent = vehicle.getComponent(LocationComponent.class);
            if (locationComponent == null) {
                continue;
            }
            Quat4f rotate = new Quat4f(0, 0, 0, 1);

            float yawSide = Math.round(minecartComponent.yaw/90f);
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

    private Vector3f setPositionOnTheRail(Vector3f direction, BlockInfo block, Vector3f position, Vector3f minecartMaxExtends, MotionState motionState) {
        Vector3f fixedPosition = new Vector3f(position);
        if (direction.x == 0 || direction.z == 0) {
            if (direction.z != 0) {
                fixedPosition.x = block.getBlockPosition().x;
            } else {
                fixedPosition.z = block.getBlockPosition().z;
            }
        }

        fixedPosition.y = block.hitPoint().y + minecartMaxExtends.y/2 + 0.05f;

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
