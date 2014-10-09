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
package org.terasology.rails.trains.blocks.system;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.input.internal.BindableAxisImpl;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.health.DoDamageEvent;
import org.terasology.logic.health.EngineDamageTypes;
import org.terasology.math.Side;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.protobuf.EntityData;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.registry.CoreRegistry;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.entity.placement.PlaceBlocks;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.items.BlockItemComponent;
import org.terasology.world.block.items.BlockItemFactory;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by adeon on 09.09.14.
 */
public class Railway {
    private Map<String, ArrayList<EntityRef>> chunks = Maps.newHashMap();
    private BlockEntityRegistry blockEntityRegistry;
    private Physics physics;
    private EntityManager entityManager;
    private BlockManager blockManager;
    private WorldProvider worldProvider;

    public static final String GHOST_KEY = "ghost";
    public static final float TRACK_LENGTH = 1f;
    public static final float STANDARD_ANGLE_CHANGE = 7.5f;
    public static final float STANDARD_PITCH_ANGLE_CHANGE = 7.5f;
    private final Logger logger = LoggerFactory.getLogger(Railway.class);

    private static Railway instance;

    private Railway() {
        this.blockEntityRegistry = CoreRegistry.get(BlockEntityRegistry.class);
        this.physics = CoreRegistry.get(Physics.class);
        this.entityManager = CoreRegistry.get(EntityManager.class);
        this.blockManager = CoreRegistry.get(BlockManager.class);
        this.worldProvider = CoreRegistry.get(WorldProvider.class);
    }

    public static Railway getInstance() {
        if (instance == null) {
            instance = new Railway();
        }

        return instance;
    }


    public ArrayList<EntityRef> getChunk(String chunkKey) {
        return chunks.get(chunkKey);
    }

    public String createChunk(Vector3f position) {
        String key = "{" + position.toString() + ")";
        chunks.put(key, new ArrayList<EntityRef>());
        return key;
    }

    public void removeChunk(String key) {
        if (chunks.containsKey(key)) {
            for(EntityRef track : chunks.get(key)) {
                track.destroy();
            }
            chunks.remove(key);
        }
    }

    public String createPreviewChunk() {
        if (chunks.containsKey(GHOST_KEY)) {
            removeChunk(GHOST_KEY);
        }
        chunks.put(GHOST_KEY, new ArrayList<EntityRef>());
        return GHOST_KEY;
    }

    public Vector3f calculateStartTrackPosition(Orientation orientation, Vector3f position) {
        return  new Vector3f(
                position.x - (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.yaw) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Railway.TRACK_LENGTH / 2),
                position.y - (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.pitch ) * Railway.TRACK_LENGTH / 2),
                position.z - (float)(Math.cos(TeraMath.DEG_TO_RAD * orientation.yaw ) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Railway.TRACK_LENGTH / 2)
        );

    }

    public Vector3f calculateEndTrackPosition(Orientation orientation, Vector3f position) {
        return new Vector3f(
                position.x + (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.yaw) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Railway.TRACK_LENGTH / 2),
                position.y + (float)(Math.sin(TeraMath.DEG_TO_RAD * orientation.pitch ) * Railway.TRACK_LENGTH / 2),
                position.z + (float)(Math.cos(TeraMath.DEG_TO_RAD * orientation.yaw ) * Math.cos(TeraMath.DEG_TO_RAD * orientation.pitch) * Railway .TRACK_LENGTH /2)
        );
    }

    public void createTunnel(Vector3f position, Vector3f direction, boolean createTorch) {
        for(int y = 0; y<5; y++) {
            for(int z = -2; z<3; z++) {
                for(int x = -2; x<3; x++) {
                    EntityRef blockEntity = blockEntityRegistry.getBlockEntityAt(new Vector3f(position.x + x, position.y + y, position.z + z));
                    if (blockEntity!=null) {
                        blockEntity.send(new DoDamageEvent(1000, EngineDamageTypes.EXPLOSIVE.get()));
                    }
                }
            }
        }

        if (createTorch) {
            createTorch(position, direction);
        }
    }

    public void createSlope(Vector3f position, int direction) {
        BlockItemFactory blockFactory = new BlockItemFactory(entityManager);
        Side surfaceSide = Side.TOP;
        Side secondaryDirection = Side.BACK;

        switch (direction) {
            case 0:
                secondaryDirection = Side.FRONT;
                break;
            case 90:
                secondaryDirection = Side.LEFT;
                break;
            case 180:
                secondaryDirection = Side.BACK;
                break;
            case 270:
                secondaryDirection = Side.RIGHT;
                break;
        }

        Vector3i dir = secondaryDirection.getVector3i();
        dir.negate();

        for (int i = 0; i < 8; i++) {
            for (int j = -1; j <= 1; j++) {
                EntityRef item = blockFactory.newInstance(blockManager.getBlockFamily("rails:block_" + (i + 1) +"_8"), 1);
                BlockItemComponent blockItem = item.getComponent(BlockItemComponent.class);
                BlockFamily type = blockItem.blockFamily;
                Vector3i placementPos = new Vector3i(position);
                placementPos.x += i*dir.x + dir.z*j;
                placementPos.y += i*dir.y  + 1;
                placementPos.z += i*dir.z + dir.x*j;
                Block block = type.getBlockForPlacement(worldProvider, blockEntityRegistry, placementPos, surfaceSide, secondaryDirection);
                PlaceBlocks placeBlocks = new PlaceBlocks(placementPos, block, EntityRef.NULL);
                worldProvider.getWorldEntity().send(placeBlocks);
            }
        }
    }

    private void createTorch(Vector3f centerPosition, Vector3f direction) {
        float tx = direction.x;
        direction.x = -direction.z;
        direction.z = tx;
        HitResult hit = physics.rayTrace(centerPosition, direction, 10f, StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD);
    }
}
