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
package org.terasology.rails.trains.blocks.system.Tasks.Target;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.rails.trains.blocks.components.TrainRailComponent;
import org.terasology.rails.trains.blocks.system.Builder.Command;
import org.terasology.rails.trains.blocks.system.Builder.CommandHandler;
import org.terasology.rails.trains.blocks.system.Builder.TaskResult;
import org.terasology.rails.trains.blocks.system.Config;
import org.terasology.rails.trains.blocks.system.Misc.Orientation;
import org.terasology.rails.trains.blocks.system.Tasks.Task;
import org.terasology.rails.trains.blocks.system.Track;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuildToXYZTask implements Task {
    @Override
    public boolean run(CommandHandler commandHandler, Map<EntityRef, Track> tracks, Track selectedTrack, Vector3f position, Orientation orientation, boolean reverse) {
        float zone = 5f;
        boolean firstStraightTrack = true;
        boolean buildPass = true;
        float last = 0;
        float lastDif = 0;
        float toYaw = 0;
        float toPitch = 0;
        float withInY = 20;
        float withIn = 5;
        ArrayList<Command> commands = new ArrayList<>();

        if (tracks.isEmpty()) {
            return false;
        }

        Track lastTrack = tracks.get(tracks.size()-1);

        while (!((lastTrack.getPosition().x < position.x + (withIn / 2) && lastTrack.getPosition().x > position.x - (withIn / 2)) &&
                 (lastTrack.getPosition().z < position.z + (withIn / 2) && lastTrack.getPosition().z > position.z - (withIn / 2)) &&
                 (lastTrack.getPosition().y <= (position.y + (withInY / 2)) && lastTrack.getPosition().y >= (position.y - (withInY / 2)))) && buildPass) {
            Vector3f dir = new Vector3f(position);
            dir.sub(lastTrack.getPosition());
            toYaw = (float)(Math.atan2(dir.z, dir.x) * 180 / Math.PI);

            if(toYaw < 0) {
                toYaw += 360;
            }

            int totalAdjustments = (int)(toYaw / Config.STANDARD_ANGLE_CHANGE);

            if((toYaw % 15) > Config.STANDARD_ANGLE_CHANGE / 2) {
                totalAdjustments++;
            }

            toYaw = totalAdjustments * Config.STANDARD_ANGLE_CHANGE;

            if (lastTrack.getPosition().y <= (position.y + (withInY / 2)) && lastTrack.getPosition().y >= (position.y - (withInY / 2))) {
                toPitch = 0;
            } else if(position.y > 0) {
                toPitch = 90 + Config.STANDARD_ANGLE_CHANGE;
            } else {
                toPitch = 270 + Config.STANDARD_ANGLE_CHANGE;
            }

            if (lastTrack.getYaw() == toYaw && lastTrack.getPitch() == toPitch) {
                commands.clear();
                commands.add(new Command(true, TrainRailComponent.TrackType.STRAIGHT, position, new Orientation(0, 0, 0), false, reverse));
                TaskResult result = commandHandler.run(commands, tracks, selectedTrack, reverse);
                buildPass = result.success;

                if (buildPass) {
                    selectedTrack = result.track;
                }

                float distanceX = lastTrack.getPosition().x - position.x;
                float distanceZ = lastTrack.getPosition().z - position.z;

                float differnce = Math.abs(distanceX + distanceZ);

                if (!firstStraightTrack) {
                    if (differnce > lastDif) {
                        return false;
                    }
                }

                last = lastTrack.getPosition().x + lastTrack.getPosition().z;
                lastDif = differnce;

            } else {
                int yawDirection = 0;
                int pitchDirection = 0;
                if (lastTrack.getYaw() != toYaw) {
                    if (lastTrack.getYaw() - toYaw > 0) {
                        if (Math.abs(lastTrack.getYaw() - toYaw) < 180) {
                            yawDirection = -1; //Right
                        } else {
                            yawDirection = 1; //Left
                        }
                    } else {
                        if (Math.abs(toYaw - lastTrack.getYaw()) < 180) {
                            yawDirection = 1; //Left
                        } else {
                            yawDirection = -1; //Right
                        }

                    }
                }

                if (lastTrack.getPitch() != toPitch) {
                    if (lastTrack.getPitch() - toPitch > 0) {
                        if ((lastTrack.getPitch() - toPitch > 360 - lastTrack.getPitch())) {
                            pitchDirection = 1; //Up
                        } else {
                            pitchDirection = -1; //Down
                        }

                    } else {
                        if ((toPitch - lastTrack.getPitch() > 360 - toPitch)) {
                            pitchDirection = -1; //Down
                        } else {
                            pitchDirection = 1; //Up
                        }
                    }
                }
                commands.clear();
                commands.add(new Command(true, TrainRailComponent.TrackType.CUSTOM, position, new Orientation(Config.STANDARD_ANGLE_CHANGE * yawDirection, Config.STANDARD_ANGLE_CHANGE * pitchDirection, 0), false, reverse));
                TaskResult result = commandHandler.run(commands, tracks, selectedTrack, reverse);
                buildPass = result.success;

                if (buildPass) {
                    selectedTrack = result.track;
                }

            }
            lastTrack = tracks.get(tracks.size() - 1);

        }


        if ((lastTrack.getPosition().x < position.x + (withIn / 2) && lastTrack.getPosition().x > position.x - (withIn / 2)) &&
            (lastTrack.getPosition().z < position.z + (withIn / 2) && lastTrack.getPosition().z > position.z - (withIn / 2)) &&
            (lastTrack.getPosition().y <= (position.y + (withIn / 2)) && lastTrack.getPosition().y >= (position.y - (withInY / 2)))) {
            return true;
        } else {
            return false;
        }

    }
}
