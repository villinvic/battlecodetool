package  ChocoBananaV1;

import battlecode.common.*;

public class Muckracker extends UnitMethods {

    double RANDOM_DIR_PROB = 0.3;
    double RANDOM_DIR_CENTER_PROB = 0.8;

    public Muckracker(RobotController rc){
        this.rc = rc;
        this.rt = RobotType.MUCKRAKER;
        this.r = this.rt.sensorRadiusSquared;
    }

    public void senseEnemies(){
        MapLocation loc = rc.getLocation();
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam().opponent())) {
            flag |= Flags.NEAROPP;
            switch ( robot.type){
                case POLITICIAN:
                    flag |= Flags.NEARPOL;
                    int conviction = rc.getConviction();
                    if (conviction > 1 && (int)(robot.conviction*rc.getEmpowerFactor(rc.getTeam().opponent(),
                            0))>= 10+conviction){
                        vector.update(robot.location, loc, -20);
                    }
                    break;
                case SLANDERER:
                    flag |= Flags.NEARSLAND;
                    if (rt == RobotType.MUCKRAKER) {
                        vector.update(robot.location, loc, 40);
                    }
                    break;
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARCENTER;
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARMUCK;
                    break;
                default:
                    break;
            }

        }
    };

    public void senseAllies() throws GameActionException{
        int count = 0;
        MapLocation loc = rc.getLocation();
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam())) {
            count++;
            switch (robot.type){
                case ENLIGHTENMENT_CENTER:
                    vector.update(robot.location, loc, -8);
                    flag |= Flags.NEARSELFCENTER;
                    break;
                case POLITICIAN:
                    flag |= Flags.NEARSELFPOL;
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARSELFMUCK;
                    break;
                case SLANDERER:
                    flag |= Flags.NEARSELFSLAND;
                    break;
                default:
                    break;
            }

            // Dispersion strategy
            vector.update(robot.location, loc, -1);

            if (count< rt.bytecodeLimit/600) {
                if (rc.canGetFlag(robot.ID)) {
                    int allyFlag = rc.getFlag(robot.ID);
                    if ((allyFlag & Flags.NEARSLAND) == Flags.NEARSLAND) {
                        vector.update(robot.location, loc, 50);
                    }

                    boolean notnear = (flag & Flags.NEARCENTER)!=Flags.NEARCENTER;

                    if ((allyFlag & Flags.NEARCENTER) == Flags.NEARCENTER) {
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

    public void run() throws GameActionException{
        Direction dir;
        senseEnemies();
        senseAllies();
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
