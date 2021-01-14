package  ChocoBananaV1;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static UnitMethods methods;


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
        switch (rc.getType()) {
            case ENLIGHTENMENT_CENTER:
                methods = new EC(rc);
                break;
            case POLITICIAN:
                methods = new Politician(rc);
                break;
            case SLANDERER:
                methods = new Slanderer(rc);
                break;
            case MUCKRAKER:
                methods = new Muckracker(rc);
                break;
        }


        System.out.println("I'm a " + rc.getType() + " and I just got created! I have influence " + rc.getInfluence());
        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                if (methods.rt != rc.getType()){
                    // transformed
                    System.out.println("changed");
                    methods = new Politician(rc);
                }
                methods.step();
                methods.run();
                rc.setFlag( methods.flag);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}