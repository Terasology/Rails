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
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.rails.carts.components.MinecartComponent;
import javax.vecmath.Vector3f;
import java.util.HashMap;
import java.util.Map;

public class MoveDescriptor {

    private final Logger logger = LoggerFactory.getLogger(MoveDescriptor.class);
    private static final Map<Side, Vector3f> CORNER_ROTATE_OFFSET_CENTER =
            new HashMap<Side, Vector3f>() { {
                put(Side.LEFT, new Vector3f(-0.5f, 0, 0.5f));
                put(Side.RIGHT, new Vector3f(0.5f, 0, -0.5f));
                put(Side.FRONT, new Vector3f(-0.5f, 0, -0.5f));
                put(Side.BACK, new Vector3f(0.5f, 0, 0.5f));
            }};

    public void calculateDirection(Vector3f velocity, ConnectsToRailsComponent.RAILS blockType, Side side, MinecartComponent minecart) {
        side = correctSide(blockType, side);
        switch (blockType) {
            case SLOPE:
                minecart.pathDirection = getDirectPath(side);
                minecart.pathDirection.y = 1;
                break;
            case PLANE:
                minecart.pathDirection = getDirectPath(side);
                break;
            case INTERSECTION:
                Vector3f currentBlock = new Vector3f(minecart.currentBlockPosition);
                currentBlock.sub(minecart.prevBlockPosition);
                currentBlock.absolute();
                currentBlock.y = 0;
                break;
            case TEE_INVERSED:
            case TEE:
            case CURVE:
                Vector3f direction =  new Vector3f(minecart.currentBlockPosition);
                direction.sub(minecart.prevBlockPosition);
                setCornerDirection(side, minecart, direction);
                break;
        }
        correctVelocity(minecart, velocity, blockType);
    }

    public Vector3f getRotationOffsetPoint(Side cornerSide) {
        return CORNER_ROTATE_OFFSET_CENTER.get(cornerSide);
    }

    public Side correctSide(ConnectsToRailsComponent.RAILS blockType, Side side) {
        switch (blockType) {
            case PLANE:
                side = side.yawClockwise(1);
                break;
            case TEE:
                side = side.yawClockwise(-1);
                break;
        }
        return side;
    }

    public void getYawOnPath(MinecartComponent minecart, Side side, Vector3f cornerDistanceMoved) {

        boolean isCorner = minecart.pathDirection.x!=0 && minecart.pathDirection.z!=0;

        if (isCorner && cornerDistanceMoved != null) {
            float percent = cornerDistanceMoved.length() / 0.007f;
            if (percent > 100) {
                minecart.yaw += minecart.angleSign * 90f;
            } else {
                minecart.yaw += minecart.angleSign * 90f * percent / 100;
            }

            if (minecart.yaw < 0) {
                minecart.yaw = 360 + minecart.yaw;
            } else if (minecart.yaw > 360) {
                minecart.yaw = minecart.yaw - 360;
            }
            return;
        }


        switch (side) {
            case LEFT:
            case RIGHT:
                if (minecart.yaw >= 360 || minecart.yaw < 45 && minecart.yaw > 0) {
                    minecart.yaw = 0;
                }

                if (minecart.yaw >= 135 && minecart.yaw < 180 || minecart.yaw > 180 && minecart.yaw < 225) {
                    minecart.yaw = 180;
                }

                if (minecart.yaw != 180 && minecart.yaw != 0) {
                    minecart.yaw = 0;
                }
                break;
            case FRONT:
            case BACK:
                if (minecart.yaw == 0 || minecart.yaw >= 45 && minecart.yaw < 90 || minecart.yaw > 90 && minecart.yaw < 135) {
                    minecart.yaw = 90;
                    break;
                }

                if (minecart.yaw >= 225 && minecart.yaw < 270 || minecart.yaw > 270 && minecart.yaw < 315) {
                    minecart.yaw = 270;
                }

                if (minecart.yaw != 90 && minecart.yaw != 270) {
                    minecart.yaw = 90;
                }

                break;
        }

    }

    private Vector3f getDirectPath(Side side) {
        Vector3f directPath = side.getVector3i().toVector3f();
        directPath.absolute();
        return directPath;
    }

    private void setCornerDirection(Side side, MinecartComponent minecart, Vector3f direction) {
        switch (side) {
            case LEFT:
                if (direction.x > 0) {
                    minecart.angleSign = -1;
                    minecart.pathDirection.set(1f, 0, 1f);
                } else if (direction.z < 0) {
                    minecart.angleSign = 1;
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
                    minecart.angleSign = -1;
                    minecart.pathDirection.set(-1f, 0, -1f);
                } else if (direction.z > 0) {
                    minecart.angleSign = 1;
                    minecart.pathDirection.set(1f, 0, 1f);
                }
                break;
            case FRONT:
                if (direction.x > 0) {
                    minecart.angleSign = 1;
                    minecart.pathDirection.set(1f, 0, -1f);
                } else if (direction.z < 0) {
                    minecart.pathDirection.set(0, 0, -1f);
                } else if (direction.x < 0) {
                    minecart.pathDirection.set(0, 0, -1f);
                } else if (direction.z > 0) {
                    minecart.angleSign = -1;
                    minecart.pathDirection.set(-1f, 0, 1f);
                }
                break;
            case BACK:
                if (direction.x > 0) {
                    minecart.pathDirection.set(-1f, 0, 0);
                } else if (direction.z < 0) {
                    minecart.angleSign = -1;
                    minecart.pathDirection.set(1f, 0, -1f);
                } else if (direction.x < 0) {
                    minecart.angleSign = 1;
                    minecart.pathDirection.set(-1, 0, 1f);
                } else if (direction.z > 0) {
                    minecart.pathDirection.set(0, 0, 1f);
                }
                break;
        }
    }

    private void correctVelocity(MinecartComponent minecartComponent, Vector3f velocity, ConnectsToRailsComponent.RAILS blockType) {

        if (
            blockType == ConnectsToRailsComponent.RAILS.CURVE ||
            blockType == ConnectsToRailsComponent.RAILS.TEE ||
            blockType == ConnectsToRailsComponent.RAILS.TEE_INVERSED
           ) {

            velocity.absolute();
            minecartComponent.drive.absolute();

            if (velocity.x > velocity.z) {
                velocity.z = velocity.x;
                minecartComponent.drive.z = minecartComponent.drive.x;
            } else {
                velocity.x = velocity.z;
                minecartComponent.drive.x = minecartComponent.drive.z;
            }
        }

        velocity.x = velocity.x * minecartComponent.pathDirection.x;
        velocity.y = velocity.y * minecartComponent.pathDirection.y;
        velocity.z = velocity.z * minecartComponent.pathDirection.z;

        /*if (blockType == ConnectsToRailsComponent.RAILS.SLOPE && velocity.y <= 0) {
            //velocity.y *= 2f;
        }*/

        if ((minecartComponent.drive.lengthSquared() - velocity.lengthSquared()) > 0.1) {
            minecartComponent.drive.absolute();
            minecartComponent.drive.x *= Math.signum(velocity.x) * Math.abs(minecartComponent.pathDirection.x);
            minecartComponent.drive.z *= Math.signum(velocity.z) * Math.abs(minecartComponent.pathDirection.z);
            velocity.interpolate(minecartComponent.drive, 0.2f);
        }

    }

    public void getPitchOnPath(MinecartComponent minecart, ConnectsToRailsComponent.RAILS blockType) {

        minecart.pitch = 0;

        if (blockType.equals(ConnectsToRailsComponent.RAILS.SLOPE)) {
            minecart.pitch = 45;
        }
    }
}
