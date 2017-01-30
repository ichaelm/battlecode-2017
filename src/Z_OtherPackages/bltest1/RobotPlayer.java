package bltest1;

import battlecode.common.*;
import robotcore.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            while (true) {
                MapLocation[] locs = rc.senseBroadcastingRobotLocations();
                for (MapLocation loc : locs) {
                    RobotGlobal.debug_dot(loc, 255, 0, 0);
                }
                Clock.yield();
            }
        } catch (Exception e) {
            RobotGlobal.debug_exception(e);
        }
	}

}
