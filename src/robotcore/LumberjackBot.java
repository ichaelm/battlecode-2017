package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;

public class LumberjackBot extends RobotGlobal {

    private static int farmNum = -1;

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
    
    static int a = (int) (Math.random()*enemyInitialArchonLocations.length);

    public static void turn() throws GameActionException {
        processNearbyRobots();
        processNearbyBullets();
        processNearbyTrees();

        
        int roundnum = rc.getRoundNum();
        RobotInfo nearestEnemy = getNearestEnemy();
        Direction invadeDir = myLoc.directionTo(enemyInitialArchonLocations[a]);
        rc.setIndicatorDot(enemyInitialArchonLocations[a], 255, 0, 255);
        boolean enemyInRange = false;
        Direction combatDir = null;
        if (nearestEnemy != null) {
            combatDir = myLoc.directionTo(nearestEnemy.location);
            float distToEnemy = myLoc.distanceTo(nearestEnemy.location);
            enemyInRange = distToEnemy <= myType.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS + nearestEnemy.type.bodyRadius;
        }

        boolean moved = false;
        boolean attacked = false;
        boolean treeInRange = false;
        Direction treeDir = null;
        TreeInfo nearestTree = getNearestUnfriendlyTree();
        if (nearestTree != null) {
            float distToTree = myLoc.distanceTo(nearestTree.location);
            treeDir = myLoc.directionTo(nearestTree.location);
            treeInRange = distToTree <= myType.bodyRadius + myType.strideRadius + nearestTree.radius;
        }

        // Decide on mode
        if (enemyInRange) {
            // combat
            if (rc.canStrike()) {
                rc.strike();
                attacked = true;
            }
            if (!attacked) {
                if (treeInRange) {
                    if (rc.canShake(nearestTree.ID)) {
                        rc.shake(nearestTree.ID);
                    }
                    if (rc.canChop(nearestTree.ID)) {
                        rc.chop(nearestTree.ID);
                        attacked = true;
                    }
                }
            }
            moved = tryMoveElseBack(combatDir);
        } else {
            if (farmNum < 0) {
                farmNum = popLumberjackJobFarmNum();
            }
            if (farmNum >= 0) {
                // do farm job
                MapLocation farmLoc = readFarmTableEntryLocation(farmNum);
                writeFarmTableEntry(farmNum, farmLoc, false, true);
                Direction farmDir = myLoc.directionTo(farmLoc);
                if (enemyInRange) {
                    if (rc.canStrike()) {
                        rc.strike();
                        attacked = true;
                    }
                }
                if (!attacked) {
                    if (treeInRange) {
                        if (rc.canShake(nearestTree.ID)) {
                            rc.shake(nearestTree.ID);
                        }
                        if (rc.canChop(nearestTree.ID)) {
                            rc.chop(nearestTree.ID);
                            attacked = true;
                        }
                    }
                }
                if (treeInRange && nearestTree.location.distanceTo(farmLoc) - nearestTree.radius < 5.75f) {
                    moved = tryMoveElseBack(treeDir);
                } else {
                    moved = tryMoveDistFrom(farmLoc, 4.75f);
                    if (!moved) {
                        moved = tryMoveDistFrom(farmLoc, 4.75f);
                    }
                }
            } else {
                // invade
                if (enemyInRange) {
                    if (rc.canStrike()) {
                        rc.strike();
                        attacked = true;
                    }
                }
                if (!attacked) {
                    if (treeInRange) {
                        if (rc.canShake(nearestTree.ID)) {
                            rc.shake(nearestTree.ID);
                        }
                        if (rc.canChop(nearestTree.ID)) {
                            rc.chop(nearestTree.ID);
                            attacked = true;
                        }
                    }
                }
                moved = tryMoveElseLeftRight(invadeDir);
                if (!moved) {
                    moved = tryMoveElseBack(invadeDir);
                }
            }
        }






        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
