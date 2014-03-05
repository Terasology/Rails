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
import org.terasology.rails.blocks.ConnectsToRailsComponent;

import javax.vecmath.Vector3f;

public class MoveDescriptor {

    protected boolean useGravity = true;

    protected float groundFriction = 0.7f;
    protected float pathFriction = 0.25f;
    protected float airFriction = 0.25f;

    protected float maxGroundSpeed = 5.0f;
    protected float maxPathSpeed = 10.0f;
    protected float maxAirSpeed = 25.0f;
    protected float maxLiquidSpeed = 10f;

    private Vector3f drive = new Vector3f();
    private Vector3f pathDirection = new Vector3f(1f, 1f, 1f);
    private Vector3f curveDirection = new Vector3f(1f, 1f, 1f);
    private Vector3f currentPosition = new Vector3f();
    private Vector3f prevPosition = new Vector3f();

    private Vector3f direction = new Vector3f();

    private final Logger logger = LoggerFactory.getLogger(MoveDescriptor.class);

    private int angleSign = 1;
    private float pitch;
    private float yaw;

    private boolean isCorner;
    private boolean checkNextBlockForCorner;
    private boolean nextBlockIsCorner;

    private ConnectsToRailsComponent.RAILS blockType;
    private Side side;

    private Vector3f currentBlockPos;
    private Vector3f prevBlockPos;
    private Vector3f nextBlockPos;

    public static enum POSITION_STATUS {ON_THE_AIR, ON_THE_GROUND, ON_THE_PATH, ON_THE_LIQUID};

    private POSITION_STATUS currentPositionStatus = POSITION_STATUS.ON_THE_AIR;

    public void calculateDirection(Vector3f blockPosition) {

        if (blockType == null) {
            return;
        }

        if (currentBlockPos != null) {
            if (!blockPosition.equals(currentBlockPos)) {
                prevBlockPos = new Vector3f(currentBlockPos);
                currentBlockPos = blockPosition;
            }
        } else {
            prevBlockPos = new Vector3f();
            currentBlockPos = new Vector3f(blockPosition);
        }

        if (isCorner) {
            isCorner = false;
        }

        switch (blockType) {
            case PLANE:
            case INTERSECTION:
                side = side.yawClockwise(1);
            case SLOPE:
                pathDirection = new Vector3f(side.getVector3i().toVector3f());

                if (blockType.equals(ConnectsToRailsComponent.RAILS.SLOPE)) {
                    Vector3f slide = new Vector3f(pathDirection);
                    slide.y = -1f;
                    slide.scale(3f);
                    drive.add(slide);
                } else {
                    pitch = 0f;
                }

                pathDirection.absolute();
                setYawOnPath();

                if (nextBlockPos == null) {
                    nextBlockPos = new Vector3f(currentBlockPos);
                }

                if (nextBlockPos.equals(currentBlockPos)) {
                    nextBlockPos.sub(currentBlockPos, prevBlockPos);
                    nextBlockPos.add(currentBlockPos, nextBlockPos);
                }

                break;
            case CURVE:
                isCorner = true;
                nextBlockPos = null;
                curveDirection = getCornerDirection();
                break;
            case TEE:
                break;
        }

    }

    public void setCurrentPosition(Vector3f currentPosition){
        this.prevPosition = this.currentPosition;
        this.currentPosition = currentPosition;
    }


    public boolean isCorner() {
        return isCorner;
    }

    private void setYawOnPath() {
        switch (side) {
            case LEFT:
                if (yaw == 0) {
                    yaw = 180;
                }
            case RIGHT:
                if (yaw >= 360 || yaw < 45 && yaw > 0) {
                    yaw = 0;
                }

                if (yaw >= 135 && yaw < 180 || yaw > 180 && yaw < 225) {
                    yaw = 180;
                }

                if (yaw != 180 && yaw != 0) {
                    yaw = 0;
                }
                break;
            case FRONT:
                if (yaw == 0) {
                    yaw = 270;
                }
            case BACK:
                if (yaw == 0 || yaw >= 45 && yaw < 90 || yaw > 90 && yaw < 135) {
                    yaw = 90;
                    break;
                }

                if (yaw >= 225 && yaw < 270 || yaw > 270 && yaw < 315) {
                    yaw = 270;
                }

                if (yaw != 90 && yaw != 270) {
                    yaw = 90;
                }

                break;
        }
    }

    protected Vector3f getCornerDirection() {

        if (nextBlockPos == null) {
            nextBlockPos = currentBlockPos;
        }

        switch (side) {
            case LEFT:
                if (prevBlockPos.x < currentBlockPos.x) {
                    angleSign = 1;
                    if (nextBlockPos.equals(currentBlockPos)) {
                        nextBlockPos = new Vector3f(currentBlockPos.x, currentBlockPos.y, currentBlockPos.z + 1f);
                        checkNextBlockForCorner = true;
                    }
                    return new Vector3f(1f, 0, 1f);
                } else if (prevBlockPos.z > currentBlockPos.z) {
                    angleSign = -1;
                    if (nextBlockPos.equals(currentBlockPos)) {
                        nextBlockPos = new Vector3f(currentBlockPos.x - 1f, currentBlockPos.y, currentBlockPos.z);
                        checkNextBlockForCorner = true;
                    }
                    return new Vector3f(-1f, 0, -1f);
                } else if (prevBlockPos.x > currentBlockPos.x) {
                    return new Vector3f(-1f, 0, 0);
                } else if (prevBlockPos.z < currentBlockPos.z) {
                    return new Vector3f(0, 0, 1f);
                }

                break;
            case RIGHT:
                if (prevBlockPos.x < currentBlockPos.x) {
                    return new Vector3f(1f, 0, 0f);
                } else if (prevBlockPos.z > currentBlockPos.z) {
                    return new Vector3f(0, 0, -1f);
                } else if (prevBlockPos.x > currentBlockPos.x) {
                    angleSign = 1;
                    if (nextBlockPos.equals(currentBlockPos)) {
                        nextBlockPos = new Vector3f(currentBlockPos.x, currentBlockPos.y, currentBlockPos.z - 1f);
                        checkNextBlockForCorner = true;
                    }
                    return new Vector3f(-1f, 0, -1f);
                } else if (prevBlockPos.z < currentBlockPos.z) {
                    angleSign = -1;
                    if (nextBlockPos.equals(currentBlockPos)) {
                        nextBlockPos = new Vector3f(currentBlockPos.x + 1f, currentBlockPos.y, currentBlockPos.z);
                        checkNextBlockForCorner = true;
                    }
                    return new Vector3f(1f, 0, 1f);
                }

                break;
            case FRONT:
                if (prevBlockPos.x < currentBlockPos.x) {
                    angleSign = -1;
                    if (nextBlockPos.equals(currentBlockPos)) {
                        nextBlockPos = new Vector3f(currentBlockPos.x, currentBlockPos.y, currentBlockPos.z - 1f);
                        checkNextBlockForCorner = true;
                    }
                    return new Vector3f(1f, 0, -1f);
                } else if (prevBlockPos.z > currentBlockPos.z) {
                    return new Vector3f(0, 0, -1f);
                } else if (prevBlockPos.x > currentBlockPos.x) {
                    return new Vector3f(0, 0, -1f);
                } else if (prevBlockPos.z < currentBlockPos.z) {
                    angleSign = 1;
                    if (nextBlockPos.equals(currentBlockPos)) {
                        nextBlockPos = new Vector3f(currentBlockPos.x - 1f, currentBlockPos.y, currentBlockPos.z);
                        checkNextBlockForCorner = true;
                    }
                    return new Vector3f(-1f, 0, 1f);
                }

                break;
            case BACK:
                if (prevBlockPos.x < currentBlockPos.x) {
                     return new Vector3f(-1f, 0, 0);
                } else if (prevBlockPos.z > currentBlockPos.z) {
                    angleSign = 1;
                    if (nextBlockPos.equals(currentBlockPos)) {
                        nextBlockPos = new Vector3f(currentBlockPos.x + 1f, currentBlockPos.y, currentBlockPos.z);
                        checkNextBlockForCorner = true;
                    }
                    return new Vector3f(1f, 0, -1f);
                } else if (prevBlockPos.x > currentBlockPos.x) {
                    angleSign = -1;
                    if (nextBlockPos.equals(currentBlockPos)) {
                        nextBlockPos = new Vector3f(currentBlockPos.x, currentBlockPos.y, currentBlockPos.z + 1f);
                        checkNextBlockForCorner = true;
                    }
                    return new Vector3f(-1, 0, 1f);
                } else if (prevBlockPos.z < currentBlockPos.z) {
                    return new Vector3f(0, 0, 1f);
                }

                break;
        }

        return null;

    }

    ;

    public Vector3f getNextBlockPos() {
        return nextBlockPos;
    }

    public boolean isNextBlockIsCorner() {
        return nextBlockIsCorner;
    }

    public void setNextBlockIsCorner(boolean isCorner) {

        nextBlockIsCorner = isCorner;
    }

    public boolean getCheckNextBlockForCorner() {
        return checkNextBlockForCorner;
    }

    public void setCheckNextBlockForCorner(boolean checkNextBlockForCorner) {
        this.checkNextBlockForCorner = checkNextBlockForCorner;
    }

    protected void correctVelocity(Vector3f velocity) {
        Vector3f desiredVelocity = new Vector3f(drive);

        if (isCorner) {
            velocity.x = velocity.x * pathDirection.x;
            velocity.z = velocity.z * pathDirection.z;

            velocity.absolute();
            drive.absolute();

            if (velocity.x > velocity.z) {
                velocity.z = velocity.x;
                drive.z = drive.x;
            } else {
                velocity.x = velocity.z;
                drive.z = drive.x;
            }

            velocity.x = velocity.x * curveDirection.x;
            velocity.z = velocity.z * curveDirection.z;
            drive.x = drive.x * curveDirection.x;
            drive.z = drive.z * curveDirection.z;
        } else {
            velocity.x = velocity.x * pathDirection.x;
            velocity.z = velocity.z * pathDirection.z;
            drive.x = velocity.x * pathDirection.x;
            drive.z = velocity.z * pathDirection.z;
        }

        if ((drive.lengthSquared()  - velocity.lengthSquared()) > 0.1) {
            velocity.interpolate(drive, 0.2f);
        }

    }

    public Vector3f getDrive() {
        return drive;
    }

    public void setDrive(Vector3f drive) {
        this.drive = drive;
    }

    public void setCurrentPositionStatus(POSITION_STATUS positionStatus) {
        currentPositionStatus = positionStatus;
    }

    public POSITION_STATUS getCurrentPositionStatus() {
        return currentPositionStatus;
    }

    private float getMaxSpeed() {

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

    private float getFriction() {

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

    public void setCurrentBlockOfPathType(ConnectsToRailsComponent.RAILS blockType) {
        this.blockType = blockType;
    }

    public ConnectsToRailsComponent.RAILS getCurrentBlockOfPathType() {
        return blockType;
    }

    public void setCurrentBlockOfPathSide(Side side) {
        this.side = side;
    }

    public void setPathDirection(Vector3f patDirection) {
        this.pathDirection = patDirection;
    }

    public Vector3f getPathDirection() {
        return pathDirection;
    }

    public void setUseGravity(boolean useGravity) {
        this.useGravity = useGravity;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getPitch() {

        if (blockType == null) {
            return pitch;
        }

        pitch = 0;

        if (blockType.equals(ConnectsToRailsComponent.RAILS.SLOPE)) {
            pitch = 45;
        }
        return pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getYaw() {
        Vector3f distanceMoved = new Vector3f(currentPosition);
        distanceMoved.sub(prevPosition);
        if (isCorner) {
            float sign = 1;

            if (nextBlockIsCorner) {

                if (side.equals(Side.BACK) || side.equals(Side.FRONT)) {
                    sign = 1;
                } else {
                    sign = 1;
                }

                float tYaw = yaw + sign * 45f;
                return tYaw;
            }

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
        }

        return yaw;
    }

    public Vector3f getCurrentBlockPosition() {
        return currentBlockPos;
    }

    public Vector3f getprevBlockPos() {
        return prevBlockPos;
    }
}
