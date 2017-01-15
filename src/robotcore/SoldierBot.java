package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;



public class SoldierBot extends RobotGlobal {
	static RobotInfo nearestEnemy = null;
	static boolean[] visitedArchonLocations = new boolean[enemyInitialArchonLocations.length];
	static boolean wander = false;
	static int goCount = 0;
	static Direction goDir;
	
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
    
    static int a = (int) (Math.random()*enemyInitialArchonLocations.length);
    
 // Makes soldiers explore more archon locations once reaching one and later wander the whole map
    public static void coordinate() throws GameActionException { 
    	MapLocation arcLoc = enemyInitialArchonLocations[a];
    	nearestEnemy = getNearestEnemy();
    	if (wander) return;
    	// if I can see the Archon location I'm headed to and there isn't an enemy near it...
    	if (rc.canSenseLocation(arcLoc) && nearestEnemy == null) { 
    		rc.setIndicatorLine(myLoc, arcLoc, 222, 222, 222);
    		visitedArchonLocations[a] = true;
    		
    		if (enemyInitialArchonLocations.length == 1) return; // if only one, do nothing
    		int i = 0;
    		for (boolean v: visitedArchonLocations) { // find next unvisited location
    			
    			if (v) i++;
    			else {
    				a = i;
    				return;
    			}
    		}
    		
    		System.out.println("All archon locations visited!");
    		wander = true;			// activate wander mode once Archon locations have been checked
    	}
    	
    }
    
    public static void turn() throws GameActionException {
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
        debugTick(1);
        processNearbyRobots();
        debugTick(2);
        processNearbyBullets();
        debugTick(3);
        coordinate();
        debugTick(4);
        
        if (!wander) {
        	rc.setIndicatorDot(enemyInitialArchonLocations[a], 255, 0, 255);
            //rc.setIndicatorLine(myLoc, enemyInitialArchonLocations[a], 1, 1, 1);
        	goDir = myLoc.directionTo(enemyInitialArchonLocations[a]);
        	goDir = rc.canMove(goDir) ? goDir: randomDirection();
        }
        else {
        	rc.setIndicatorDot(myLoc, 0, 200, 0);
        	if (goCount < 20 && rc.canMove(goDir)) {
        		goCount ++;
        	} 
        	else {
        		goCount = 0;
        		goDir = randomDirection();
        	}
        	
        }
        debugTick(5);
        
        
        if (nearestEnemy != null) {
            goDir = myLoc.directionTo(nearestEnemy.location);
        }

      
        boolean moved = tryMoveElseLeftRight(goDir);
        debugTick(6);
        if (!moved) {
            moved = tryMoveElseBack(goDir);
        }
        debugTick(7);

        if (nearestEnemy != null) {
            if (rc.canFireSingleShot()) {
                rc.fireSingleShot(myLoc.directionTo(nearestEnemy.location));
            }
        }
        debugTick(8);

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
