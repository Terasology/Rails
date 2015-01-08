/*
 * Copyright 2015 MovingBlocks
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
package org.terasology.rails.carts.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Side;
import org.terasology.math.geom.BaseVector3f;
import org.terasology.math.geom.Vector3f;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.rails.carts.components.RailVehicleComponent;
import org.terasology.rails.carts.utils.MinecartHelper;

public class MoveDescriptor {

    private final Logger logger = LoggerFactory.getLogger(MoveDescriptor.class);

    public void calculateDirection(Vector3f velocity, BlockInfo blockInfo, RailVehicleComponent railVehicleComponent,
                                   MotionState motionState, Vector3f position, int slopeFactor) {
        Side side = correctSide(blockInfo);

        switch (blockInfo.getType()) {
            case SLOPE:
                railVehicleComponent.pathDirection = getDirectPath(side);
                break;
            case PLANE:
                railVehicleComponent.pathDirection = getDirectPath(side);
                break;
            case INTERSECTION:
                Vector3f currentBlock = new Vector3f(motionState.currentBlockPosition);
                currentBlock.sub(motionState.prevBlockPosition);
                currentBlock.absolute();
                currentBlock.y = 0;
                break;
            case TEE_INVERSED:
            case TEE:
            case CURVE:
                setCornerDirection(side, railVehicleComponent, motionState, position);
                break;
        }

        railVehicleComponent.pathDirection.y = 1;
        if (slopeFactor != 0) {
            railVehicleComponent.direction.y = slopeFactor;
        } else {
            railVehicleComponent.direction.y = 0;
        }

        correctVelocity(railVehicleComponent, velocity, blockInfo, position);
    }

    public Side correctSide(BlockInfo blockInfo) {
        Side side = blockInfo.getBlock().getDirection();
        switch (blockInfo.getType()) {
            case PLANE:
                side = side.yawClockwise(1);
                break;
            case TEE:
                side = side.yawClockwise(-1);
                break;
        }
        return side;
    }

    public void setYawOnPath(RailVehicleComponent railVehicle, MotionState motionState, BlockInfo blockInfo, Vector3f distanceMoved) {
        Side side = correctSide(blockInfo);

        if (blockInfo.isCorner() && distanceMoved != null) {
            if (railVehicle.prevYaw == -1) {
                railVehicle.prevYaw = railVehicle.yaw;
            }
            distanceMoved.y = 0;
            float percent = distanceMoved.length() / 0.01f;
            railVehicle.yaw += motionState.yawSign * 90f * percent / 100;
            if ((motionState.yawSign > 0 && railVehicle.yaw > railVehicle.prevYaw + 90f) || (motionState.yawSign < 0 && railVehicle.yaw < railVehicle.prevYaw - 90f)) {
                railVehicle.yaw = railVehicle.prevYaw + motionState.yawSign * 90f;
            }

            if (railVehicle.yaw < 0) {
                railVehicle.yaw = 360 + railVehicle.yaw;
            } else if (railVehicle.yaw > 360) {
                railVehicle.yaw = railVehicle.yaw - 360;
            }
            return;
        }

        switch (side) {
            case FRONT:
            case BACK:
                railVehicle.yaw = 180 * Math.round(railVehicle.yaw / 180);
                break;
            case LEFT:
            case RIGHT:
                railVehicle.yaw = 90 * Math.round(railVehicle.yaw / 90);
                if (railVehicle.yaw == 0) {
                    railVehicle.yaw = 90;
                }
                break;
        }

        if (railVehicle.yaw >= 360) {
            railVehicle.yaw = 360 - railVehicle.yaw;
        }
    }

    private Vector3f getDirectPath(Side side) {
        Vector3f directPath = side.getVector3i().toVector3f();
        directPath.absolute();
        return directPath;
    }

    private void setCornerDirection(Side side, RailVehicleComponent railVehicle, MotionState motionState, Vector3f position) {
        boolean rotate = false;
        boolean checkBouds = false;
        switch (side) {
            case LEFT:
                if (railVehicle.direction.x > 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                } else if (railVehicle.direction.z < 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                }
                break;
            case RIGHT:
                if (railVehicle.direction.x < 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                } else if (railVehicle.direction.z > 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                }
                break;
            case FRONT:
                if (railVehicle.direction.x > 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                } else if (railVehicle.direction.z > 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                }
                break;
            case BACK:
                if (railVehicle.direction.z < 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                } else if (railVehicle.direction.x < 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                }
                break;
        }

        if (checkBouds) {
            if (railVehicle.direction.x != 0) {
                if (railVehicle.direction.x > 0 && motionState.currentBlockPosition.x  < position.x) {
                    rotate = true;
                } else if (railVehicle.direction.x < 0 && motionState.currentBlockPosition.x  > position.x) {
                    rotate = true;
                }
            } else if (railVehicle.direction.z != 0) {
                if (railVehicle.direction.z > 0 && motionState.currentBlockPosition.z  < position.z) {
                    rotate = true;
                } else if (railVehicle.direction.z < 0 && motionState.currentBlockPosition.z  > position.z) {
                    rotate = true;
                }
            }
        }

        if (rotate) {
            rotatePathDirection(railVehicle.direction, motionState.yawSign);
        }

        railVehicle.pathDirection.set(railVehicle.direction);
        railVehicle.pathDirection.absolute();
    }

    private void rotatePathDirection(Vector3f dir, int angle) {
        if (angle != 0) {
            Vector3f newDir = new Vector3f();
            newDir.x = angle * dir.z;
            newDir.z = -1 * angle * dir.x;
            dir.set(newDir);
        }
    }

    private void correctVelocity(RailVehicleComponent railVehicleComponent, Vector3f velocity, BlockInfo blockInfo, Vector3f position) {

        if (blockInfo.isCorner()) {
            if (velocity.x != 0) {
                velocity.z = velocity.x;
            } else {
                velocity.x = velocity.z;
            }
        }

        if (railVehicleComponent.direction.x != 0) {
            railVehicleComponent.direction.z = railVehicleComponent.direction.x;
        } else {
            railVehicleComponent.direction.x = railVehicleComponent.direction.z;
        }

        if (railVehicleComponent.parentNode != null) {
            LocationComponent parentLocation = railVehicleComponent.parentNode.getComponent(LocationComponent.class);
            Vector3f parentPos = parentLocation.getWorldPosition();
            Vector3f dir = new Vector3f(parentPos);
            dir.sub(position);
            dir.y = 0;
            float dirLength = dir.length();
            if (dir.length() > 1.46f) {
                RigidBodyComponent rb = railVehicleComponent.parentNode.getComponent(RigidBodyComponent.class);
                railVehicleComponent.direction.set(Math.signum(dir.x), railVehicleComponent.direction.y, Math.signum(dir.z));
                velocity.set(1, velocity.y, 1);
                velocity.scale(rb.velocity.length() * (dir.length() - 0.8f) + velocity.length());
            } else if (dir.length() < 1.2f) {
                RigidBodyComponent rb = railVehicleComponent.parentNode.getComponent(RigidBodyComponent.class);
                railVehicleComponent.direction.set(Math.signum(dir.x), railVehicleComponent.direction.y, Math.signum(dir.z));
                railVehicleComponent.direction.negate();
                velocity.set(1, velocity.y, 1);
                velocity.scale(velocity.length() * (dir.length() - 0.8f));
            }
        }

        MinecartHelper.setVectorToDirection(railVehicleComponent.direction, railVehicleComponent.pathDirection);

        velocity.absolute();
        float restoreLength = 0;
        if (railVehicleComponent.parentNode != null) {
            velocity.y = 0;
            restoreLength = Math.abs(velocity.x) > Math.abs(velocity.z) ? Math.abs(velocity.x) : Math.abs(velocity.z);
        }

        if (restoreLength == 0) {
            MinecartHelper.setVectorToDirection(velocity, railVehicleComponent.direction);
        } else {
            velocity.set(railVehicleComponent.direction);
            velocity.scale(restoreLength);
        }

        if (railVehicleComponent.drive > 0) {
            float speed = velocity.length();
            if (Math.abs((speed - railVehicleComponent.drive)) > 0.1f) {
                Vector3f drive = new Vector3f(railVehicleComponent.drive, railVehicleComponent.drive, railVehicleComponent.drive);
                drive.x *= railVehicleComponent.direction.x;
                drive.z *= railVehicleComponent.direction.z;
                velocity.set(BaseVector3f.lerp(velocity, drive, 0.5f));
            }
        }

        if (railVehicleComponent.direction.y != 0) {
            velocity.y =  railVehicleComponent.direction.y * Math.abs(railVehicleComponent.direction.x != 0 ? velocity.x : velocity.z);
        }

        if (railVehicleComponent.needRevertVelocity > 0) {
            velocity.negate();
            velocity.scale(0.7f);
            railVehicleComponent.needRevertVelocity--;
        }

    }

    public void setPitchOnPath(RailVehicleComponent railVehicle, Vector3f position, MotionState motionState, BlockInfo blockInfo) {
        railVehicle.pitch = 0;

        if (blockInfo.isSlope() || motionState.nextBlockIsSlope) {

            float reverseSign = 1;
            if (railVehicle.direction.x < 0 || railVehicle.direction.z < 0) {
                reverseSign = -1;
            }

            float yawSide = Math.round(railVehicle.yaw / 90f);
            if (yawSide > 1) {
                reverseSign = -reverseSign;
            }

            if (railVehicle.direction.y > 0) {
                railVehicle.pitch = -45f;
            } else {
                railVehicle.pitch = 45f;
            }

            if (motionState.nextBlockIsSlope) {
                float targetY = motionState.currentBlockPosition.y + 0.5f;
                float sourceY = position.y - 0.5f;

                targetY = targetY - sourceY;

                if (targetY > 1) {
                    targetY = 1;
                }
                float percent = 3.5f * (1 - targetY);

                if (percent > 1) {
                    percent = 1;
                }

                railVehicle.pitch = railVehicle.pitch * percent;

                if (railVehicle.pitch > 45 || railVehicle.pitch < -45) {
                    railVehicle.pitch = 45 * Math.signum(railVehicle.pitch);
                }
            }
            railVehicle.pitch *= reverseSign;
        }
    }
}
