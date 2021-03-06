package  ChocoBananaV1;

import battlecode.common.*;

public class Slanderer extends UnitMethods {

    public Slanderer(RobotController rc){

        this.rc = rc;
        this.rt = RobotType.SLANDERER;
        this.r = this.rt.sensorRadiusSquared;
    }

    public void senseEnemies(){
        RobotType rt = rc.getType();
        MapLocation loc = rc.getLocation();
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam().opponent())) {
            flag |= Flags.NEAROPP;
            switch ( robot.type){
                case POLITICIAN:
                    flag |= Flags.NEARPOL;
                    if ((int)(robot.conviction*rc.getEmpowerFactor(rc.getTeam().opponent(), 0))>= 10+rc.getConviction()) {
                        vector.update(robot.location, loc, -20);
                    }
                    break;
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARCENTER;
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARMUCK;
                    vector.update(robot.location, loc, -40);
                    break;
                default:
                    break;
            }
        }
    };

    public void senseAllies() throws GameActionException {
        MapLocation loc = rc.getLocation();
        int count = 0;
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam())) {
            count ++;
            switch (robot.type){
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARSELFCENTER;
                    if (robot.location.isAdjacentTo(loc)){
                        vector.update(robot.location, loc, -1);
                    }

                    break;
                case POLITICIAN:
                    flag |= Flags.NEARSELFPOL;
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARSELFMUCK;
                    break;
                default:
                    break;
            }
            if (count< rt.bytecodeLimit/600) {
                if (rc.canGetFlag(robot.ID)) {
                    int allyFlag = rc.getFlag(robot.ID);
                    if ((allyFlag & Flags.NEAROPP) == Flags.NEAROPP) {
                        if ((allyFlag & Flags.NEARMUCK) == Flags.NEARMUCK){
                            vector.update(robot.location, loc, -10);
                        } else {
                            vector.update(robot.location, loc, -5);
                        }

                    } else if ( robot.type == RobotType.MUCKRAKER ) {
                        vector.update(robot.location, loc, 1);
                    }
                }
            }
        }
    };

    public void run() throws GameActionException {
        senseEnemies();
        senseAllies();
        Direction dir = move();
        if (dir != null && rc.canMove(dir)){
            rc.move(dir);
        }
    }


}
