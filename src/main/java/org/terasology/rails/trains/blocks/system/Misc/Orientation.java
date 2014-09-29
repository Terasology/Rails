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
package org.terasology.rails.trains.blocks.system.Misc;

/**
 * Created by adeon on 09.09.14.
 */
public class Orientation {
    public float yaw = 0;
    public float pitch = 0;
    public float roll = 0;

    public Orientation (float yaw, float pitch, float roll) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    public void add (Orientation orientation) {
        this.yaw += orientation.yaw;
        this.pitch += orientation.pitch;
        this.roll += orientation.roll;

        if (this.yaw >= 360) {
            this.yaw = 360 - this.yaw;
        }
        if (this.pitch >= 360) {
            this.pitch = 360 - this.pitch;
        }
        if (this.roll >= 360) {
            this.roll = 360 - this.roll;
        }

    }
}
