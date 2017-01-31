package robotcore;

import battlecode.common.*;

public class GardenerBot extends RobotGlobal {

	// Farm geometry constants
	static final float hexFarmRadius = 3f;

	// My state
    private enum FarmingMode {SEARCHING, FARMING};
    static FarmingMode mode = FarmingMode.SEARCHING;
	static int myFarmNum = -1;
	static boolean goingToFarm = false;

	// Farm geometry
	static ProposedFarm farmGeo;
	static int maxTrees = 5;

    // Farm state
    static boolean[] isTreeAlive = new boolean[maxTrees];
	static boolean[] isTreeBlocked = new boolean[maxTrees]; // A spot is marked as blocked if the block is not  due to one of our non-archon robots

    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                debug_print("Gardener: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
                Clock.yield();
            } catch (Exception e) {
                debug_print("Gardener: exception during turn");
                e.printStackTrace();
            }
        }
    }
    
    // quick and easy method to check if a tree or build location is occupied
    public static boolean spotBlocked(int treeNum) throws GameActionException {
        //debug_dot(farmGeo.getTreeLocs()[treeNum], 0, 55, 255);
        boolean occupied = !rc.onTheMap(farmGeo.getTreeLocs()[treeNum], 1) || rc.isCircleOccupiedExceptByThisRobot(farmGeo.getTreeLocs()[treeNum], 1);
    	return occupied;
    }

    public static void countTrees() throws GameActionException {
		for (int t = 0; t < maxTrees; t++) {
			boolean treeNotExists = false;
			MapLocation l = farmGeo.getTreeLocs()[t];
			TreeInfo info = rc.senseTreeAtLocation(l);
			if (info == null) {
				treeNotExists = true;
			} else if (info.team != rc.getTeam()) {
				//debug_print("Wait... that's not our tree!!! WTF!!!");
				treeNotExists = true;
			}
			// treeNotExists is correct
			if (treeNotExists) {
				//debug_print("Tree #" + t + " was killed...");
				//debug_dot(l, 255, 0, 0);
				isTreeBlocked[t] = spotBlocked(t);
				isTreeAlive[t] = false;
			} else {
				isTreeBlocked[t] = false;
				isTreeAlive[t] = true;
			}
		}
    }

    private static ProposedFarm proposeFarmHere(Direction buildDir) {
    	return new ProposedFarm(myLoc, buildDir);
	}

	private static boolean proposedFarmIsOnMap(ProposedFarm farm) throws GameActionException {
    	return rc.onTheMap(myLoc, hexFarmRadius);
	}

	private static boolean proposedFarmBuildClear(ProposedFarm farm) throws GameActionException {
		//debug_dot(farm.getConstructionZone(), 255, 255, 255);
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
    	return tryProposeFarm(usefulRandomDir());
	}

	private static ProposedFarm tryProposeFarm(Direction buildDir) throws GameActionException {
    	ProposedFarm farm = proposeFarmHere(buildDir);
    	boolean buildClear = proposedFarmBuildClear(farm);
    	if (buildClear) {
    		return farm;
		} else {
    		return null;
		}
	}

    public static void turn() throws GameActionException {
		processNearbyRobots();
		processNearbyBullets();
		if (!queryFirstFarmExists()) { // must happen before processing trees
			sendFirstFarm(myLoc);
			debug_print("Sent first farm");
		}
		processNearbyTrees();
		tryToShake();
		elections();
	
		registerGardener();
		leadIfLeader();

        RobotType currentBuildOrder = peekBuildQueue1();

        boolean moved = false;
		float skippedCost = 0;

        // Build


		if (currentBuildOrder != null) {
			Direction buildDir;
			boolean built = false;
			if (farmGeo != null) {
				buildDir = farmGeo.getBuildDirection();
			} else {
				buildDir = randomDirection();
			}
			if (rc.hasRobotBuildRequirements(currentBuildOrder)) {
				boolean success = tryBuildRobot(currentBuildOrder, buildDir);
				if (success) {
					popBuildQueue1();
					built = true;
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
				makeCurrentFarmLocOnMap();
				MapLocation farmLoc = queryCurrentFarmLoc();
				if (farmLoc != null) {
					moved = tryMoveElseLeftRight(myLoc.directionTo(farmLoc), 20, 5);
					if (!moved) {
						debug_print("Can't move to explore farms");
					}
				}
			}
        }
        if (mode == FarmingMode.FARMING) {
			if (goingToFarm) {
				debug_print("Going to farm");
				MapLocation farmLoc = farmNumToLoc(myFarmNum);
				if (myLoc.distanceTo(farmLoc) < myType.strideRadius) {

					moved = tryMoveExact(farmLoc);
					if (moved) {
						FarmTableEntry e = readFarmTableEntry(myFarmNum);
						e.setArrived();
						writeFarmTableEntry(myFarmNum, e);
						goingToFarm = false;

						debug_print("Got there");
					} else {
						debug_print("This should never happen!!!");
					}
					// transition to planting

				} else {
					moved = tryMoveElseLeftRight(myLoc.directionTo(farmLoc), 20, 5);
					if (!moved) {
						debug_print("Can't move to my farm");
					}
				}
			}

			if (!goingToFarm && farmGeo == null) {
				FarmTableEntry e = readFarmTableEntry(myFarmNum);
				ProposedFarm farm = tryProposeFarm(usefulDirections[e.getBuildDir()]);

				int i = 0;
				while (farm == null && i < 10) {
					farm = tryProposeFarm();
					i++;
				}
				if(farm != null) {
					farmGeo = farm;
				} else {
					debug_print("Failed to build farm");
				}
			}
			if (farmGeo != null) {
				countTrees();
				// Plant a plant if needed

				//farmGeo.debug_drawFarm(rc);

				boolean builtTree = false;
				boolean couldBuildTree = false;
				boolean haveBullets = teamBullets > GameConstants.BULLET_TREE_COST + skippedCost;
				for (int t = 0; t < maxTrees; t++) {
					boolean isAlive = isTreeAlive[t];
					boolean isBlocked = isTreeBlocked[t];
					if (!isAlive && !isBlocked) {
						// try to plant this one
						couldBuildTree = true;
						MapLocation pLoc = farmGeo.getTreePlantingLocs()[t];

						if (haveBullets && rc.onTheMap(pLoc, 1) && rc.isBuildReady()){ // check location, cooldown, & bullets
							Direction tDir = farmGeo.getTreeDirections()[t];
							debug_print("Attempting to plant Tree #" + t);

							if (rc.canPlantTree(tDir)) {						// check if can plant
								rc.plantTree(myLoc.directionTo(farmGeo.getTreeLocs()[t])); 	// Success! now account for the new tree
								debug_print("Tree #" + t + " is planted.");
								isTreeAlive[t] = true;
								builtTree = true;
							} else {
								debug_print("Couldn't plant tree # " + t + " in treeDir specified.");
							}

						} else {
							debug_print("Waiting for cooldown...");
							break;
						}
					} else {
						debug_print("Tree " + t + " is alive or blocked");
					}
				}
				if (!builtTree) {
					skippedCost += GameConstants.BULLET_TREE_COST;
					debug_print("no tree built");
				}
				if (!couldBuildTree) {
					rc.broadcast(CAN_PLANT_COUNTER_CHANNEL, rc.readBroadcast(CAN_PLANT_COUNTER_CHANNEL) + 1);
				}

				// Water the neediest friendly plant
				TreeInfo lowestFriendlyTree = getLowestFriendlyTree();
				if (lowestFriendlyTree != null) {
					if (rc.canWater(lowestFriendlyTree.ID)) {
						rc.water(lowestFriendlyTree.ID);
					}
				}
				//debug_drawFarm();

				// Build a unit if possible
				float so = GameConstants.GENERAL_SPAWN_OFFSET;
				//debug_dot(farmGeo.getConstructionZone(), 55, 55, 55);

				if (currentBuildOrder != null && teamBullets > skippedCost + currentBuildOrder.bulletCost) {
					if (rc.hasRobotBuildRequirements(currentBuildOrder) && !rc.isCircleOccupiedExceptByThisRobot(farmGeo.getConstructionZone(), 1)) {

						if (rc.canBuildRobot(currentBuildOrder, farmGeo.getBuildDirection())){
							rc.buildRobot(currentBuildOrder, farmGeo.getBuildDirection());
						}
					}
				}
				//debug_print("watering");
			} else {
				debug_print("farmGeo is null");
			}

		}

		/*
		// Update farm table
		boolean isFull;
		if (mode == FarmingMode.FARMING) {
			isFull = true;
			for (int i = 0; i < maxTrees; i++) {
				if (!isTreeAlive[i] && !isTreeBlocked[i]) {
					isFull = false;
				} else {
					debug_dot(farmGeo.getTreeLocs()[i], 0, 0, 0);
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

        debug_tick(7);
    }

	
}
