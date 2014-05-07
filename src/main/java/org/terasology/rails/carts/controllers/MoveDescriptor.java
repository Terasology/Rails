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
                Vector3f direction =  new Vector3f(velocity);
                setCornerDirection(side, minecart, motionState, direction, position);
                break;
        }
        minecart.pathDirection.y = 1;
        correctVelocity(minecart, velocity, blockInfo, slopeFactor);
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

        minecart.prevYaw = -1;
        switch (side) {
            case FRONT:
            case BACK:
                if (minecart.yaw >= 360 || minecart.yaw < 45 && minecart.yaw > 0) {
                    minecart.yaw = 0;
                    motionState.pitchSign = 1;
                }

                if (minecart.yaw >= 135 && minecart.yaw < 180 || minecart.yaw > 180 && minecart.yaw < 225) {
                    minecart.yaw = 180;
                    motionState.pitchSign = -1;
                }

                if (minecart.yaw != 180 && minecart.yaw != 0) {
                    minecart.yaw = 0;
                    motionState.pitchSign = 1;
                }

                break;
            case LEFT:
            case RIGHT:
                if (minecart.yaw == 0 || minecart.yaw >= 45 && minecart.yaw < 90 || minecart.yaw > 90 && minecart.yaw < 135) {
                    minecart.yaw = 90;
                    motionState.pitchSign = 1;
                    break;
                }

                if (minecart.yaw >= 225 && minecart.yaw < 270 || minecart.yaw > 270 && minecart.yaw < 315) {
                    minecart.yaw = 270;
                    motionState.pitchSign = -1;
                }

                if (minecart.yaw != 90 && minecart.yaw != 270) {
                    minecart.yaw = 90;
                    motionState.pitchSign = 1;
                }
                break;
        }
    }

    private Vector3f getDirectPath(Side side) {
        Vector3f directPath = side.getVector3i().toVector3f();
        directPath.absolute();
        return directPath;
    }

    private void setCornerDirection(Side side, MinecartComponent minecart, MotionState motionState, Vector3f direction, Vector3f position) {
        boolean rotate = false;
        boolean checkBouds = false;
        switch (side) {
            case LEFT:
                if (direction.x > 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                   // logger.info("LEFT direction.x > 0");
                } else if (direction.z < 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                    //logger.info("LEFT direction.z < 0");
                }
                break;
            case RIGHT:
                if (direction.x < 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                  //  logger.info("RIGHT direction.x < 0");
                } else if (direction.z > 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                   // logger.info("RIGHT direction.z > 0");
                }
                break;
            case FRONT:
                if (direction.x > 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                    //logger.info("FRONT direction.x > 0");
                } else if (direction.z > 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                    //logger.info("FRONT direction.z > 0");
                }
                break;
            case BACK:
                if (direction.z < 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                   // logger.info("BACK direction.z < 0");
                } else if (direction.x < 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                  //  logger.info("BACK direction.x < 0");
                }
                break;
        }

        if (checkBouds) {
            if (direction.x != 0) {
                if (direction.x > 0 && motionState.currentBlockPosition.x  < position.x) {
                    rotate = true;
                } else if (direction.x < 0 && motionState.currentBlockPosition.x  > position.x) {
                    rotate = true;
                }
            } else if (direction.z != 0) {
                if (direction.z > 0 && motionState.currentBlockPosition.z  < position.z) {
                    rotate = true;
                } else if (direction.z < 0 && motionState.currentBlockPosition.z  > position.z) {
                    rotate = true;
                }
            }
        }

        minecart.pathDirection.set(1 * Math.signum(direction.x), 1 * Math.signum(direction.y), 1 * Math.signum(direction.z));
        if (rotate) {
            rotatePathDirection(minecart.pathDirection, motionState.yawSign);
        }
    }

    private void rotatePathDirection(Vector3f dir, int angle) {
        if (angle != 0) {
            Vector3f newDir = new Vector3f();
            newDir.x = angle * dir.z;
            newDir.z = -1 * angle * dir.x;
            dir.set(newDir);
        }
    }

    private void correctVelocity(MinecartComponent minecartComponent, Vector3f velocity, BlockInfo blockInfo, int slopeFactor) {

        if (blockInfo.isCorner()) {
            if ( velocity.x != 0 ) {
                velocity.z = velocity.x;
            } else {
                velocity.x = velocity.z;
            }
            velocity.absolute();
            MinecartHelper.setVectorToDirection(velocity, minecartComponent.pathDirection);
            minecartComponent.pathDirection.absolute();
        } else {
            MinecartHelper.setVectorToDirection(velocity, minecartComponent.pathDirection);
        }

        if ((minecartComponent.drive.lengthSquared() - velocity.lengthSquared()) > 0.1) {
            if ( minecartComponent.drive.x != 0 ) {
                minecartComponent.drive.z = minecartComponent.drive.x;
            } else {
                minecartComponent.drive.x = minecartComponent.drive.z;
            }
            minecartComponent.drive.absolute();
            minecartComponent.drive.x *= Math.signum(velocity.x) * minecartComponent.pathDirection.x;
            minecartComponent.drive.z *= Math.signum(velocity.z) * minecartComponent.pathDirection.z;
            velocity.interpolate(minecartComponent.drive, 0.5f);
        }

        if (slopeFactor != 0) {
            velocity.y = slopeFactor* Math.abs(minecartComponent.pathDirection.x !=0 ? velocity.x : velocity.z);
        }

    }

    public void setPitchOnPath(MinecartComponent minecart, Vector3f position, MotionState motionState, BlockInfo blockInfo) {
        Side side = correctSide(blockInfo);
        minecart.pitch = 0;

        Vector3f dir = new Vector3f(position);
        dir.sub(motionState.prevPosition);

        if (motionState.nextBlockIsSlope && (dir.x < 0 || dir.z > 0)) {
            side = side.reverse();
        }

        if (blockInfo.isSlope() || motionState.nextBlockIsSlope) {
            switch (side) {
                case LEFT:
                case BACK:
                    minecart.pitch = -45 * motionState.pitchSign;
                    break;
                case RIGHT:
                case FRONT:
                    minecart.pitch = 45 * motionState.pitchSign;
                    break;
            }

            if (motionState.nextBlockIsSlope) {
                float targetY = motionState.currentBlockPosition.y + 0.5f;
                float sourceY = position.y - 0.5f;

                targetY = targetY - sourceY;

                if ( targetY > 1 ) {
                    targetY = 1;
                }
                float percent = 3.5f*(1 - targetY);

                if (percent > 1) {
                    percent = 1;
                }

                minecart.pitch = minecart.pitch*percent;
                if (minecart.pitch > 45 || minecart.pitch < -45) {
                    minecart.pitch = 45*Math.signum(minecart.pitch);
                }
            }
        }
    }
}
