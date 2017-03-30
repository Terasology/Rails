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
package org.terasology.rails.tracks;

import org.terasology.math.TeraMath;
import org.terasology.math.geom.Vector3f;
import org.terasology.network.Replicate;
import org.terasology.reflection.MappedContainer;

@MappedContainer
public class CubicBezier {
    @Replicate
    Vector3f f1;
    @Replicate
    Vector3f f2;
    @Replicate
    Vector3f f3;
    @Replicate
    Vector3f f4;

    public CubicBezier()
    {
    }
    public CubicBezier(Vector3f f1, Vector3f f2, Vector3f f3, Vector3f f4)
    {
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
    }

    public Vector3f getPoint(float t) {
        t = TeraMath.clamp(t,0,1f);
        float num = 1f - t;
        Vector3f vf1 = new Vector3f(f1).mul(num*num*num);
        Vector3f vf2 = new Vector3f(f2).mul(3f*num*num*t);
        Vector3f vf3 = new Vector3f(f3).mul(3f*num*t*t);
        Vector3f vf4 = new Vector3f(f4).mul(t*t*t);
        return vf1.add(vf2).add(vf3).add(vf4);
    }

    public Vector3f getTangent(float t)
    {
        t = TeraMath.clamp(t,0,1f);
        float num = 1f -t;
        Vector3f vf1 = new Vector3f(f2).sub(f1).mul(3f*num*num);
        Vector3f vf2 = new Vector3f(f3).sub(f2).mul(6f*num*t);
        Vector3f vf3 = new Vector3f(f4).sub(f3).mul(3*t*t);

        return vf1.add(vf2).add(vf3).normalize();
    }


}
