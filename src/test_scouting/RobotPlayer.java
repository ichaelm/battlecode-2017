package test_scouting;

import battlecode.common.*;
import robotcore.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            boolean leader = false;
            RobotGlobal.init(rc);

            while (true) {

                RobotGlobal.update();


                RobotGlobal.scoutHex();

                rc.move(Direction.getNorth().rotateRightDegrees(10), 0.5f);
                Clock.yield();

            }

        } catch (Exception e) {
            System.out.println("Exception!");
            e.printStackTrace();
        }
	}

}
