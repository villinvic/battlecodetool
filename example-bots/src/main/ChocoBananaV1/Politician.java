package  ChocoBananaV1;

import battlecode.common.*;

public class Politician extends UnitMethods {
    int speachConviction;

    RobotType followPolicy = RobotType.MUCKRAKER;
    double FOLLOW_SLAND_PROB = 0.1;
    double RANDOM_DIR_PROB = 0.4;

    public Politician(){
    }

    public Politician(RobotController rc){
        this.rc = rc;
        this.rt = RobotType.POLITICIAN;
        this.r = this.rt.sensorRadiusSquared;
        if (Math.random() < FOLLOW_SLAND_PROB){
            followPolicy = RobotType.SLANDERER;
        }
    }


    public void senseEnemies(){
        MapLocation loc = rc.getLocation();
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam().opponent())) {
            flag |= Flags.NEAROPP;
            switch ( robot.type){
                case POLITICIAN:
                    flag |= Flags.NEARPOL;
                    if ((int)(robot.conviction*rc.getEmpowerFactor(rc.getTeam().opponent(), 0)) >=
                            10+rc.getConviction()){
                        vector.update(robot.location, loc, -10);
                    } else {
                        vector.update(robot.location, loc, 10);
                    }
                    break;
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARCENTER;
                    if (rc.getConviction() > 10) {
                        vector.update(robot.location, loc, 30);
                    }
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARMUCK;
                    if (speachConviction >= robot.conviction) {
                        int weight = (int) ((rc.getConviction() / (robot.conviction+1)) * 1.5);
                        vector.update(robot.location, loc, weight);
                    }
                    break;
                default:
                    break;
            }
        }

        for (RobotInfo robot2 : rc.senseNearbyRobots(r, Team.NEUTRAL)){
            flag |= Flags.NEARCENTER;
            if (rc.getConviction() > 10) {
                vector.update(robot2.location, loc, 100);
            }
        }
    };

    public void senseAllies() throws GameActionException{
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
                    if (rc.getConviction() <= 10 && robot.conviction>10){
                        vector.update(robot.location, loc, 100);
                    }
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARSELFMUCK;
                    break;
                default:
                    break;
            }

            if (robot.type == followPolicy) {
                vector.update(robot.location, loc, 1);
            }

            if (count< rt.bytecodeLimit/600) {
                if (rc.canGetFlag(robot.ID)) {
                    int allyFlag = rc.getFlag(robot.ID);
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
                            vector.update(robot.location, loc, 5);
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
        int score = 0;
        int maxheal = 0;
        int dmg;
        double allycount=0, oppcount=0;

        for (RobotInfo robot: rc.senseNearbyRobots(actionRadius, rc.getTeam())) {
            dmg =robot.influence - robot.conviction;
            if (dmg > 0 && robot.conviction> 10 && robot.type == RobotType.POLITICIAN) {
                score += 1;
            }

            if (robot.type !=RobotType.ENLIGHTENMENT_CENTER) {
                allycount++;
                maxheal += dmg;
            }

        }

        RobotInfo [] toCapture = rc.senseNearbyRobots(actionRadius, Team.NEUTRAL);
        if (toCapture.length > 0){
            score += 5;
        }

        for (RobotInfo robot2: rc.senseNearbyRobots(actionRadius, rc.getTeam().opponent())) {
            oppcount++;

            int val = 1;
            if (robot2.conviction+10 < speachConviction) {
                val = 2;
            }

            if (robot2.type == RobotType.POLITICIAN) {
                score += 100;
            } else if (robot2.type == RobotType.ENLIGHTENMENT_CENTER) {
                score += val*5;
            } else if (robot2.type == RobotType.MUCKRAKER && (((flag & Flags.NEARSELFPOL) == Flags.NEARSELFPOL) ||
                    ((flag & Flags.NEARSELFCENTER) == Flags.NEARSELFCENTER))) {
                score += val*5;
            } else {
                score += val;
            }
        }

        if ( (allycount+oppcount) > 0 && maxheal * (allycount/(allycount+oppcount)) >= speachConviction){
            score += 2;
        }
        return score;
    }

    public void run() throws GameActionException{
        speachConviction = (int) (rc.getConviction()*rc.getEmpowerFactor(rc.getTeam(), 0)) - 10;
        senseEnemies();
        senseAllies();
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
