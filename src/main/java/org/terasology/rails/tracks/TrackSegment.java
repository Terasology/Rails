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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.math.geom.Quat4f;
import org.terasology.math.geom.Vector3f;

public abstract class TrackSegment {

    public static final float ARC_SEGMENT_ITERATIONS = 100;

    private CubicBezier[] curves;
    private  float[] argLengths;

    private  Vector3f startingBinormal;
    private  Vector3f startingNormal;

    public static class TrackSegmentPair
    {
        public float t;
        public TrackSegment segment;
        public EntityRef association;

        public  TrackSegmentPair(float t, TrackSegment segment ,EntityRef association)
        {
            this.t = t;
            this.segment = segment;
            this.association = association;
        }

        public  TrackSegmentPair(TrackSegment segment, EntityRef association)
        {
            this.segment = segment;
            this.association = association;
        }
    }


    public TrackSegment(CubicBezier[] curves,Vector3f startingBinromal) {
        this.curves = curves;
        this.startingBinormal = startingBinromal;
        this.argLengths = new float[this.curves.length];

        Vector3f normal = new Vector3f();
        normal.cross(curves[0].getTangent(0),startingBinromal);
        this.startingNormal = normal;

        CalculateLengths();

    }

    public  void  CalculateLengths()
    {
        if(this.curves.length == 0)
            return;

        float distance = 0f;

        Vector3f previous = curves[0].getPoint(0);

        Vector3f normal = new Vector3f();
        normal.cross(this.curves[0].getTangent(0),startingBinormal);

        for (int x = 0; x < curves.length; x++)
        {

            for(int y = 0; y <= ARC_SEGMENT_ITERATIONS; y++)
            {
                Vector3f current = curves[x].getPoint(y/ARC_SEGMENT_ITERATIONS);
                distance += current.distance(previous);
                previous = current;
            }
            this.argLengths[x] = distance;


        }
    }

    public  float getNearestT(Vector3f pos, Vector3f position, Quat4f rotation)
    {
        if(this.curves.length == 0)
            return 0f;

        float result = 0;
        float closest = Float.MAX_VALUE;

        float tvalue = 0f;
        Vector3f previous = curves[0].getPoint(0);
        for (int x = 0; x < curves.length; x++)
        {
            for(int y = 0; y <= ARC_SEGMENT_ITERATIONS; y++)
            {
                Vector3f current = curves[x].getPoint(y/ARC_SEGMENT_ITERATIONS);
                tvalue += current.distance(previous);
                previous = current;

                current = rotation.rotate(current).add(position);

                float distance = current.distance(pos);
                if(distance < closest) {
                    closest = distance;
                    result = tvalue;
                }
            }
        }
        return  result;
    }

    private  int getIndex(float t)
    {
        if(t < 0)
            return  -1;
        for(int x = 0; x < argLengths.length; x++)
        {
            if(t < argLengths[x])
            {
                return x;
            }
        }
        return argLengths.length-1;
    }

    private  float getT(int index,float t)
    {
        if(index -1 < 0)
        {
            return  (t/argLengths[0]);
        }
        return  ((t - argLengths[index -1])/(argLengths[index -1]-argLengths[index]));
    }


    public  float getMaxDistance()
    {
        return  argLengths[argLengths.length -1];
    }

    public  Vector3f getNormal(float t, EntityRef ref)
    {
        TrackSegmentPair pair =  getTrackSegment(t,ref);
        if(pair == null)
            return null;
        int index= pair.segment.getIndex(pair.t);

        Vector3f startingTangent = pair.segment.curves[0].getTangent(0);
        Vector3f tangent = pair.segment.curves[index].getTangent(pair.segment.getT(index,(pair.t)) );

        Quat4f arcCurve = Quat4f.shortestArcQuat(startingTangent,tangent);
        return arcCurve.rotate(pair.segment.startingNormal);
    }

    public  Vector3f getNormal(float t,Quat4f rotation, EntityRef ref)
    {
        return rotation.rotate(getNormal(t,ref));
    }


    public  Vector3f getPoint(float t,EntityRef ref)
    {
        TrackSegmentPair pair =  getTrackSegment(t,ref);
        if(pair == null)
            return null;
        int index= pair.segment.getIndex(pair.t);
        return pair.segment.curves[index].getPoint(pair.segment.getT(index,(pair.t)) );
    }


    public Vector3f getPoint(float t,Vector3f position,Quat4f rotation,EntityRef ref)
    {
        return rotation.rotate(getPoint(t,ref)).add(position);
    }

    public  Vector3f getTangent(float t,EntityRef ref)
    {
        TrackSegmentPair pair =  getTrackSegment(t,ref);
        if(pair == null)
            return null;
        int index= pair.segment.getIndex(pair.t);
        return pair.segment.curves[index].getTangent(pair.segment.getT(index,pair.t));
    }

    public Vector3f getTangent(float t,Quat4f rotation,EntityRef ref)
    {
        return rotation.rotate(getTangent(t,ref));
    }


    public TrackSegmentPair getTrackSegment(float t, EntityRef ref)
    {

        TrackSegmentPair previous = this.getPreviousSegment(ref);
        TrackSegmentPair next = this.getNextSegment(ref);

        int index = getIndex(t);

        if(t > getMaxDistance())
        {
            if(next == null)
                return  null;

            float result = t - this.getMaxDistance();
            if(invertSegment(this,next.segment))
                result = next.segment.getMaxDistance() - result;
            next.t = result;
            return next;
        }
        else if(t < 0)
        {
            if(previous == null)
                return  null;

            float result = previous.segment.getMaxDistance()+t;
            if(invertSegment(previous.segment,this))
                result = previous.segment.getMaxDistance() - result;
            previous.t = result;
            return  previous;
        }
        return  new TrackSegmentPair(t,this,ref);
    }
    public  abstract  boolean invertSegment(TrackSegment previous,TrackSegment next);
    public abstract TrackSegmentPair getNextSegment(EntityRef ref);
    public abstract TrackSegmentPair getPreviousSegment(EntityRef ref);
}
