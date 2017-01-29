package robotcore;

import battlecode.common.*;

public class ScoutBot extends RobotGlobal {

	static ScoutMode mode = null;
	static MapLocation selectedGardenerLoc = null;
	static MapLocation selectedCampingTreeLoc = null;
	static Direction goDir;
	static boolean firstTurn = true;

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
				Clock.yield();
            } catch (Exception e) {
                System.out.println("Scout: exception during turn");
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

		registerScout();

		if (firstTurn) {
			goDir = randomDirection();
		}
    	mode = queryScoutMode(mode);
    	if (mode == ScoutMode.COLLECT) {
    		
    		//Set a direction randomly to start
        	if (currentDirection == null) {
        		currentDirection = randomDirection();
        	}
        	
        	//React to enemy nearby
        	RobotInfo nearestEnemy = getNearestEnemy();
        	
        	if (nearestEnemy != null) {
        		//Only shoot 20% of the time, and if we have many bullets
        		if (Math.random() < 0.2 && teamBullets > 310) {
	        		Direction enemyDirection = myLoc.directionTo(nearestEnemy.location);
	                if (rc.canFireSingleShot() && (friendlyFireOn || hasLineOfSight(nearestEnemy.location))) {
	                    rc.fireSingleShot(enemyDirection);
	                }
        		}
        		
        		//Report the enemy on the broadcast
        		
                // Turn the opposite direction to run away
                // currentDirection = turn180(currentDirection);
            }
        	
        	//Check if there's a tree nearby
        	TreeInfo nearestTree = getNearestTree();
        	
        	if (nearestTree != null) {
        		//Move towards nearest tree if not already shaken
        		if (nearestTree.getContainedBullets() > 0) {
        			Direction treeDirection = myLoc.directionTo(nearestTree.location);
        			tryMoveElseLeftRight(treeDirection);
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
					tryMoveElseLeftRightExcludeCircle(
							myLoc.directionTo(selectedGardenerLoc),
							targetLumberjack.location,
							myType.bodyRadius + RobotType.LUMBERJACK.bodyRadius + RobotType.LUMBERJACK.strideRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS
					);
					// Shoot at the gardener
					Direction atEnemy = myLoc.directionTo(selectedGardenerInfo.location);
		            float dist = selectedGardenerInfo.location.distanceTo(myLoc); 
	        	
	        		if (usePentad && rc.canFirePentadShot() && dist < pentadDist) { // if scout shoots, canFire becomes false
	                	rc.firePentadShot(atEnemy);
	                }
	        		else if (useTriad && rc.canFireTriadShot() && dist < triadDist) {
	                	rc.fireTriadShot(atEnemy);
	                }
	        		else if (rc.canFireSingleShot() && (friendlyFireOn || hasLineOfSight(selectedGardenerInfo.location))) {
	            		rc.fireSingleShot(atEnemy);
	            	}	
		            
				} else {
					// Move so that the lumberjack can't hit me, but towards it
					tryMoveDistFrom(
							targetLumberjack.location,
							myType.bodyRadius + RobotType.LUMBERJACK.bodyRadius + RobotType.LUMBERJACK.strideRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS
					);
					// Shoot at the lumberjack
					/*
					if (rc.canFireSingleShot() && (friendlyFireOn || hasLineOfSight(targetLumberjack.location))) {
						rc.fireSingleShot(myLoc.directionTo(targetLumberjack.location));
					}
					*/
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
					Direction atEnemy = myLoc.directionTo(selectedGardenerLoc);
		            float dist = selectedGardenerLoc.distanceTo(myLoc); 
	        	
	        		if (usePentad && rc.canFirePentadShot() && dist < pentadDist) { // if soldier shoots, canFire becomes false
	                	rc.firePentadShot(atEnemy);
	                }
	        		else if (useTriad && rc.canFireTriadShot() && dist < triadDist) {
	                	rc.fireTriadShot(atEnemy);
	                }
	        		else if (rc.canFireSingleShot() && (friendlyFireOn || hasLineOfSight(selectedGardenerLoc))) {
	            		rc.fireSingleShot(atEnemy);
	            	}	
		            
				} else { // Otherwise:
					MapLocation attackLoc = peekAttackLocation();
					if (attackLoc != null) { // If there is a target location:
						// Move towards the target location
						boolean moved = tryMoveElseLeftRight(myLoc.directionTo(attackLoc));
						if (myLoc.distanceTo(attackLoc) < 4) { // If I am at the target location:
							// We already know there are no gardeners here
							// Pop this one and add it to the end, to cycle
							addAttackLocation(popAttackLocation());
						}
					} else { // Otherwise
						boolean moved = tryMoveElseBack(goDir);
						if (!moved) {
							goDir = randomDirection();
						}
					}
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
    }
}
