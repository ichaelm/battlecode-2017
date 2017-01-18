package robotcore;

import battlecode.common.*;


public class NewTankBot extends RobotGlobal {
	static Direction goDir;
	static boolean firstTurn = true;
	static int goCount = 0;
	static RobotInfo nearestEnemy = null; 
	static MapLocation attackLoc = null;
	static boolean friendlyFireOn = true;

    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                System.out.println("Tank: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
            } catch (Exception e) {
                System.out.println("Tank: exception during turn");
                e.printStackTrace();
            }
        }
    }
    
    
    
    public static void turn() throws GameActionException {
        if (firstTurn) {
            goDir = randomDirection();
        }
        boolean shoot = true;
        
        tryToShake();
        processNearbyRobots();
        processNearbyBullets();
        processNearbyTrees();
        tryToShake();

        attackLoc = peekAttackLocation();
        nearestEnemy = getNearestEnemy();
        
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

        if (nearestEnemy != null) {
            goDir = myLoc.directionTo(nearestEnemy.location);
            if (!friendlyFireOn) {
                shoot = hasLineOfSightFF(nearestEnemy.location); // if this tank is to avoid FriendlyFire
            }
        } else if (attackLoc != null) {
            goDir = myLoc.directionTo(attackLoc);
            if (treeInRange) { rc.setIndicatorDot(nearestTree.location, 100, 100, 100);} 	// Indicate nearest tree
            if (myLoc.distanceTo(attackLoc) < myType.bodyRadius * 2) {
                popAttackLocation();
            }
        }
      
        if (nearestEnemy == null && attackLoc == null) {
            if (treeInRange) { 			// Body-attack the nearest tree
            	rc.setIndicatorDot(nearestTree.location, 100, 100, 100);
            	moved = rc.canMove(treeDir);
            	if (moved) rc.move(treeDir);
            	else moved = false;
            }
            else moved = tryMoveElseLeftRight(goDir, 15, 2);
        } else {
            moved = tryMoveElseLeftRight(goDir);
        }
        if (!moved) {
            moved = tryMoveElseBack(goDir);
            if (!moved) {
                goDir = randomDirection();
            }
        }

        if (nearestEnemy != null) {
        	Direction atEnemy = myLoc.directionTo(nearestEnemy.location);
            float dist = nearestEnemy.location.distanceTo(myLoc); 
        	if (shoot) { // if this soldier is to avoid FriendlyFire
        		if (usePentad && rc.canFirePentadShot() && dist < pentadDist) { // if soldier shoots, canFire becomes false
                	rc.firePentadShot(atEnemy);
                }
        		else if (useTriad && rc.canFireTriadShot() && dist < triadDist) {
                	rc.fireTriadShot(atEnemy);
                }
        		else if (rc.canFireSingleShot()) {
            		rc.fireSingleShot(atEnemy);
            	}	
            }
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

        firstTurn = false;

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
