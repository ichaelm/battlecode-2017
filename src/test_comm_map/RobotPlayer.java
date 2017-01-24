package test_comm_map;

import battlecode.common.*;
import robotcore.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            RobotGlobal.init(rc);
            RobotGlobal.CommMap.sendOrigin(rc.getLocation());

            while (true) {
                RobotGlobal.update();

            /*
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
            */

                Clock.yield();

            /*
            float[] myExactHexCoord = CommMap.exactHexCoord(RobotGlobal.myLoc);
            float minAExact = myExactHexCoord[0] - (RobotGlobal.myType.sensorRadius / CommMap.CELL_RESOLUTION);
            float maxAExact = myExactHexCoord[0] + (RobotGlobal.myType.sensorRadius / CommMap.CELL_RESOLUTION);
            float minBExact = myExactHexCoord[1] - (RobotGlobal.myType.sensorRadius / CommMap.CELL_RESOLUTION);
            float maxBExact = myExactHexCoord[1] + (RobotGlobal.myType.sensorRadius / CommMap.CELL_RESOLUTION);
            int minA = (int)Math.ceil(minAExact);
            int maxA = (int)Math.floor(maxAExact);
            int minB = (int)Math.ceil(minBExact);
            int maxB = (int)Math.floor(maxBExact);
            */
            /*
            int[] RobotGlobal.myHexCoord = CommMap.nearestHexCoord(RobotGlobal.myLoc);
            CommMap.HexCoord.Iterator it = CommMap.hexFullIterator(RobotGlobal.myHexCoord.a, RobotGlobal.myHexCoord.b, (int)(RobotGlobal.myType.sensorRadius / CommMap.CELL_RESOLUTION));
            */
                int[] hexBounds = RobotGlobal.CommMap.circleHexBounds(RobotGlobal.myLoc, 10);

                int total = 0;
                int totalInside = 0;

            /*
            for (int a = minA; a <= maxA; a++) {
                for (int b = minB; b <= maxB; b++) {
                    MapLocation loc = CommMap.hexCoordToLoc(a, b);
                    if (loc.distanceTo(RobotGlobal.myLoc) <= RobotGlobal.myType.sensorRadius) {
                        rc.setIndicatorDot(loc, 255, 0, 0);
                        totalInside++;
                    }
                    total++;
                }
            }
            */
            /*
            while (it.hasNext()) {
                CommMap.HexCoord hc = it.next();
                MapLocation loc = CommMap.hexCoordToLoc(hc.a, hc.b);
                if (loc.distanceTo(RobotGlobal.myLoc) <= RobotGlobal.myType.sensorRadius) {
                    rc.setIndicatorDot(loc, 255, 0, 0);
                    totalInside++;
                }
                total++;
            }
            */
                int minA = hexBounds[0];
                int maxA = hexBounds[1];
                int minB = hexBounds[2];
                int maxB = hexBounds[3];
                //int minSum = hexBounds[4];
                //int maxSum = hexBounds[5];
                for (int a = minA; a <= maxA; a++) {
                    for (int b = minB; b <= maxB; b++) {
                        //if ((a + b >= minSum) && (a + b <= maxSum)) {
                        MapLocation loc = RobotGlobal.CommMap.hexCoordToLoc(a, b);
                        if (loc.distanceTo(RobotGlobal.myLoc) <= 10) {
                            rc.setIndicatorDot(loc, 255, 0, 0);
                            totalInside++;
                        }
                        total++;
                        //}
                    }
                }

                Clock.yield();

                HexCoord.Iterator it = HexCoord.hexPerimeterIterator(RobotGlobal.myHexCoord.a, RobotGlobal.myHexCoord.b, (int) (10 / RobotGlobal.CommMap.CELL_RESOLUTION));

                Clock.yield();

                while (it.hasNext()) {
                    HexCoord hc = it.next();
                    MapLocation loc = RobotGlobal.CommMap.hexCoordToLoc(hc.a, hc.b);
                    if (loc.distanceTo(RobotGlobal.myLoc) <= 10) {
                        rc.setIndicatorDot(loc, 255, 0, 0);
                    }
                }

                Clock.yield();

                int maxSteps = (int)Math.floor((10 / (2f / 1.15470053838f)) + 1.15470053838f);
                minA = RobotGlobal.myHexCoord.a - maxSteps;
                maxA = RobotGlobal.myHexCoord.a + maxSteps;
                minB = RobotGlobal.myHexCoord.b - maxSteps;
                maxB = RobotGlobal.myHexCoord.b + maxSteps;
                int minSum = RobotGlobal.myHexCoord.a + RobotGlobal.myHexCoord.b - maxSteps;
                int maxSum = RobotGlobal.myHexCoord.a + RobotGlobal.myHexCoord.b + maxSteps;

                Clock.yield();

                for (int a = minA; a <= maxA; a++) {
                    for (int b = Math.max(minB, (minSum - a)); b <= maxB && b <= (maxSum - a); b++) {
                        //if ((a + b >= minSum) && (a + b <= maxSum)) {
                        MapLocation loc = RobotGlobal.CommMap.hexCoordToLoc(a, b);
                        if (loc.distanceTo(RobotGlobal.myLoc) <= 10) {
                            rc.setIndicatorDot(loc, 255, 0, 0);
                            totalInside++;
                        }
                        total++;
                        //}
                    }
                }

                Clock.yield();

                System.out.println(totalInside);
                System.out.println(total);
                rc.move(Direction.getEast(), 0.1f);
                Clock.yield();

            }

        } catch (Exception e) {
            System.out.println("Exception!");
            e.printStackTrace();
        }
	}

}
