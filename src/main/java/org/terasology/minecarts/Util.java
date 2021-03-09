// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.minecarts;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Util {
    public static Vector3f project(Vector3fc u, Vector3fc v, Vector3f dest) {
        return dest.set(v).mul(u.dot(v) / v.lengthSquared());
    }

    public static Quaternionf rotation(Vector3fc direction) {
        Vector3f horizontal = new Vector3f(direction);
        horizontal.y = 0;
        horizontal.normalize();
        Quaternionf horizontalRotation = new Quaternionf().rotateTo(new Vector3f(0, 0, 1), horizontal);
        Quaternionf verticalRotation = new Quaternionf().rotateTo(horizontal, direction);
        verticalRotation.mul(horizontalRotation);
        return verticalRotation;
    }

}
