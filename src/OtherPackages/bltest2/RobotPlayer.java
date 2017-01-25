package bltest2;

import battlecode.common.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            while (true) {
                if (rc.getRoundNum() % 2 == 0) {
                    System.out.println("move before bc");
                    rc.broadcast(0, 0);
                    rc.move(Direction.getNorth());
                    rc.broadcast(0, 0);
                } else {
                    System.out.println("bc before move");
                    rc.broadcast(0, 0);
                    rc.move(Direction.getNorth());
                    rc.broadcast(0, 0);
                }
                Clock.yield();
            }
        } catch (Exception e) {
            System.out.println("Exception!");
            e.printStackTrace();
        }
	}

}
