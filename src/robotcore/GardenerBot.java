package robotcore;

import battlecode.common.*;

public class GardenerBot extends RobotGlobal {

    private enum FarmingMode {SEARCHING, PLANTING, WATERING};

    static int farmTableEntryNum = -1;
    static Direction goDir = null;
    static int birthTurn = -1;
    static MapLocation birthLocation = null;
    static Direction buildDirection = null;
    static FarmingMode mode = FarmingMode.SEARCHING;
    static Direction firstMove = Direction.getEast();
    static MapLocation farmCenter = null;
    static MapLocation buildLoc = null;
    static float octEdge = (float) 0.4693;
    static float octDiag = (float) 0.6132;
    static float octagonFarmRadius = (float) 3.625;
    //static int plantStartTurn = 0;
    static int numPlanted = 0;
    static boolean amPlanting = false;
    static boolean[] isPlanted = new boolean[7];
    static MapLocation[] treeLocs = new MapLocation[7];
    static MapLocation[] treePlantingLocs = new MapLocation[7];
    static boolean goBack = false;
    
    static Direction[] treeDirections = new Direction[7];
    
    // Sets up everything needed to plant a farm and maintain it etc.
    public static void getTreeLocations(Direction missingTreeDir) {
    	buildDirection = missingTreeDir;
    	firstMove = missingTreeDir.rotateLeftDegrees(45);
    	
    	if (farmCenter == null) farmCenter = myLoc;
    	else if (myLoc.distanceTo(farmCenter) > 1) farmCenter = myLoc;
    	
    	Direction treeDir = firstMove;
    	Direction moveDir = treeDir.rotateLeftDegrees((float) 67.5);
    	
    	buildLoc = farmCenter.add(missingTreeDir, octDiag);
    	treePlantingLocs[0] = farmCenter.add(firstMove, octDiag);
    	treeLocs[0] = farmCenter.add(firstMove, octDiag + 2);
    	treeDirections[0] = treeDir;
    	
    	MapLocation curLoc = treePlantingLocs[0];
    	for(int T = 1; T < 7; T++) {
    		curLoc = treePlantingLocs[T-1];
    		treeDir = firstMove.rotateLeftDegrees(45*T);
        	moveDir = treeDir.rotateLeftDegrees((float) 67.5);
    		
    		treePlantingLocs[T] = curLoc.add(moveDir, octEdge);
    		treeLocs[T] = treePlantingLocs[T].add(treeDir, 2);
    		treeDirections[T] = treeDir;
    	}

    }
    
    public static void drawFarm() {
    	try{
    		for (MapLocation r: treePlantingLocs) {
    			if (rc.onTheMap(r)) rc.setIndicatorDot(r, 255, 255, 0);
    		}
    		for (MapLocation t: treeLocs) {
    			if (rc.onTheMap(t)) rc.setIndicatorDot(t, 0, 255, 0);
    		}
    		//if (rc.onTheMap(farmCenter)) rc.setIndicatorDot(farmCenter, 0, 0, 255);

    	} catch (Exception e) {
    		System.out.println("Exception during Dot Drawing");
    		e.printStackTrace();
    	}
    }

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
    
    public static void clk() throws GameActionException {
        /*
    	if (Clock.getBytecodeNum() > 2000) System.out.println("\t\tBytecodes: " + Clock.getBytecodeNum());
    	if (Clock.getBytecodeNum() > 8500) {
    		System.out.println("\n\t\tWARNING\n");
    		
    		 //Visual warning of bytecode limit
    		for(int i = 0; i < 20; i++) {
    			
    				Direction rd = randomDirection();
        			MapLocation lEnd = myLoc.add(rd, 3); 
				
					rc.setIndicatorLine(myLoc, lEnd, (int) (255*Math.random()), (int) (255*Math.random()), (int) (255*Math.random()));	
    		}
    		
    		
    	}
    	*/
    }
    
    public static void countTrees() {
    	try {
    		int prev = numPlanted;
    		TreeInfo[] inFarm = rc.senseNearbyTrees(myLoc, octagonFarmRadius, rc.getTeam());
    		numPlanted = inFarm.length;

    		if (prev < numPlanted) {
    			//System.out.println("A tree has magically appeared!");
    		}
    		
    		boolean anyKilled = false;

    		for (int t = 0; t < 7; t++) {
    			boolean killed = false;
    			MapLocation l = treeLocs[t];
    			TreeInfo info = rc.senseTreeAtLocation(l);
    			if (info == null) {
    				killed = true;
    			} else if (info.team != rc.getTeam()) {
    				//System.out.println("Wait... that's not our tree!!! WTF!!!");
    				killed = true;
    			}
    			if (killed) {
    				//System.out.println("Tree #" + t + " was killed...");
    				isPlanted[t] = false;
    				rc.setIndicatorDot(l, 255, 0, 0);
    			}
    			
    			
    			if (killed) anyKilled = true;

    		}
    		
    		if (anyKilled) {
    			//System.out.println("Attempting to replant...");
    			mode = FarmingMode.PLANTING;
    		}

    		
    	} catch (GameActionException e) {
    		System.out.println("countTrees Exception");
    		e.printStackTrace();
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
            birthLocation = myLoc;
        }

        processNearbyBullets();

        Direction buildDir = randomDirection();

        RobotType currentBuildOrder = getBuildOrder();

        Direction toBirthLocation = myLoc.directionTo(birthLocation);
        if (toBirthLocation == null) {
            toBirthLocation = randomDirection();
        }

        processNearbyTrees();
        boolean moved = false;
        if (mode == FarmingMode.SEARCHING) {
            // move
        	amPlanting = false;
            moved = tryMoveElseBack(goDir);
            if (!moved) {
                moved = tryMoveElseBack(goDir);
                if (!moved) {
                    goDir = randomDirection();
                }
            }
            
            if (rc.getRoundNum() > 70 && Math.random() < .05) {	// if 70 rounds pass before farm is planted, attmpt to build lumberjack to assist.
            	Direction rd = randomDirection();
            	//System.out.println("Trying to build Lumberjacks...");
            	if(rc.canBuildRobot(RobotType.LUMBERJACK, rd)) rc.buildRobot(RobotType.LUMBERJACK, rd);
            }
            
            
            // transition to planting
            float minFriendlyTreeDist = Float.POSITIVE_INFINITY;
            TreeInfo nearestFriendlyTree = getNearestFriendlyTree();
            if (nearestFriendlyTree != null) {
                minFriendlyTreeDist = myLoc.distanceTo(getNearestFriendlyTree().location);
            }
            MapLocation[] archonLocations = getMyArchonLocations();
            float minArchonDist = minDistBetween(myLoc, archonLocations);
            if (minFriendlyTreeDist > 10 && minArchonDist > 6) {
                if (rc.onTheMap(myLoc, octagonFarmRadius) 
                		&& (rc.getRoundNum() > 80 || !rc.isCircleOccupiedExceptByThisRobot(myLoc, octagonFarmRadius))) {
                	mode = FarmingMode.PLANTING;
                }
            }
        }

        if (mode == FarmingMode.PLANTING) {
        	// Plant a plant if needed
        	if (rc.isCircleOccupiedExceptByThisRobot(myLoc, octagonFarmRadius) && numPlanted == 0) {
        		//System.out.println("Octagon Farm may not be complete! No space!");
        	}
        	if (buildDirection == null)  buildDirection = enemyInitialArchonLocations[0].directionTo(myLoc).opposite();
        	
        	getTreeLocations(buildDirection); // can change missing tree direction later
        	buildDir = firstMove;
        
        	drawFarm();

        	boolean haveBullets = rc.hasTreeBuildRequirements();

        	if (!amPlanting) {
        		//System.out.println("Begin planting!");
        		amPlanting = true;
        	} else if (goBack) {
        		if (!moved){ // if not in center, goback to center
        			moved = tryMoveExact(farmCenter);
        			goBack = !moved;
        		}
        	} else {
        		int nextToPlant = 0;
        		for (boolean b: isPlanted) {
        			if(!b) break;
        			nextToPlant ++;
        		}

        		int t = nextToPlant;
        		if (nextToPlant > 6) { // when done planting, set to watering mode
        			mode = FarmingMode.WATERING;
        		}
        		else { 															// if not finished planting trees...
        			MapLocation pLoc = treePlantingLocs[t];
        			
        			if (haveBullets && rc.onTheMap(pLoc) && rc.isBuildReady()){ // check location, cooldown, & bullets
        				Direction tDir = treeDirections[t];
        				//System.out.println("Attempting to plant Tree #" + t);
        				
        				if (rc.canMove(pLoc) && !moved) {						// check if can move this turn, then do
        					rc.move(pLoc);	// Go to plantLoc
        					goBack = true; 
        					
        					if (rc.canPlantTree(tDir)) {						// check if can plant
        						rc.plantTree(myLoc.directionTo(treeLocs[t])); 	// Success! now account for the new tree
        						//System.out.println("Tree #" + t + " is planted.");
        						numPlanted++;
        						isPlanted[t] = true;
        					} else {
        						//System.out.println("Couldn't plant tree # " + t + " in treeDir specified.");
        					}
        				} else {
        					//System.out.println("Couldn't get to Tree Planting Location #" + t);
        				}
        			}
        			//System.out.println("Waiting for cooldown...");
        		}
        		
        	}

        }

        if (mode == FarmingMode.PLANTING || mode == FarmingMode.WATERING) {
        	// Water the neediest friendly plant
        	TreeInfo lowestFriendlyTree = getLowestFriendlyTree();
        	if (lowestFriendlyTree != null) {
        		if (rc.canWater(lowestFriendlyTree.ID)) {
        			rc.water(lowestFriendlyTree.ID);
        		}
        	}
        	//drawFarm();
        }

        if (mode == FarmingMode.WATERING) {

        	// Build a unit if possible
        	float so = GameConstants.GENERAL_SPAWN_OFFSET;
        	MapLocation constructionZone = farmCenter.add(buildDirection, octDiag + 2 + so);
        	rc.setIndicatorDot(constructionZone, 55, 55, 55);
        	clk();
        	if (rc.hasRobotBuildRequirements(currentBuildOrder) && !rc.isCircleOccupiedExceptByThisRobot(constructionZone, 1)) {
        		//System.out.println("Moved: " + moved);
        		if (!moved) {
        			moved = tryMoveExact(buildLoc);
        			if (rc.canBuildRobot(currentBuildOrder, buildDirection)){
        				rc.buildRobot(currentBuildOrder, buildDirection);
        			}
        			goBack = true;
        		}
        	}
        	
        	if (goBack) {
        		if (!moved) {
        			moved = tryMoveExact(farmCenter);
        			goBack = false;
        		}
        		
        	}
        	
        	clk();
        	countTrees();
        	//System.out.println("watering");
        	
        }

        // Update farm table

        if (farmTableEntryNum < 0) {
            // Create farm table entry
            farmTableEntryNum = createFarmTableEntry();
        } else {
            writeFarmTableEntry(farmTableEntryNum, myLoc, true, false);
        }
        
        Clock.yield();
    }
}
