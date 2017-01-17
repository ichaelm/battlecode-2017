package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;



public class SoldierBot extends RobotGlobal {
	static Direction goDir;
	static boolean firstTurn = true;
	static int goCount = 0;
	static boolean dontShoot = false;

	public static void friendlyFire(Direction d) throws GameActionException { // determines whether or not friendly fire will occur
		RobotInfo[] enemiesInFront = rc.senseNearbyRobots(myLoc.add(d, 2), 0.95f, myTeam.opponent());
		RobotInfo[] alliesInFront = rc.senseNearbyRobots(myLoc.add(d, 2), 0.95f, myTeam);

		if (enemiesInFront.length > 0) dontShoot = false; // if enemies right in front, shoot
		else dontShoot = true; // else, don't

		int numAllies = alliesInFront.length;
		int numEnemies = enemiesInFront.length;
		if (numAllies > numEnemies) dontShoot = true; // if more allies right in front, don't
		//if (dontShoot) return; // if allies are too close, return don't shoot

		for (float dist = 3f; dist < 6.3f; dist = 0.6f) {
			enemiesInFront = rc.senseNearbyRobots(myLoc.add(d, dist), 0.4f, myTeam.opponent());
			alliesInFront = rc.senseNearbyRobots(myLoc.add(d, dist), 0.3f, myTeam);
			numAllies += alliesInFront.length;
			numEnemies += enemiesInFront.length;
		}

		if (numAllies > numEnemies) dontShoot = true; // if more allies right in front, don't

	}


    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                System.out.println("Soldier: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
            } catch (Exception e) {
                System.out.println("Soldier: exception during turn");
                e.printStackTrace();
            }
        }
    }
    
    
    
    public static void turn() throws GameActionException {
        if (firstTurn) {
            goDir = randomDirection();
        }
        
        tryToShake();
        processNearbyRobots();
        processNearbyBullets();

        MapLocation attackLoc = queryAttackLocation();
        RobotInfo nearestEnemy = getNearestEnemy();

        if (nearestEnemy != null) {
            goDir = myLoc.directionTo(nearestEnemy.location);
            if (!friendlyFireOn) friendlyFire(goDir); // if this soldier is to avoid FriendlyFire
        } else if (attackLoc != null) {
            goDir = myLoc.directionTo(attackLoc);
            if (myLoc.distanceTo(attackLoc) < myType.bodyRadius * 2) {
                sendAttackFinished();
            }
        }
      
        boolean moved;
        if (nearestEnemy == null && attackLoc == null) {
            moved = tryMoveElseLeftRight(goDir, 15, 2);
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
            if (rc.canFireSingleShot() && !dontShoot) { // if this soldier is to avoid FriendlyFire
                rc.fireSingleShot(myLoc.directionTo(nearestEnemy.location));
            }
        }

        firstTurn = false;

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
    
    void doesNothing(){
   	 /*
       int moveMode = 0; // 0 is approach, 1 is kite lumberjack, 2 is run from lumberjack
       Direction moveDir = null;

       MapLocation myLocation = rc.getLocation();
       MapLocation closestEnemyLoc = null;
       float closestEnemyDist = 9999999;

       // See if there are any nearby enemy robots
       RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

       for (RobotInfo robot : robots) {
           MapLocation robotLocation = robot.getLocation();
           float dist = robot.getLocation().distanceTo(myLocation);
           if (dist < closestEnemyDist) {
               closestEnemyLoc = robotLocation;
               closestEnemyDist = dist;
           }
           RobotType type = robot.getType();
           if (type == RobotType.LUMBERJACK) {
               if (dist <= GameConstants.LUMBERJACK_STRIKE_RADIUS + RobotType.LUMBERJACK.bodyRadius) {
                   moveMode = 2; // run
                   moveDir = robotLocation.directionTo(myLocation);
               } else if (dist <= GameConstants.LUMBERJACK_STRIKE_RADIUS + RobotType.LUMBERJACK.bodyRadius + RobotType.SOLDIER.strideRadius) {
                   moveMode = 1; // kite
                   moveDir = robotLocation.directionTo(myLocation).rotateLeftDegrees(90);
               }
           }
       }
       if (moveMode == 0) {
           if (closestEnemyLoc == null) {
               moveDir = randomDirection();
           } else {
               moveDir = myLocation.directionTo(closestEnemyLoc);
           }
       }
   	  */

   }
    
}
