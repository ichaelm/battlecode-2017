package robotcore;

import battlecode.common.*;

public class ArchonBot extends RobotGlobal {
    static int archonOrder = -1;
    static int gardenersBuilt = 0;

    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                System.out.println("Archon: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
            } catch (Exception e) {
                System.out.println("Archon: exception during turn");
                e.printStackTrace();
            }
        }
    }

    public static void turn() throws GameActionException {
        RobotType currentBuildOrder = getBuildOrder();
        float currentBuildOrderCost = currentBuildOrder.bulletCost;

        // Archon count and leader selection
        if (isLeader()) {
            // Since I'm the leader and it's not turn 0, I reset the counter (by broadcasting 1 below)
        } else {
            // Either it's turn 0, or I'm not the leader. So count.
            int newArchonOrder = rc.readBroadcast(CHANNEL_ARCHON_COUNTER);
            if (newArchonOrder > archonOrder) {
                // Leader died, so now I am the leader
                newArchonOrder = 0;
            }
            archonOrder = newArchonOrder;
        }
        rc.broadcast(CHANNEL_ARCHON_COUNTER, archonOrder + 1);

        // Broadcast location
        int locChannel = CHANNEL_ARCHON_LOCATION_ARRAY_START + (archonOrder*2);
        rc.broadcast(locChannel, Float.floatToIntBits(myLoc.x));
        rc.broadcast(locChannel + 1, Float.floatToIntBits(myLoc.y));

        Direction gardenerDir = randomDirection();

        if (gardenersBuilt < 3) {
            if (rc.canHireGardener(gardenerDir)) {
                rc.hireGardener(gardenerDir);
                gardenersBuilt++;
            }
        }

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }

    private static boolean isLeader() {
        return archonOrder == 0;
    }
}
