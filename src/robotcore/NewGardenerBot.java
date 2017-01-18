package robotcore;

import battlecode.common.*;

public class NewGardenerBot extends RobotGlobal {

	// Farm geometry constants
	static final float octEdge = (float) 0.4693;
	static final float octDiag = (float) 0.6132;
	static final float octagonFarmRadius = (float) 3.625;

	// My state
    private enum FarmingMode {SEARCHING, FARMING};
    static Direction goDir = null;
    static int birthTurn = -1;
    static FarmingMode mode = FarmingMode.SEARCHING;
	static boolean goBack = false;
    static boolean builtSecondary = false;
	static int farmTableEntryNum = -1;

	// Farm geometry
	static ProposedFarm farmGeo;

    // Farm state
    static boolean[] isTreeAlive = new boolean[7];
	static boolean[] isTreeBlocked = new boolean[7]; // A spot is marked as blocked if the block is not  due to one of our non-archon robots

    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                System.out.println("Gardener: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
            } catch (Exception e) {
                System.out.println("Gardener: exception during turn");
                e.printStackTrace();
            }
        }
    }
    
    // quick and easy method to check if a tree or build location is occupied
    public static boolean spotBlocked(int treeNum) throws GameActionException {
        rc.setIndicatorDot(farmGeo.getTreeLocs()[treeNum], 0, 55, 255);
        boolean occupied = !rc.onTheMap(farmGeo.getTreeLocs()[treeNum], 1) || rc.isCircleOccupiedExceptByThisRobot(farmGeo.getTreeLocs()[treeNum], 1);
    	return occupied;
    }

    public static void countTrees() throws GameActionException {
		for (int t = 0; t < 7; t++) {
			boolean treeNotExists = false;
			MapLocation l = farmGeo.getTreeLocs()[t];
			TreeInfo info = rc.senseTreeAtLocation(l);
			if (info == null) {
				treeNotExists = true;
			} else if (info.team != rc.getTeam()) {
				//System.out.println("Wait... that's not our tree!!! WTF!!!");
				treeNotExists = true;
			}
			// treeNotExists is correct
			if (treeNotExists) {
				//System.out.println("Tree #" + t + " was killed...");
				rc.setIndicatorDot(l, 255, 0, 0);
				isTreeBlocked[t] = spotBlocked(t);
				isTreeAlive[t] = false;
			} else {
				isTreeBlocked[t] = false;
				isTreeAlive[t] = true;
			}
		}
    }

    private static ProposedFarm proposeRandomFarmHere() {
    	return new ProposedFarm(myLoc, usefulRandomDir());
	}

	private static boolean proposedFarmIsOnMap(ProposedFarm farm) throws GameActionException {
    	return rc.onTheMap(myLoc, octagonFarmRadius);
	}

	private static boolean proposedFarmBuildClear(ProposedFarm farm) throws GameActionException {
    	return !rc.isCircleOccupiedExceptByThisRobot(farm.getConstructionZone(), 1);
	}

	private static boolean[] proposedFarmQueryBlocked(ProposedFarm farm) throws GameActionException {
		MapLocation[] treeLocs = farm.getTreeLocs();
		boolean[] blocked = new boolean[treeLocs.length];
		for (int i = 0; i < treeLocs.length; i++) {
			blocked[i] = !rc.onTheMap(treeLocs[i], 1) || rc.isCircleOccupiedExceptByThisRobot(treeLocs[i], 1);
		}
		return blocked;
	}

	private static int proposedFarmQueryNumBlocked(ProposedFarm farm) throws GameActionException {
		MapLocation[] treeLocs = farm.getTreeLocs();
		int numBlocked = 0;
		for (int i = 0; i < treeLocs.length; i++) {
			if (rc.isCircleOccupiedExceptByThisRobot(treeLocs[i], 1)) {
				numBlocked++;
			}
		}
		return numBlocked;
	}

	private static ProposedFarm tryProposeFarm() throws GameActionException {
		int plantIfNum = (int) Math.max((7 - (Math.ceil(7*roundNum/200))), 1);
		if (roundNum > 200) plantIfNum = 1;
    	ProposedFarm farm = proposeRandomFarmHere();
    	boolean onMap = proposedFarmIsOnMap(farm);
    	boolean buildClear = proposedFarmBuildClear(farm);
    	int blockedNum = proposedFarmQueryNumBlocked(farm);
		boolean enoughClear = (7-blockedNum) >= plantIfNum;
    	if (onMap && buildClear && enoughClear) {
    		return farm;
		} else {
    		return null;
		}
	}

    public static void turn() throws GameActionException {
    	if(teamBullets >= 10000) rc.donate(10000);
        if(rc.getRoundLimit() - rc.getRoundNum() < 2) {
        	//System.out.println("Game is ending! All bullets are being donated.");
        	rc.donate(teamBullets);
        }
    	if (birthTurn < 0) {
            birthTurn = roundNum;
            goDir = randomDirection();
        }

        processNearbyBullets();

        RobotType currentBuildOrder = getGlobalDefaultBuild();

        processNearbyTrees();
        boolean moved = false;


        if (mode == FarmingMode.SEARCHING) {

			RobotType secondaryBuild = peekBuildQueue2();
			if (secondaryBuild == RobotType.TANK) {
			// if secondary build is a Tank, attempt to build upon birth (since trees block it from being constructed later)
				if (rc.getRoundNum() < birthTurn + 50) {
					
					for (Direction dir: usefulDirections) {
						if (rc.canBuildRobot(secondaryBuild, dir)) {
							rc.buildRobot(secondaryBuild, dir);
							builtSecondary = true;
						}
					}
				}
			}
			
			
			// move
			moved = tryMoveElseBack(goDir);
			if (!moved) {
				moved = tryMoveElseBack(goDir);
				if (!moved) {
					goDir = randomDirection();
				}
			}
            
            // transition to planting
            float minFriendlyTreeDist = Float.POSITIVE_INFINITY;
            TreeInfo nearestFriendlyTree = getNearestFriendlyTree();
            if (nearestFriendlyTree != null) {
                minFriendlyTreeDist = myLoc.distanceTo(getNearestFriendlyTree().location);
            }
            MapLocation[] archonLocations = getMyArchonLocations();
            float minArchonDist = minDistBetween(myLoc, archonLocations);
            if (minFriendlyTreeDist > 8 && minArchonDist > 8) {
            	ProposedFarm farm = tryProposeFarm();
            	if(farm != null) {
            		farmGeo = farm;
            		mode = FarmingMode.FARMING;
            		if (!getLateLumberjacks()) {
						addBuildQueue1(RobotType.LUMBERJACK);
					}
				}
            	
            }
        }

		RobotType priorityBuild = peekBuildQueue1();
		RobotType secondaryBuild = peekBuildQueue2();
		float skippedCost = 0;

		if (priorityBuild != null) {
			// Build a unit if possible
			boolean builtPriority = false;
			float so = GameConstants.GENERAL_SPAWN_OFFSET;
			if (farmGeo != null) {
				rc.setIndicatorDot(farmGeo.getBuildLoc(), 55, 55, 55);
				if (rc.hasRobotBuildRequirements(priorityBuild) && !rc.isCircleOccupiedExceptByThisRobot(farmGeo.getConstructionZone(), 1)) {
					//System.out.println("Moved: " + moved);
					if (!moved) {
						moved = tryMoveExact(farmGeo.getBuildLoc());
						if (moved) {
							goBack = true;
							if (rc.canBuildRobot(priorityBuild, farmGeo.getBuildDirection())) {
								rc.buildRobot(priorityBuild, farmGeo.getBuildDirection());
								popBuildQueue1();
								builtPriority = true;
							}
						}
					}
				}
			} else {
				if (rc.hasRobotBuildRequirements(priorityBuild)) {
					boolean success = tryBuildRobot(priorityBuild, randomDirection());
					if (success) {
						popBuildQueue1();
						builtPriority = true;
					}
				}
			}

			if (!builtPriority) {
				skippedCost += priorityBuild.bulletCost;
			}
		}

		if (mode == FarmingMode.FARMING) {

			countTrees();
        	// Plant a plant if needed
        	
        	farmGeo.drawFarm(rc);

            if (goBack) {
                if (!moved){ // if not in center, goback to center
                    moved = tryMoveExact(farmGeo.getFarmCenter());
                    goBack = !moved;
                }
            } else {
            	boolean builtTree = false;
				boolean haveBullets = teamBullets > GameConstants.BULLET_TREE_COST + skippedCost;
				for (int t = 0; t < 7; t++) {
					boolean isAlive = isTreeAlive[t];
					boolean isBlocked = isTreeBlocked[t];
					if (!isAlive && !isBlocked) {
						// try to plant this one
						MapLocation pLoc = farmGeo.getTreePlantingLocs()[t];

						if (haveBullets && rc.onTheMap(pLoc, 1) && rc.isBuildReady()){ // check location, cooldown, & bullets
							Direction tDir = farmGeo.getTreeDirections()[t];
							//System.out.println("Attempting to plant Tree #" + t);

							if (rc.canMove(pLoc) && !moved) {						// check if can move this turn, then do
								moved = tryMoveExact(pLoc);	// Go to plantLoc
								if (moved) {
									goBack = true;

									if (rc.canPlantTree(tDir)) {						// check if can plant
										rc.plantTree(myLoc.directionTo(farmGeo.getTreeLocs()[t])); 	// Success! now account for the new tree
										//System.out.println("Tree #" + t + " is planted.");
										isTreeAlive[t] = true;
										builtTree = true;
									} else {
										//System.out.println("Couldn't plant tree # " + t + " in treeDir specified.");
									}
								} else {
									//System.out.println("Couldn't get to Tree Planting Location #" + t);
								}
							} else {
								//System.out.println("Couldn't get to Tree Planting Location #" + t);
							}
						}
						//System.out.println("Waiting for cooldown...");
						break;
					}
				}
				if (!builtTree) {
					skippedCost += GameConstants.BULLET_TREE_COST;
				}
        	}
        	if (secondaryBuild != null && teamBullets > skippedCost + secondaryBuild.bulletCost) {
				// Build a unit if possible
				float so = GameConstants.GENERAL_SPAWN_OFFSET;
				rc.setIndicatorDot(farmGeo.getConstructionZone(), 55, 55, 55);
				boolean builtSecondary = false;
				if (rc.hasRobotBuildRequirements(secondaryBuild) && !rc.isCircleOccupiedExceptByThisRobot(farmGeo.getConstructionZone(), 1)) {
					//System.out.println("Moved: " + moved);
					if (!moved) {
						moved = tryMoveExact(farmGeo.getBuildLoc());
						if (moved) {
							goBack = true;
							if (rc.canBuildRobot(secondaryBuild, farmGeo.getBuildDirection())) {
								rc.buildRobot(secondaryBuild, farmGeo.getBuildDirection());
								popBuildQueue2();
								builtSecondary = true;
							}
						}
					}
				}
				if (!builtSecondary) {
					skippedCost += secondaryBuild.bulletCost;
				}
			}

        	// Water the neediest friendly plant
        	TreeInfo lowestFriendlyTree = getLowestFriendlyTree();
        	if (lowestFriendlyTree != null) {
        		if (rc.canWater(lowestFriendlyTree.ID)) {
        			rc.water(lowestFriendlyTree.ID);
        		}
        	}
        	//drawFarm();

        	// Build a unit if possible
        	float so = GameConstants.GENERAL_SPAWN_OFFSET;
        	rc.setIndicatorDot(farmGeo.getConstructionZone(), 55, 55, 55);

            if (goBack) {
                if (!moved) {
                    moved = tryMoveExact(farmGeo.getFarmCenter());
                    goBack = false;
                }
            } else if (currentBuildOrder != null && teamBullets > skippedCost + currentBuildOrder.bulletCost) {
                if (rc.hasRobotBuildRequirements(currentBuildOrder) && !rc.isCircleOccupiedExceptByThisRobot(farmGeo.getConstructionZone(), 1)) {
                    //System.out.println("Moved: " + moved);
                    if (!moved) {
                        moved = tryMoveExact(farmGeo.getBuildLoc());
                        if (rc.canBuildRobot(currentBuildOrder, farmGeo.getBuildDirection())){
                            rc.buildRobot(currentBuildOrder, farmGeo.getBuildDirection());
                        }
                        goBack = true;
                    }
                }
            }
        	//System.out.println("watering");
        }


		// Update farm table
		boolean isFull;
        if (mode == FarmingMode.FARMING) {
        	isFull = true;
			for (int i = 0; i < 7; i++) {
				if (!isTreeAlive[i] && !isTreeBlocked[i]) {
					isFull = false;
				} else {
					rc.setIndicatorDot(farmGeo.getTreeLocs()[i], 0, 0, 0);
				}
			}
		} else {
        	isFull = false;
		}

		if (farmTableEntryNum < 0) {
			// Create farm table entry
			farmTableEntryNum = createFarmTableEntry();
		} else {
			writeFarmTableEntry(farmTableEntryNum, myLoc, true, false, isFull);
		}

		processNearbyRobots();

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

        debugTick(7);
        Clock.yield();
    }

	
}
