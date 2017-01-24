package test_scouting;

import battlecode.common.*;
import robotcore.CommMap;
import robotcore.RobotGlobal;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            boolean leader = false;
            RobotGlobal.init(rc);
            int x = rc.readBroadcast(4999);
            if (x == 0) {
                leader = true;
                rc.broadcast(4999, 1);
            }
            CommMap.setRC(rc);
            if (leader) {
                CommMap.sendOrigin(rc.getLocation());
            }

            while (true) {
                CommMap.refresh(RobotGlobal.knownMapBounds);

                RobotGlobal.update();


                RobotGlobal.scoutHex();

                rc.move(Direction.getEast(), 0.5f);
                Clock.yield();

            }

        } catch (Exception e) {
            System.out.println("Exception!");
            e.printStackTrace();
        }
	}

}
