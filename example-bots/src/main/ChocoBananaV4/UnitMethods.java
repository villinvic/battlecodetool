package ChocoBananaV4;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class UnitMethods {

    RobotType rt;
    int r;

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST,
    };

    static final List<Direction> diags = Arrays.asList(
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST
    );

    RobotController rc;
    int flag = 0, persistentFLag = 0;
    static int turnCount = 0;
    boolean nearEdge= false;
    Vect vector = new Vect();

    public void step() throws GameActionException {
        vector.reset();
        turnCount ++;
        flag = 0;
    }

    public void buildRobot() throws GameActionException {}
    public int bid(){return 0;}
    public boolean attack() throws GameActionException {return false;} ;

    public void updateObstruction() throws GameActionException{
        MapLocation loc, neighbour;
        loc = rc.getLocation();
        int max = 8, count = 0;
        for( Direction dir: directions) {
            neighbour = loc.add(dir);
            if (rc.onTheMap(neighbour)) {
                if (rc.isLocationOccupied(neighbour)) {
                    count++;
                }
            } else {
                max--;
            }
        }
        if (count == max){
            flag |= Flags.OBSTRUCTED;
        }
    }

    public Direction move() throws GameActionException{
        if (vector.isnull()){
            return null;
        }
        MapLocation center = rc.getLocation();
        MapLocation newLoc;
        Direction bestDir = null;
        nearEdge = false;
        boolean exists = false;
        double value, bestValue=0;
        double ndx = vector.x/vector.totalWeight;
        double ndy =  vector.y/vector.totalWeight;
        int dx, dy;
        if (Math.abs(ndx) < 0.2){
            dx = 0;
        } else {
            dx = (int) Math.signum( ndx);
        }

        if (Math.abs(ndy) < 0.2){
            dy = 0;
        } else {
            dy = (int) Math.signum( ndy);
        }

        if (dx>0){
            newLoc = center.add( Direction.EAST);
            if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                bestValue = 1.0 * rc.sensePassability(newLoc);
                bestDir = Direction.EAST;
                exists = true;
            } else if (!rc.onTheMap(newLoc)){
                nearEdge = true;
            }
            if (dy>0){
                newLoc = center.add( Direction.NORTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 2.0 * rc.sensePassability(newLoc);
                    if (value > bestValue) {
                        bestValue = value;
                        bestDir = Direction.NORTHEAST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

                newLoc = center.add( Direction.NORTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTH;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

            } else if (dy==0){
                newLoc = center.add( Direction.NORTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHEAST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

                newLoc = center.add( Direction.SOUTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHEAST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

            } else {
                newLoc = center.add( Direction.SOUTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 2.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHEAST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

                newLoc = center.add( Direction.SOUTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTH;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }
            }
        } else if (dx==0) {
            if (dy>0){
                newLoc = center.add( Direction.NORTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTH;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

                newLoc = center.add( Direction.NORTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHEAST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

                newLoc = center.add( Direction.NORTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHWEST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

            } else if (dy==0){
                return null;

            } else {
                newLoc = center.add( Direction.SOUTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTH;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

                newLoc = center.add( Direction.SOUTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHEAST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

                newLoc = center.add( Direction.SOUTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHWEST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }
            }
        } else {
            newLoc = center.add( Direction.WEST);
            if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                bestValue = 1.0 * rc.sensePassability(newLoc);
                bestDir = Direction.WEST;
                exists = true;
            } else if (!rc.onTheMap(newLoc)){
                nearEdge = true;
            }
            if (dy>0){
                newLoc = center.add( Direction.NORTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 2.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHWEST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

                newLoc = center.add( Direction.NORTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTH;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

            } else if (dy==0){
                newLoc = center.add( Direction.NORTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHWEST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

                newLoc = center.add( Direction.SOUTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHWEST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

            } else {
                newLoc = center.add( Direction.SOUTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 2.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHWEST;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }

                newLoc = center.add( Direction.SOUTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTH;
                        exists = true;
                    }
                } else if (!rc.onTheMap(newLoc)){
                    nearEdge = true;
                }
            }
        }

        if (exists){
            nearEdge = false;
        }

        return bestDir;
    };

    Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    Direction randomDirectionv2() throws  GameActionException {
        MapLocation loc = rc.getLocation();
        MapLocation next;
        double bestValue=0, value;
        Direction bestDir=randomDirection();
        List<Direction> dirs = Arrays.asList(directions);
        Collections.shuffle(dirs);
        for( Direction dir: dirs){
            next = loc.add(dir);
            if (rc.onTheMap(next) && !rc.isLocationOccupied(next)){
                if (diags.contains(dir)) {
                    value = 2 * rc.sensePassability(next);
                } else {
                    value = rc.sensePassability(next);
                }

                if (value > bestValue){
                    bestValue = value;
                    bestDir = dir;
                }

            }
        }

        return bestDir;
    }



    public abstract void senseEnemies() throws GameActionException;
    public abstract void senseAllies() throws GameActionException;
    public abstract void run() throws GameActionException;





}
