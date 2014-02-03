package org.terasology.rails.carts.componentsystem.controllers;


import org.terasology.rails.carts.components.DynamicBlockComponent;
import org.terasology.math.Side;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockAdjacentType;
import org.terasology.world.block.BlockUri;

import javax.vecmath.Vector3f;

public class RailsDirectionFinder {


    final private static Vector3f[] pathDirectionMap = { new Vector3f( -1f, 0, -1f), new Vector3f( -1f, 0, 0), new Vector3f( 1f, 0, 1f), new Vector3f( 0, 0, 1f), new Vector3f( 0, 0, -1f), new Vector3f( 1f, 0, -1f), new Vector3f( -1, 0, 1f) };

    protected  void setCornerDirection( DynamicBlockComponent dynamicBlockComponent ){

        DynamicBlockComponent.EnvironmentInfo environmentInfo = dynamicBlockComponent.environmentInfo;

        if ( environmentInfo.nextBlockPos == null ){
            environmentInfo.nextBlockPos = environmentInfo.currentBlockPos;
        }
        switch ( environmentInfo.currentBlock.getDirection() ){
            case LEFT:

                if ( environmentInfo.prevBlockPos.x < environmentInfo.currentBlockPos.x ) {
                    if ( environmentInfo.nextBlockPos.equals( environmentInfo.currentBlockPos ) ){
                        environmentInfo.nextBlockPos = new Vector3f( environmentInfo.currentBlockPos.x, environmentInfo.currentBlockPos.y, environmentInfo.currentBlockPos.z + 1f);
                    }
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[2];
                }else if ( environmentInfo.prevBlockPos.z > environmentInfo.currentBlockPos.z ) {
                    if ( environmentInfo.nextBlockPos.equals( environmentInfo.currentBlockPos ) ){
                        environmentInfo.nextBlockPos = new Vector3f( environmentInfo.currentBlockPos.x - 1f, environmentInfo.currentBlockPos.y, environmentInfo.currentBlockPos.z);
                    }
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[0];
                }else if ( environmentInfo.prevBlockPos.x > environmentInfo.currentBlockPos.x ) {
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[1];
                }else if ( environmentInfo.prevBlockPos.z < environmentInfo.currentBlockPos.z ) {
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[3];
                }

                break;
            case RIGHT:

                if ( environmentInfo.prevBlockPos.x < environmentInfo.currentBlockPos.x ) {
                    environmentInfo.pathDirection = new Vector3f( 1f, 0, 0f);
                }else if ( environmentInfo.prevBlockPos.z > environmentInfo.currentBlockPos.z ) {
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[4];
                }else if ( environmentInfo.prevBlockPos.x > environmentInfo.currentBlockPos.x ) {
                    if ( environmentInfo.nextBlockPos.equals( environmentInfo.currentBlockPos ) ){
                        environmentInfo.nextBlockPos = new Vector3f( environmentInfo.currentBlockPos.x, environmentInfo.currentBlockPos.y, environmentInfo.currentBlockPos.z - 1f);
                    }
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[0];
                }else if ( environmentInfo.prevBlockPos.z < environmentInfo.currentBlockPos.z ) {
                    if ( environmentInfo.nextBlockPos.equals( environmentInfo.currentBlockPos ) ){
                        environmentInfo.nextBlockPos = new Vector3f( environmentInfo.currentBlockPos.x + 1f, environmentInfo.currentBlockPos.y, environmentInfo.currentBlockPos.z);
                    }
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[2];
                }

                break;
            case FRONT:

                if ( environmentInfo.prevBlockPos.x < environmentInfo.currentBlockPos.x ) {
                    if ( environmentInfo.nextBlockPos.equals( environmentInfo.currentBlockPos ) ){
                        environmentInfo.nextBlockPos = new Vector3f( environmentInfo.currentBlockPos.x, environmentInfo.currentBlockPos.y, environmentInfo.currentBlockPos.z - 1f);
                    }
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[5];
                }else if ( environmentInfo.prevBlockPos.z > environmentInfo.currentBlockPos.z ) {
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[4];
                }else if ( environmentInfo.prevBlockPos.x > environmentInfo.currentBlockPos.x ) {
                    environmentInfo. pathDirection = RailsDirectionFinder.pathDirectionMap[4];
                }else if ( environmentInfo.prevBlockPos.z < environmentInfo.currentBlockPos.z ) {
                    if ( environmentInfo.nextBlockPos.equals( environmentInfo.currentBlockPos ) ){
                        environmentInfo.nextBlockPos = new Vector3f( environmentInfo.currentBlockPos.x - 1f,environmentInfo. currentBlockPos.y, environmentInfo.currentBlockPos.z);
                    }
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[6];
                }

                break;
            case BACK:

                if ( environmentInfo.prevBlockPos.x < environmentInfo.currentBlockPos.x ) {
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[1];
                }else if ( environmentInfo.prevBlockPos.z > environmentInfo.currentBlockPos.z ) {
                    if ( environmentInfo.nextBlockPos.equals( environmentInfo.currentBlockPos ) ){
                        environmentInfo.nextBlockPos = new Vector3f( environmentInfo.currentBlockPos.x + 1f, environmentInfo.currentBlockPos.y, environmentInfo.currentBlockPos.z);
                    }
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[5];
                }else if ( environmentInfo.prevBlockPos.x > environmentInfo.currentBlockPos.x ) {
                    if ( environmentInfo.nextBlockPos.equals( environmentInfo.currentBlockPos ) ){
                        environmentInfo.nextBlockPos = new Vector3f( environmentInfo.currentBlockPos.x, environmentInfo.currentBlockPos.y, environmentInfo.currentBlockPos.z + 1f);
                    }
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[6];
                }else if ( environmentInfo.prevBlockPos.z < environmentInfo.currentBlockPos.z ) {
                    environmentInfo.pathDirection = RailsDirectionFinder.pathDirectionMap[3];
                }

                break;
        }

        switch( environmentInfo.currentBlock.getDirection() ){
            case BACK:
                if ( environmentInfo.pathDirection.z > 0 ){
                    environmentInfo.currentCornerSign = 1;
                }else{
                    environmentInfo.currentCornerSign = -1;
                }
                break;
            case FRONT:
                if ( environmentInfo.pathDirection.z > 0 ){
                    environmentInfo.currentCornerSign = -1;
                }else{
                    environmentInfo.currentCornerSign = 1;
                }
                break;
            case LEFT:
                if ( environmentInfo.pathDirection.z > 0 ){
                    environmentInfo.currentCornerSign = -1;
                }else{
                    environmentInfo.currentCornerSign = 1;
                }
                break;
            case RIGHT:
                if ( environmentInfo.pathDirection.z > 0 ){
                    environmentInfo.currentCornerSign = 1;
                }else{
                    environmentInfo.currentCornerSign = -1;
                }
                break;
        }

    };

    private void setYawOnPath( DynamicBlockComponent dynamicBlockComponent, Side side ){
        switch ( side ){
            case FRONT:
                if ( dynamicBlockComponent.getYaw() == 0 ){
                    dynamicBlockComponent.setYaw( 180 );
                }
            case BACK:
                if (  dynamicBlockComponent.getYaw() >= 360 || dynamicBlockComponent.getYaw() < 45 && dynamicBlockComponent.getYaw() > 0 ){
                    dynamicBlockComponent.setYaw( 0 );
                }

                if ( dynamicBlockComponent.getYaw() >= 135 && dynamicBlockComponent.getYaw() < 180 || dynamicBlockComponent.getYaw() > 180 && dynamicBlockComponent.getYaw() < 225 ){
                    dynamicBlockComponent.setYaw( 180 );
                }

                if ( dynamicBlockComponent.getYaw() != 180 && dynamicBlockComponent.getYaw() != 0 ){
                    dynamicBlockComponent.setYaw( 0 );
                }
                break;
            case LEFT:
                if ( dynamicBlockComponent.getYaw() == 0 ){
                    dynamicBlockComponent.setYaw( 270 );
                }
            case RIGHT:
                if ( dynamicBlockComponent.getYaw() == 0 || dynamicBlockComponent.getYaw() >= 45 && dynamicBlockComponent.getYaw() < 90 || dynamicBlockComponent.getYaw() > 90 && dynamicBlockComponent.getYaw() < 135 ){
                    dynamicBlockComponent.setYaw( 90 );
                    break;
                }

                if ( dynamicBlockComponent.getYaw() >= 225 && dynamicBlockComponent.getYaw() < 270 || dynamicBlockComponent.getYaw() > 270 && dynamicBlockComponent.getYaw() < 315){
                    dynamicBlockComponent.setYaw( 270 );
                }

                if ( dynamicBlockComponent.getYaw() != 90 && dynamicBlockComponent.getYaw() != 270 ){
                    dynamicBlockComponent.setYaw( 90 );
                }

                break;
        }
    }

    public void findDirection( DynamicBlockComponent dynamicBlockComponent, Block block, Vector3f blockPos ){

        DynamicBlockComponent.EnvironmentInfo environmentInfo = dynamicBlockComponent.environmentInfo;
        Side side = null;

        if ( environmentInfo.currentBlock.getAdjacentBlockType() == null ){
            return;
        }

        if ( environmentInfo.currentBlockPos != null ){
            if ( !environmentInfo.currentBlockPos.equals( blockPos ) ){
                environmentInfo.prevBlockPos = new Vector3f( blockPos );
                environmentInfo.currentBlockPos = blockPos;

                environmentInfo.prevBlock = environmentInfo.currentBlock;
                environmentInfo.currentBlock = block;
            }
        }else{
            environmentInfo.prevBlockPos = new Vector3f();
            environmentInfo.currentBlockPos = new Vector3f( blockPos );
        }

        switch( environmentInfo.currentBlock.getAdjacentBlockType() ){
            case SINGLE:
            case END:
            case CROSS:
            case PLAIN:
                side = environmentInfo.currentBlock.getDirection().rotateClockwise(1);
            case SLOPE:
                environmentInfo.lastCornerSign = 0;
                environmentInfo.pathDirection = new Vector3f(side.getVector3i().toVector3f());

                if ( environmentInfo.currentBlock.getAdjacentBlockType().equals(BlockAdjacentType.SLOPE) ){
                    Vector3f slide = new Vector3f( environmentInfo.pathDirection );
                    slide.y = -1f;
                    slide.scale(0.3f);
                    //velocityDescriptor.getDrive().add(slide);
                }else{
                   // pitch = 0f;
                }

                environmentInfo.pathDirection.absolute();
                setYawOnPath( dynamicBlockComponent, side );

                if ( environmentInfo.nextBlockPos == null ){
                    environmentInfo.nextBlockPos = new Vector3f( environmentInfo.currentBlockPos );
                }

                if ( environmentInfo.nextBlockPos.equals(environmentInfo.currentBlockPos) ){
                    environmentInfo.nextBlockPos.sub(environmentInfo.currentBlockPos, environmentInfo.prevBlockPos);
                    environmentInfo.nextBlockPos.add(environmentInfo.currentBlockPos, environmentInfo.nextBlockPos);
                }

                break;
            case CORNER:
                environmentInfo.nextBlockPos = null;
                setCornerDirection( dynamicBlockComponent );
                break;
            case TEE:
                break;
        }

    }


    private float setPitchOnPath( DynamicBlockComponent.EnvironmentInfo environmentInfo, float currentPitch ){

        if ( environmentInfo.currentBlock.getAdjacentBlockType() == null ){
            return currentPitch;
        }

        currentPitch = 0;

        if ( environmentInfo.currentBlock.getAdjacentBlockType().equals(BlockAdjacentType.SLOPE) ){
            currentPitch = 45;
        }
        return currentPitch;
    }

    private float setYawOnPath( DynamicBlockComponent.EnvironmentInfo environmentInfo, float currentYaw ){
        if ( environmentInfo.currentBlock.getAdjacentBlockType().equals( BlockAdjacentType.CORNER ) ){

            if ( environmentInfo.nextBlock.getAdjacentBlockType().equals( BlockAdjacentType.CORNER ) ){

                if ( environmentInfo.lastCornerSign == 0 ){
                    environmentInfo.lastCornerSign = environmentInfo.currentCornerSign;
                }

                float tYaw = currentYaw + ( environmentInfo.lastCornerSign ) * 45f;

                return tYaw;
            }

            environmentInfo.lastCornerSign = environmentInfo.currentCornerSign;

            float percent = environmentInfo.distanceMoved.length() / 0.007f;

            if ( percent > 100 ){
                currentYaw += ( environmentInfo.currentCornerSign ) * 90f;
            }else{
                currentYaw += ( environmentInfo.currentCornerSign ) * 90f * percent / 100;
            }

            if ( currentYaw < 0 ){
                currentYaw = 360 + currentYaw;
            }else if ( currentYaw > 360 ){
                currentYaw = currentYaw - 360;
            }

            return currentYaw;
        }

        return currentYaw;
    }

    public boolean isOnPath(DynamicBlockComponent dynamicBlockComp, Block block){

        for( String pathBlocks :  dynamicBlockComp.pathBlocks ){
            BlockUri blockUri = new BlockUri( pathBlocks );
            Block testBlock = block.getBlockFamily().getBlockFor( blockUri );
            if ( testBlock!=null && testBlock.getBlockFamily().getArchetypeBlock().equals( block.getBlockFamily().getArchetypeBlock() ) ){
                return true;
            }
        }

        return false;

    }

}
