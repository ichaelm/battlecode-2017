package robotcore;

import battlecode.common.*;

public class LumberjackBot extends RobotGlobal {

    static Direction goDir;
    private static int farmNum = -1;
    private static float strikeRadius = GameConstants.LUMBERJACK_STRIKE_RADIUS;

    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                debug_print("Lumberjack: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
                Clock.yield();
            } catch (Exception e) {
                debug_print("Lumberjack: exception during turn");
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
		
        registerLumberjack();
        leadIfLeader();

        if (firstTurn) {
            goDir = randomDirection();
        }
        
        RobotInfo nearestEnemy = getNearestEnemy();
        boolean enemyInRange = false;
        Direction combatDir = null;
        if (nearestEnemy != null) {
            combatDir = myLoc.directionTo(nearestEnemy.location);
            float distToEnemy = myLoc.distanceTo(nearestEnemy.location);
            enemyInRange = distToEnemy <= myType.bodyRadius + strikeRadius + nearestEnemy.type.bodyRadius;
        }

        boolean moved = false;
        boolean attacked = false;
        boolean treeSeen = false;
        Direction treeDir = null;
        boolean robotTreeSeen = false;
        Direction robotTreeDir = null;
        TreeInfo nearestTree = getNearestUnfriendlyTree();
        if (nearestTree != null) {
            float distToTree = myLoc.distanceTo(nearestTree.location);
            treeDir = myLoc.directionTo(nearestTree.location);
            treeSeen = true;
        }
        TreeInfo nearestRobotTree = getNearestRobotTree();
        if (nearestRobotTree != null) {
            float distToRobotTree = myLoc.distanceTo(nearestRobotTree.location);
            robotTreeDir = myLoc.directionTo(nearestRobotTree.location);
            robotTreeSeen = true;
        }

        // Decide on mode
        if (enemyInRange) {
        	// combat
        	if (rc.canStrike()) {
        		if (rc.senseNearbyRobots(strikeRadius, myTeam).length < 2) {
        			rc.strike();
        			attacked = true;
        		}
        	}
        	if (!attacked) {
        		if (robotTreeSeen) {
                    if (rc.canChop(nearestRobotTree.ID)) {
                        rc.chop(nearestRobotTree.ID);
                        attacked = true;
                    }
                } else if (treeSeen) {
                    if (rc.canChop(nearestTree.ID)) {
                        rc.chop(nearestTree.ID);
                        attacked = true;
                    }
                }
            }
            moved = tryMoveElseBack(combatDir);
        } else {

            if (farmNum < 0) {
                int[] lumberjackJob = lumberjackJobsQueue.pop();
                if (lumberjackJob != null) {
                    farmNum = lumberjackJob[0];

                }
            }
            if (farmNum >= 0) {
                // do farm job
                debug_print("my farm num = " + farmNum);
                MapLocation farmLoc = farmNumToLoc(farmNum);
                FarmTableEntry e = readFarmTableEntry(farmNum);
                e.registerLumberjack();
                writeFarmTableEntry(farmNum, e);
                Direction farmDir = myLoc.directionTo(farmLoc);
                if (enemyInRange) {
                    if (rc.canStrike()) {
                        rc.strike();
                        attacked = true;
                    }
                }
                if (!attacked) {
                    if (robotTreeSeen) {
                        if (rc.canChop(nearestRobotTree.ID)) {
                            rc.chop(nearestRobotTree.ID);
                            attacked = true;
                        }
                    } else if (treeSeen) {
                        if (rc.canChop(nearestTree.ID)) {
                            rc.chop(nearestTree.ID);
                            attacked = true;
                        }
                    }
                }
                if (robotTreeSeen && nearestRobotTree.location.distanceTo(farmLoc) - nearestRobotTree.radius < 5.05f) {
                    moved = tryMoveElseBack(robotTreeDir);
                } else if (treeSeen && nearestTree.location.distanceTo(farmLoc) - nearestTree.radius < 5.05f) {
                    moved = tryMoveElseBack(treeDir);
                    debug_dot(nearestTree.location, 255, 0, 0);
                } else {
                    debug_dot(farmLoc, 255, 255, 0);
                    moved = tryMoveDistFrom(farmLoc, 4.05f);
                    if (!moved) {
                        moved = tryMoveDistFrom(farmLoc, 4.05f);
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
                    if (robotTreeSeen) {
                        if (rc.canChop(nearestRobotTree.ID)) {
                            rc.chop(nearestRobotTree.ID);
                            attacked = true;
                        }
                    } else if (treeSeen) {
                        if (rc.canChop(nearestTree.ID)) {
                            rc.chop(nearestTree.ID);
                            attacked = true;
                        }
                    }
                }
                if (prioritizeRobotTrees && nearestRobotTree != null) {
                    moved = tryMoveElseLeftRight(myLoc.directionTo(nearestRobotTree.location), 30, 5);
                    if (!moved) {
                        moved = tryMoveElseBack(myLoc.directionTo(nearestRobotTree.location));
                    }
                } else {
                    MapLocation defendLoc = peekDefendLocation();
                    MapLocation invadeLoc = peekAttackLocation();
                    if (defendLoc != null) {
                        Direction defendDir = myLoc.directionTo(defendLoc);
                        moved = tryMoveElseLeftRight(defendDir, 30, 5);
                        // If I'm close to the defend target, I already know there's no hostile, so pop it
                        if (myLoc.distanceTo(defendLoc) < myType.bodyRadius * 2) {
                            popDefendLocation();
                        }
                    } else if (invadeLoc != null) {
                        Direction invadeDir = myLoc.directionTo(invadeLoc);
                        moved = tryMoveElseLeftRight(invadeDir, 30, 5);
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
    }
}
