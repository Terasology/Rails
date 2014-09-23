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
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.input.internal.BindableAxisImpl;
import org.terasology.protobuf.EntityData;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by adeon on 09.09.14.
 */
public class Railway {
    private Map<String, ArrayList<EntityRef>> chunks = Maps.newHashMap();
    private int id;

    private static Railway instance;

    private Railway() {}

    public static Railway getInstance(){
        if (instance == null) {
            instance = new Railway();
        }

        return instance;
    }


    public ArrayList<EntityRef> getChunk(String chunkKey) {
        return chunks.get(chunkKey);
    }

    public String createChunk(Vector3f position) {
        String key = "{" + position.toString() + ")_" + id;
        chunks.put(key, new ArrayList<EntityRef>());
        id++;
        return key;
    }

    public void removeChunk(String key) {
        if (chunks.containsKey(key)) {
            ArrayList<EntityRef> tracks = chunks.get(key);
            for(EntityRef track : tracks) {
                track.destroy();
            }
        }
    }

}
