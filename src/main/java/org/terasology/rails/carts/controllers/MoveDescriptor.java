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

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final Map<Side, ArrayList<Vector3f>> CORNER_ROTATE_DIRECTION =
            new HashMap<Side, ArrayList<Vector3f>>() { {
                put(Side.LEFT, Lists.newArrayList(new Vector3f(1f, 0, 1f), new Vector3f(-1f, 0, -1f), new Vector3f(-1f, 0, 0), new Vector3f(0, 0, 1f)));
                put(Side.RIGHT, Lists.newArrayList(new Vector3f(1f, 0, 0f), new Vector3f(0, 0, -1f), new Vector3f(-1f, 0, -1f), new Vector3f(1f, 0, 1f)));
                put(Side.FRONT, Lists.newArrayList(new Vector3f(1f, 0, -1f), new Vector3f(-1f, 0, 0), new Vector3f(0, 0, -1f), new Vector3f(-1f, 0, 1f)));
                put(Side.BACK, Lists.newArrayList(new Vector3f(1f, 0, 0), new Vector3f(1f, 0, -1f), new Vector3f(-1, 0, 1f), new Vector3f(0, 0, 1f)));
            }};

    public void calculateDirection(Vector3f velocity, ConnectsToRailsComponent.RAILS blockType, Side side, MinecartComponent minecart, MotionState motionState, Vector3f position, int slopeFactor) {
        side = correctSide(blockType, side);
        switch (blockType) {
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
        correctVelocity(minecart, motionState, velocity, blockType, side, slopeFactor);
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

    public void getYawOnPath(MinecartComponent minecart, Side side, MotionState motionState, ConnectsToRailsComponent.RAILS type, Vector3f distanceMoved) {

        if (isCorner(type) && distanceMoved != null) {
            logger.info("--------------------------------------------");
            logger.info("distanceMoved: " + distanceMoved);
            if (minecart.prevYaw == -1) {
                minecart.prevYaw = minecart.yaw;
            }
            distanceMoved.y = 0;
            logger.info("distanceMoved.length(): " + distanceMoved.length());
            float percent = distanceMoved.length() / 0.01f;
            //logger.info("percent: " + percent);

            //logger.info("BEFORE: " + minecart.yaw);
            minecart.yaw += motionState.yawSign * 90f * percent / 100;
            //logger.info("AFTER: " + minecart.yaw);
            if ((motionState.yawSign > 0 && minecart.yaw > minecart.prevYaw + 90f) || (motionState.yawSign < 0 && minecart.yaw < minecart.prevYaw-90f)) {
                minecart.yaw = minecart.prevYaw + motionState.yawSign*90f;
            }

            if (minecart.yaw < 0) {
                minecart.yaw = 360 + minecart.yaw;
            } else if (minecart.yaw > 360) {
                minecart.yaw = minecart.yaw - 360;
            }
            //logger.info("Yaw on the corner: " + minecart.yaw);
            //logger.info("--------------------------------------------");
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
                } else if (direction.z < 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                }
                break;
            case RIGHT:
                if (direction.x < 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                } else if (direction.z > 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                }
                break;
            case FRONT:
                if (direction.x > 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
                } else if (direction.z > 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                }
                break;
            case BACK:
                if (direction.z < 0) {
                    motionState.yawSign = -1;
                    checkBouds = true;
                } else if (direction.x < 0) {
                    motionState.yawSign = 1;
                    checkBouds = true;
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

    private void correctVelocity(MinecartComponent minecartComponent, MotionState motionState, Vector3f velocity, ConnectsToRailsComponent.RAILS blockType, Side side, int slopeFactor) {

        if (isCorner(blockType)) {
            if ( velocity.x != 0 ) {
                velocity.z = velocity.x;
            } else {
                velocity.x = velocity.z;
            }
            velocity.absolute();
            velocity.x = velocity.x * minecartComponent.pathDirection.x;
            velocity.y = velocity.y * minecartComponent.pathDirection.y;
            velocity.z = velocity.z * minecartComponent.pathDirection.z;
            minecartComponent.pathDirection.absolute();
        } else {
            velocity.x = velocity.x * minecartComponent.pathDirection.x;
            velocity.y = velocity.y * minecartComponent.pathDirection.y;
            velocity.z = velocity.z * minecartComponent.pathDirection.z;
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

    public void getPitchOnPath(MinecartComponent minecart, Vector3f position, MotionState motionState, Side side, ConnectsToRailsComponent.RAILS blockType) {

        minecart.pitch = 0;

        Vector3f dir = new Vector3f(position);
        dir.sub(motionState.prevPosition);

        if (motionState.nextBlockIsSlope && (dir.x < 0 || dir.z > 0)) {
            side = side.reverse();
        }

        if (blockType.equals(ConnectsToRailsComponent.RAILS.SLOPE) || motionState.nextBlockIsSlope) {
            switch (side) {
                case LEFT:
                case BACK:
                    minecart.pitch = 45 * motionState.pitchSign;
                    break;
                case RIGHT:
                case FRONT:
                    minecart.pitch = -45 * motionState.pitchSign;
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
            //logger.info("yaw: " + minecart.yaw);
        }
    }


    public boolean isCorner(ConnectsToRailsComponent.RAILS type) {
        return type == ConnectsToRailsComponent.RAILS.CURVE ||
                type == ConnectsToRailsComponent.RAILS.TEE ||
                type == ConnectsToRailsComponent.RAILS.TEE_INVERSED;
    }
}
