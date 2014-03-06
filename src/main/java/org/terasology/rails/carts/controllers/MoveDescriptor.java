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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.rails.carts.components.MinecartComponent;

import javax.vecmath.Vector3f;
import java.util.HashMap;
import java.util.Map;

public class MoveDescriptor {

    protected float groundFriction = 0.7f;
    protected float pathFriction = 0.25f;
    protected float airFriction = 0.25f;

    protected float maxGroundSpeed = 5.0f;
    protected float maxPathSpeed = 10.0f;
    protected float maxAirSpeed = 25.0f;
    protected float maxLiquidSpeed = 10f;

    private final Logger logger = LoggerFactory.getLogger(MoveDescriptor.class);

    private int angleSign = 1;

    public void calculateDirection(Vector3f blockPosition, Vector3f velocity, Vector3f direction, ConnectsToRailsComponent.RAILS blockType, Side side, MinecartComponent minecart) {
        boolean isCorner = false;
        switch (blockType) {
            case PLANE:
            case INTERSECTION:
                side = side.yawClockwise(1);
            case SLOPE:
                minecart.pathDirection = side.getVector3i().toVector3f();
                minecart.pathDirection.absolute();
                logger.info("pathDirection:" + minecart.pathDirection);
                break;
            case CURVE:
                isCorner = true;
                setCornerDirection(side, minecart, direction);
                break;
            case TEE:
                break;
        }

        correctVelocity(minecart, velocity, isCorner);

    }

    public float getYawOnPath(Side side, float currentYaw) {
        float yaw = 0;

        switch (side) {
            case LEFT:
                if (currentYaw == 0) {
                    yaw = 180;
                }
            case RIGHT:
                if (currentYaw >= 360 || currentYaw < 45 && currentYaw > 0) {
                    yaw = 0;
                }

                if (currentYaw >= 135 && currentYaw < 180 || currentYaw > 180 && currentYaw < 225) {
                    yaw = 180;
                }

                if (currentYaw != 180 && currentYaw != 0) {
                    yaw = 0;
                }
                break;
            case FRONT:
                if (currentYaw == 0) {
                    yaw = 270;
                }
            case BACK:
                if (currentYaw == 0 || currentYaw >= 45 && currentYaw < 90 || currentYaw > 90 && currentYaw < 135) {
                    yaw = 90;
                    break;
                }

                if (currentYaw >= 225 && currentYaw < 270 || currentYaw > 270 && currentYaw < 315) {
                    yaw = 270;
                }

                if (currentYaw != 90 && currentYaw != 270) {
                    yaw = 90;
                }

                break;
        }
        return yaw;

    }

    private void setCornerDirection(Side side, MinecartComponent minecart, Vector3f direction) {
        logger.info("Corner Direction:" + direction);

        if (Math.abs(direction.x) < Math.abs(direction.z)) {
            direction.x = 0;
        } else if (Math.abs(direction.x) > Math.abs(direction.z)) {
            direction.z = 0;
        }

        switch (side) {
            case LEFT:
                if (direction.x > 0) {
                    angleSign = 1;
                    minecart.pathDirection.set(1f, 0, 1f);
                } else if (direction.z < 0) {
                    angleSign = -1;
                    minecart.pathDirection.set(-1f, 0, -1f);
                } else if (direction.x < 0) {
                    minecart.pathDirection.set(-1f, 0, 0);
                } else if (direction.z > 0) {
                    minecart.pathDirection.set(0, 0, 1f);
                }
                break;
            case RIGHT:
                if (direction.x > 0) {
                    minecart.pathDirection.set(1f, 0, 0f);
                } else if (direction.z < 0) {
                    minecart.pathDirection.set(0, 0, -1f);
                } else if (direction.x < 0) {
                    angleSign = 1;
                    minecart.pathDirection.set(-1f, 0, -1f);
                } else if (direction.z > 0) {
                    angleSign = -1;
                    minecart.pathDirection.set(1f, 0, 1f);
                }
                break;
            case FRONT:
                if (direction.x > 0) {
                    angleSign = -1;
                    minecart.pathDirection.set(1f, 0, -1f);
                } else if (direction.z < 0) {
                    minecart.pathDirection.set(0, 0, -1f);
                } else if (direction.x < 0) {
                    minecart.pathDirection.set(0, 0, -1f);
                } else if (direction.z > 0) {
                    angleSign = 1;
                    minecart.pathDirection.set(-1f, 0, 1f);
                }
                break;
            case BACK:
                if (direction.x > 0) {
                    minecart.pathDirection.set(-1f, 0, 0);
                } else if (direction.z < 0) {
                    angleSign = 1;
                    minecart.pathDirection.set(1f, 0, -1f);
                } else if (direction.x < 0) {
                    angleSign = -1;
                    minecart.pathDirection.set(-1, 0, 1f);
                } else if (direction.z > 0) {
                    minecart.pathDirection.set(0, 0, 1f);
                }
                break;
        }
    }

    private void correctVelocity(MinecartComponent minecartComponent, Vector3f velocity, boolean isCorner) {
        //Vector3f desiredVelocity = new Vector3f(minecartComponent.drive);

        if (minecartComponent.drive.length() > 0) {
            logger.info("drive:" + minecartComponent.drive);
        }
        if (isCorner) {
            velocity.x = velocity.x * minecartComponent.pathDirection.x;
            velocity.z = velocity.z * minecartComponent.pathDirection.z;
            minecartComponent.drive.x = minecartComponent.drive.x * minecartComponent.pathDirection.x;
            minecartComponent.drive.z = minecartComponent.drive.z * minecartComponent.pathDirection.z;

            velocity.absolute();
            minecartComponent.drive.absolute();

            if (velocity.x > velocity.z) {
                velocity.z = velocity.x;
                minecartComponent.drive.z = minecartComponent.drive.x;
            } else {
                velocity.x = velocity.z;
                minecartComponent.drive.x = minecartComponent.drive.z;
            }

        } else {
            velocity.x = velocity.x * minecartComponent.pathDirection.x;
            velocity.z = velocity.z * minecartComponent.pathDirection.z;
            minecartComponent.drive.x = minecartComponent.drive.x * minecartComponent.pathDirection.x;
            minecartComponent.drive.z = minecartComponent.drive.z * minecartComponent.pathDirection.z;
        }

        if ((minecartComponent.drive.lengthSquared() - velocity.lengthSquared()) > 0.1) {
            velocity.interpolate(minecartComponent.drive, 0.2f);
        }

    }

    public float getMaxSpeed(MinecartComponent.PositionStatus currentPositionStatus) {

        switch (currentPositionStatus) {
            case ON_THE_AIR:
                return maxAirSpeed;
            case ON_THE_GROUND:
                return maxGroundSpeed;
            case ON_THE_PATH:
                return maxPathSpeed;
            case ON_THE_LIQUID:
                return maxLiquidSpeed;
        }

        return maxGroundSpeed;

    }

    public float getFriction(MinecartComponent.PositionStatus currentPositionStatus) {

        switch (currentPositionStatus) {
            case ON_THE_AIR:
                return airFriction;
            case ON_THE_GROUND:
                return groundFriction;
            case ON_THE_PATH:
                return pathFriction;
        }

        return groundFriction;

    }

    public float getPitch(ConnectsToRailsComponent.RAILS blockType) {

        float pitch = 0;

        if (blockType.equals(ConnectsToRailsComponent.RAILS.SLOPE)) {
            pitch = 45;
        }
        return pitch;
    }

    /*public float getYawOnCorner(Vector3f distanceMoved) {
        float percent = distanceMoved.length() / 0.007f;

        if (percent > 100) {
            yaw += angleSign * 90f;
        } else {
            yaw += angleSign * 90f * percent / 100;
        }

        if (yaw < 0) {
            yaw = 360 + yaw;
        } else if (yaw > 360) {
            yaw = yaw - 360;
        }

        return yaw;
    } */
}
