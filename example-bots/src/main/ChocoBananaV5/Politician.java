package ChocoBananaV5;

import battlecode.common.*;

public class Politician extends UnitMethods {
    static int speechConviction;
    int parentID;
    MapLocation followTarget, origin;
    int obstructedCount, OBSTRUCTED_LENGTH=30;
    int followingDirectionFlag;
    boolean nearSelfSland = false;
    int FOLLOWING_LENGTH = 150, followingCount = 0;
    double  RANDOM_DIR_PROB = 0.4,
            PROPAGATE_FOUND_SIGNAL_PROB = 0.,
            LISTEN_EC_ORDER_PROB = 1.0;
    boolean found = false;

    public Politician(){
    }

    public Politician(RobotController rc) throws GameActionException {
        int d;
        this.rc = rc;
        this.rt = RobotType.POLITICIAN;
        this.r = this.rt.sensorRadiusSquared;
        origin = rc.getLocation();
        MapLocation neighbour;
        RobotInfo robot;

        MapLocation current = origin;

        for (Direction dir: directions) {
            neighbour = rc.getLocation().add(dir);
            if (rc.canSenseLocation(neighbour)) {
                robot = rc.senseRobotAtLocation(neighbour);
                if (robot != null && robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    parentID = robot.ID;
                    int parentFlag = rc.getFlag(parentID);
                    if ((parentFlag & Flags.FOUND) == Flags.FOUND){
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
                            followingDirectionFlag = Flags.LEFT;
                            followTarget = current.translate(-d, 0);
                        } else if ((parentFlag & Flags.UPLEFT) == Flags.UPLEFT){
                            followingDirectionFlag = Flags.UPLEFT;
                            followTarget = current.translate(-d, d);
                        } else if ((parentFlag & Flags.UP) == Flags.UP){
                            followingDirectionFlag = Flags.UP;
                            followTarget = current.translate(0, d);
                        } else if ((parentFlag & Flags.UPRIGHT) == Flags.UPRIGHT){
                            followingDirectionFlag = Flags.UPRIGHT;
                            followTarget = current.translate(d, d);
                        } else if ((parentFlag & Flags.RIGHT) == Flags.RIGHT){
                            followingDirectionFlag = Flags.RIGHT;
                            followTarget = current.translate(d, 0);
                        } else if ((parentFlag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT){
                            followingDirectionFlag = Flags.DOWNRIGHT;
                            followTarget = current.translate(d, -d);
                        } else if ((parentFlag & Flags.DOWN) == Flags.DOWN){
                            followingDirectionFlag = Flags.DOWN;
                            followTarget = current.translate(0, -d);
                        } else if ((parentFlag & Flags.DOWNLEFT) == Flags.DOWNLEFT){
                            followingDirectionFlag = Flags.DOWNLEFT;
                            followTarget = current.translate(-d, -d);
                        }
                        followingCount = FOLLOWING_LENGTH;

                    }
                    break;
                }
            }
        }
    }


    public void senseEnemies(){
        MapLocation loc = rc.getLocation();
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam().opponent())) {

            switch ( robot.type){
                case POLITICIAN:
                    flag |= Flags.NEARPOL;
                    break;
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARCENTER;
                    found = true;
                    if (rc.getConviction() > 10) {
                        vector.update(robot.location, loc, 4000);
                    }
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARMUCK;
                    if (speechConviction >= robot.conviction) {
                        vector.update(robot.location, loc, 1);
                    }
                    break;
                default:
                    break;
            }
        }

        for (RobotInfo robot2 : rc.senseNearbyRobots(r, Team.NEUTRAL)){
            flag |= Flags.NEARCENTER;
            if (rc.getConviction() > 10) {
                vector.update(robot2.location, loc, 4000);
            }
        }
    };

    public void senseAllies() throws GameActionException{
        nearSelfSland = false;
        MapLocation loc = rc.getLocation();
        int count = 0;
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam())) {
            count ++;
            switch (robot.type){
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARSELFCENTER;
                    break;
                case POLITICIAN:
                    flag |= Flags.NEARSELFPOL;
                    if ((flag & Flags.NEARSELFCENTER ) == Flags.NEARSELFCENTER){
                        vector.update(robot.location, loc, -100);
                    } else if ((flag & Flags.NEARCENTER ) == Flags.NEARCENTER){
                        vector.update(robot.location, loc, -4000);
                    } else if ((flag & Flags.NEARCENTER ) == Flags.NEARCENTER_2){
                        vector.update(robot.location, loc, -600);
                    } else {
                        vector.update(robot.location, loc, -100);
                    }

                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARSELFMUCK;
                    break;
                default:
                    break;
            }

            // Dispersion strategy
            if (followingCount == 0){
                vector.update(robot.location, loc, -200);
            } else {
                vector.update(robot.location, loc, -10);
            }

            if (count< 20) {
                if (rc.canGetFlag(robot.ID)) {
                    int allyFlag = rc.getFlag(robot.ID);



                    if ((allyFlag & Flags.NEARCENTER_2) == Flags.NEARCENTER_2){
                        vector.update(robot.location, loc, 150);
                    }

                    if ((allyFlag & Flags.NEARCENTER) == Flags.NEARCENTER){
                        vector.update(robot.location, loc, 250);
                        flag |= Flags.NEARCENTER_2;
                    }

                    if ((allyFlag & Flags.OBSTRUCTED) == Flags.OBSTRUCTED){
                        vector.update(robot.location, loc, -500);
                    }

                    if ( followingCount==0 && (allyFlag & Flags.DANGER) == Flags.DANGER) {
                        vector.update(robot.location, loc, 10);
                        if (Math.random() < PROPAGATE_FOUND_SIGNAL_PROB && robot.type != RobotType.ENLIGHTENMENT_CENTER){
                            int d;
                            MapLocation current = rc.getLocation();
                            if ((allyFlag & Flags.FAR) == Flags.FAR){
                                if((allyFlag & Flags.MEDIUM) == Flags.MEDIUM) {
                                    d = 40;
                                } else {
                                    d = 50;
                                }
                            } else if((allyFlag & Flags.MEDIUM) == Flags.MEDIUM){
                                d = 30;
                            } else {
                                d = 20;
                            }
                            if ((allyFlag & Flags.LEFT) == Flags.LEFT){
                                followingDirectionFlag = Flags.LEFT;
                                followTarget = current.translate(-d, 0);
                            } else if ((allyFlag & Flags.UPLEFT) == Flags.UPLEFT){
                                followingDirectionFlag = Flags.UPLEFT;
                                followTarget = current.translate(-d, d);
                            } else if ((followingCount & Flags.UP) == Flags.UP){
                                followingDirectionFlag = Flags.UP;
                                followTarget = current.translate(0, d);
                            } else if ((followingCount & Flags.UPRIGHT) == Flags.UPRIGHT){
                                followingDirectionFlag = Flags.UPRIGHT;
                                followTarget = current.translate(d, d);
                            } else if ((followingCount & Flags.RIGHT) == Flags.RIGHT){
                                followingDirectionFlag = Flags.RIGHT;
                                followTarget = current.translate(d, 0);
                            } else if ((followingCount & Flags.DOWNRIGHT) == Flags.DOWNRIGHT){
                                followingDirectionFlag = Flags.DOWNRIGHT;
                                followTarget = current.translate(d, -d);
                            } else if ((followingCount & Flags.DOWN) == Flags.DOWN){
                                followingDirectionFlag = Flags.DOWN;
                                followTarget = current.translate(0, -d);
                            } else if ((followingCount & Flags.DOWNLEFT) == Flags.DOWNLEFT){
                                followingDirectionFlag = Flags.DOWNLEFT;
                                followTarget = current.translate(-d, -d);
                            }

                            followingCount = FOLLOWING_LENGTH;

                        }
                    }


                    if ((allyFlag & Flags.NEARSELFSLAND) == Flags.NEARSELFSLAND) {
                        nearSelfSland = true;
                    }
                }
            }
        }
    };

    public boolean attack() throws GameActionException {
        int actionRadius = rt.actionRadiusSquared;
        int score = computeAttackScore(actionRadius);
        if (score>=3) {
            if (rc.canEmpower(actionRadius)){
                rc.empower(actionRadius);
                return true;
            }
        }

        return false;

    }

    /**
     *
     * @return score of politician action if empowering
     */
    public int computeAttackScore(int actionRadius){
        int oppcount=0, allycount=0, score = 0;
        boolean center = false;

        allycount+= rc.senseNearbyRobots(actionRadius, rc.getTeam()).length;
        score+= 5*rc.senseNearbyRobots(actionRadius, Team.NEUTRAL).length;
        for (RobotInfo robot: rc.senseNearbyRobots(actionRadius, rc.getTeam().opponent()) ) {
            oppcount ++;
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER){
                return 100;
            } else if (robot.type == RobotType.MUCKRAKER){
                if ((nearSelfSland && rc.getConviction()<=40) || rc.getRobotCount()>300){
                    return 100;
                } else if ((flag & Flags.NEARSELFCENTER)==Flags.NEARSELFCENTER && (rc.getConviction()<=40|| rc.getRobotCount()>300)){
                    score +=2;
                }
            } else if ((flag & Flags.NEARCENTER)!=Flags.NEARCENTER && robot.type == RobotType.POLITICIAN && robot.conviction <= 10 && rc.getConviction()>50){
                score +=2;
            }

        }

        if (rc.getConviction()<=10){
            return 100;
        }


        if (rc.getConviction()>100){
            if (center && oppcount+allycount>2 && rc.getRobotCount() < 200){
                return 0;
            }
            allycount++;
        }

        if (allycount>1){
            if (center) {
                obstructedCount = OBSTRUCTED_LENGTH;
            }
            if (rc.getRobotCount() < 300) {
                return 0;
            }
        }

        return score;
    }

    public void readParent() throws GameActionException{
        MapLocation tmp=null;
        int obtainedFlag, d;
        if (rc.canGetFlag(parentID)){
            obtainedFlag = rc.getFlag(parentID);
            if ((obtainedFlag & Flags.FOUND) == Flags.FOUND){
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
                    tmp = origin.translate(-d, 0);
                } else if ((obtainedFlag & Flags.UPLEFT) == Flags.UPLEFT){
                    tmp = origin.translate(-d, d);
                } else if ((obtainedFlag & Flags.UP) == Flags.UP){
                    tmp = origin.translate(0, d);
                } else if ((obtainedFlag & Flags.UPRIGHT) == Flags.UPRIGHT){
                    tmp = origin.translate(d, d);
                } else if ((obtainedFlag & Flags.RIGHT) == Flags.RIGHT){
                    tmp = origin.translate(d, 0);
                } else if ((obtainedFlag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT){
                    tmp = origin.translate(d, -d);
                } else if ((obtainedFlag & Flags.DOWN) == Flags.DOWN){
                    tmp = origin.translate(0, -d);
                } else if ((obtainedFlag & Flags.DOWNLEFT) == Flags.DOWNLEFT){
                    tmp = origin.translate(-d, -d);
                }
                double distance = rc.getLocation().distanceSquaredTo(tmp);
                if (Math.sqrt(distance) < 100) {
                    followingCount = FOLLOWING_LENGTH;
                    followTarget = tmp;
                    Direction scoutDir = rc.getLocation().directionTo(followTarget);
                    switch (scoutDir){
                        case EAST:
                            followingDirectionFlag = Flags.RIGHT;
                            break;
                        case WEST:
                            followingDirectionFlag = Flags.LEFT;
                            break;
                        case SOUTH:
                            followingDirectionFlag = Flags.DOWN;
                            break;
                        case NORTH:
                            followingDirectionFlag = Flags.UP;
                            break;
                        case NORTHEAST:
                            followingDirectionFlag = Flags.UPRIGHT;
                            break;
                        case SOUTHEAST:
                            followingDirectionFlag = Flags.DOWNRIGHT;
                            break;
                        case NORTHWEST:
                            followingDirectionFlag = Flags.UPLEFT;
                            break;
                        case SOUTHWEST:
                            followingDirectionFlag = Flags.DOWNLEFT;
                            break;
                        default:
                            break;
                    }
                }

            }
        }
    }

    public void run() throws GameActionException{
        found = false;
        speechConviction = (int) (rc.getConviction()*rc.getEmpowerFactor(rc.getTeam(), 0)) - 10;
        senseEnemies();
        senseAllies();
        if (Math.random() < LISTEN_EC_ORDER_PROB) {
            readParent();
        }

        if (found) {
            flag |= Flags.FOUND;
            double distance = Math.sqrt(rc.getLocation().distanceSquaredTo(origin));
            if (distance > 30) {
                flag |= Flags.FAR;
            } else if (distance > 20) {
                flag |= (Flags.FAR | Flags.MEDIUM);
            } else if (distance > 15) {
                flag |= Flags.MEDIUM;
            }
            Direction dir = rc.getLocation().directionTo(origin).opposite();
            switch (dir) {
                case EAST:
                    flag |= Flags.RIGHT;
                    break;
                case WEST:
                    flag |= Flags.LEFT;
                    break;
                case SOUTH:
                    flag |= Flags.DOWN;
                    break;
                case NORTH:
                    flag |= Flags.UP;
                    break;
                case NORTHEAST:
                    flag |= Flags.UPRIGHT;
                    break;
                case SOUTHEAST:
                    flag |= Flags.DOWNRIGHT;
                    break;
                case NORTHWEST:
                    flag |= Flags.UPLEFT;
                    break;
                case SOUTHWEST:
                    flag |= Flags.DOWNLEFT;
                    break;
                default:
                    break;
            }
        }

        if ( followingCount> 0 && !nearEdge){
            vector.update(followTarget, rc.getLocation(), 200);
            flag |= Flags.DANGER; // used to signal others that this units follows EC's instructed direction
            flag |= followingDirectionFlag;
            followingCount --;
        } else if (nearEdge) {
            followingCount = 0;
        }

        if (obstructedCount>0){
            obstructedCount--;
            flag |= Flags.OBSTRUCTED;;
        }

        if (!attack()){
            Direction dir = move();
            if (dir == null && rc.getConviction()>10) {
                if ( Math.random() > RANDOM_DIR_PROB){
                    dir = randomDirectionv2();
                } else {
                    dir = randomDirection();
                }
            }
            if (dir != null && rc.canMove(dir)){
                rc.move(dir);
            }
        }
    }
}
