/*
 * Copyright 2017 MovingBlocks
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
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;
import org.terasology.rails.tracks.components.PathComponent;

/**
 * Created by michaelpollind on 4/2/17.
 */
public class Segment {

    public static final float ARC_SEGMENT_ITERATIONS = 100;

    private PathComponent.CubicBezier[] curves;
    private float[] argLengths;

    private Vector3f startingBinormal;
    private Vector3f startingNormal;


    public Segment(PathComponent.CubicBezier[] curves, Vector3f startingBinormal) {
        this.curves = curves;
        this.startingBinormal = startingBinormal;
        this.argLengths = new float[this.curves.length];
        Vector3f normal = new Vector3f();
        normal.cross(tangent(0, 0), startingBinormal);
        this.startingNormal = normal;

        CalculateLength();

    }

    public void CalculateLength() {
        if (this.curves.length == 0)
            return;

        float distance = 0f;

        Vector3f previous = point(0, 0);// curves[0].getPoint(0);

        Vector3f normal = new Vector3f();
        normal.cross(tangent(0, 0), startingBinormal);

        for (int x = 0; x < curves.length; x++) {

            for (int y = 0; y <= ARC_SEGMENT_ITERATIONS; y++) {
                Vector3f current = point(x, y / ARC_SEGMENT_ITERATIONS);
                distance += current.distance(previous);
                previous = current;
            }
            this.argLengths[x] = distance;
        }
    }

    public int index(float t) {
        if (t < 0)
            return 0;
        for (int x = 0; x < argLengths.length; x++) {
            if (t < argLengths[x]) {
                return x;
            }
        }
        return argLengths.length - 1;
    }

    public int maxIndex() {
        return argLengths.length - 1;
    }


    public float t(int index, float t) {
        if (index - 1 < 0) {
            return (t / argLengths[0]);
        }
        return ((t - argLengths[index - 1]) / (argLengths[index - 1] - argLengths[index]));
    }

    public float nearestT(Vector3f pos, Vector3f position, Quat4f rotation) {
        if (this.curves.length == 0)
            return 0f;

        float result = 0;
        float closest = Float.MAX_VALUE;

        float tvalue = 0f;
        Vector3f previous = point(0, 0,position,rotation);// curves[0].getPoint(0);
        for (int x = 0; x < curves.length; x++) {
            for (int y = 0; y <= ARC_SEGMENT_ITERATIONS; y++) {
                Vector3f current = point(x, y / ARC_SEGMENT_ITERATIONS,position,rotation);
                tvalue += current.distance(previous);
                previous = current;

                float distance = current.distance(pos);
                if (distance < closest) {
                    closest = distance;
                    result = tvalue;
                }
            }
        }
        return result;
    }

    public float maxDistance() {
        return argLengths[argLengths.length - 1];
    }

    public Vector3f tangent(int index, float t) {
        PathComponent.CubicBezier curve = curves[index];

        t = TeraMath.clamp(t, 0, 1f);
        float num = 1f - t;
        Vector3f vf1 = new Vector3f(curve.f2).sub(curve.f1).mul(3f * num * num);
        Vector3f vf2 = new Vector3f(curve.f3).sub(curve.f2).mul(6f * num * t);
        Vector3f vf3 = new Vector3f(curve.f4).sub(curve.f3).mul(3 * t * t);

        return vf1.add(vf2).add(vf3).normalize();
    }

    public Vector3f tangent(int index, float t, Quat4f rotation) {
        return rotation.rotate(this.tangent(index, t));
    }

    public Vector3f point(int index, float t) {
        PathComponent.CubicBezier curve = curves[index];

        t = TeraMath.clamp(t, 0, 1f);
        float num = 1f - t;
        Vector3f vf1 = new Vector3f(curve.f1).mul(num * num * num);
        Vector3f vf2 = new Vector3f(curve.f2).mul(3f * num * num * t);
        Vector3f vf3 = new Vector3f(curve.f3).mul(3f * num * t * t);
        Vector3f vf4 = new Vector3f(curve.f4).mul(t * t * t);
        return vf1.add(vf2).add(vf3).add(vf4);

    }

    public Vector3f normal(int index, float t) {
        Vector3f startingTangent = tangent(0, 0);
        Vector3f tangent = tangent(index, t);
        Quat4f arcCurve = Quat4f.shortestArcQuat(startingTangent, tangent);

        return arcCurve.rotate(startingNormal);
    }

    public Vector3f normal(int index, float t, Quat4f rotation) {
        return rotation.rotate(normal(index, t));
    }

    public Vector3f point(int index, float t, Vector3f position, Quat4f rotation) {
        return rotation.rotate(point(index, t)).add(position);
    }


}
