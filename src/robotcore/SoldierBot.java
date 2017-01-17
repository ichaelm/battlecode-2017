package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;



public class SoldierBot extends RobotGlobal {
	static Direction goDir;
	static boolean firstTurn = true;
	static int goCount = 0;
	static boolean shoot = true;
	static RobotInfo nearestEnemy = null; 
	static MapLocation attackLoc = null;

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
        shoot = true;
        
        tryToShake();
        processNearbyRobots();
        processNearbyBullets();

        attackLoc = queryAttackLocation();
        nearestEnemy = getNearestEnemy();

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
