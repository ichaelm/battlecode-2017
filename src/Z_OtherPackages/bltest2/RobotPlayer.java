package bltest2;

import battlecode.common.*;
import robotcore.RobotGlobal;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            while (true) {
                if (rc.getRoundNum() % 2 == 0) {
                    RobotGlobal.debug_print("move before bc");
                    rc.broadcast(0, 0);
                    rc.move(Direction.getNorth());
                    rc.broadcast(0, 0);
                } else {
                    RobotGlobal.debug_print("bc before move");
                    rc.broadcast(0, 0);
                    rc.move(Direction.getNorth());
                    rc.broadcast(0, 0);
                }
                Clock.yield();
            }
        } catch (Exception e) {
            RobotGlobal.debug_exception(e);
        }
	}

}
