package org.terasology.rails.carts.componentsystem.controllers;

import com.bulletphysics.BulletGlobals;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.GhostObject;
import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.linearmath.Transform;

import org.terasology.rails.carts.components.DynamicBlockComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Vector3fUtil;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

public class DynamicMover {

    /**
     * The amount of extra distance added to vertical movement to allow for penetration.
     */
    private static final float VERTICAL_PENETRATION_LEEWAY = 0.05f;
    /**
     * The amount of vertical penetration to allow.
     */
    private static final float VERTICAL_PENETRATION = 0.04f;
    /**
     * The amount of extra distance added to horizontal movement to allow for penentration.
     */
    private static final float HORIZONTAL_PENETRATION_LEEWAY = 0.04f;
    /**
     * The amount of horizontal penetration to allow.
     */
    private static final float HORIZONTAL_PENETRATION = 0.03f;
    private float steppedUpDist = 0;
    private boolean stepped = false;

    public MoveResult updatePosition(final float delta, LocationComponent location, DynamicBlockComponent dynamicBlockComponent) {

        //boolean isCorner = dynamicBlockComponent.getVelocityDescriptor().isCorner();

        Vector3f moveDelta = new Vector3f(dynamicBlockComponent.getVelocityDescriptor().getVelocity(delta));
        moveDelta.scale(delta);
        //System.out.println( " moveDelta: " + moveDelta );


        MoveResult moveResult = move(location.getWorldPosition(), moveDelta, dynamicBlockComponent.collider);

        Vector3f distanceMoved = new Vector3f(moveResult.finalPosition);
        distanceMoved.sub(location.getWorldPosition());
        //System.out.println( " distanceMoved: " + distanceMoved );

        if ( distanceMoved.y > 0 ){
            Vector3f newDistance = new Vector3f(distanceMoved);
            boolean changed = false;
            if ( distanceMoved.x != 0 && Math.abs( distanceMoved.x ) > distanceMoved.y ){
               newDistance.y = Math.abs(distanceMoved.x);
               changed = true;
            }else if ( distanceMoved.z != 0 && Math.abs( distanceMoved.z ) > distanceMoved.y ){
               newDistance.y = Math.abs(distanceMoved.z);
               changed = true;
            }else if ( distanceMoved.z == 0 && distanceMoved.x == 0 ) {
                newDistance.y = 0f;
            }

            if ( changed ){
                 moveResult.finalPosition.sub(distanceMoved);
                 moveResult.finalPosition.add(newDistance);
            }
        }

        location.setWorldPosition(moveResult.finalPosition);
        dynamicBlockComponent.collider.setWorldTransform(new Transform(new Matrix4f(new Quat4f(0, 0, 0, 1), moveResult.finalPosition, 1.0f)));


            Vector3f direction = new Vector3f(distanceMoved);

            Quat4f yawPitch = new Quat4f(0, 0, 0, 1);
          //  float yaw   = dynamicBlockComponent.getVelocityDescriptor().getYaw( distanceMoved );
          //  float pitch = dynamicBlockComponent.getVelocityDescriptor().getPitch( );


            /*if ( isCorner ){
                yaw += Math.atan2( direction.z, direction.x) * TeraMath.RAD_TO_DEG;
            }*/
            //(float)Math.atan2( direction.z, direction.x) * TeraMath.RAD_TO_DEG

           // float pitch = 0f;//(float)Math.acos(direction.y);//0.7854021f;//(float)Math.atan2( direction.y, new Vector3f(direction.x, 0, direction.z).length());
            //QuaternionUtil.setEuler(yawPitch,  TeraMath.DEG_TO_RAD * yaw, TeraMath.DEG_TO_RAD * pitch, 0);
            // location.getLocalRotation().set(TeraMath.createViewMatrix(direction, new Vector3f(), up));
            location.getLocalRotation().set(yawPitch);

//            if ( direction != null ){


               // System.out.println("Yaw my: " + yaw + " Yaw atan: " + (float)Math.atan2( direction.z, direction.x) * TeraMath.RAD_TO_DEG );
                
                //yaw = 0;
               /* if ( pitch != 0 ){
                    if ( direction.y < 0 ){
                        pitch += Math.PI/2;
                    }else if (direction.y > 0){
                        pitch -= Math.PI/2;
                 }
                }*/

              //  System.out.println("direction: " + direction);
                //System.out.println("yaw: " + yaw);
               // System.out.println("pitch: " + pitch);


        //System.out.println("pitch " + pitch);
  //          }


        return moveResult;

    }

    private MoveResult move(Vector3f startPosition, Vector3f moveDelta, PairCachingGhostObject collider) {
        stepped = false;
        MoveResult result = new MoveResult();
        Vector3f position = new Vector3f(startPosition);
        result.finalPosition = position;

        // Actual upwards movement
        if (moveDelta.y > 0) {
            result.hitTop = moveDelta.y - moveUp(moveDelta.y, collider, position) > BulletGlobals.SIMD_EPSILON;
        }

        moveHorizontal(new Vector3f(moveDelta.x, 0, moveDelta.z), collider, position, result);
        if (moveDelta.y < 0 || steppedUpDist > 0) {
            float dist = (moveDelta.y < 0) ? moveDelta.y : 0;
            dist -= steppedUpDist;
            moveDown(dist, 0.8f, collider, position, result);
        }
        if (!result.hitBottom) {
            Vector3f tempPos = new Vector3f(position);
            moveDown(-0.35f, 0.8f, collider, tempPos, result);
            // Don't apply step down if nothing to step onto
            if (result.hitBottom) {
                position.set(tempPos);
            }
        }
        return result;
    }

    public static class MoveResult {
        public Vector3f finalPosition;
        public boolean  hitHoriz  = false;
        public boolean  hitBottom = false;
        public boolean  hitTop    = false;

        public Vector3f bottomHitPoint     = new Vector3f();
        public Vector3f horizontalHitPoint = new Vector3f();

        public MoveResult() {

        }

    }

      private boolean moveHorizontal(Vector3f horizMove, PairCachingGhostObject collider, Vector3f position, MoveResult result) {
        float remainingFraction = 1.0f;
        float dist = horizMove.length();
        if (dist < BulletGlobals.SIMD_EPSILON) {
            return false;
        }

        boolean horizontalHit = false;
        Vector3f normalizedDir = Vector3fUtil.safeNormalize(horizMove, new Vector3f());
        Vector3f targetPos = new Vector3f(normalizedDir);
        targetPos.scale(dist + HORIZONTAL_PENETRATION_LEEWAY);
        targetPos.add(position);
        int iteration = 0;
        Vector3f lastHitNormal = new Vector3f(0, 1, 0);
        while (remainingFraction >= 0.01f && iteration++ < 10) {
            SweepCallback callback = sweep(position, targetPos, collider, 0.65f, HORIZONTAL_PENETRATION);

            /* Note: this isn't quite correct (after the first iteration the closestHitFraction is only for part of the moment)
               but probably close enough */
            float actualDist = Math.max(0, (dist + HORIZONTAL_PENETRATION_LEEWAY) * callback.closestHitFraction - HORIZONTAL_PENETRATION_LEEWAY);
            if (actualDist != 0) {
                remainingFraction -= actualDist / dist;
            }
            if (callback.hasHit()) {
                if (actualDist > BulletGlobals.SIMD_EPSILON) {
                    Vector3f actualMove = new Vector3f(normalizedDir);
                    actualMove.scale(actualDist);
                    position.add(actualMove);
                }
                result.hitHoriz = true;
                result.horizontalHitPoint = callback.hitPointWorld;
                dist -= actualDist;
                Vector3f newDir = new Vector3f(normalizedDir);
                newDir.scale(dist);

                float slope = callback.hitNormalWorld.dot(new Vector3f(0, 1, 0));
                // We step up if we're hitting a big slope, or if we're grazing the ground)
                if (slope < 0.8f || 1 - slope < BulletGlobals.SIMD_EPSILON) {
                   // System.out.println("slope: " + slope);
                    boolean stepping = checkStep(collider, position, dist);
                    if (!stepping) {
                        horizontalHit = true;

                        Vector3f newHorizDir = new Vector3f(newDir.x, 0, newDir.z);
                        Vector3f horizNormal = new Vector3f(callback.hitNormalWorld.x, 0, callback.hitNormalWorld.z);
                        if (horizNormal.lengthSquared() > BulletGlobals.SIMD_EPSILON) {
                            horizNormal.normalize();
                            if (lastHitNormal.dot(horizNormal) > BulletGlobals.SIMD_EPSILON) {
                                break;
                            }
                            lastHitNormal.set(horizNormal);
                            extractResidualMovement(horizNormal, newHorizDir);
                        }

                        newDir.set(newHorizDir);
                    }
                } else {
                    // Hitting a shallow slope, move up it
                    Vector3f newHorizDir = new Vector3f(newDir.x, 0, newDir.z);
                    extractResidualMovement(callback.hitNormalWorld, newDir);
                    Vector3f modHorizDir = new Vector3f(newDir);
                    modHorizDir.y = 0;
                    newDir.scale(newHorizDir.length() / modHorizDir.length());
                }

                float sqrDist = newDir.lengthSquared();
                if (sqrDist > BulletGlobals.SIMD_EPSILON) {
                    newDir.normalize();
                    if (newDir.dot(normalizedDir) <= 0.0f) {
                        break;
                    }
                } else {
                    break;
                }
                dist = (float) Math.sqrt(sqrDist);
                normalizedDir.set(newDir);
                targetPos.set(normalizedDir);
                targetPos.scale(dist + HORIZONTAL_PENETRATION_LEEWAY);
                targetPos.add(position);
            } else {
                normalizedDir.scale(dist);
                position.add(normalizedDir);
                break;
            }
        }
        return horizontalHit;
    }

    private boolean checkStep(PairCachingGhostObject collider, Vector3f position, float stepHeight) {
        if (!stepped) {
            stepped = true;
            steppedUpDist = moveUp(stepHeight, collider, position);
            return true;
        }
        return false;
    }

    private boolean moveDown(float dist, float slopeFactor, PairCachingGhostObject collider, Vector3f position, MoveResult result) {

        float remainingDist = -dist;

        Vector3f targetPos = new Vector3f(position);
        targetPos.y -= remainingDist + VERTICAL_PENETRATION_LEEWAY;
        Vector3f normalizedDir = new Vector3f(0, -1, 0);
        boolean hit = false;

        int iteration = 0;
        while (remainingDist > BulletGlobals.SIMD_EPSILON && iteration++ < 10) {
            SweepCallback callback = sweep(position, targetPos, collider, -1.0f, VERTICAL_PENETRATION);

            float actualDist = Math.max(0, (remainingDist + VERTICAL_PENETRATION_LEEWAY) * callback.closestHitFraction - VERTICAL_PENETRATION_LEEWAY);
            Vector3f expectedMove = new Vector3f(targetPos);
            expectedMove.sub(position);
            if (expectedMove.lengthSquared() > BulletGlobals.SIMD_EPSILON) {
                expectedMove.normalize();
                expectedMove.scale(actualDist);
                position.add(expectedMove);
            }

            remainingDist -= actualDist;
            if (remainingDist < BulletGlobals.SIMD_EPSILON) {
                break;
            }

            if (callback.hasHit()) {

                result.bottomHitPoint = callback.hitPointWorld;
                result.hitBottom = true;

                Vector3f contactPoint = callback.hitPointWorld;
                float originalSlope = callback.hitNormalWorld.dot(new Vector3f(0, 1, 0));
                break;
            } else {
                break;
            }
        }

        if (iteration >= 10) {
            hit = true;
        }

        return hit;
    }

    private float moveUp(float riseAmount, GhostObject collider, Vector3f position) {
        SweepCallback callback = sweep(position, new Vector3f(position.x, position.y + riseAmount + VERTICAL_PENETRATION_LEEWAY, position.z), collider, -1.0f, VERTICAL_PENETRATION_LEEWAY);

        if (callback.hasHit()) {
            float actualDist = Math.max(0, ((riseAmount + VERTICAL_PENETRATION_LEEWAY) * callback.closestHitFraction) - VERTICAL_PENETRATION_LEEWAY);
            position.y += actualDist;
            return actualDist;
        }
        position.y += riseAmount;
        return riseAmount;
    }

    private SweepCallback sweep(Vector3f from, Vector3f to, GhostObject collider, float slopeFactor, float allowedPenetration) {
        Transform startTransform = new Transform(new Matrix4f(new Quat4f(0, 0, 0, 1), from, 1.0f));
        Transform endTransform = new Transform(new Matrix4f(new Quat4f(0, 0, 0, 1), to, 1.0f));
        SweepCallback callback = new SweepCallback(collider, new Vector3f(0, 1, 0), slopeFactor);
        callback.collisionFilterGroup = collider.getBroadphaseHandle().collisionFilterGroup;
        callback.collisionFilterMask = collider.getBroadphaseHandle().collisionFilterMask;

        collider.convexSweepTest((ConvexShape) (collider.getCollisionShape()), startTransform, endTransform, callback, allowedPenetration);
        return callback;
    }


    private Vector3f extractResidualMovement(Vector3f hitNormal, Vector3f direction) {
        return extractResidualMovement(hitNormal, direction, 1f);
    }

    private Vector3f extractResidualMovement(Vector3f hitNormal, Vector3f direction, float normalMag) {
        float movementLength = direction.length();
        if (movementLength > BulletGlobals.SIMD_EPSILON) {
            direction.normalize();

            Vector3f reflectDir = Vector3fUtil.reflect(direction, hitNormal, new Vector3f());
            reflectDir.normalize();

            Vector3f perpindicularDir = Vector3fUtil.getPerpendicularComponent(reflectDir, hitNormal, new Vector3f());


            if (normalMag != 0.0f) {
                Vector3f perpComponent = new Vector3f();
                perpComponent.scale(normalMag * movementLength, perpindicularDir);
                direction.set(perpComponent);
            }
        }
        return direction;
    }

    private static class SweepCallback extends CollisionWorld.ClosestConvexResultCallback {
        protected CollisionObject me;
        protected final Vector3f up;
        protected float minSlopeDot;

        public SweepCallback(CollisionObject me, final Vector3f up, float minSlopeDot) {
            super(new Vector3f(), new Vector3f());
            this.me = me;
            this.up = up;
            this.minSlopeDot = minSlopeDot;
        }

        @Override
        public float addSingleResult(CollisionWorld.LocalConvexResult convexResult, boolean normalInWorldSpace) {
            if (convexResult.hitCollisionObject == me) {
                return 1.0f;
            }

            return super.addSingleResult(convexResult, normalInWorldSpace);
        }
    }

}