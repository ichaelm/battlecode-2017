package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;

public class ScoutBot extends RobotGlobal {

	static ScoutMode mode = null;
	static MapLocation selectedGardenerLoc = null;
	static MapLocation selectedCampingTreeLoc = null;

    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                System.out.println("Scout: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
            } catch (Exception e) {
                System.out.println("Scout: exception during turn");
                e.printStackTrace();
            }
        }
    }

    public static void turn() throws GameActionException {
    	mode = queryScoutMode(mode);
    	if (mode == ScoutMode.COLLECT) {
    		
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
        		if (nearestTree.getContainedBullets() > 0) {
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
	        	if (!tryMoveElseBack(currentDirection)) {
	        		//If it fails, choose a new direction
	        		currentDirection = randomDirection();
	        		tryMoveElseBack(currentDirection);
	        	}
        	}
    	} else if (mode == ScoutMode.HARASS) {
    		processNearbyRobots();
    		processNearbyTrees();
    		processNearbyBullets();

			RobotInfo targetLumberjack = getNearestEnemyLumberjack();
			RobotInfo selectedGardenerInfo = null;
			if (selectedGardenerLoc != null) { // If an enemy gardener is selected:
				// If the gardener is dead or out of range, forget it
				if (myLoc.distanceTo(selectedGardenerLoc) < myType.sensorRadius) {
					selectedGardenerInfo = rc.senseRobotAtLocation(selectedGardenerLoc);
				}
				if (selectedGardenerInfo == null) {
					selectedGardenerLoc = null;
				}
			}
			if (selectedGardenerLoc == null) { // If there is no enemy gardener selected:
				// If there is a nearby enemy gardener, select it
				selectedGardenerInfo = getNearestEnemyGardener();
				if (selectedGardenerInfo != null) {
					selectedGardenerLoc = selectedGardenerInfo.location;
				}
			}
			if (targetLumberjack != null && targetLumberjack.location.distanceTo(myLoc) <= myType.strideRadius + myType.bodyRadius + RobotType.LUMBERJACK.bodyRadius + RobotType.LUMBERJACK.strideRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS) { // If an enemy lumberjack is close enough to hit me:
				if (selectedGardenerLoc != null) { // If an enemy gardener is selected
					// Move so that the lumberjack can't hit me, but towards the correct distance from the gardener
					tryMoveDistFromExcludeCircle(
							selectedGardenerLoc,
							2,
							targetLumberjack.location,
							myType.bodyRadius + RobotType.LUMBERJACK.bodyRadius + RobotType.LUMBERJACK.strideRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS
					);
					// Shoot at the gardener
					if (rc.canFireSingleShot()) {
						rc.fireSingleShot(myLoc.directionTo(selectedGardenerLoc));
					}
				} else {
					// Move so that the lumberjack can't hit me, but towards it
					tryMoveDistFrom(
							targetLumberjack.location,
							myType.bodyRadius + RobotType.LUMBERJACK.bodyRadius + RobotType.LUMBERJACK.strideRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS
					);
					// Shoot at the lumberjack
					if (rc.canFireSingleShot()) {
						rc.fireSingleShot(myLoc.directionTo(targetLumberjack.location));
					}
				}
			} else { // Otherwise:
				if (selectedGardenerLoc != null) { // If an enemy gardener is selected:
					TreeInfo selectedCampingTreeInfo = null;
					if (selectedCampingTreeLoc != null) { // If a camping tree is selected:
						// If the camping tree is dead or too far, forget it
						if (myLoc.distanceTo(selectedCampingTreeLoc) < myType.sensorRadius) {
							selectedCampingTreeInfo = rc.senseTreeAtLocation(selectedCampingTreeLoc);
						}
						if (selectedCampingTreeInfo == null || selectedCampingTreeInfo.location.distanceTo(selectedGardenerLoc) > 3) {
							selectedCampingTreeLoc = null;
						}
					}
					if (selectedCampingTreeLoc == null) { // If no camping tree is selected:
						// If I sense a good camping tree, select it
						TreeInfo[] possibleCampingTrees = rc.senseNearbyTrees(selectedGardenerLoc, 3, enemyTeam);
						if (possibleCampingTrees.length > 0) {
							selectedCampingTreeInfo = possibleCampingTrees[0];
							selectedCampingTreeLoc = selectedCampingTreeInfo.location;
						}
					}
					if (selectedCampingTreeLoc != null) { // If a camping tree is selected:
						RobotInfo targetShooter = getNearestEnemyShooter();
						// Move to the camping tree center
						MapLocation moveToLoc = selectedCampingTreeLoc;
						if (targetShooter != null) {// If an enemy that shoots is near:
							// Offset the target move location by a tiny bit away from the enemy
							moveToLoc.add(targetShooter.location.directionTo(myLoc), GameConstants.BULLET_SPAWN_OFFSET / 2f);
						}
						if (myLoc.distanceTo(moveToLoc) > myType.strideRadius) {
							boolean moved = tryMoveElseLeftRight(myLoc.directionTo(moveToLoc));
							if (!moved) {
								tryMoveElseBack(myLoc.directionTo(moveToLoc));
							}
						} else {
							tryMoveExact(moveToLoc);
						}
					} else {
						// Move next to the gardener
						boolean moved = tryMoveElseLeftRight(myLoc.directionTo(selectedGardenerLoc));
						if (!moved) {
							tryMoveElseBack(myLoc.directionTo(selectedGardenerLoc));
						}
					}
					// Shoot at the gardener
					if (rc.canFireSingleShot()) {
						rc.fireSingleShot(myLoc.directionTo(selectedGardenerLoc));
					}
				} else { // Otherwise:
					MapLocation invasionLoc = queryAttackLocation();
					if (invasionLoc != null) { // If there is a target archon location:
						// Move towards the target archon location
						boolean moved = tryMoveElseLeftRight(myLoc.directionTo(invasionLoc));
						if (!moved) {
							tryMoveElseBack(myLoc.directionTo(invasionLoc));
						}
						if (myLoc.distanceTo(invasionLoc) < myType.bodyRadius * 2) { // If I am at the target archon location:
							// We already know there are no gardeners here
							sendAttackFinished(); // Broadcast the target location clear signal
						}
					} else { // Otherwise
						// TODO: unimplemented
					}
				}
			}
    		            
    	}
    	
        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
