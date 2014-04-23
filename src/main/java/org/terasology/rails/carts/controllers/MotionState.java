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

import javax.vecmath.Vector3f;

public class MotionState {
    public Vector3f prevPosition    = new Vector3f();
    public Vector3f currentBlockPosition = null;
    public Vector3f prevBlockPosition = null;
    public boolean positionCorrected = false;
    public int yawSign = 1;
    public int pitchSign = 1;
    public PositionStatus currentPositionStatus = PositionStatus.ON_THE_AIR;
    public static enum PositionStatus {ON_THE_AIR, ON_THE_GROUND, ON_THE_PATH, ON_THE_LIQUID};
}
