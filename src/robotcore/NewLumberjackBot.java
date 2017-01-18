package robotcore;

import battlecode.common.*;

public class NewLumberjackBot extends RobotGlobal {

    static Direction goDir;
    static boolean firstTurn = true;
    private static int farmNum = -1;

    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                System.out.println("Lumberjack: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
            } catch (Exception e) {
                System.out.println("Lumberjack: exception during turn");
                e.printStackTrace();
            }
        }
    }

    public static void turn() throws GameActionException {
        if (firstTurn) {
            goDir = randomDirection();
        }
        processNearbyRobots();
        processNearbyBullets();
        processNearbyTrees();
        tryToShake();
        
        RobotInfo nearestEnemy = getNearestEnemy();
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
        boolean robotTreeInRange = false;
        Direction robotTreeDir = null;
        TreeInfo nearestTree = getNearestUnfriendlyTree();
        if (nearestTree != null) {
            float distToTree = myLoc.distanceTo(nearestTree.location);
            treeDir = myLoc.directionTo(nearestTree.location);
            treeInRange = distToTree <= myType.bodyRadius + myType.strideRadius + nearestTree.radius;
        }
        TreeInfo nearestRobotTree = getNearestRobotTree();
        if (nearestRobotTree != null) {
            float distToRobotTree = myLoc.distanceTo(nearestRobotTree.location);
            robotTreeDir = myLoc.directionTo(nearestRobotTree.location);
            robotTreeInRange = distToRobotTree <= myType.bodyRadius + myType.strideRadius + nearestRobotTree.radius;
        }

        // Decide on mode
        if (enemyInRange) {
            // combat
            if (rc.canStrike()) {
                rc.strike();
                attacked = true;
            }
            if (!attacked) {
                if (robotTreeInRange) {
                    if (rc.canShake(nearestRobotTree.ID)) {
                        rc.shake(nearestRobotTree.ID);
                    }
                    if (rc.canChop(nearestRobotTree.ID)) {
                        rc.chop(nearestRobotTree.ID);
                        attacked = true;
                    }
                } else if (treeInRange) {
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
                    if (robotTreeInRange) {
                        if (rc.canShake(nearestRobotTree.ID)) {
                            rc.shake(nearestRobotTree.ID);
                        }
                        if (rc.canChop(nearestRobotTree.ID)) {
                            rc.chop(nearestRobotTree.ID);
                            attacked = true;
                        }
                    } else if (treeInRange) {
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
                    if (robotTreeInRange) {
                        if (rc.canShake(nearestRobotTree.ID)) {
                            rc.shake(nearestRobotTree.ID);
                        }
                        if (rc.canChop(nearestRobotTree.ID)) {
                            rc.chop(nearestRobotTree.ID);
                            attacked = true;
                        }
                    } else if (treeInRange) {
                        if (rc.canShake(nearestTree.ID)) {
                            rc.shake(nearestTree.ID);
                        }
                        if (rc.canChop(nearestTree.ID)) {
                            rc.chop(nearestTree.ID);
                            attacked = true;
                        }
                    }
                }
                if (prioritizeRobotTrees && nearestRobotTree != null) {
                    moved = tryMoveElseLeftRight(myLoc.directionTo(nearestRobotTree.location));
                    if (!moved) {
                        moved = tryMoveElseBack(myLoc.directionTo(nearestRobotTree.location));
                    }
                } else {
                    MapLocation defendLoc = peekDefendLocation();
                    MapLocation invadeLoc = peekAttackLocation();
                    if (defendLoc != null) {
                        Direction defendDir = myLoc.directionTo(defendLoc);
                        moved = tryMoveElseLeftRight(defendDir);
                        // If I'm close to the defend target, I already know there's no hostile, so pop it
                        if (myLoc.distanceTo(defendLoc) < myType.bodyRadius * 2) {
                            popDefendLocation();
                        }
                    } else if (invadeLoc != null) {
                        Direction invadeDir = myLoc.directionTo(invadeLoc);
                        moved = tryMoveElseLeftRight(invadeDir);
                        // If I'm close to the attack target, I already know there's no non-hostile, so pop it
                        if (myLoc.distanceTo(invadeLoc) < myType.bodyRadius * 2) {
                            popAttackLocation();
                        }
                    } else {
                        moved = tryMoveElseBack(goDir);
                        if (!moved) {
                            goDir = randomDirection();
                        }
                    }
                }
            }
        }

        firstTurn = false;

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
