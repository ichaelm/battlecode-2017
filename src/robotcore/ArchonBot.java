package robotcore;

import battlecode.common.*;

public class ArchonBot extends RobotGlobal {
    static int archonOrder = -1;
    static boolean firstTurn = true;
    
    // Check all directions and see if I'm stuck
    public static boolean trapped() throws GameActionException {
    	boolean canBuild = false;
    	boolean canMove = false;
    	for (Direction d: usefulDirections) {
    		if (rc.canMove(d)) {
    			canMove = true;
    		}
    		if (!rc.isCircleOccupied(myLoc.add(d, 3 + GameConstants.GENERAL_SPAWN_OFFSET), 
    				RobotType.GARDENER.bodyRadius)) {
    			canBuild = true;
    		}
    	}
    	
    	if (!canBuild) return true;
    	if (!canMove) return true;
    	
    	return false;
    }

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
        processNearbyRobots();
        processNearbyBullets();
        processNearbyTrees();
        tryToShake();
        elections();

        registerArchon();

        if (isLeader) {
            leaderVP();
            if (firstTurn) {
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
            if (isLeader) {
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

        if (isLeader) {
            System.out.println("I am leader");
            System.out.println(firstTurn);
            System.out.println(iMakeGardeners);
            if (firstTurn && iMakeGardeners) {
                sendFirstFarm(myLoc);
            }
            leaderClearOrders();
            leaderManageActiveFarms();
            leaderStoreCounters();
        } else {
            System.out.println("I am not leader");
        }

        if (iMakeGardeners) {
            Direction gardenerDir;
            float mapCenterX = (knownMapBounds.getInnerBound(MapBounds.EAST) + knownMapBounds.getInnerBound(MapBounds.WEST)) / 2f;
            float mapCenterY = (knownMapBounds.getInnerBound(MapBounds.NORTH) + knownMapBounds.getInnerBound(MapBounds.SOUTH)) / 2f;
            MapLocation knownMapCenter = new MapLocation(mapCenterX, mapCenterY);
            gardenerDir = myLoc.directionTo(knownMapCenter);
            if (gardenerDir == null) {
                gardenerDir = randomDirection();
            }

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
                    /*
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
                    */
                    break;
            }

            /*
            System.out.println(exploredFarmsQueue.count());
            if (exploredFarmsQueue.count() > 0) {
                System.out.println(farmNumToLoc(exploredFarmsQueue.peek()[0]));
            }
            */
        }

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

        MapLocation farmLoc = queryCurrentFarmLoc();
        if (farmLoc != null) {
            // Found an explored farm to go to
            boolean moved = tryMoveElseLeftRight(myLoc.directionTo(farmLoc), 20, 5);
            if (!moved) {
                System.out.println("Can't move to farm i should explore");
            }
        }

        firstTurn = false;
    }
}
