// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.minecarts;

import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Util {
    public static Vector3f project(Vector3fc u, Vector3fc v, Vector3f dest) {
        return dest.set(v).mul(u.dot(v) / v.lengthSquared());
    }
}
