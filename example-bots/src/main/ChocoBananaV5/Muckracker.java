package ChocoBananaV5;

import battlecode.common.*;

public class Muckracker extends UnitMethods {

    double RANDOM_DIR_PROB = 0.3;
    MapLocation origin, current;
    boolean protect = false, found=false, done = false, nearCenter=false;
    MapLocation scoutTarget;
    int parentID;
    int nothingFoundCount=0, NOTHING_FOUND_LENGTH=150;
    Direction scoutDirection;
    int scoutDirectionFlag;
    int FOLLOWING_LENGTH = 50, followingCount = 0;
    int allyNear = 0;

    double LISTEN_PROB = 0.8;

    public Muckracker(RobotController rc) throws  GameActionException {
        int d;
        this.rc = rc;
        this.rt = RobotType.MUCKRAKER;
        r = this.rt.sensorRadiusSquared;
        origin = rc.getLocation();
        current = origin;
        MapLocation neighbour;
        RobotInfo robot;
        for (Direction dir: directions){

            neighbour = rc.getLocation().add(dir);
            if (rc.canSenseLocation(neighbour)) {
                robot = rc.senseRobotAtLocation(neighbour);
                if (robot != null && robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    parentID = robot.ID;
                    int parentFlag = rc.getFlag(parentID);
                    if ( (Math.random() < LISTEN_PROB) && (parentFlag & Flags.FOUND) == Flags.FOUND){
                        if ((parentFlag & Flags.FAR) == Flags.FAR){
                            if((parentFlag & Flags.MEDIUM) == Flags.MEDIUM) {
                                d = 35;
                            } else {
                                d = 45;
                            }
                        } else if((parentFlag & Flags.MEDIUM) == Flags.MEDIUM){
                            d = 25;
                        } else {
                            d = 15;
                        }
                        if ((parentFlag & Flags.LEFT) == Flags.LEFT){
                            scoutDirectionFlag = Flags.LEFT;
                            scoutTarget = current.translate(-d, 0);
                        } else if ((parentFlag & Flags.UPLEFT) == Flags.UPLEFT){
                            scoutDirectionFlag = Flags.UPLEFT;
                            scoutTarget = current.translate(-d, d);
                        } else if ((parentFlag & Flags.UP) == Flags.UP){
                            scoutDirectionFlag = Flags.UP;
                            scoutTarget = current.translate(0, d);
                        } else if ((parentFlag & Flags.UPRIGHT) == Flags.UPRIGHT){
                            scoutDirectionFlag = Flags.UPRIGHT;
                            scoutTarget = current.translate(d, d);
                        } else if ((parentFlag & Flags.RIGHT) == Flags.RIGHT){
                            scoutDirectionFlag = Flags.RIGHT;
                            scoutTarget = current.translate(d, 0);
                        } else if ((parentFlag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT){
                            scoutDirectionFlag = Flags.DOWNRIGHT;
                            scoutTarget = current.translate(d, -d);
                        } else if ((parentFlag & Flags.DOWN) == Flags.DOWN){
                            scoutDirectionFlag = Flags.DOWN;
                            scoutTarget = current.translate(0, -d);
                        } else if ((parentFlag & Flags.DOWNLEFT) == Flags.DOWNLEFT){
                            scoutDirectionFlag = Flags.DOWNLEFT;
                            scoutTarget = current.translate(-d, -d);
                        }
                        followingCount = FOLLOWING_LENGTH;

                    } else {
                        dir = dir.opposite();
                        scoutTarget = current.translate(dir.getDeltaX()*60, dir.getDeltaY()*60);
                        scoutDirection = dir;
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
                    }
                }
            }
        }
    }

    public void senseEnemies(){
        MapLocation loc = current;
        int conviction = rc.getConviction();
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam().opponent())) {

            switch ( robot.type){
                case POLITICIAN:
                    flag |= Flags.NEARPOL;
                    if (nearCenter) {
                        vector.update(robot.location, loc, 2000);
                    }
                    break;
                case SLANDERER:
                    flag |= Flags.NEARSLAND;
                    if (rt == RobotType.MUCKRAKER) {
                        vector.update(robot.location, loc, 4000);
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

        if (rc.getRoundNum() > 1000){
            for (RobotInfo robot : rc.senseNearbyRobots(r, Team.NEUTRAL)) {
                flag |= Flags.NEARCENTER;
                flag |= Flags.FOUND_NEUTRAL_EC;
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
                    //if (!protect) { ;
                    //    vector.update(robot.location.add(scootDirection), loc, -8);
                    //}
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
            vector.update(robot.location, loc, -10);

            if (count< 20) {
                if (rc.canGetFlag(robot.ID)) {
                    int allyFlag = rc.getFlag(robot.ID);

                    if ((allyFlag & Flags.NEARCENTER) == Flags.NEARCENTER){
                        flag |= Flags.NEARCENTER_2;
                    }

                    if ((allyFlag & Flags.OBSTRUCTED) == Flags.OBSTRUCTED){
                        vector.update(robot.location, loc, -1000);
                    }

                    if ( followingCount==0 && (allyFlag & Flags.DANGER) == Flags.DANGER) {
                        vector.update(robot.location, loc, 65);
                    }

                    if ( robot.type != RobotType.MUCKRAKER && (allyFlag & Flags.NEARSLAND) == Flags.NEARSLAND) {
                        vector.update(robot.location, loc, 1000);
                    }
                    boolean notnear = (flag & Flags.NEARCENTER)!=Flags.NEARCENTER;

                    if ((allyFlag & Flags.NEARCENTER) == Flags.NEARCENTER) {
                        if ( rc.getConviction() == 1 && !protect && (turnCount <= 7)){
                            protect = true;
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
        int obtainedFlag, d;
        if (rc.canGetFlag(parentID)){
            obtainedFlag = rc.getFlag(parentID);
            if ((obtainedFlag & Flags.DANGER) == Flags.DANGER){
                if (vector.distance(origin, current) < 10) {
                    vector.update(origin, current,1000);
                }
            } else if ( Math.random() < LISTEN_PROB && (obtainedFlag & Flags.FOUND) == Flags.FOUND && done){
                if ((obtainedFlag & Flags.FAR) == Flags.FAR){
                    if((obtainedFlag & Flags.MEDIUM) == Flags.MEDIUM) {
                        d = 35;
                    } else {
                        d = 45;
                    }
                } else if((obtainedFlag & Flags.MEDIUM) == Flags.MEDIUM){
                    d = 25;
                } else {
                    d = 15;
                }
                if ((obtainedFlag & Flags.LEFT) == Flags.LEFT){
                    scoutTarget = origin.translate(-d, 0);
                } else if ((obtainedFlag & Flags.UPLEFT) == Flags.UPLEFT){
                    scoutTarget = origin.translate(-d, d);
                } else if ((obtainedFlag & Flags.UP) == Flags.UP){
                    scoutTarget = origin.translate(0, d);
                } else if ((obtainedFlag & Flags.UPRIGHT) == Flags.UPRIGHT){
                    scoutTarget = origin.translate(d, d);
                } else if ((obtainedFlag & Flags.RIGHT) == Flags.RIGHT){
                    scoutTarget = origin.translate(d, 0);
                } else if ((obtainedFlag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT){
                    scoutTarget = origin.translate(d, -d);
                } else if ((obtainedFlag & Flags.DOWN) == Flags.DOWN){
                    scoutTarget = origin.translate(0, -d);
                } else if ((obtainedFlag & Flags.DOWNLEFT) == Flags.DOWNLEFT){
                    scoutTarget = origin.translate(-d, -d);
                }
                followingCount = FOLLOWING_LENGTH;

                Direction scoutDir = current.directionTo(scoutTarget);
                switch (scoutDir){
                    case EAST:
                        scoutDirectionFlag = Flags.RIGHT;
                        break;
                    case WEST:
                        scoutDirectionFlag = Flags.LEFT;
                        break;
                    case SOUTH:
                        scoutDirectionFlag = Flags.DOWN;
                        break;
                    case NORTH:
                        scoutDirectionFlag = Flags.UP;
                        break;
                    case NORTHEAST:
                        scoutDirectionFlag = Flags.UPRIGHT;
                        break;
                    case SOUTHEAST:
                        scoutDirectionFlag = Flags.DOWNRIGHT;
                        break;
                    case NORTHWEST:
                        scoutDirectionFlag = Flags.UPLEFT;
                        break;
                    case SOUTHWEST:
                        scoutDirectionFlag = Flags.DOWNLEFT;
                        break;
                    default:
                        break;
                }

            }
        }
    }



    public void run() throws GameActionException{
        found = false;
        current = rc.getLocation();
        Direction dir;
        senseEnemies();
        senseAllies();
        readParent();
        updateObstruction();

        if ( rc.getRoundNum() > 50 && allyNear < 4 && ((flag & Flags.NEARSELFCENTER)==Flags.NEARSELFCENTER)) {
            vector.update(origin, current, 1000);
        }
        if (found) {
            flag |= Flags.FOUND;
            double distance = Math.sqrt(current.distanceSquaredTo(origin));
            if (distance > 30) {
                flag |= Flags.FAR;
            } else if (distance > 20) {
                flag |= (Flags.FAR | Flags.MEDIUM);
            } else if (distance > 15) {
                flag |= Flags.MEDIUM;
            }

            dir = current.directionTo(origin).opposite();
            scoutDirection = dir;
            switch (dir) {
                case EAST:
                    scoutDirectionFlag = Flags.RIGHT;
                    break;
                case WEST:
                    scoutDirectionFlag = Flags.LEFT;
                    break;
                case SOUTH:
                    scoutDirectionFlag = Flags.DOWN;
                    break;
                case NORTH:
                    scoutDirectionFlag = Flags.UP;
                    break;
                case NORTHEAST:
                    scoutDirectionFlag = Flags.UPRIGHT;
                    break;
                case SOUTHEAST:
                    scoutDirectionFlag = Flags.DOWNRIGHT;
                    break;
                case NORTHWEST:
                    scoutDirectionFlag = Flags.UPLEFT;
                    break;
                case SOUTHWEST:
                    scoutDirectionFlag = Flags.DOWNLEFT;
                    break;
                default:
                    break;
            }
        } else {
            nothingFoundCount++;
            if (nothingFoundCount==NOTHING_FOUND_LENGTH){
                nothingFoundCount =0;
                nearEdge= true;
            }
        }
        if (!nearEdge) {
            vector.update(scoutTarget, current, 500);
            if (followingCount > 0) {
                flag |= Flags.DANGER; // used to signal others that this units follows EC's instructed direction
                followingCount--;
            }
        } else {
            flag |= Flags.NOT_SCOUTABLE;
            Direction newDir;
            do {
                newDir = directions[ (int) (Math.random() * 8)];

            } while (newDir == scoutDirection);
            scoutDirection = newDir;
            scoutTarget = current.translate(newDir.getDeltaX()*60, newDir.getDeltaY()*60);
            switch (newDir) {
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
            vector.update(scoutTarget, current, 500);
        }

        flag |= scoutDirectionFlag;

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
