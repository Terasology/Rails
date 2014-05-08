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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.Vector3i;
import org.terasology.rails.blocks.ConnectsToRailsComponent;
import org.terasology.world.block.Block;

import javax.vecmath.Vector3f;

public class BlockInfo {
    private Block block = null;
    private Vector3i blockPosition = new Vector3i();
    private EntityRef blockEntity = EntityRef.NULL;
    private ConnectsToRailsComponent rails = null;

    public BlockInfo(Block block, Vector3i blockPosition, EntityRef entity, ConnectsToRailsComponent component) {
        this.block = block;
        this.blockEntity = entity;
        this.rails = component;
        this.blockPosition = blockPosition;
    }

    public boolean isEmptyBlock() {
        if (block != null) {
            return false;
        }
        return true;
    }

    public boolean isRails() {
        if (rails != null) {
            return true;
        }
        return false;
    }

    public ConnectsToRailsComponent.RAILS getType() {
        if (isRails()) {
            return rails.type;
        } else {
            return null;
        }
    }

    public Block getBlock() {
        return block;
    }

    public Vector3i getBlockPosition() {
        return blockPosition;
    }

    public boolean isCorner() {
        return  isRails() &&
               (rails.type == ConnectsToRailsComponent.RAILS.CURVE ||
                rails.type == ConnectsToRailsComponent.RAILS.TEE ||
                rails.type == ConnectsToRailsComponent.RAILS.TEE_INVERSED);
    }

    public boolean isSlope() {
        return isRails() && rails.type == ConnectsToRailsComponent.RAILS.SLOPE;
    }

    public boolean isIntersection() {
        return isRails() && rails.type == ConnectsToRailsComponent.RAILS.INTERSECTION;
    }

    public boolean isSameBlock(Vector3f anotherBlock) {
        if (blockPosition.lengthSquared() == 0 || anotherBlock == null) {
            return false;
        }
        return blockPosition.x == anotherBlock.x && blockPosition.y == anotherBlock.y && blockPosition.z == anotherBlock.z;
    }
}
