package ChocoBananaV2;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EC extends UnitMethods {
    int convictionNear=11;
    int slandererSpawned = 0;
    int allyNear = 0;
    boolean dangerous = false;
    static final int MIN_REQUIRED_INFLUANCE = 10;
    static final int SLAND_SPAWN_TURNS = 10;
    static final int BID_START = 250;
    int lastbid=0, lastvote=1, dvote=0;
    static final double RANDOM_PROB = 0.2,
                        SAFE_SLANDERER_PROB = 0.4,
                        SLANDERER_PROB = 0.1,
                        EXPLO_PROB = 0.1,

                        TRADEOFF = 0.2,
                        ROBOT_WEIGHT = 0.3, // TRADEOFF+ROBOT_WEIGHT < 1!!!!
                        ROBOT_MAX = 0.5;

    static final RobotType[] toolRobots = {
            RobotType.POLITICIAN,
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
                    if (!((int)(convictionNear+robot.conviction*rc.getEmpowerFactor(rc.getTeam().opponent(),
                            0))>= 10+rc.getConviction())) {
                        dangerous = true;
                    }
                    convictionNear += robot.conviction;
                    break;
                case SLANDERER:
                    flag |= Flags.NEARSLAND;
                    break;
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARCENTER;
                    dangerous = true;
                    //convictionNear += robot.conviction;
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

            if (count < 30) {
                if (rc.canGetFlag(robot.ID)) {
                    int allyFlag = rc.getFlag(robot.ID);
                    if ((allyFlag & Flags.NEARCENTER) == Flags.NEARCENTER){
                        flag |= Flags.NEARCENTER_2;
                        dangerous = true;
                    } else if ((allyFlag & Flags.NEARCENTER_2) == Flags.NEARCENTER_2){
                        flag |= Flags.NEARCENTER_3;
                    }
                }
            }
        }

        allyNear = count;
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
        double robotWeight = Math.min(rc.getRobotCount()*ROBOT_WEIGHT, inf*ROBOT_MAX);
        int influence = (int) (TRADEOFF * inf + robotWeight);

        if (!dangerous && Math.random() < EXPLO_PROB) {
            robot = RobotType.MUCKRAKER;
            influence = 1; // Explores
        } else if (slandererSpawned>0 && allyNear>1 && (flag & Flags.NEAROPP) == Flags.NEAROPP &&
                (convictionNear <= (int) (inf * 0.5 * rc.getEmpowerFactor(rc.getTeam(), 4)) - 10 &&
                (int) (inf * 0.5 * rc.getEmpowerFactor(rc.getTeam(), 4)) - 10 > 10)) {
            robot = RobotType.POLITICIAN;
            influence = (int) (inf * 0.5);
        } else if (dangerous && allyNear<4) {
            robot = RobotType.MUCKRAKER;
            influence = 1; // protects building if near enemy center
            // If no muckraker near and early turns or ally politician near
        } else if ( slandererSpawned==0 || (rc.getRoundNum() + 25 < 3000 && ((((flag & Flags.NEARMUCK) != Flags.NEARMUCK) ||
                ((flag & Flags.NEARCENTER) == Flags.NEARCENTER)) &&
                ((flag & Flags.NEARSELFPOL) == Flags.NEARSELFPOL) && Math.random() < SAFE_SLANDERER_PROB))) {
            robot = RobotType.SLANDERER;
            influence += 20;
        } else if ( rc.getRoundNum() + 25 < 3000 && ((rc.getRoundNum() < SLAND_SPAWN_TURNS  ||  Math.random() < SLANDERER_PROB)) &&
                (flag & Flags.NEARMUCK) != Flags.NEARMUCK) {
            robot = RobotType.SLANDERER;
            influence = (int) (0.7 * inf);
            influence += 20;
        }  else if ( Math.random() < RANDOM_PROB){
            robot = toolRobots[(int) (Math.random() * 2)];
            influence += 20;
        }
        else {return;}

        if (rc.canBuildRobot(robot, dir, influence)){
            rc.buildRobot(robot, dir, influence);
            if (robot==RobotType.SLANDERER){
                slandererSpawned ++;
            }
        }
    }

    @Override
    public int bid() {
        dvote = rc.getTeamVotes() - lastvote;
        lastvote = rc.getTeamVotes();
        if (rc.getRoundNum() < BID_START){
            if (dvote < 0){
                return 1;
            }
            else {
                return 2;
            }

        }

        return (int) (TRADEOFF*0.5 * rc.getInfluence());
    }

    public double infThreshold(){
        return MIN_REQUIRED_INFLUANCE + (int) (Math.sqrt(rc.getRoundNum()) * 0.2 + rc.getRobotCount() * ROBOT_WEIGHT);
    }
    public void run() throws GameActionException {
        dangerous = false;
        senseEnemies();
        senseAllies();
        updateObstruction();

        if (rc.getInfluence() > infThreshold()) {
            buildRobot();
        }

        int b = bid();
        if (rc.canBid(b)) {
            rc.bid(bid());
        }
    }
}
