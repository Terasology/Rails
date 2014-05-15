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
package org.terasology.rails.carts.controllers;

import com.bulletphysics.linearmath.QuaternionUtil;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.math.Side;
import org.terasology.math.TeraMath;
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.rails.carts.components.MinecartComponent;
import org.terasology.rails.carts.utils.MinecartHelper;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoveDescriptor {

    private final Logger logger = LoggerFactory.getLogger(MoveDescriptor.class);

    public void calculateDirection(Vector3f velocity, BlockInfo blockInfo, MinecartComponent minecart, MotionState motionState, Vector3f position, int slopeFactor) {
        Side side = correctSide(blockInfo);
        switch (blockInfo.getType()) {
            case SLOPE:
                minecart.pathDirection = getDirectPath(side);
                break;
            case PLANE:
                minecart.pathDirection = getDirectPath(side);
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
                setCornerDirection(side, minecart, motionState, position);
                break;
        }

        minecart.pathDirection.y = 1;
        if (slopeFactor != 0) {
            minecart.direction.y = slopeFactor;
        } else {
            minecart.direction.y = 0;
        }

        correctVelocity(minecart, velocity, blockInfo);
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

    public void setYawOnPath(MinecartComponent minecart, MotionState motionState, BlockInfo blockInfo, Vector3f distanceMoved) {
        Side side = correctSide(blockInfo);
        if (blockInfo.isCorner() && distanceMoved != null) {
            if (minecart.prevYaw == -1) {
                minecart.prevYaw = minecart.yaw;
            }
            distanceMoved.y = 0;
            float percent = distanceMoved.length() / 0.01f;
            minecart.yaw += motionState.yawSign * 90f * percent / 100;
            if ((motionState.yawSign > 0 && minecart.yaw > minecart.prevYaw + 90f) || (motionState.yawSign < 0 && minecart.yaw < minecart.prevYaw-90f)) {
                minecart.yaw = minecart.prevYaw + motionState.yawSign*90f;
            }

            if (minecart.yaw < 0) {
                minecart.yaw = 360 + minecart.yaw;
            } else if (minecart.yaw > 360) {
                minecart.yaw = minecart.yaw - 360;
            }
            return;
        }

        switch (side) {
            case FRONT:
            case BACK:
                minecart.yaw = 180 * Math.round(minecart.yaw / 180);
                break;
            case LEFT:
            case RIGHT:
                minecart.yaw = 90 * Math.round(minecart.yaw / 90);
                if (minecart.yaw == 0) {
                    minecart.yaw = 90;
                }
                break;
        }

        if (minecart.yaw >= 360) {
            minecart.yaw = 360 - minecart.yaw;
        }
    }

    private Vector3f getDirectPath(Side side) {
        Vector3f directPath = side.getVector3i().toVector3f();
        directPath.absolute();
        return directPath;
    }

    private void setCornerDirection(Side side, MinecartComponent minecart, MotionState motionState, Vector3f position) {
        boolean rotate = false;
        boolean checkBouds = false;
        switch (side) {
            case LEFT:
                if (minecart.direction.x > 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                } else if (minecart.direction.z < 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                }
                break;
            case RIGHT:
                if (minecart.direction.x < 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                } else if (minecart.direction.z > 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                }
                break;
            case FRONT:
                if (minecart.direction.x > 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                } else if (minecart.direction.z > 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                }
                break;
            case BACK:
                if (minecart.direction.z < 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                } else if (minecart.direction.x < 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                }
                break;
        }

        if (checkBouds) {
            if (minecart.direction.x != 0) {
                if (minecart.direction.x > 0 && motionState.currentBlockPosition.x  < position.x) {
                    rotate = true;
                } else if (minecart.direction.x < 0 && motionState.currentBlockPosition.x  > position.x) {
                    rotate = true;
                }
            } else if (minecart.direction.z != 0) {
                if (minecart.direction.z > 0 && motionState.currentBlockPosition.z  < position.z) {
                    rotate = true;
                } else if (minecart.direction.z < 0 && motionState.currentBlockPosition.z  > position.z) {
                    rotate = true;
                }
            }
        }

        if (rotate) {
            rotatePathDirection(minecart.direction, motionState.yawSign);
        }

        minecart.pathDirection.set(minecart.direction);
        minecart.pathDirection.absolute();
    }

    private void rotatePathDirection(Vector3f dir, int angle) {
        if (angle != 0) {
            Vector3f newDir = new Vector3f();
            newDir.x = angle * dir.z;
            newDir.z = -1 * angle * dir.x;
            dir.set(newDir);
        }
    }

    private void correctVelocity(MinecartComponent minecartComponent, Vector3f velocity, BlockInfo blockInfo) {

        if (blockInfo.isCorner()) {
            if (velocity.x != 0) {
                velocity.z = velocity.x;
            } else {
                velocity.x = velocity.z;
            }
        }

        if (minecartComponent.direction.x != 0) {
            minecartComponent.direction.z = minecartComponent.direction.x;
        } else {
            minecartComponent.direction.x = minecartComponent.direction.z;
        }

        MinecartHelper.setVectorToDirection(minecartComponent.direction, minecartComponent.pathDirection);

        velocity.absolute();
        MinecartHelper.setVectorToDirection(velocity, minecartComponent.direction);
        float speed = velocity.length();
        if (Math.abs((speed - minecartComponent.drive)) > 0.1f) {
            Vector3f drive = new Vector3f(minecartComponent.drive, minecartComponent.drive, minecartComponent.drive);
            drive.x *= minecartComponent.direction.x;
            drive.z *= minecartComponent.direction.z;
            velocity.interpolate(drive, 0.5f);
        }

        if (minecartComponent.direction.y != 0) {
            velocity.y =  minecartComponent.direction.y * Math.abs(minecartComponent.direction.x != 0 ? velocity.x : velocity.z);
        }
    }

    public void setPitchOnPath(MinecartComponent minecart, Vector3f position, MotionState motionState, BlockInfo blockInfo) {
        minecart.pitch = 0;

        if (blockInfo.isSlope() || motionState.nextBlockIsSlope) {

            float reverseSign = 1;
            if (minecart.direction.x < 0 || minecart.direction.z < 0) {
                reverseSign = -1;
            }

            float yawSide = Math.round(minecart.yaw/90f);
            if (yawSide > 1) {
                reverseSign = -reverseSign;
            }

            if (minecart.direction.y > 0) {
                minecart.pitch = -45f;
            } else {
                minecart.pitch = 45f;
            }

            if (motionState.nextBlockIsSlope) {
                float targetY = motionState.currentBlockPosition.y + 0.5f;
                float sourceY = position.y - 0.5f;

                targetY = targetY - sourceY;

                if (targetY > 1) {
                    targetY = 1;
                }
                float percent = 3.5f*(1 - targetY);

                if (percent > 1) {
                    percent = 1;
                }

                minecart.pitch = minecart.pitch * percent;

                if (minecart.pitch > 45 || minecart.pitch < -45) {
                    minecart.pitch = 45 * Math.signum(minecart.pitch);
                }
            }
            minecart.pitch *= reverseSign;
        }
    }
}
