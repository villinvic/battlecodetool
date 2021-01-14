package  ChocoBananaV1;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EC extends UnitMethods {
    int convictionNear=11;
    static final int MIN_REQUIRED_INFLUANCE = 10;
    static final int SLAND_SPAWN_TURNS = 10;
    static final int BID_START = 250;
    int lastbid=0, lastvote=0, dvote=0;
    static final double RANDOM_PROB = 0.02, SLANDERER_PROB = 0.1, EXPLO_PROB = 0.05;

    static final RobotType[] spawnableRobot = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };

    public EC(RobotController rc){
        this.rc = rc;
        this.rt = RobotType.ENLIGHTENMENT_CENTER;
        this.r = this.rt.sensorRadiusSquared;
    }

    public void senseEnemies(){
        convictionNear = 0;
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam().opponent())) {
            flag |= Flags.NEAROPP;
            switch ( robot.type){
                case POLITICIAN:
                    flag |= Flags.NEARPOL;
                    if ((int)(robot.conviction*rc.getEmpowerFactor(rc.getTeam().opponent(), 0))>= 10+rc.getConviction()) {
                        convictionNear += robot.conviction;
                    }
                    break;
                case SLANDERER:
                    flag |= Flags.NEARSLAND;
                    break;
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARCENTER;
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARMUCK;
                    convictionNear += robot.conviction;
                    break;
                default:
                    break;
            }
        }
    };

    public void senseAllies() throws GameActionException {
        int count=0;
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
                    break;
                case SLANDERER:
                    flag |= Flags.NEARSELFSLAND;
                    break;
                default:
                    break;
            }

            if (count < rt.bytecodeLimit /600) {
                if (rc.canGetFlag(robot.ID)) {
                    int allyFlag = rc.getFlag(robot.ID);
                    if ((allyFlag & Flags.NEARCENTER) == Flags.NEARCENTER){
                        flag |= Flags.NEARCENTER_2;
                    } else if ((allyFlag & Flags.NEARCENTER_2) == Flags.NEARCENTER_2){
                        flag |= Flags.NEARCENTER_3;
                    }
                }
            }
        }
    };

    @Override
    public void buildRobot() throws  GameActionException {
        List<Direction> dirs = Arrays.asList(directions);
        Collections.shuffle(dirs);
        int currentInfluence = rc.getInfluence();
            for (Direction dir : dirs) {
                // spawn policy
                chooseAndSpawn( currentInfluence, dir);
            }
    }

    public void chooseAndSpawn(int inf, Direction dir) throws GameActionException {
        RobotType robot = null;
        int influence = (int) (0.15 * inf);

        if ((Math.random() < EXPLO_PROB)){
            robot = RobotType.MUCKRAKER;
            influence = 1;
        } else
        // If no muckraker close and early turns or ally politician near
        if ((flag & Flags.NEARMUCK) != Flags.NEARMUCK &&
                ((flag & Flags.NEARSELFPOL) == Flags.NEARSELFPOL)) {
            robot = RobotType.SLANDERER;
            influence += 20;
        } else if ((rc.getRoundNum() < SLAND_SPAWN_TURNS  ||  Math.random() < SLANDERER_PROB ) && (flag & Flags.NEARMUCK) != Flags.NEARMUCK) {
            robot = RobotType.SLANDERER;
            influence = (int) (0.7 * inf);
            influence += 20;
        } else if ((flag & Flags.NEAROPP) == Flags.NEAROPP &&
                (convictionNear <= (int) (inf * 0.8 * rc.getEmpowerFactor(rc.getTeam(), 4)) - 10)) {
            robot = RobotType.POLITICIAN;
            influence = (int) (inf * 0.8);
        } else if ( Math.random() < RANDOM_PROB){
            robot = spawnableRobot[(int) (Math.random() * 3)];
            influence += 20;
        }
        else {return;}

        if (rc.canBuildRobot(robot, dir, influence)){
            rc.buildRobot(robot, dir, influence);
        }
    }

    @Override
    public int bid() {
        dvote = rc.getTeamVotes() - lastvote;
        lastvote = rc.getTeamVotes();
        if (dvote<0 || rc.getRoundNum() < BID_START){
            return 1;
        }

        return (int) (0.1 * rc.getInfluence());
    }

    public double infThreshold(){
        return MIN_REQUIRED_INFLUANCE + (int) (Math.sqrt(rc.getRoundNum()) * 0.2 + rc.getRobotCount() * 0.1);
    }
    public void run() throws GameActionException {
        senseEnemies();
        senseAllies();

        if (rc.getInfluence() > infThreshold()) {
            buildRobot();
        }

        int b = bid();
        if (rc.canBid(b)) {
            rc.bid(bid());
        }
    }
}
