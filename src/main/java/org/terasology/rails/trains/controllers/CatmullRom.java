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
package org.terasology.rails.trains.controllers;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by adeon on 06.10.14.
 * P(t) = (2t³ - 3t² + 1)p0 + (t³ - 2t² + t)m0 + ( -2t³ + 3t²)p1 + (t³ - t²)m1
 */
public class CatmullRom {

    private List<Vector3f> controlPoints;
    private List<Vector3f> factor;

    public CatmullRom(List<Vector3f> controlPoints, float curveTension) {
        for (int i = 0; i < controlPoints.size(); i++) {
            Vector3f vector3f = controlPoints.get(i);
            this.controlPoints.add(vector3f);
        }
        this.compute();
    }

    private void compute() {
        int s = controlPoints.size();
        int k0 = 0;
        int k1 = s==4?1 : 0;
        int k2 = s==4?2 : 1;
        int k3 = s==4?3 : s==3?2 : 1;

        factor.clear();

        Vector3f m0 = controlPoints.get(k0);
        Vector3f p0 = controlPoints.get(k1);
        Vector3f p1 = controlPoints.get(k2);
        Vector3f m1 = controlPoints.get(k3);


        Vector3f c1 = new Vector3f();
        Vector3f c2 = new Vector3f();

        c1.sub(p0, p1);
        c1.scale(1.5f);
        c2.sub(m1, m0);
        c2.scale(0.5f);
        factor.add(new Vector3f());
        factor.get(0).add(c1, c2);

        c1.set(m1);
        c1.scale(0.5f);
        c1.add(m0);
        factor.add(new Vector3f());
        factor.get(1).sub(c1);

        c1.set(p1);
        c1.scale(2f);
        factor.get(1).add(c1);

        c1.set(p0);
        c1.scale(-2.5f);
        factor.get(1).add(c1);

        c1.sub(p1,m0);
        c1.scale(0.5f);
        factor.add(new Vector3f(c1));
        factor.add(new Vector3f(p0));
    }

    public Vector3f getPosition(float t) {
        Vector3f pos = new Vector3f(factor.get(0));
        pos.scale(t);
        pos.add(factor.get(1));
        pos.scale(t);
        pos.add(factor.get(2));
        pos.scale(t);
        pos.add(factor.get(3));
        return pos;
    }

    public Vector3f getVelocity(float t) {
        Vector3f v = new Vector3f(factor.get(0));
        Vector3f tV = new Vector3f(factor.get(1));
        v.scale(3*t);
        tV.scale(2);
        v.add(tV);
        v.scale(t);
        v.add(factor.get(2));
        return v;
    }

    /*
    *    velocity = curve.getVelocity(t);
    t += myVelocity / velocity.length();
    position = curve(t);
    */

}
