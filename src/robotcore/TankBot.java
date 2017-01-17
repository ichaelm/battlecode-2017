package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;


public class TankBot extends RobotGlobal {
	static Direction goDir;
	static boolean firstTurn = true;
	static int goCount = 0;
	static boolean shoot = true;
	static RobotInfo nearestEnemy = null; 
	static MapLocation attackLoc = null;
	static boolean friendlyFireOn = true;

	public static void friendlyFire(Direction d) throws GameActionException { // determines whether or not friendly fire will occur
		MapLocation target = null; 
		if (nearestEnemy != null) {
			 target = nearestEnemy.location;
		} else return;
		
		shoot = hasLineOfSight(target); // if I have line of sight, I want to shoot
		if (!shoot) {
			rc.setIndicatorLine(myLoc, target, 255, 0, 0);
		}
		RobotInfo[] robots = rc.senseNearbyRobots(myType.sensorRadius, myTeam);
		if (robots.length <= 1) { // if no nearby friendlies, shoot anyway
			shoot = true;
		}
	}


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
        shoot = true;
        
        tryToShake();
        processNearbyRobots();
        processNearbyBullets();
        processNearbyTrees();
        tryToShake();

        attackLoc = queryAttackLocation();
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
            if (!friendlyFireOn) friendlyFire(goDir); // if this tank is to avoid FriendlyFire
        } else if (attackLoc != null) {
            goDir = myLoc.directionTo(attackLoc);
            if (treeInRange) { rc.setIndicatorDot(nearestTree.location, 100, 100, 100);} 	// Indicate nearest tree
            if (myLoc.distanceTo(attackLoc) < myType.bodyRadius * 2) {
                sendAttackFinished();
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
            if (rc.canFireSingleShot() && shoot) { // if this tank is to avoid FriendlyFire
                rc.fireSingleShot(myLoc.directionTo(nearestEnemy.location));
            }
        }

        firstTurn = false;

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
