/*
 * Copyright 2017 MovingBlocks
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
package org.terasology.minecarts;

/**
 * Created by michaelpollind on 7/15/17.
 */
public class Constants {
    public static final float GRAVITY = 9.8f;
    public static final float FRICTION_COFF = .1f;
    public static final float BAUMGARTE_COFF = .1f;
    public static final float VELOCITY_CAP = 15f;
    public static final float PLAYER_MASS = 30f;
    public static final float MAX_VEHICLE_JOIN_DISTANCE = 5f;
    // TODO: Make configurable?
    public static final float JOINT_DISTANCE = 1.4f;
}
