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

import org.terasology.math.Vector3i;
import org.terasology.rails.carts.components.MinecartComponent;

import javax.vecmath.Vector3f;

public class MotionState {
    public Vector3f prevPosition    = new Vector3f();
    public Vector3f currentBlockPosition = new Vector3f();
    public Vector3f prevBlockPosition = new Vector3f();
    public Vector3f angularFactor = new Vector3f();
    public MinecartComponent minecartComponent = null;
    public int yawSign = 1;
    public int pitchSign = 1;
    public boolean nextBlockIsSlope = false;
    public PositionStatus currentPositionStatus = PositionStatus.ON_THE_AIR;
    public static enum PositionStatus {ON_THE_AIR, ON_THE_GROUND, ON_THE_PATH, ON_THE_LIQUID};

    public void setCurrentState(Vector3f pathDirection, Vector3f angularFactor, Vector3i newBlockPosition, PositionStatus currentPositionStatus) {
        this.angularFactor = angularFactor;
        this.minecartComponent.pathDirection.set(pathDirection);
        this.currentPositionStatus = currentPositionStatus;
        if (newBlockPosition != null) {
            setCurrentBlockPosition(newBlockPosition.toVector3f());
        }
    }

    public void setCurrentBlockPosition(Vector3f currentBlockPosition) {
        if (this.currentBlockPosition.x != currentBlockPosition.x || this.currentBlockPosition.y != currentBlockPosition.y || this.currentBlockPosition.z != currentBlockPosition.z) {
            this.prevBlockPosition = this.currentBlockPosition;
            this.currentBlockPosition = currentBlockPosition;
        }
    }
}
