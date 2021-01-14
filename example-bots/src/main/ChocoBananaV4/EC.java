package ChocoBananaV4;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

public class EC extends UnitMethods {
    int convictionNear=11;
    int slandererSpawned = 0;
    int muckSpawned = 0;
    int allyNear = 0;
    int direction, scoutableDirection;
    int foundDirection=0;
    int DISTANCE_LEVEL = 0;
    boolean dangerous = false;
    static final int MIN_REQUIRED_INFLUENCE = 10;
    static final int SLAND_SPAWN_TURNS = 10;
    static final int BID_START = 250;
    int lastbid=0, lastvote=1, dvote=0;
    static final double SAFE_SLANDERER_PROB = 1.0,
                        EXPLO_PROB = 0.67,
                        TRADEOFF = 0.2,
                        ROBOT_WEIGHT = 0.3, // TRADEOFF+ROBOT_WEIGHT < 1!!!!
                        ROBOT_MAX = 0.5,
                        POLITICIAN_RUSH_PROB = 0.7;
    int foundMemoryCount = 0, FOUND_MEMORY_LENGTH=60;
    boolean foundNeutral = true;

    int MAX_TRACK = 350;
    int[] tracked = new int[MAX_TRACK];
    List<Direction> scoutable;
    List<Direction> scoutableInit = new ArrayList<>();
    int track_index = 0;

    int spawnInfluence = 0;



    public EC(RobotController rc) throws GameActionException {
        this.direction = (int) (Math.random()*8);
        this.rc = rc;
        this.rt = RobotType.ENLIGHTENMENT_CENTER;
        this.r = this.rt.sensorRadiusSquared;

        assessMapEdges();
        scoutableDirection = (int)(Math.random()*scoutable.size());
    }

    public void assessMapEdges() throws  GameActionException {
        MapLocation next, previous;
        int c=0;

        for (Direction dir: Direction.allDirections()) {
            if (dir != Direction.CENTER) {

                next = rc.getLocation();
                try {
                    do {
                        previous = next;
                        next = next.add(dir);
                    } while (rc.onTheMap(previous));

                } catch (GameActionException e) {
                    scoutableInit.add(dir);
                }
            }
        }
        scoutable = new ArrayList<>(scoutableInit);
    }

    public void senseEnemies(){
        convictionNear = 0;
        for (RobotInfo robot : rc.senseNearbyRobots(r, rc.getTeam().opponent())) {
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
                }
            }
        }

        allyNear = count;

        readTracked();
    }

    @Override
    public void buildRobot() throws  GameActionException {
        RobotType robot = chooseRobot();
        Direction dir;
        if (robot != null) {
            for (int i = 0; i < 8; i++) {
                if (robot == RobotType.MUCKRAKER) {
                    dir = nextScoutableDirection();
                } else {
                    dir = nextDirection();
                }
                if (rc.canBuildRobot(robot, dir, spawnInfluence)) {
                    rc.buildRobot(robot, dir, spawnInfluence);

                    if (robot == RobotType.SLANDERER) {
                        slandererSpawned++;
                    } else if (robot == RobotType.MUCKRAKER) {
                        MapLocation neighbour = rc.getLocation().add(dir);
                        track( rc.senseRobotAtLocation(neighbour).ID);
                        muckSpawned++;
                    } else {
                        MapLocation neighbour = rc.getLocation().add(dir);
                        track( rc.senseRobotAtLocation(neighbour).ID);
                    }

                    break;
                }
            }
        }
    }

    public RobotType chooseRobot() {
        RobotType robot = null;
        int inf = rc.getInfluence();
        double robotWeight = Math.min(rc.getRobotCount() * ROBOT_WEIGHT, inf * ROBOT_MAX);
        spawnInfluence = 0;

        boolean NEAROPP = (flag & (Flags.NEARCENTER | Flags.NEARPOL |Flags.NEARMUCK | Flags.NEARSLAND)) > 0;

        if (rc.getRoundNum()==1 ) {
            robot = RobotType.SLANDERER;
            spawnInfluence = inf;
        } else if (Math.random() < EXPLO_PROB){
            robot = RobotType.MUCKRAKER;
            spawnInfluence = 1; // Explores
        } else if ( (foundMemoryCount>0 && Math.random() < POLITICIAN_RUSH_PROB) || (allyNear > 1 && NEAROPP) ||
                muckSpawned % scoutable.size() == 0) {
            robot = RobotType.POLITICIAN;
            spawnInfluence = (int) (inf * 0.3);
            spawnInfluence += 10;
            if (Math.random()<0.3) {
                spawnInfluence += 40;
            }
        } else if ((!dangerous && (muckSpawned <= scoutable.size())) || (dangerous && allyNear < 4)) {
            robot = RobotType.MUCKRAKER;
            spawnInfluence = 1; // Explores
            // If no muckraker near and early turns or ally politician near
        } else if ( (rc.getRoundNum() + 25 < 3000 && (((flag & Flags.NEARMUCK) != Flags.NEARMUCK) ||
                ((flag & Flags.NEARCENTER) == Flags.NEARCENTER))) && Math.random() < SAFE_SLANDERER_PROB) {
            robot = RobotType.SLANDERER;
            spawnInfluence = (int) (inf * 0.9);
        }
        
        return  robot;

        

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
    
    public Direction nextScoutableDirection(){
        return scoutable.get((scoutableDirection++) % scoutable.size());
    }

    public void track(int ID){
        tracked[track_index % MAX_TRACK] = ID;
        track_index++;
    }

    public void readTracked() throws GameActionException {
        int obtainedflag, d;
        for(int ID: tracked){
            if (rc.canGetFlag(ID)) {
                obtainedflag = rc.getFlag(ID);
                if ((obtainedflag & Flags.FOUND) == Flags.FOUND) {
                    foundNeutral = (obtainedflag & Flags.FOUND_NEUTRAL_EC) == Flags.FOUND_NEUTRAL_EC;
                    if ((obtainedflag & Flags.FAR) == Flags.FAR){
                        if((obtainedflag & Flags.MEDIUM) == Flags.MEDIUM) {
                            d = 3;
                        } else {
                            d = 4;
                        }

                    } else if((obtainedflag & Flags.MEDIUM) == Flags.MEDIUM){
                        d = 2;
                    } else {
                        d = 1;
                    }
                    if (foundMemoryCount== 0 || d < DISTANCE_LEVEL || foundNeutral) {
                        foundNeutral = (obtainedflag & Flags.FOUND_NEUTRAL_EC) == Flags.FOUND_NEUTRAL_EC;

                        DISTANCE_LEVEL = d;

                        if ((obtainedflag & Flags.LEFT) == Flags.LEFT) {
                            foundDirection = Flags.LEFT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                        } else if ((obtainedflag & Flags.UPLEFT) == Flags.UPLEFT) {
                            foundDirection = Flags.UPLEFT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                        } else if ((obtainedflag & Flags.UP) == Flags.UP) {
                            foundDirection = Flags.UP;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                        } else if ((obtainedflag & Flags.UPRIGHT) == Flags.UPRIGHT) {
                            foundDirection = Flags.UPRIGHT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                        } else if ((obtainedflag & Flags.RIGHT) == Flags.RIGHT) {
                            foundDirection = Flags.RIGHT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                        } else if ((obtainedflag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT) {
                            foundDirection = Flags.DOWNRIGHT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                        } else if ((obtainedflag & Flags.DOWN) == Flags.DOWN) {
                            foundDirection = Flags.DOWN;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                        } else if ((obtainedflag & Flags.DOWNLEFT) == Flags.DOWNLEFT) {
                            foundDirection = Flags.DOWNLEFT;
                            foundMemoryCount = FOUND_MEMORY_LENGTH;
                        }
                    }
                } else if ((obtainedflag & Flags.NOT_SCOUTABLE) == Flags.NOT_SCOUTABLE){
                    if ((obtainedflag & Flags.LEFT) == Flags.LEFT) {
                        scoutable.remove(Direction.WEST);
                    } else if ((obtainedflag & Flags.UPLEFT) == Flags.UPLEFT) {
                        scoutable.remove(Direction.NORTHWEST);
                    } else if ((obtainedflag & Flags.UP) == Flags.UP) {
                        scoutable.remove(Direction.NORTH);
                    } else if ((obtainedflag & Flags.UPRIGHT) == Flags.UPRIGHT) {
                        scoutable.remove(Direction.NORTHEAST);
                    } else if ((obtainedflag & Flags.RIGHT) == Flags.RIGHT) {
                        scoutable.remove(Direction.EAST);
                    } else if ((obtainedflag & Flags.DOWNRIGHT) == Flags.DOWNRIGHT) {
                        scoutable.remove(Direction.SOUTHEAST);
                    } else if ((obtainedflag & Flags.DOWN) == Flags.DOWN) {
                        scoutable.remove(Direction.SOUTH);
                    } else if ((obtainedflag & Flags.DOWNLEFT) == Flags.DOWNLEFT) {
                        scoutable.remove(Direction.SOUTHWEST);
                    }

                    if (scoutable.size() == 0){
                        scoutable = new ArrayList<>(scoutableInit);
                    }
                }
            }
        }

        flag |= foundDirection;
        if (foundMemoryCount >  0) {
            switch (DISTANCE_LEVEL){
                case 1:
                    //
                    break;
                case 2:
                    flag |= Flags.MEDIUM;
                    break;
                case 3:
                    flag |= (Flags.FAR|Flags.MEDIUM);
                    break;
                case 4:
                    flag |= Flags.FAR;
                    break;
                default:
                    break;

            }
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
            if (rc.getCooldownTurns()<1) {
                buildRobot();
            }
        }

        int b = bid();
        if (rc.canBid(b)) {
            rc.bid(bid());
        }
    }
}
