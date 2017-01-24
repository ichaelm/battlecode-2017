package test_scouting;

import battlecode.common.*;
import robotcore.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            boolean leader = false;
            RobotGlobal.init(rc);
            int x = rc.readBroadcast(9999);
            if (x == 0) {
                leader = true;
                rc.broadcast(9999, 1);
            }
            if (leader) {
                RobotGlobal.CommMap.sendOrigin(rc.getLocation());
            }

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
