// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.minecarts;

import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;

public class Util {
    public static void bound(Vector3f v) {
        if (Float.isNaN(v.x) || Float.isInfinite(v.x))
            v.x = 0.0f;
        if (Float.isNaN(v.y) || Float.isInfinite(v.y))
            v.y = 0.0f;
        if (Float.isNaN(v.z) || Float.isInfinite(v.z))
            v.z = 0.0f;
    }

    public static Vector3f localToWorldPosition(Vector3f localPosition, LocationComponent locationComponent) {
        Vector3f worldPosition = new Vector3f(localPosition);

        LocationComponent parentLocation = locationComponent;
        while (parentLocation != null) {
            worldPosition.scale(parentLocation.getLocalScale());
            parentLocation.getLocalRotation().rotate(worldPosition, worldPosition);
            worldPosition.add(parentLocation.getLocalPosition());
            parentLocation = parentLocation.getParent().getComponent(LocationComponent.class);
        }

        return worldPosition;
    }
}
