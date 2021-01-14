package FullChocoV1;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Muckracker extends UnitMethods{

    public Muckracker(RobotController rc){
        this.rc = rc;
        this.rt = RobotType.MUCKRAKER;
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
