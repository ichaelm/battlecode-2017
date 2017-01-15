package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;

public class ScoutBot extends RobotGlobal {

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

    public static void turn() throws GameActionException {
    	//Time to be a tree-seeking scout
    	if (roundNum < 100) {
    		//Set to 100 assuming that after that point we're out 
    		
    		//Set a direction randomly to start
        	if (currentDirection == null) {
        		currentDirection = randomDirection();
        	}
        	
        	//React to enemy nearby
        	processNearbyRobots();
        	RobotInfo nearestEnemy = getNearestEnemy();
        	
        	if (nearestEnemy != null) {
        		//Only shoot 50% of the time
        		if (Math.random() < 0.5) {
	        		Direction enemyDirection = myLoc.directionTo(nearestEnemy.location);
	                if (rc.canFireSingleShot()) {
	                    rc.fireSingleShot(enemyDirection);
	                }
        		}
        		
        		//Report the enemy on the broadcast
        		
                // Turn the opposite direction to run away
                // currentDirection = turn180(currentDirection);
            }
        	
        	//Check if there's a tree nearby
        	processNearbyTrees();
        	TreeInfo nearestTree = getNearestTree();
        	
        	if (nearestTree != null) {
        		//Move towards nearest tree if not already shaken
        		if (nearestTree.getContainedRobot() != null || nearestTree.getContainedBullets() > 0) {
        			Direction treeDirection = myLoc.directionTo(nearestTree.location);
        			tryMoveElseLeftRight(treeDirection);
        			
        			if (rc.canShake(nearestTree.ID)) {
            			rc.shake(nearestTree.ID);
            			System.out.println("Shook tree: " + nearestTree.ID);
            		}
        		} else {
        			//Move in the chosen direction
    	        	if (!tryMoveElseLeftRight(currentDirection)) {
    	        		//If it fails, choose a new direction
    	        		currentDirection = randomDirection();
    	        		tryMoveElseLeftRight(currentDirection);
    	        	}
        		}
        	} else {
	        	// no trees case
        		
	        	//Move in the chosen direction
	        	if (!tryMoveElseLeftRight(currentDirection)) {
	        		//If it fails, choose a new direction
	        		currentDirection = randomDirection();
	        		tryMoveElseLeftRight(currentDirection);
	        	}
        	}
    	} else {
    		//Have some other default scout behavior for when we're done shaking trees
    		
    		//Move towards enemy archon initial
            Direction goDir = myLoc.directionTo(enemyInitialArchonLocations[0]);
            tryMoveElseLeftRight(goDir);
            
    		//If enemy seen, broadcast it
    		
    			// Only shoot a small amount of the time? since bad value for a bullet
    		            
    	}
    	
        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
