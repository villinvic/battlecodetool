package ChocoBananaV2;

import battlecode.common.*;

import javax.xml.stream.FactoryConfigurationError;

public class Muckracker extends UnitMethods {

    double RANDOM_DIR_PROB = 0.3;
    MapLocation foundCenter, origin, current;
    boolean found= false, bring=false, protect = false;

    public Muckracker(RobotController rc){
        this.rc = rc;
        this.rt = RobotType.MUCKRAKER;
        this.r = this.rt.sensorRadiusSquared;
        this.origin = rc.getLocation();
    }

    public void senseEnemies(){
        MapLocation loc = current;
        int conviction = rc.getConviction();
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam().opponent())) {
            flag |= Flags.NEAROPP;
            switch ( robot.type){
                case POLITICIAN:
                    flag |= Flags.NEARPOL;
                    if (conviction > 1 && (int)(robot.conviction*rc.getEmpowerFactor(rc.getTeam().opponent(),
                            0))>= 10+conviction){
                        vector.update(robot.location, loc, -20);
                    }
                    break;
                case SLANDERER:
                    flag |= Flags.NEARSLAND;
                    if (rt == RobotType.MUCKRAKER) {
                        vector.update(robot.location, loc, 100);
                    }
                    break;
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARCENTER;
                    vector.update(robot.location, loc, -50);
                    if (conviction==1){
                        if (!protect && (turnCount > 7) ) {
                            this.found = true;
                            this.foundCenter = robot.location;
                        } else {
                            protect = true;
                        }
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
                flag |= Flags.NEARCENTER;
                this.found = true;
                this.foundCenter = robot.location;
                break;
            }
        }

    };

    public void senseAllies() throws GameActionException{
        int count = 0;
        MapLocation loc = current;
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam())) {
            count++;
            switch (robot.type){
                case ENLIGHTENMENT_CENTER:
                    if (!protect) { ;
                        vector.update(robot.location, loc, -8);
                    }
                    flag |= Flags.NEARSELFCENTER;
                    break;
                case POLITICIAN:
                    flag |= Flags.NEARSELFPOL;
                    if (rc.getConviction() == 1 ){
                        vector.update(robot.location, loc, -2);
                    }
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

            if (count< 14) {
                if (rc.canGetFlag(robot.ID)) {
                    int allyFlag = rc.getFlag(robot.ID);

                    if ( !bring && !found && (allyFlag & Flags.BRING) == Flags.BRING) {
                        vector.update(robot.location, loc, 50);
                    }

                    if ((allyFlag & Flags.NEARSLAND) == Flags.NEARSLAND) {
                        vector.update(robot.location, loc, 25);
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
        current = rc.getLocation();
        Direction dir;
        senseEnemies();
        senseAllies();
        updateObstruction();

        if (found){
            // Come back
            if (!bring) {
                if ((flag & Flags.NEARSELFCENTER)==Flags.NEARSELFCENTER){
                    bring = true;
                } else {
                    vector.update(origin, current, 50);
                }
            }
            if (bring){
                flag |= Flags.BRING;
                vector.update(foundCenter, current, 50);
                if ((flag & Flags.NEARCENTER)==Flags.NEARCENTER || foundCenter.isAdjacentTo(current)){
                    bring = false;
                    found = false;
                    origin = current;
                    //do it again !
                }
            }
        }

        if (protect){
            vector.update(origin, current, 100);
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
