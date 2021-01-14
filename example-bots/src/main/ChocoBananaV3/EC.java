package ChocoBananaV3;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EC extends UnitMethods {
    int convictionNear=11;
    int slandererSpawned = 0;
    int muckSpawned = 0;
    int allyNear = 0;
    int direction;
    int foundDirection=0;
    boolean dangerous = false;
    static final int MIN_REQUIRED_INFLUENCE = 10;
    static final int SLAND_SPAWN_TURNS = 10;
    static final int BID_START = 250;
    int lastbid=0, lastvote=1, dvote=0;
    static final double SAFE_SLANDERER_PROB = 0.9,
                        EXPLO_PROB = 0.4,
                        STRONG_MUCK_PROB = 0.1,
                        TRADEOFF = 0.2,
                        ROBOT_WEIGHT = 0.3, // TRADEOFF+ROBOT_WEIGHT < 1!!!!
                        ROBOT_MAX = 0.5;
    int foundMemoryCount = 0, FOUND_MEMORY_LENGTH=50;
    static final RobotType[] toolRobots = {
            RobotType.POLITICIAN,
            RobotType.MUCKRAKER,
    };

    int MAX_TRACK = 70;
    int[] tracked = new int[MAX_TRACK];
    int track_index = 0;



    public EC(RobotController rc){
        this.direction = (int) (Math.random()*8);
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
                    dangerous = true;
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
    }

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

        readTracked();
    }

    @Override
    public void buildRobot() throws  GameActionException {
        Direction dir;
        List<Direction> dirs = Arrays.asList(directions);
        Collections.shuffle(dirs);
        int currentInfluence = rc.getInfluence();
        for (int i = 0; i < 8; i++) {
            dir = nextDirection();
            if (chooseAndSpawn(currentInfluence, dir)){
                return;
            }
        }
    }

    public boolean chooseAndSpawn(int inf, Direction dir) throws GameActionException {
        RobotType robot = null;
        double robotWeight = Math.min(rc.getRobotCount() * ROBOT_WEIGHT, inf * ROBOT_MAX);
        int influence = (int) (TRADEOFF * inf + robotWeight);

        if (rc.getRoundNum()==1){
            robot = RobotType.SLANDERER;
            influence = inf;
        } else if ((!dangerous && (Math.random() < EXPLO_PROB || muckSpawned < 9)) || (dangerous && allyNear < 4)) {
            robot = RobotType.MUCKRAKER;
            influence = 1; // Explores
        } else if ( foundMemoryCount>0 || (allyNear > 1 && (flag & Flags.NEAROPP) == Flags.NEAROPP)) {
            robot = RobotType.POLITICIAN;
            influence = (int) (inf * 0.7);
            influence += 20;
            // If no muckraker near and early turns or ally politician near
        } else if ( (rc.getRoundNum() + 25 < 3000 && (((flag & Flags.NEARMUCK) != Flags.NEARMUCK) ||
                ((flag & Flags.NEARCENTER) == Flags.NEARCENTER))) && Math.random() < SAFE_SLANDERER_PROB) {
            robot = RobotType.SLANDERER;
            influence = (int) (inf * 0.5);
            influence += 20;
        } else if (Math.random() < STRONG_MUCK_PROB){
            System.out.println("okokok");
            robot = RobotType.MUCKRAKER;
            influence = (int) (inf * 0.7);
            influence += 20;
        }
        else {return false;}

        if (rc.canBuildRobot(robot, dir, influence)){
            rc.buildRobot(robot, dir, influence);
            MapLocation neighbour = rc.getLocation().add(dir);
            if (robot == RobotType.MUCKRAKER && influence==1){
                track( rc.senseRobotAtLocation(neighbour).ID);
            }
            if (robot==RobotType.SLANDERER){
                slandererSpawned ++;
            } else if (robot==RobotType.MUCKRAKER) {
                muckSpawned ++;
            }
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public int bid() {
        dvote = rc.getTeamVotes() - lastvote;
        lastvote = rc.getTeamVotes();
        if (rc.getRoundNum() < BID_START || rc.getRobotCount()*17 < rc.getRoundNum()){
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
        return MIN_REQUIRED_INFLUENCE + (int) (Math.sqrt(rc.getRoundNum()) * 0.2 + rc.getRobotCount() * ROBOT_WEIGHT);
    }

    public Direction nextDirection(){
        return directions[ (direction++) % 8];
    }

    public void track(int ID){
        tracked[track_index % MAX_TRACK] = ID;
        track_index++;
    }

    public void readTracked() throws GameActionException {
        int obtainedflag;
        for(int ID: tracked){
            if (rc.canGetFlag(ID)) {
                obtainedflag = rc.getFlag(ID);
                if ((obtainedflag & Flags.FOUND) == Flags.FOUND) {
                    if ((obtainedflag & Flags.NEARSELFCENTER) == Flags.NEARSELFCENTER){
                        // Far from EC and done mission, reset order
                        System.out.println("iduhcizeuh");
                        foundMemoryCount = 0;
                    } else {
                        if ((obtainedflag & Flags.LEFT) == Flags.LEFT) {
                            foundDirection = Flags.LEFT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                            break;
                        } else if ((obtainedflag & Flags.UPLEFT) == Flags.UPLEFT) {
                            foundDirection = Flags.UPLEFT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                            break;
                        } else if ((obtainedflag & Flags.UP) == Flags.UP) {
                            foundDirection = Flags.UP;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                            break;
                        } else if ((obtainedflag & Flags.UPRIGHT) == Flags.UPRIGHT) {
                            foundDirection = Flags.UPRIGHT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                            break;
                        } else if ((obtainedflag & Flags.RIGHT) == Flags.RIGHT) {
                            foundDirection = Flags.RIGHT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                            break;
                        } else if ((obtainedflag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT) {
                            foundDirection = Flags.DOWNRIGHT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                            break;
                        } else if ((obtainedflag & Flags.DOWN) == Flags.DOWN) {
                            foundDirection = Flags.DOWN;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                            break;
                        } else if ((obtainedflag & Flags.DOWNLEFT) == Flags.DOWNLEFT) {
                            foundDirection = Flags.DOWNLEFT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                            break;
                        }
                    }
                }
            }
        }

        System.out.println(foundDirection);
        flag |= foundDirection;
        if (foundMemoryCount >  0) {
            flag |= Flags.FOUND;
            foundMemoryCount--;
        }
    }

    public void run() throws GameActionException {
        dangerous = false;
        senseEnemies();
        senseAllies();
        updateObstruction();
        if (dangerous){
            flag |= Flags.DANGER;
        }

        if (rc.getInfluence() > infThreshold()) {
            buildRobot();
        }

        int b = bid();
        if (rc.canBid(b)) {
            rc.bid(bid());
        }
    }
}
