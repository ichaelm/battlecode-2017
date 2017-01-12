package robotcore;

import battlecode.common.*;

public class GardenerBot extends RobotGlobal {

    private enum FarmingMode {SEARCHING, PLANTING, WATERING};

    static Direction goDir = null;
    static int numPlanted = 0;
    static int birthTurn = -1;
    static MapLocation birthLocation = null;
    static FarmingMode mode = FarmingMode.SEARCHING;
    static Direction[] farmDirections = new Direction[]{
            Direction.getEast(),
            Direction.getEast().rotateLeftDegrees(60),
            Direction.getEast().rotateLeftDegrees(120),
            Direction.getEast().rotateLeftDegrees(180),
            Direction.getEast().rotateLeftDegrees(240),
            Direction.getEast().rotateLeftDegrees(300),
    };

    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                System.out.println("Gardener: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
            } catch (Exception e) {
                System.out.println("Gardener: exception during turn");
                e.printStackTrace();
            }
        }
    }

    public static void turn() throws GameActionException {
        if (birthTurn < 0) {
            birthTurn = roundNum;
            goDir = randomDirection();
            birthLocation = myLoc;
        }

        processNearbyBullets();

        Direction buildDir = randomDirection();

        RobotType currentBuildOrder = getBuildOrder();

        Direction toBirthLocation = myLoc.directionTo(birthLocation);
        if (toBirthLocation == null) {
            toBirthLocation = randomDirection();
        }

        processNearbyTrees();

        if (mode == FarmingMode.SEARCHING) {
            // move
            boolean moved = tryMoveElseLeftRight(goDir);
            if (!moved) {
                moved = tryMoveElseBack(goDir);
                if (!moved) {
                    goDir = randomDirection();
                }
            }
            // transition to planting
            float minFriendlyTreeDist = Float.POSITIVE_INFINITY;
            TreeInfo nearestFriendlyTree = getNearestFriendlyTree();
            if (nearestFriendlyTree != null) {
                minFriendlyTreeDist = myLoc.distanceTo(getNearestFriendlyTree().location);
            }
            MapLocation[] archonLocations = getMyArchonLocations();
            float minArchonDist = minDistBetween(myLoc, archonLocations);
            if (minFriendlyTreeDist > 6.5 && minArchonDist > 5.5) {
                mode = FarmingMode.PLANTING;
            }
        }

        if (mode == FarmingMode.PLANTING) {
            // Plant a plant if needed
            for (Direction farmDirection : farmDirections) {
                if (rc.hasTreeBuildRequirements()) {
                    if (rc.canPlantTree(buildDir)) {
                        rc.plantTree(buildDir);
                        numPlanted++;
                        if (numPlanted >= 3) {
                            mode = FarmingMode.WATERING;
                            break;
                        }
                    }
                } else {
                    break;
                }
            }

        }

        if (mode == FarmingMode.PLANTING || mode == FarmingMode.WATERING) {
            // Water the neediest friendly plant
            TreeInfo lowestFriendlyTree = getLowestFriendlyTree();
            if (lowestFriendlyTree != null) {
                if (rc.canWater(lowestFriendlyTree.ID)) {
                    rc.water(lowestFriendlyTree.ID);
                }
            }
        }

        if (mode == FarmingMode.WATERING) {
            // Build a unit if possible
            for (Direction farmDirection : farmDirections) {
                if (rc.hasRobotBuildRequirements(currentBuildOrder)) {
                    if (rc.canBuildRobot(currentBuildOrder, buildDir)) {
                        rc.buildRobot(currentBuildOrder, buildDir);
                    }
                } else {
                    break;
                }
            }
        }

        /*
        if (numPlanted >= 4) {
            if (rc.canBuildRobot(RobotType.LUMBERJACK, buildDir) && Math.random() < 0.02) {
                rc.buildRobot(RobotType.LUMBERJACK, buildDir);
            } else if (teamBullets > 400 && Math.random() < .02 &&
                    rc.canBuildRobot(RobotType.LUMBERJACK, buildDir)) {
                rc.buildRobot(RobotType.LUMBERJACK, buildDir);
            }
            numPlanted = Math.min(numPlanted, rc.senseNearbyTrees(10, rc.getTeam()).length);
        } else {
            if (rc.canPlantTree(toBirthLocation)) {
                rc.plantTree(toBirthLocation);
                numPlanted++;
            }
        }

        TreeInfo[] myNearbyTrees = rc.senseNearbyTrees(50, rc.getTeam());
        if (myNearbyTrees.length > 0) {
            TreeInfo minH = myNearbyTrees[0]; // min HP
            TreeInfo minD = myNearbyTrees[0]; // min Dist

            for (TreeInfo t: myNearbyTrees) {
                if ( myLoc.distanceTo(t.location) < myLoc.distanceTo(minD.location) ){
                    minD = t;
                }
                if (t.getHealth() < 10){
                    continue;
                }
                if (t.getHealth() < minH.getHealth()) minH = t; // find min HP tree
            }
            if (minH.getHealth() > 45) {
                tryMoveElseLeftRight(randomDirection());
            } else {
                if (!tryMoveElseLeftRight(myLoc.directionTo(minH.location))) {
                    tryMoveElseLeftRight(myLoc.directionTo(minD.location));
                }
                if(rc.canWater(minH.ID)) {
                    rc.water(minH.ID);
                }
                if(rc.canWater(minD.ID)) {
                    rc.water(minD.ID);
                }
            }
        } else {
            // Move randomly
            if(Math.random() < 0.6) {
                tryMoveElseLeftRight(toBirthLocation.opposite());
            } else {
                tryMoveElseLeftRight(randomDirection());
            }
        }
        */

        Clock.yield();
    }
}
