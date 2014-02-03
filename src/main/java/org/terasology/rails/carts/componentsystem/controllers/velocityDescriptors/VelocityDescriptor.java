package org.terasology.rails.carts.componentsystem.controllers.velocityDescriptors;

import com.bulletphysics.BulletGlobals;
import org.terasology.rails.carts.components.DynamicBlockComponent;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.location.LocationComponent;

import javax.vecmath.Vector3f;


public abstract class VelocityDescriptor {

    protected float   gravity = 28;
    protected boolean useGravity = true;

    protected float   groundFriction = 8f;
    protected float   pathFriction   = 2f;
    protected float   liquidFriction = 8f;
    protected float   airFriction    = 2f;

    protected float maxGroundSpeed   = 15.0f;
    protected float maxPathSpeed     = 40.0f;
    protected float maxAirSpeed      = 20.0f;
    protected float maxLiquidSpeed   = 10f;

    protected float terminalVelocity = 20.0f;
    protected float mass             = 10f;
    
    private Vector3f velocity         = new Vector3f();
    private Vector3f drive            = new Vector3f();

    private DynamicBlockComponent dynamicBlockComponent = null;


    public static enum POSITION_STATUS {ON_THE_AIR, ON_THE_GROUND, ON_THE_PATH, ON_THE_LIQUID };
    private POSITION_STATUS currentPositionStatus = POSITION_STATUS.ON_THE_AIR;

    public VelocityDescriptor( DynamicBlockComponent dynamicBlockComponent ){
        this.dynamicBlockComponent = dynamicBlockComponent;
    }

    public Vector3f getDirection(){
       return dynamicBlockComponent.getDirection();
    }


    public void onBump(EntityRef dynamicEntity, EntityRef otherEntity){

        DynamicBlockComponent dynamicBlockComponent = dynamicEntity.getComponent(DynamicBlockComponent.class);

        if ( !otherEntity.hasComponent( CharacterComponent.class ) ){
            return;
        }

        if ( !dynamicBlockComponent.getStandingPlayer().equals( EntityRef.NULL ) && dynamicBlockComponent.getStandingPlayer().equals( otherEntity ) ){
            return;
        }

        LocationComponent location  = dynamicEntity.getComponent(LocationComponent.class);
        LocationComponent location2 = otherEntity.getComponent(LocationComponent.class);

        Vector3f positonEnity = location.getWorldPosition();
        Vector3f hitVector = new Vector3f( positonEnity );
        hitVector.sub( location2.getWorldPosition() );
        hitVector.normalize();
        hitVector.scale(2f);
        hitVector.y = 0;

        if ( currentPositionStatus.equals(POSITION_STATUS.ON_THE_PATH) ){
            hitVector.x *= getDirection().x;
            hitVector.z *= getDirection().z;
        }

        dynamicBlockComponent.getVelocityDescriptor().setDrive(hitVector);

        dynamicEntity.saveComponent(dynamicBlockComponent);
    };

    protected void calculateVelocity(float delta){
        Vector3f desiredVelocity = new Vector3f(drive);

        float maxSpeed = getMaxSpeed();
        float friction = getFriction();

        if (desiredVelocity.y != 0) {
            float speed = desiredVelocity.length();
            desiredVelocity.y = 0;
            if (desiredVelocity.x != 0 || desiredVelocity.z != 0) {
                desiredVelocity.normalize();
                desiredVelocity.scale(speed);
            }
        }
        desiredVelocity.scale(maxSpeed);

        // Modify velocity towards desired, up to the maximum rate determined by friction
        Vector3f velocityDiff = new Vector3f(desiredVelocity);
        velocityDiff.sub(velocity);
        velocityDiff.scale(Math.min(friction * delta, 1.0f));

        if ( dynamicBlockComponent.environmentInfo.currentBlock.equals( BlockAdjacentType.CORNER ) ){
            velocity.x += velocityDiff.x;
            velocity.z += velocityDiff.z;

            if ( velocity.x != 0 ){
                velocity.z = velocity.x;
            }else{
                velocity.x = velocity.z;
            }

            velocity.y = 0;
            velocity.absolute();
            velocity.x = velocity.x * getDirection().x;
            velocity.z = velocity.z * getDirection().z;
        }else{
            velocity.x += velocityDiff.x;
            velocity.z += velocityDiff.z;

            velocity.x = velocity.x * getDirection().x;
            velocity.z = velocity.z * getDirection().z;
        }
        velocity.y = Math.max(-maxSpeed, (velocity.y - 28f * delta));

        if ( velocity.length() < BulletGlobals.SIMD_EPSILON ){
            velocity.set(0,0,0);
        }

        if ( dynamicBlockComponent.environmentInfo.currentBlock.getAdjacentBlockType() != null  ){

            if ( dynamicBlockComponent.environmentInfo.currentBlock.getAdjacentBlockType().equals( BlockAdjacentType.SLOPE)  ){
                float downValue = Math.abs(velocity.y);
                if ( getDirection().x != 0 && Math.abs( velocity.x ) > downValue ){
                    velocity.x = (velocity.x/Math.abs( velocity.x )) * downValue;
                }else if( getDirection().z != 0 && Math.abs( velocity.z ) > downValue ){
                    velocity.z = (velocity.z/Math.abs( velocity.z )) * downValue;
                }
            }
        }/*else{
            if ( !possibleEdge ){
               drive.set(0,0,0);
            }
        } */
        drive.set(0,0,0);
        /*System.out.println("velocity " + velocity);
        System.out.println("------------------------------------------");*/

    }

    public Vector3f getVelocity(float delta){
        calculateVelocity(delta);
        return velocity;
    }

    public void setDrive( Vector3f drive ){
        this.drive = drive;
    }

    public void setCurrentPositionStatus ( POSITION_STATUS positionStatus ){
        currentPositionStatus = positionStatus;
    }

    public POSITION_STATUS getCurrentPositionStatus ( ){
        return currentPositionStatus;
    }

    private float getMaxSpeed(){

        switch( currentPositionStatus ){
            case ON_THE_AIR:
                return maxAirSpeed;
            case ON_THE_GROUND:
                return maxGroundSpeed;
            case ON_THE_PATH:
                return maxPathSpeed;
            case ON_THE_LIQUID:
                return maxLiquidSpeed;
        }

        return maxGroundSpeed;

    }

    private float getFriction(){

        switch( currentPositionStatus ){
            case ON_THE_AIR:
                return airFriction;
            case ON_THE_GROUND:
                return groundFriction;
            case ON_THE_PATH:
                return pathFriction;
            case ON_THE_LIQUID:
                return liquidFriction;
        }

        return groundFriction;

    }
    public void setMass( float mass ){
        this.mass = mass;
    }
    
    public Vector3f getDrive(){
        return drive;
    }

    public float getMass( ){
        return mass;
    }

    public void setUseGravity( boolean useGravity ){
        this.useGravity = useGravity;
    }

    public DynamicBlockComponent getDynamicBlockComponent(){
        return dynamicBlockComponent;
    }

}
