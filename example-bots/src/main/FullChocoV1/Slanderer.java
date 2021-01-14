package FullChocoV1;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Slanderer extends UnitMethods{

    public Slanderer(RobotController rc){
        this.rc = rc;
        this.rt = RobotType.SLANDERER;
        this.r = this.rt.sensorRadiusSquared;
    }


    @Override
    public void senseEnemies() throws GameActionException {

    }

    @Override
    public void senseAllies() throws GameActionException{

    }
    @Override
    public void playTurn() throws GameActionException {

    }
}
