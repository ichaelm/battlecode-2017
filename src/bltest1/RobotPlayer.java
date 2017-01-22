package bltest1;

import battlecode.common.*;
import robotcore.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            while (true) {
                MapLocation[] locs = rc.senseBroadcastingRobotLocations();
                for (MapLocation loc : locs) {
                    rc.setIndicatorDot(loc, 255, 0, 0);
                }
                Clock.yield();
            }
        } catch (Exception e) {
            System.out.println("Exception!");
            e.printStackTrace();
        }
	}

}
