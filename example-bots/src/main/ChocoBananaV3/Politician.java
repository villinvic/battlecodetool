package ChocoBananaV3;

import battlecode.common.*;

public class Politician extends UnitMethods {
    static int speechConviction;
    int parentID;
    Direction followingDirection;
    int followingDirectionFlag;
    boolean nearSelfSland = false;
    int FOLLOWING_LENGTH = 35, followingCount = 0;
    double  RANDOM_DIR_PROB = 0.4,
            PROPAGATE_FOUND_SIGNAL_PROB = 0.7;

    public Politician(){
    }

    public Politician(RobotController rc) throws GameActionException {
        this.rc = rc;
        this.rt = RobotType.POLITICIAN;
        this.r = this.rt.sensorRadiusSquared;
        MapLocation neighbour;
        RobotInfo robot;

        for (Direction dir: directions) {
            neighbour = rc.getLocation().add(dir);
            if (rc.canSenseLocation(neighbour)) {
                robot = rc.senseRobotAtLocation(neighbour);
                if (robot != null && robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    parentID = robot.ID;
                    int parentFlag = rc.getFlag(parentID);
                    if ((parentFlag & Flags.FOUND) == Flags.FOUND) {

                        if ((parentFlag & Flags.LEFT) == Flags.LEFT) {
                            followingDirection = Direction.WEST;
                            followingDirectionFlag = Flags.LEFT;
                        } else if ((parentFlag & Flags.UPLEFT) == Flags.UPLEFT) {
                            followingDirection = Direction.NORTHWEST;
                            followingDirectionFlag = Flags.UPLEFT;
                        } else if ((parentFlag & Flags.UP) == Flags.UP) {
                            followingDirection = Direction.NORTH;
                            followingDirectionFlag = Flags.UP;
                        } else if ((parentFlag & Flags.UPRIGHT) == Flags.UPRIGHT) {
                            followingDirection = Direction.NORTHEAST;
                            followingDirectionFlag = Flags.UPRIGHT;
                        } else if ((parentFlag & Flags.RIGHT) == Flags.RIGHT) {
                            followingDirection = Direction.EAST;
                            followingDirectionFlag = Flags.RIGHT;
                        } else if ((parentFlag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT) {
                            followingDirection = Direction.SOUTHEAST;
                            followingDirectionFlag = Flags.DOWNRIGHT;
                        } else if ((parentFlag & Flags.DOWN) == Flags.DOWN) {
                            followingDirection = Direction.SOUTH;
                            followingDirectionFlag = Flags.DOWN;
                        } else if ((parentFlag & Flags.DOWNLEFT) == Flags.DOWNLEFT) {
                            followingDirection = Direction.SOUTHWEST;
                            followingDirectionFlag = Flags.DOWNLEFT;
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
            flag |= Flags.NEAROPP;

            switch ( robot.type){
                case POLITICIAN:
                    flag |= Flags.NEARPOL;
                    break;
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARCENTER;
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
                vector.update(robot2.location, loc, 1000);
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
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARSELFMUCK;
                    vector.update(robot.location, loc, 3);
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
                        vector.update(robot.location, loc, -2000);
                    }

                    if ( followingCount==0 && (allyFlag & Flags.DANGER) == Flags.DANGER) {
                        vector.update(robot.location, loc, 25);
                        if (Math.random() < PROPAGATE_FOUND_SIGNAL_PROB && robot.type != RobotType.ENLIGHTENMENT_CENTER){
                            if ((allyFlag & Flags.LEFT) == Flags.LEFT) {
                                followingDirection = Direction.WEST;
                                followingDirectionFlag = Flags.LEFT;
                            } else if ((allyFlag & Flags.UPLEFT) == Flags.UPLEFT) {
                                followingDirection = Direction.NORTHWEST;
                                followingDirectionFlag = Flags.UPLEFT;
                            } else if ((allyFlag & Flags.UP) == Flags.UP) {
                                followingDirection = Direction.NORTH;
                                followingDirectionFlag = Flags.UP;
                            } else if ((allyFlag & Flags.UPRIGHT) == Flags.UPRIGHT) {
                                followingDirection = Direction.NORTHEAST;
                                followingDirectionFlag = Flags.UPRIGHT;
                            } else if ((allyFlag & Flags.RIGHT) == Flags.RIGHT) {
                                followingDirection = Direction.EAST;
                                followingDirectionFlag = Flags.RIGHT;
                            } else if ((allyFlag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT) {
                                followingDirection = Direction.SOUTHEAST;
                                followingDirectionFlag = Flags.DOWNRIGHT;
                            } else if ((allyFlag & Flags.DOWN) == Flags.DOWN) {
                                followingDirection = Direction.SOUTH;
                                followingDirectionFlag = Flags.DOWN;
                            } else if ((allyFlag & Flags.DOWNLEFT) == Flags.DOWNLEFT) {
                                followingDirection = Direction.SOUTHWEST;
                                followingDirectionFlag = Flags.DOWNLEFT;
                            }
                            followingCount = FOLLOWING_LENGTH;

                        }
                    }

                    if ((allyFlag & Flags.NEARSELFSLAND) == Flags.NEARSELFSLAND) {
                        nearSelfSland = true;
                    }

                    if ((allyFlag & Flags.NEARCENTER) == Flags.NEARCENTER) {
                        flag |= Flags.NEARCENTER_2;
                        if (rc.getConviction() > 10) {
                            vector.update(robot.location, loc, 20);
                        }
                    } else if ((allyFlag & Flags.NEARCENTER_2) == Flags.NEARCENTER_2) {
                        flag |= Flags.NEARCENTER_3;
                        if (rc.getConviction() > 10) {
                            vector.update(robot.location, loc, 10);
                        }
                    } else if ((allyFlag & Flags.NEARCENTER_3) == Flags.NEARCENTER_3) {
                        if (rc.getConviction() > 10) {
                            vector.update(robot.location, loc, 2);
                        }
                    }
                }
            }
        }
    };

    public boolean attack() throws GameActionException {
        int actionRadius = rt.actionRadiusSquared;
        int score = computeAttackScore(actionRadius);
        if (rc.getConviction()>10 && score>=5 || score >= 100) {
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
        int oppcount=0, allycount=0;

        allycount+= rc.senseNearbyRobots(actionRadius, rc.getTeam()).length;
        oppcount+= 5*rc.senseNearbyRobots(actionRadius, Team.NEUTRAL).length;
        for (RobotInfo robot: rc.senseNearbyRobots(actionRadius, rc.getTeam().opponent()) ) {
            oppcount ++;
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER){
                oppcount += 100;
            } else if (robot.type == RobotType.MUCKRAKER){
                if (nearSelfSland || rc.getRobotCount()>300){
                    return 100;
                }
            }

        }

        if (rc.getConviction()<=10){
            return 100;
        }

        if (allycount>2){
            if (oppcount>1) {
                flag |= Flags.OBSTRUCTED;
            }
            if (rc.getRobotCount() < 200) {
                return 0;
            }
        }
        return oppcount;
    }

    public void run() throws GameActionException{
        speechConviction = (int) (rc.getConviction()*rc.getEmpowerFactor(rc.getTeam(), 0)) - 10;
        senseEnemies();
        senseAllies();

        if ( followingCount> 0 && !nearEdge){
            vector.update(followingDirection, 250);
            flag |= Flags.DANGER; // used to signal others that this units follows EC's instructed direction
            flag |= followingDirectionFlag;
            followingCount --;
        } else if (nearEdge) {
            followingCount = 0;
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
