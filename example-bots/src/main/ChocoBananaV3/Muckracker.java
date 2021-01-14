package ChocoBananaV3;

import battlecode.common.*;

public class Muckracker extends UnitMethods {

    double RANDOM_DIR_PROB = 0.3;
    MapLocation origin, current;
    boolean protect = false, found=false, done = false, nearCenter=false;
    Direction scootDirection;
    int parentID;
    int scoutDirectionFlag;
    int FOLLOWING_LENGTH = 35, followingCount = 0;
    int allyNear = 0;

    double PROPAGATE_FOUND_SIGNAL_PROB = 0.7;

    public Muckracker(RobotController rc) throws  GameActionException {
        this.rc = rc;
        this.rt = RobotType.MUCKRAKER;
        this.r = this.rt.sensorRadiusSquared;
        this.origin = rc.getLocation();
        MapLocation neighbour;
        RobotInfo robot;
        for (Direction dir: directions){

            neighbour = rc.getLocation().add(dir);
            if (rc.canSenseLocation(neighbour)) {
                robot = rc.senseRobotAtLocation(neighbour);
                if (robot != null && robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    parentID = robot.ID;
                    int parentFlag = rc.getFlag(parentID);
                    if ((parentFlag & Flags.FOUND) == Flags.FOUND){
                        if ((parentFlag & Flags.LEFT) == Flags.LEFT){
                            scoutDirectionFlag = Flags.LEFT;
                            scootDirection = Direction.WEST;
                        } else if ((parentFlag & Flags.UPLEFT) == Flags.UPLEFT){
                            scoutDirectionFlag = Flags.UPLEFT;
                            scootDirection = Direction.NORTHWEST;
                        } else if ((parentFlag & Flags.UP) == Flags.UP){
                            scoutDirectionFlag = Flags.UP;
                            scootDirection = Direction.NORTH;
                        } else if ((parentFlag & Flags.UPRIGHT) == Flags.UPRIGHT){
                            scoutDirectionFlag = Flags.UPRIGHT;
                            scootDirection = Direction.NORTHEAST;
                        } else if ((parentFlag & Flags.RIGHT) == Flags.RIGHT){
                            scoutDirectionFlag = Flags.RIGHT;
                            scootDirection = Direction.EAST;
                        } else if ((parentFlag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT){
                            scoutDirectionFlag = Flags.DOWNRIGHT;
                            scootDirection = Direction.SOUTHEAST;
                        } else if ((parentFlag & Flags.DOWN) == Flags.DOWN){
                            scoutDirectionFlag = Flags.DOWN;
                            scootDirection = Direction.SOUTH;
                        } else if ((parentFlag & Flags.DOWNLEFT) == Flags.DOWNLEFT){
                            scoutDirectionFlag = Flags.DOWNLEFT;
                            scootDirection = Direction.SOUTHWEST;
                        }
                        followingCount = FOLLOWING_LENGTH;

                    } else {
                        dir = dir.opposite();
                        scootDirection = dir;
                        switch (dir) {
                            case WEST:
                                scoutDirectionFlag = Flags.LEFT;
                                break;
                            case NORTHWEST:
                                scoutDirectionFlag = Flags.UPLEFT;
                                break;
                            case NORTH:
                                scoutDirectionFlag = Flags.UP;
                                break;
                            case NORTHEAST:
                                scoutDirectionFlag = Flags.UPRIGHT;
                                break;
                            case EAST:
                                scoutDirectionFlag = Flags.RIGHT;
                                break;
                            case SOUTHEAST:
                                scoutDirectionFlag = Flags.DOWNRIGHT;
                                break;
                            case SOUTH:
                                scoutDirectionFlag = Flags.DOWN;
                                break;
                            case SOUTHWEST:
                                scoutDirectionFlag = Flags.DOWNLEFT;
                                break;
                            default:
                                break;
                        }
                        break;
                    }
                }
            }
        }
    }

    public void senseEnemies(){
        MapLocation loc = current;
        int conviction = rc.getConviction();
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam().opponent())) {
            flag |= Flags.NEAROPP;

            switch ( robot.type){
                case POLITICIAN:
                    flag |= Flags.NEARPOL;
                    if (protect || nearCenter) {
                        vector.update(robot.location, loc, 5000);
                    }
                    break;
                case SLANDERER:
                    flag |= Flags.NEARSLAND;
                    if (rt == RobotType.MUCKRAKER) {
                        vector.update(robot.location, loc, 2000);
                    }
                    break;
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARCENTER;
                    found = true;
                    if (conviction==1 && !protect && (turnCount < 7)) {
                        protect = true;
                    } else {
                        vector.update(robot.location, loc, -250);
                    }
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARMUCK;
                    break;
                default:
                    break;
            }
        }

        if (conviction==1){
            for (RobotInfo robot : rc.senseNearbyRobots(r, Team.NEUTRAL)) {
                vector.update(robot.location, loc, -250);
                flag |= Flags.NEARCENTER;
                found = true;
                break;
            }
        }

    };

    public void senseAllies() throws GameActionException{
        int count = 0, muckNear=0;
        nearCenter = false;
        MapLocation loc = current;
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam())) {
            count++;
            switch (robot.type){
                case ENLIGHTENMENT_CENTER:
                    if (!protect) { ;
                        vector.update(robot.location.add(scootDirection), loc, -8);
                    }
                    flag |= Flags.NEARSELFCENTER;
                    nearCenter = true;
                    break;
                case POLITICIAN:
                    flag |= Flags.NEARSELFPOL;
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARSELFMUCK;
                    muckNear++;
                    break;
                case SLANDERER:
                    flag |= Flags.NEARSELFSLAND;
                    break;
                default:
                    break;
            }

            // Dispersion strategy
            vector.update(robot.location, loc, -1);

            if (count< 14) {
                if (rc.canGetFlag(robot.ID)) {
                    int allyFlag = rc.getFlag(robot.ID);

                    if ((allyFlag & Flags.OBSTRUCTED) == Flags.OBSTRUCTED){
                        vector.update(robot.location, loc, -125);
                    }

                    if ( followingCount==0 && (allyFlag & Flags.DANGER) == Flags.DANGER) {
                        vector.update(robot.location, loc, 65);
                        if (Math.random() < PROPAGATE_FOUND_SIGNAL_PROB && robot.type != RobotType.ENLIGHTENMENT_CENTER){
                            if ((allyFlag & Flags.LEFT) == Flags.LEFT){
                                scootDirection = Direction.WEST;
                            } else if ((allyFlag & Flags.UPLEFT) == Flags.UPLEFT){
                                scootDirection = Direction.NORTHWEST;
                            } else if ((allyFlag & Flags.UP) == Flags.UP){
                                scootDirection = Direction.NORTH;
                            } else if ((allyFlag & Flags.UPRIGHT) == Flags.UPRIGHT){
                                scootDirection = Direction.NORTHEAST;
                            } else if ((allyFlag & Flags.RIGHT) == Flags.RIGHT){
                                scootDirection = Direction.EAST;
                            } else if ((allyFlag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT){
                                scootDirection = Direction.SOUTHEAST;
                            } else if ((allyFlag & Flags.DOWN) == Flags.DOWN){
                                scootDirection = Direction.SOUTH;
                            } else if ((allyFlag & Flags.DOWNLEFT) == Flags.DOWNLEFT){
                                scootDirection = Direction.SOUTHWEST;
                            }
                            followingCount = FOLLOWING_LENGTH;

                        }
                    }

                    if ( robot.type != RobotType.MUCKRAKER && (allyFlag & Flags.NEARSLAND) == Flags.NEARSLAND) {
                        vector.update(robot.location, loc, 100);
                    }

                    boolean notnear = (flag & Flags.NEARCENTER)!=Flags.NEARCENTER;

                    if ((allyFlag & Flags.NEARCENTER) == Flags.NEARCENTER) {
                        if ( rc.getConviction() == 1 && !protect && (turnCount <= 7)){
                            protect = true;
                        }

                        flag |= Flags.NEARCENTER_2;
                        if (notnear) {
                            vector.update(robot.location, loc, 4);
                        }
                    } else if ((allyFlag & Flags.NEARCENTER_2) == Flags.NEARCENTER_2) {
                        if (notnear) {
                            vector.update(robot.location, loc, 2);
                        }
                        flag |= Flags.NEARCENTER_3;
                    } else if ((allyFlag & Flags.NEARCENTER_3) == Flags.NEARCENTER_3) {
                        if (notnear) {
                            vector.update(robot.location, loc, 1);
                        }
                    }

                    if ((allyFlag & Flags.NEARSELFCENTER) == Flags.NEARSELFCENTER) {
                        if (notnear) {
                            vector.update(robot.location, loc, -4);
                        }
                        flag |= Flags.NEARSELFCENTER_2;
                    } else if ((allyFlag & Flags.NEARSELFCENTER_2) == Flags.NEARSELFCENTER_2) {
                        if (notnear) {
                            vector.update(robot.location, loc, -2);
                        }
                        flag |= Flags.NEARSELFCENTER_3;
                    } else if ((allyFlag & Flags.NEARSELFCENTER_3) == Flags.NEARSELFCENTER_3){
                        if (notnear) {
                            vector.update(robot.location, loc, -1);
                        }
                    }
                }
            }
        }

        allyNear = muckNear;

    };

    public boolean attack() throws GameActionException {
        MapLocation toExpose = null;
        int bestConviction=0;
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, rc.getTeam().opponent())) {
            if (robot.type.canBeExposed()) {
                // Expose most powerful slanderer first
                if (robot.conviction > bestConviction){
                    toExpose = robot.location;
                    bestConviction = robot.conviction;
                }
            }
        }

        if (toExpose != null && rc.canExpose(toExpose)){
            rc.expose( toExpose);
            return true;
        }
        return false;
    }

    public void readParent() throws GameActionException{
        int obtainedFlag;
        if (rc.canGetFlag(parentID)){
            obtainedFlag = rc.getFlag(parentID);
            if ((obtainedFlag & Flags.DANGER) == Flags.DANGER){
                if (vector.distance(origin, current) < 15) {
                    vector.update(origin, current,1000);
                }
            }
        }
    }



    public void run() throws GameActionException{
        current = rc.getLocation();
        Direction dir;
        senseEnemies();
        senseAllies();
        readParent();
        updateObstruction();

        if (protect || (allyNear < 4 && ((flag & Flags.NEARSELFCENTER)==Flags.NEARSELFCENTER) && rc.getRoundNum()>100)){
            vector.update(origin.add(scootDirection), rc.getLocation(), 250);
        } else {
            flag |= scoutDirectionFlag;

            if (!done) {
                if (found) {
                    flag |= Flags.FOUND;
                    done = true;
                } else if (!nearEdge) {
                    vector.update(scootDirection, 500);
                    if (followingCount > 0) {
                        flag |= Flags.DANGER; // used to signal others that this units follows EC's instructed direction
                        followingCount--;
                    }
                } else {
                    done = true;
                }
            }
        }

        if (!attack()){
            dir = move();
            if (dir == null) {
                if ( Math.random() < RANDOM_DIR_PROB){
                    dir = randomDirection();
                } else {
                    dir = randomDirectionv2();
                }
            }
            if (rc.canMove(dir)){
                rc.move(dir);
            }
        }
    }

}
