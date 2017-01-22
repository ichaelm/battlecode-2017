package test_comm_map;

import battlecode.common.*;
import robotcore.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            RobotGlobal.init(rc);
            RobotGlobal.update();

            CommMap.initialize(rc, RobotGlobal.myLoc);
            CommMap.refresh(RobotGlobal.knownMapBounds);

            CommMap.Cell c = CommMap.queryCell(0,0);
            System.out.println(c);
            c.setExplored(true);
            c.setClear(false);
            CommMap.sendCell(c);
            c = CommMap.queryCell(0, 0);
            System.out.println(c);

            c = CommMap.queryCell(0,1);
            System.out.println(c);
            c.setExplored(true);
            c.setClear(false);
            CommMap.sendCell(c);
            c = CommMap.queryCell(0, 1);
            System.out.println(c);

            c = CommMap.queryCell(1,0);
            System.out.println(c);
            c.setExplored(true);
            c.setClear(false);
            CommMap.sendCell(c);
            c = CommMap.queryCell(1, 0);
            System.out.println(c);

            c = CommMap.queryCell(0,-1);
            System.out.println(c);
            c.setExplored(true);
            c.setClear(false);
            CommMap.sendCell(c);
            c = CommMap.queryCell(0, -1);
            System.out.println(c);

            c = CommMap.queryCell(-1,0);
            System.out.println(c);
            c.setExplored(true);
            c.setClear(false);
            CommMap.sendCell(c);
            c = CommMap.queryCell(-1, 0);
            System.out.println(c);

        } catch (Exception e) {
            System.out.println("Exception!");
            e.printStackTrace();
        }
	}

}
