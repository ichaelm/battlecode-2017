package robotcore;

import battlecode.common.*;

public class GardenerBot extends RobotGlobal {

	// Farm geometry constants
	static final float hexFarmRadius = 3f;

	// My state
    private enum FarmingMode {SEARCHING, FARMING};
    static int birthTurn = -1;
    static FarmingMode mode = FarmingMode.SEARCHING;
	static boolean goBack = false;
	static int myFarmNum = -1;
	static boolean goingToFarm = false;

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
                Clock.yield();
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
		for (int t = 0; t < 5; t++) {
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
    	return rc.onTheMap(myLoc, hexFarmRadius);
	}

	private static boolean proposedFarmBuildClear(ProposedFarm farm) throws GameActionException {
		rc.setIndicatorDot(farm.getConstructionZone(), 255, 255, 255);
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
    	ProposedFarm farm = proposeRandomFarmHere();
    	boolean buildClear = proposedFarmBuildClear(farm);
    	if (buildClear) {
    		return farm;
		} else {
    		return null;
		}
	}

    public static void turn() throws GameActionException {
    	VP();
		processNearbyRobots();
		processNearbyBullets();
		processNearbyTrees();
		tryToShake();

		rc.broadcast(GARDENER_COUNTER_CHANNEL, rc.readBroadcast(GARDENER_COUNTER_CHANNEL) + 1);
    	
    	if (birthTurn < 0) {
            birthTurn = roundNum;
        }

        RobotType currentBuildOrder = peekBuildQueue1();

        boolean moved = false;
		float skippedCost = 0;

        // Build


		if (currentBuildOrder != null) {
			Direction buildDir;
			boolean built = false;
			if (farmGeo != null) {
				rc.setIndicatorDot(farmGeo.getBuildLoc(), 55, 55, 55);
				if (rc.hasRobotBuildRequirements(currentBuildOrder) && !rc.isCircleOccupiedExceptByThisRobot(farmGeo.getConstructionZone(), 1)) {
					//System.out.println("Moved: " + moved);
					if (!moved) {
						moved = tryMoveExact(farmGeo.getBuildLoc());
						if (moved) {
							goBack = true;
							if (rc.canBuildRobot(currentBuildOrder, farmGeo.getBuildDirection())) {
								rc.buildRobot(currentBuildOrder, farmGeo.getBuildDirection());
								built = true;
								popBuildQueue1();
							}
						}
					}
				}
			} else {
				if (rc.hasRobotBuildRequirements(currentBuildOrder)) {
					boolean success = tryBuildRobot(currentBuildOrder, randomDirection());
					if (success) {
						popBuildQueue1();
						built = true;
					}
				}
			}
			if (!built) {
				skippedCost += currentBuildOrder.bulletCost;
			}
		}




        if (mode == FarmingMode.SEARCHING) {


			// modes: looking for potential farms, assigned to a farm, working a farm
			// if looking for potential farms
				// go to next potential farm
				// if unblocked
					// assign myself to it
				// else
					// assign lumberjack to it
					// increment current potential farm
			// if assigned to a farm
				// go to that location
				// if not making progress
					// assign lumberjack to it
					// switch to looking for potential farms mode
			// if working a farm
				// if one of the locations is blocked
				// assign a lumberjack to it

			int[] nextExploredFarmArr = exploredFarmsQueue.peek();
			if (nextExploredFarmArr != null) {
				// Found an explored farm to go to
				myFarmNum = nextExploredFarmArr[0];
				mode = FarmingMode.FARMING;
				exploredFarmsQueue.remove();
				activeFarmsQueue.add(nextExploredFarmArr);
				goingToFarm = true;
				FarmTableEntry e = readFarmTableEntry(myFarmNum);
				e.setActive();
				e.registerGardener();
				writeFarmTableEntry(myFarmNum, e);

			} else {
				// exploring farm locs
				MapLocation farmLoc = queryCurrentFarmLoc();
				if (rc.canMove(farmLoc)) {
					rc.move(farmLoc);
				} else {
					moved = tryMoveElseLeftRight(myLoc.directionTo(farmLoc), 20, 5);
					if (!moved) {
						System.out.println("Can't move to explore farms");
					}
				}
			}

        }
        if (mode == FarmingMode.FARMING) {
			if (goingToFarm) {
				System.out.println("Going to farm");
				MapLocation farmLoc = farmNumToLoc(myFarmNum);
				if (rc.canMove(farmLoc)) {
					FarmTableEntry e = readFarmTableEntry(myFarmNum);
					e.setArrived();
					writeFarmTableEntry(myFarmNum, e);
					rc.move(farmLoc);
					goingToFarm = false;

					System.out.println("Got there");
					// transition to planting

				} else {
					moved = tryMoveElseLeftRight(myLoc.directionTo(farmLoc), 20, 5);
					if (!moved) {
						System.out.println("Can't move to my farm");
					}
				}
			}

		}
		/*
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
		*/

		if (mode == FarmingMode.FARMING) {
			if (farmGeo == null) {
				ProposedFarm farm = null;
				int i = 0;
				while (farm == null && i < 10) {
					farm = tryProposeFarm();
					i++;
				}
				if(farm != null) {
					farmGeo = farm;
				} else {
					System.out.println("Failed to build farm");
				}
			}
			if (farmGeo != null) {
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
					boolean couldBuildTree = false;
					boolean haveBullets = teamBullets > GameConstants.BULLET_TREE_COST + skippedCost;
					for (int t = 0; t < 7; t++) {
						boolean isAlive = isTreeAlive[t];
						boolean isBlocked = isTreeBlocked[t];
						if (!isAlive && !isBlocked) {
							// try to plant this one
							couldBuildTree = true;
							MapLocation pLoc = farmGeo.getTreePlantingLocs()[t];

							if (haveBullets && rc.onTheMap(pLoc, 1) && rc.isBuildReady()){ // check location, cooldown, & bullets
								Direction tDir = farmGeo.getTreeDirections()[t];
								System.out.println("Attempting to plant Tree #" + t);

								if (rc.canMove(pLoc) && !moved) {						// check if can move this turn, then do
									moved = tryMoveExact(pLoc);	// Go to plantLoc
									if (moved) {
										goBack = true;

										if (rc.canPlantTree(tDir)) {						// check if can plant
											rc.plantTree(myLoc.directionTo(farmGeo.getTreeLocs()[t])); 	// Success! now account for the new tree
											System.out.println("Tree #" + t + " is planted.");
											isTreeAlive[t] = true;
											builtTree = true;
										} else {
											System.out.println("Couldn't plant tree # " + t + " in treeDir specified.");
										}
									} else {
										System.out.println("Couldn't get to Tree Planting Location #" + t);
									}
								} else {
									System.out.println("Couldn't get to Tree Planting Location #" + t);
								}
							} else {
								System.out.println("Waiting for cooldown...");
							}
							break;
						} else {
							System.out.println("Tree " + t + " is alive or blocked");
						}
					}
					if (!builtTree) {
						skippedCost += GameConstants.BULLET_TREE_COST;
						System.out.println("no tree built");
					}
					if (!couldBuildTree) {
						rc.broadcast(CAN_PLANT_COUNTER_CHANNEL, rc.readBroadcast(CAN_PLANT_COUNTER_CHANNEL) + 1);
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
			} else {
				System.out.println("farmGeo is null");
			}

        }

		/*
		// Update farm table
		boolean isFull;
        if (mode == FarmingMode.FARMING) {
        	isFull = true;
			for (int i = 0; i < 5; i++) {
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
		*/

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
    }

	
}
