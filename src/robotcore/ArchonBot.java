package robotcore;

import battlecode.common.*;

public class ArchonBot extends RobotGlobal {
    static int archonOrder = -1;
    static int gardenersBuilt = 0;
    static boolean firstTurn = true;

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
        if(teamBullets >= 10000) rc.donate(10000);
        if(rc.getRoundLimit() - rc.getRoundNum() < 2) {
        	System.out.println("Game is ending! All bullets are being donated.");
        	rc.donate(teamBullets);
        }

        // Archon count and leader selection
        if (isLeader()) {
            // Since I'm the leader and it's not turn 0, I reset the counter (by broadcasting 1 below)
        } else {
            // Either it's turn 0, or I'm not the leader. So count.
            int newArchonOrder = rc.readBroadcast(ARCHON_COUNTER_CHANNEL);
            if (newArchonOrder > archonOrder) {
                // Leader died, so now I am the leader
                newArchonOrder = 0;
            }
            archonOrder = newArchonOrder;
        }
        rc.broadcast(ARCHON_COUNTER_CHANNEL, archonOrder + 1);

        if (isLeader()) {
            if (firstTurn) {
                initializeBuildQueue1();
                initializeBuildQueue2();
                initializeDefaultBuild();
            }
        }

        // Broadcast location
        int locChannel = ARCHON_LOCATION_TABLE_CHANNEL + (archonOrder*ARCHON_LOCATION_TABLE_ENTRY_SIZE);
        if (archonOrder >= ARCHON_LOCATION_TABLE_NUM_ENTRIES) {
            System.out.println("More than 3 archons detected!!!");
        } else {
            rc.broadcast(locChannel, Float.floatToIntBits(myLoc.x));
            rc.broadcast(locChannel + 1, Float.floatToIntBits(myLoc.y));
        }

        Direction gardenerDir = randomDirection();

        if (gardenersBuilt < 2) {
            if (rc.canHireGardener(gardenerDir) && gardenersBuilt == 0) {
                rc.hireGardener(gardenerDir);
                gardenersBuilt++;
            } else if (rc.canHireGardener(gardenerDir) && Math.random() < 0.01) {
            	rc.hireGardener(gardenerDir);
                gardenersBuilt++;
            }
        }

        if (isLeader()) {
            maintainFarmAndLumberjackTables();
        }

        firstTurn = false;

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }

    private static void maintainFarmAndLumberjackTables() throws GameActionException {
        boolean[] farmTableHasLumberjackJob = getFarmTableHasLumberjackJob();
        int farmTableEntryCount = getFarmTableEntryCount();
        for (int farmNum = 0; farmNum < farmTableEntryCount; farmNum++) {
            int farmFlags = readFarmTableEntryFlags(farmNum);
            if ((farmFlags & FARM_TABLE_ENTRY_GARDENER_MASK) != 0) {
                if ((farmFlags & FARM_TABLE_ENTRY_LUMBERJACK_MASK) == 0) {
                    // Needs lumberjack
                    if (!farmTableHasLumberjackJob[farmNum]) {
                        // Has no job entry
                        MapLocation farmLoc = readFarmTableEntryLocation(farmNum);
                        addLumberjackJob(farmLoc, farmNum);
                    }
                }
            }
            resetFarmTableEntryFlags(farmNum);
        }
    }

    private static boolean isLeader() {
        return archonOrder == 0;
    }
}
