/*
 * Copyright 2016 MovingBlocks
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
package org.terasology.rails.minecarts.components;

import org.terasology.entitySystem.Component;
import org.terasology.reflection.MappedContainer;

import java.util.Set;

/**
 * Created by michaelpollind on 8/15/16.
 */
public class PathDescriptorComponent implements Component {
    public Set<PathNode> nodes;

    @MappedContainer
    public  static  class  PathNode{
        float p1;
        float p2;
        float p3;
    }

}
