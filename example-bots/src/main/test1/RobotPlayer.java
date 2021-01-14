package test1;
import battlecode.common.*;

import java.lang.annotation.Target;
import java.util.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static final List<Direction> diags = Arrays.asList(
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST
            );

    static final Direction[] cardinal = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
    };

    static int turnCount;

    // Memory //
    static int allycount = 0;
    static int enemycount = 0;
    static int flag;
    static Vect vector = new Vect();
    static RobotType followPolicy = RobotType.MUCKRAKER;
    static int convictionNear = 10;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        rc.setFlag(0);
        turnCount = 0;
        if (rc.getType()==RobotType.POLITICIAN){
            if ((rc.getRobotCount() % 4)==0){
            followPolicy = RobotType.SLANDERER;
            }
        }

        System.out.println("I'm a " + rc.getType() + " and I just got created! I have influence " + rc.getInfluence());
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                flag = 0;
                vector.reset();

                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER:
                        runEnlightenmentCenter();
                        break;
                    case POLITICIAN:
                        runPolitician();
                        break;
                    case SLANDERER:
                        runSlanderer();
                        break;
                    case MUCKRAKER:
                        runMuckraker();
                        break;
                }

                rc.setFlag( flag);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
        int bid, currentInfluence;
        int sensorRadius = rc.getType().sensorRadiusSquared;
        senseSurroundingEnemies(sensorRadius);
        senseSurroundingAllies(sensorRadius);
        List<Direction> dirs = Arrays.asList(directions);
        Collections.shuffle(dirs);
        currentInfluence = rc.getInfluence();
        if ( currentInfluence > turnCount/40 + 10) {
            for (Direction dir : dirs) {
                // spawn policy
                buildRobot( currentInfluence, dir);
            }
        }

        currentInfluence = rc.getInfluence();
        bid = computeBid(currentInfluence);
        if (bid >0) {
            rc.bid(bid); // submit a bid for a vote
        }




    }

    static void runPolitician() throws GameActionException {
        RobotType rt = rc.getType();
        int actionRadius = rt.actionRadiusSquared;
        int senseRadius = rt.sensorRadiusSquared;
        senseSurroundingEnemies(senseRadius);
        senseSurroundingAllies(senseRadius);

        int conv = rc.getConviction();
        int score = computePolScore(actionRadius);
        if (conv>10 && score>5) {
            if (rc.canEmpower(actionRadius)){
                rc.empower(actionRadius);
                return;
            }
        }

        Direction dir = move();
        if (dir == null && conv>10) {
            dir = randomDirection();
        }
        if (dir != null && rc.canMove(dir)){
            rc.move(dir);
        }
    }

    static void runSlanderer() throws GameActionException {
        int sensorRadius = rc.getType().sensorRadiusSquared;

        senseSurroundingEnemies(sensorRadius);
        senseSurroundingAllies(sensorRadius);

        Direction dir = move();
        if (dir != null && rc.canMove(dir)){
            rc.move(dir);
        }
    }

    static void runMuckraker() throws GameActionException {
        int sensorRadius = rc.getType().sensorRadiusSquared;
        senseSurroundingEnemies(sensorRadius);
        senseSurroundingAllies(sensorRadius);

        MapLocation toExpose = null;
        int bestConviction=0;
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, rc.getTeam().opponent())) {
            if (robot.type.canBeExposed()) {
                if (robot.conviction > bestConviction){
                    toExpose = robot.location;
                    bestConviction = robot.conviction;
                }
            }

            if (toExpose != null && rc.canExpose(toExpose)){
                rc.expose( toExpose);
                return;
            }
        }

        Direction dir = move();
        if (dir == null) {
            if ( Math.random() < 0.7){
                dir = randomDirectionv2();
            } else {
                dir = randomDirection();
            }

        }
        if (rc.canMove(dir)){
            rc.move(dir);
        }

    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static Direction randomDirectionv2() throws  GameActionException {
        MapLocation loc = rc.getLocation();
        MapLocation next;
        double bestValue=0, value;
        Direction bestDir=randomDirection();
        List<Direction> dirs = Arrays.asList(directions);
        Collections.shuffle(dirs);
        for( Direction dir: dirs){
            next = loc.add(dir);
            if (rc.onTheMap(next) && !rc.isLocationOccupied(next)){
                if (diags.contains(dir)) {
                    value = 2 * rc.sensePassability(next);
                } else {
                    value = rc.sensePassability(next);
                }

                if (value > bestValue){
                    bestValue = value;
                    bestDir = dir;
                }

            }
        }

        return bestDir;
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static RobotType chooseRobot(int currentInfluence) {
        if (turnCount < 5) {
            return RobotType.SLANDERER;
        }
        if ((flag & Flags.NEARSELFMUCK) != Flags.NEARSELFMUCK){
            return RobotType.MUCKRAKER;
        }

        if (currentInfluence > 100){
            return RobotType.POLITICIAN;
        }

        if (Math.random() < 0.1){
            return RobotType.POLITICIAN;
        } else if (Math.random() < 0.4){
            return RobotType.MUCKRAKER;
        }

        return RobotType.SLANDERER;

    }

    static int computeInfluence(RobotType robotType, int currentInfluence){
        switch (robotType) {
            case MUCKRAKER:
                return currentInfluence/5 + 1;
            case SLANDERER:
                return 10;
            default:
                return (int) Math.min(currentInfluence*0.8, 50);
        }
    }

    static void buildRobot(int currentInfluence, Direction dir) throws GameActionException {
        RobotType robot;
        int influcence = 1;
        if (turnCount < 10){
            robot = RobotType.SLANDERER;
            influcence = (int) (currentInfluence * 0.7+10);
        } else if ( convictionNear < currentInfluence && ((flag & Flags.NEARMUCK) == Flags.NEARMUCK)){
            robot = RobotType.POLITICIAN;
            influcence = (int) (convictionNear + 10 + Math.random()*currentInfluence*0.2);
        } else if ((flag & Flags.NEARSELFMUCK) != Flags.NEARSELFMUCK){
            robot = RobotType.MUCKRAKER;
            double x = Math.random() * Math.min(0, currentInfluence - 20) + 20;
            influcence = (int) x;
        } else if (Math.random() < 0.5){
            robot = RobotType.POLITICIAN;
            influcence = (int) (Math.random()*currentInfluence + 50);
        } else if (currentInfluence > 130) {
            robot = RobotType.SLANDERER;
            influcence = (int) (currentInfluence * 0.7);
        } else {
            robot = RobotType.MUCKRAKER;
            influcence = (int) (currentInfluence * 0.9);
        }

        int treshold = turnCount/100 + 5 ;

        if ( (influcence < currentInfluence - treshold) && rc.canBuildRobot(robot, dir, influcence)){
            rc.buildRobot(randomSpawnableRobotType(), dir, influcence);
        }

    }

    static int computeBid(int currentInfluence){

        int bid = (int) (Math.min( (rc.getRobotCount()+turnCount*0.001)*0.5, currentInfluence*0.1));

        return bid;
    }

    static void spawnRobot( Direction dir, RobotType robot, int influence) throws GameActionException {
        if (rc.canBuildRobot(robot, dir, influence)) {
            rc.buildRobot(robot, dir, influence);
        }
    }

    static Direction move() throws GameActionException {
        if (vector.isnull()){
            return null;
        }
        MapLocation center = rc.getLocation();
        MapLocation newLoc;
        Direction bestDir = null;
        double value, bestValue=0;
        int dx = (int) vector.x;
        int dy = (int) vector.y;

        /*
        if ((flag & Flags.FLEE) != Flags.FLEE && rc.getType().actionRadiusSquared>= dx*dx + dy*dy){
            return null;
        }
        if ((flag & Flags.FLEE) == Flags.FLEE){
            dx = -dx;
            dy = -dy;
        }*/

        if (dx>0){
            newLoc = center.add( Direction.EAST);
            if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                bestValue = 1.0 * rc.sensePassability(newLoc);
                bestDir = Direction.EAST;
            }
            if (dy>0){
                newLoc = center.add( Direction.NORTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 2.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHEAST;
                    }
                }

                newLoc = center.add( Direction.NORTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTH;
                    }
                }

            } else if (dy==0){
                newLoc = center.add( Direction.NORTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHEAST;
                    }
                }

                newLoc = center.add( Direction.SOUTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHEAST;
                    }
                }

            } else {
                newLoc = center.add( Direction.SOUTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 2.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHEAST;
                    }
                }

                newLoc = center.add( Direction.SOUTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTH;
                    }
                }
            }
        } else if (dx==0) {
            if (dy>0){
                newLoc = center.add( Direction.NORTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTH;
                    }
                }

                newLoc = center.add( Direction.NORTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHEAST;
                    }
                }

                newLoc = center.add( Direction.NORTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHWEST;
                    }
                }

            } else if (dy==0){
                return null;

            } else {
                newLoc = center.add( Direction.SOUTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTH;
                    }
                }

                newLoc = center.add( Direction.SOUTHEAST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHEAST;
                    }
                }

                newLoc = center.add( Direction.SOUTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHWEST;
                    }
                }
            }
        } else {
            newLoc = center.add( Direction.WEST);
            if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                bestValue = 1.0 * rc.sensePassability(newLoc);
                bestDir = Direction.WEST;
            }
            if (dy>0){
                newLoc = center.add( Direction.NORTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 2.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHWEST;
                    }
                }

                newLoc = center.add( Direction.NORTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTH;
                    }
                }

            } else if (dy==0){
                newLoc = center.add( Direction.NORTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.NORTHWEST;
                    }
                }

                newLoc = center.add( Direction.SOUTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHWEST;
                    }
                }

            } else {
                newLoc = center.add( Direction.SOUTHWEST);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 2.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTHWEST;
                    }
                }

                newLoc = center.add( Direction.SOUTH);
                if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
                    value = 1.0 * rc.sensePassability(newLoc);
                    if (value > bestValue){
                        bestValue = value;
                        bestDir = Direction.SOUTH;
                    }
                }
            }
        }

        return bestDir;
    }
    static void senseSurroundingEnemies(int sensorRadius) throws GameActionException {
        RobotType rt = rc.getType();
        int count = 0;
        convictionNear = 0;
        MapLocation loc = rc.getLocation();
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, rc.getTeam().opponent())) {
            count ++;
            flag |= Flags.NEAROPP;
            switch ( robot.type){
                case POLITICIAN:
                    flag |= Flags.NEARPOL;
                    if (robot.conviction>= rc.getConviction()){
                        //flag |= Flags.FLEE;
                        vector.update(robot.location, loc, -20);
                    } else if (rt == RobotType.POLITICIAN){
                        vector.update(robot.location, loc, 20);
                    }
                    break;
                case SLANDERER:
                    flag |= Flags.NEARSLAND;
                    if (rt == RobotType.MUCKRAKER) {
                        vector.update(robot.location, loc, 20);
                    }
                    break;
                case ENLIGHTENMENT_CENTER:
                    flag |= Flags.NEARCENTER;
                    if (rt == RobotType.POLITICIAN && rc.getConviction() > 10) {
                        vector.update(robot.location, loc, 20);
                    }
                    break;
                case MUCKRAKER:
                    flag |= Flags.NEARMUCK;
                    if (rt == RobotType.SLANDERER) {
                        vector.update(robot.location, loc, -20);
                        //flag |= Flags.FLEE;
                    }
                    if (rt==RobotType.ENLIGHTENMENT_CENTER){
                        convictionNear += robot.conviction;
                    }
                    if (rt == RobotType.POLITICIAN && rc.getConviction() > robot.conviction) {
                        int weight = (int) ((rc.getConviction() / (robot.conviction+1)) * 1.5);
                        vector.update(robot.location, loc, weight);
                    }
                    break;
                default:
                    break;
            }

        }

        for (RobotInfo robot2 : rc.senseNearbyRobots(sensorRadius, Team.NEUTRAL)){
            flag |= Flags.NEARCENTER;
            if (rt == RobotType.POLITICIAN && rc.getConviction() > 10) {
                vector.update(robot2.location, loc, 40);
            }


        }

        enemycount = count;

    }

    static void senseSurroundingAllies(int sensorRadius) throws GameActionException {
        RobotType rt = rc.getType();
        MapLocation loc = rc.getLocation();
        int count = 0;
        for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, rc.getTeam())) {
            count ++;
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER){
                flag |= Flags.NEARSELFCENTER;
            }
            if (robot.type == RobotType.POLITICIAN){
                flag |= Flags.NEARSELFPOL;
                if (rt == RobotType.POLITICIAN && rc.getConviction() <= 10 && robot.conviction>10){
                    vector.update(robot.location, loc, 30);
                }
            }

            if (robot.type == RobotType.MUCKRAKER){
                flag |= Flags.NEARSELFMUCK;
            }

            if (robot.type == RobotType.SLANDERER){
                flag |= Flags.NEARSELFSLAND;
            }

            // Dispersion strategy
            if (rt == RobotType.MUCKRAKER){
                //flag |= Flags.FLEE;
                vector.update(robot.location, loc, -1);
            }

            if (count< rt.bytecodeLimit/800*2) {
                if (rc.canGetFlag(robot.ID)) {
                    int allyFlag = rc.getFlag(robot.ID);
                    switch (rt) {
                        case SLANDERER:
                            if ((allyFlag & Flags.NEAROPP) == Flags.NEAROPP) {
                                if ((allyFlag & Flags.NEARMUCK) == Flags.NEARMUCK){
                                    vector.update(robot.location, loc, -10);
                                } else {
                                    vector.update(robot.location, loc, -5);
                                }

                            } else if ( robot.type == RobotType.MUCKRAKER && ((allyFlag & Flags.NEAROPP) != Flags.NEAROPP)){
                                vector.update(robot.location, loc, 1);
                            }
                            break;
                        case POLITICIAN:
                            if ((allyFlag & Flags.NEARCENTER) == Flags.NEARCENTER && rc.getConviction() > 10) {
                                vector.update(robot.location, loc, 10);
                            }
                            if (robot.type == followPolicy) {
                                vector.update(robot.location, loc, 1);
                            }
                            break;
                        case MUCKRAKER:
                            if ((allyFlag & Flags.NEARSLAND)==Flags.NEARSLAND){
                                vector.update(robot.location, loc, 5);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }


        }

        allycount = count;

    }


    static int computePolScore(int actionRadius){
        int score = 0;

        for (RobotInfo robot: rc.senseNearbyRobots(actionRadius, rc.getTeam())) {
            if (robot.conviction < 10 && robot.type == RobotType.POLITICIAN) {
                score += 5;
            }

        }

        for (RobotInfo robot2: rc.senseNearbyRobots(actionRadius, rc.getTeam().opponent())) {
            int val = 1;
            if (robot2.conviction+10 < rc.getConviction()) {
                val = 3;
            }

            if (robot2.type == RobotType.POLITICIAN) {
                score += val*2;
            } else if (robot2.type == RobotType.ENLIGHTENMENT_CENTER) {
                score += val*4;
            } else if (robot2.type == RobotType.MUCKRAKER && (((flag & Flags.NEARSELFPOL) == Flags.NEARSELFPOL) ||
                    ((flag & Flags.NEARSELFCENTER) == Flags.NEARSELFCENTER))) {
                score += val*4;
            }

        }

        return score;
    }

}