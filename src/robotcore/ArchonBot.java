package robotcore;

import battlecode.common.*;

public class ArchonBot extends RobotGlobal {
    static int archonOrder = -1;
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
                Clock.yield();
            } catch (Exception e) {
                System.out.println("Archon: exception during turn");
                e.printStackTrace();
            }
        }
    }

    public static void turn() throws GameActionException {
        VP();

        // Archon count and leader selection
        if (isLeader()) {
            // Since I'm the leader and it's not turn 0, I reset the counter
            int numArchons = rc.readBroadcast(ARCHON_COUNTER_CHANNEL);
            rc.broadcast(ARCHON_COUNTER_CHANNEL, 1);
            rc.broadcast(NUM_ARCHONS_CHANNEL, numArchons);
        } else {
            // Either it's turn 0, or I'm not the leader. So count.
            if (firstTurn) {
                archonOrder = rc.readBroadcast(ARCHON_COUNTER_CHANNEL);
                rc.broadcast(ARCHON_COUNTER_CHANNEL, archonOrder + 1);
            } else {
                int newArchonOrder = rc.readBroadcast(ARCHON_COUNTER_CHANNEL);
                if (newArchonOrder > archonOrder) {
                    // Leader died, so now I am the leader
                    newArchonOrder = 0;
                }
                archonOrder = newArchonOrder;
                rc.broadcast(ARCHON_COUNTER_CHANNEL, archonOrder + 1);
            }
        }

        if (isLeader()) {
            if (firstTurn) {
                initializeBuildQueue1();
                initializeBuildQueue2();
                initializeDefaultBuild();
                for (MapLocation attackLoc : enemyInitialArchonLocations) {
                    addAttackLocation(attackLoc);
                }
            }
            sendScoutMode(ScoutMode.HARASS, false);
        }

        boolean iMakeGardeners;
        if (firstTurn) {
            MapLocation maxMinDistLoc = null;
            float maxMinDist = -1f;
            for (MapLocation loc : myInitialArchonLocations) {
                float minDist = minDistBetween(loc, enemyInitialArchonLocations);
                if (minDist > maxMinDist) {
                    maxMinDist = minDist;
                    maxMinDistLoc = loc;
                }
            }
            if (myLoc.distanceTo(maxMinDistLoc) < RobotType.ARCHON.bodyRadius) { // if it's me
                iMakeGardeners = true;
            } else {
                iMakeGardeners = false;
            }
        } else {
            if (isLeader()) {
                MapLocation[] myArchonLocations = getMyArchonLocations();
                int maxMinDistArchon = -1;
                float maxMinDist = -1f;
                for (int i = 0; i < myArchonLocations.length; i++) {
                    MapLocation loc = myArchonLocations[i];
                    float minDist = minDistBetween(loc, enemyInitialArchonLocations);
                    if (minDist > maxMinDist) {
                        maxMinDist = minDist;
                        maxMinDistArchon = i;
                    }
                }
                rc.broadcast(WHICH_ARCHON_MAKES_GARDENERS_CHANNEL, maxMinDistArchon);
            }

            int whichArchonMakesGardeners = rc.readBroadcast(WHICH_ARCHON_MAKES_GARDENERS_CHANNEL);

            iMakeGardeners = (archonOrder == whichArchonMakesGardeners);
        }


        // Broadcast location
        int locChannel = ARCHON_LOCATION_TABLE_CHANNEL + (archonOrder*ARCHON_LOCATION_TABLE_ENTRY_SIZE);
        if (archonOrder >= ARCHON_LOCATION_TABLE_NUM_ENTRIES) {
            System.out.println("More than 3 archons detected!!!");
        } else {
            rc.broadcast(locChannel, Float.floatToIntBits(myLoc.x));
            rc.broadcast(locChannel + 1, Float.floatToIntBits(myLoc.y));
        }

        if (isLeader()) {
            maintainFarmAndLumberjackTables();
        }

        if (iMakeGardeners) {
            Direction gardenerDir;
            float mapCenterX = (knownMapBounds.getInnerBound(MapBounds.EAST) + knownMapBounds.getInnerBound(MapBounds.WEST)) / 2f;
            float mapCenterY = (knownMapBounds.getInnerBound(MapBounds.NORTH) + knownMapBounds.getInnerBound(MapBounds.SOUTH)) / 2f;
            MapLocation knownMapCenter = new MapLocation(mapCenterX, mapCenterY);
            gardenerDir = myLoc.directionTo(knownMapCenter);

            GardenerSchedule gardenerSchedule = getGardenerSchedule();
            int gardenersBuilt = rc.readBroadcast(NUM_GARDENERS_BUILT_CHANNEL);
            boolean success;
            switch (gardenerSchedule) {
                case ONCE_EVERY_N_ROUNDS:
                    int gardenerScheduleN = getGardenerScheduleN();
                    if (gardenersBuilt < (roundNum / gardenerScheduleN) + 1) {
                        success = tryHireGardener(gardenerDir);
                        if (success) {
                            gardenersBuilt++;
                            rc.broadcast(NUM_GARDENERS_BUILT_CHANNEL, gardenersBuilt);
                        }
                    }
                    break;
                case WHEN_FULL:
                    int numFarmsBuildingTrees = numFarmsBuildingTrees();
                    if (numFarmsBuildingTrees == 0) {
                        success = tryHireGardener(gardenerDir);
                        if (success) {
                            gardenersBuilt++;
                            rc.broadcast(NUM_GARDENERS_BUILT_CHANNEL, gardenersBuilt);
                            if (getLateLumberjacks() && getFarmTableEntryCount() > 0) {
                                if (getScoutWhenFull()) {
                                    addBuildQueue1(RobotType.SCOUT);
                                }
                                addBuildQueue1(RobotType.LUMBERJACK);
                            }
                        }
                    }
                    break;
            }
        }

        processNearbyRobots();

        RobotInfo nearestHostile = getNearestEnemyHostile();
        RobotInfo nearestNonHostile = getNearestEnemyNonHostile();

        // Update attack and defend locations
        if (nearestHostile != null) {
            int whichDefendLoc = whichDefendLocation(nearestHostile.location);
            if (whichDefendLoc >= 0) {
                updateDefendLocation(nearestHostile.location, whichDefendLoc);
            } else {
                addDefendLocationFirst(nearestHostile.location);
            }
        }
        if (nearestNonHostile != null) {
            int whichAttackLoc = whichAttackLocation(nearestNonHostile.location);
            if (whichAttackLoc >= 0) {
                updateAttackLocation(nearestNonHostile.location, whichAttackLoc);
            } else {
                addAttackLocationFirst(nearestNonHostile.location);
            }
        }

        firstTurn = false;
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
