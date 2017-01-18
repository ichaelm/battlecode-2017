package robotcore;

import battlecode.common.*;

/*
Gardener priorities:
 3. Primary build queue (scout, each g adds lumberjack)
 4. Trees
 6. Secondary build queue
 7. Global default build
 */

public strictfp class RobotGlobal {

    public enum GardenerSchedule {ONCE_EVERY_N_ROUNDS, WHEN_FULL};

    public enum ScoutMode {HARASS, COLLECT};

    public static RobotController rc;

    // Channel constants
    public static final int ARCHON_COUNTER_CHANNEL = 0;
    public static final int NUM_ARCHONS_CHANNEL = ARCHON_COUNTER_CHANNEL + 1;
    public static final int ARCHON_LOCATION_TABLE_CHANNEL = NUM_ARCHONS_CHANNEL + 1;
    public static final int ARCHON_LOCATION_TABLE_ENTRY_SIZE = 2;
    public static final int ARCHON_LOCATION_TABLE_NUM_ENTRIES = 3;
    public static final int ARCHON_LOCATION_TABLE_LENGTH = ARCHON_LOCATION_TABLE_ENTRY_SIZE * ARCHON_LOCATION_TABLE_NUM_ENTRIES;
    public static final int FARM_TABLE_CHANNEL = ARCHON_LOCATION_TABLE_CHANNEL + ARCHON_LOCATION_TABLE_LENGTH;
    public static final int FARM_TABLE_ENTRY_SIZE = 3;
    public static final int FARM_TABLE_NUM_ENTRIES = 30;
    public static final int FARM_TABLE_LENGTH = FARM_TABLE_ENTRY_SIZE * FARM_TABLE_NUM_ENTRIES;
    public static final int FARM_TABLE_COUNT_CHANNEL = FARM_TABLE_CHANNEL + FARM_TABLE_LENGTH;
    public static final int BOUNDS_TABLE_CHANNEL = FARM_TABLE_COUNT_CHANNEL + 1; // IN_W, IN_E, IN_S, IN_N, OUT_W, OUT_E, OUT_S, OUT_N
    public static final int BOUNDS_TABLE_LENGTH = 8;
    public static final int LJ_JOBS_TABLE_CHANNEL = BOUNDS_TABLE_CHANNEL + BOUNDS_TABLE_LENGTH;
    public static final int LJ_JOBS_TABLE_ENTRY_SIZE = 3;
    public static final int LJ_JOBS_TABLE_NUM_ENTRIES = 30;
    public static final int LJ_JOBS_TABLE_LENGTH = LJ_JOBS_TABLE_ENTRY_SIZE * LJ_JOBS_TABLE_NUM_ENTRIES;
    public static final int LJ_JOBS_TABLE_BEGIN_CHANNEL = LJ_JOBS_TABLE_CHANNEL + LJ_JOBS_TABLE_LENGTH;
    public static final int LJ_JOBS_TABLE_COUNT_CHANNEL = LJ_JOBS_TABLE_BEGIN_CHANNEL + 1;
    public static final int BUILD_QUEUE_1_CHANNEL = LJ_JOBS_TABLE_COUNT_CHANNEL + 1;
    public static final int BUILD_QUEUE_1_LENGTH = 100;
    public static final int BUILD_QUEUE_1_BEGIN_CHANNEL = BUILD_QUEUE_1_CHANNEL + BUILD_QUEUE_1_LENGTH;
    public static final int BUILD_QUEUE_1_COUNT_CHANNEL = BUILD_QUEUE_1_BEGIN_CHANNEL + 1;
    public static final int BUILD_QUEUE_2_CHANNEL = BUILD_QUEUE_1_COUNT_CHANNEL + 1;
    public static final int BUILD_QUEUE_2_LENGTH = 100;
    public static final int BUILD_QUEUE_2_BEGIN_CHANNEL = BUILD_QUEUE_2_CHANNEL + BUILD_QUEUE_2_LENGTH;
    public static final int BUILD_QUEUE_2_COUNT_CHANNEL = BUILD_QUEUE_2_BEGIN_CHANNEL + 1;
    public static final int GLOBAL_DEFAULT_BUILD_CHANNEL = BUILD_QUEUE_2_COUNT_CHANNEL + 1;
    public static final int NUM_GARDENERS_BUILT_CHANNEL = GLOBAL_DEFAULT_BUILD_CHANNEL + 1;
    public static final int WHICH_ARCHON_MAKES_GARDENERS_CHANNEL = NUM_GARDENERS_BUILT_CHANNEL + 1;
    public static final int ATTACK_LOCATION_X_CHANNEL = WHICH_ARCHON_MAKES_GARDENERS_CHANNEL + 1;
    public static final int ATTACK_LOCATION_Y_CHANNEL = ATTACK_LOCATION_X_CHANNEL + 1;
    public static final int ATTACK_LOCATION_EXISTS_CHANNEL = ATTACK_LOCATION_Y_CHANNEL + 1;
    public static final int ATTACK_FINISHED_CHANNEL = ATTACK_LOCATION_EXISTS_CHANNEL + 1;
    public static final int ATTACK_LOCATION_NUM_CHANNEL = ATTACK_FINISHED_CHANNEL + 1;
    public static final int NEW_SCOUT_MODE_CHANNEL = ATTACK_LOCATION_NUM_CHANNEL + 1;
    public static final int OVERRIDE_SCOUT_MODE_CHANNEL = NEW_SCOUT_MODE_CHANNEL + 1;

    // Performance constants
    public static final int DESIRED_ROBOTS = 20;
    public static final int DESIRED_TREES = 20;
    public static final int DESIRED_BULLETS = 20;
    
    // Scout variables
    public static Direction currentDirection;
    
    // Start of game info
    public static Team myTeam;
    public static Team enemyTeam;
    public static RobotType myType;
    public static int myID;
    public static MapLocation[] myInitialArchonLocations;
    public static MapLocation[] enemyInitialArchonLocations;
    public static int roundLimit;
    public static long[] teamMemory;

    // Round by round info
    public static MapLocation myLoc;
    public static float myHealth;
    public static int robotCount;
    public static int roundNum;
    public static int victoryPoints;
    public static int treeCount;
    public static float teamBullets;
    public static RobotInfo[] nearbyRobots;
    public static float nearbyRobotRadius;
    public static TreeInfo[] nearbyTrees;
    public static float nearbyTreeRadius;
    public static BulletInfo[] nearbyBullets;
    public static float nearbyBulletRadius;
    public static boolean neverUpdated;
    public static MapBounds knownMapBounds;

    // Results of further processing
    private static RobotInfo nearestEnemy = null;
    private static RobotInfo nearestEnemyLumberjack = null;
    private static RobotInfo nearestEnemyShooter = null;
    private static RobotInfo nearestEnemyGardener = null;
    private static TreeInfo nearestTree = null;
    private static TreeInfo nearestRobotTree = null;
    private static TreeInfo nearestFriendlyTree = null;
    private static TreeInfo nearestUnfriendlyTree = null;
    private static TreeInfo lowestFriendlyTree = null;
    private static BulletInfo[] bulletsToAvoid = new BulletInfo[0];
    private static int numBulletsToAvoid = 0;

    // Special stored values
    private static boolean circleClockwise = true;
    private static int[] debugBytecodesList = new int[100];
    private static int numDebugBytecodes = 0;
    private static boolean debugTripped = false;
    private static GardenerSchedule gardenerSchedule = GardenerSchedule.ONCE_EVERY_N_ROUNDS;
    private static int gardenerScheduleN = -1;
    private static boolean experimental = false;
    private static RobotType[] initialBuildQueue1 = new RobotType[0];
    private static RobotType[] initialBuildQueue2 = new RobotType[0];
    private static RobotType initialDefaultBuild = null;

    // Configuration for Offensive Units
    public static boolean useTriad = false;
    public static boolean usePentad = false;
    public static float triadDist = 3.0f;
    public static float pentadDist = 2.5f;
    public static boolean friendlyFireOn = true;
    public static boolean prioritizeRobotTrees = false;
    public static boolean kiteEnemyLumberjacks = true;
    public static float avoidRadius = 1.1f;
    public static boolean kite = false;

    public static void init(RobotController rc) throws GameActionException {
        RobotGlobal.rc = rc;
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        myType = rc.getType();
        myID = rc.getID();
        myInitialArchonLocations = rc.getInitialArchonLocations(rc.getTeam());
        enemyInitialArchonLocations = rc.getInitialArchonLocations(rc.getTeam().opponent());
        roundLimit = rc.getRoundLimit();
        teamMemory = rc.getTeamMemory();
        nearbyRobotRadius = myType.sensorRadius;
        nearbyTreeRadius = myType.sensorRadius;
        nearbyBulletRadius = myType.bulletSightRadius;
        neverUpdated = true;
    }

    public static void update() throws GameActionException {
        myLoc = rc.getLocation();
        myHealth = rc.getHealth();
        robotCount = rc.getRobotCount();
        int newRoundNum = rc.getRoundNum();
        if (!neverUpdated && newRoundNum == roundNum) {
            System.out.println("Restarted the same turn!");
        } else if (!neverUpdated && newRoundNum - roundNum != 1) {
            System.out.println("Skipped a turn!");
        }
        roundNum = newRoundNum;
        victoryPoints = rc.getTeamVictoryPoints();
        treeCount = rc.getTreeCount();
        teamBullets = rc.getTeamBullets();

        updateNearbyRobots();
        updateNearbyTrees();
        updateNearbyBullets();

        knownMapBounds = getMapBounds();
        updateMapBounds(knownMapBounds);

        numDebugBytecodes = 0;
        debugTripped = false;

        neverUpdated = false;
    }

    private static void updateNearbyRobots() throws GameActionException {
        nearbyRobots = rc.senseNearbyRobots(nearbyRobotRadius);
        int numRobots = nearbyRobots.length;
        if (numRobots > DESIRED_ROBOTS) {
            nearbyRobotRadius--;
        } else if (nearbyRobotRadius < myType.sensorRadius - 0.001 && numRobots < (int)(DESIRED_ROBOTS * 0.9)) {
            nearbyRobotRadius++;
        }
    }

    private static void updateNearbyTrees() throws GameActionException {
        nearbyTrees = rc.senseNearbyTrees(nearbyTreeRadius);
        int numTrees = nearbyTrees.length;
        if (numTrees > DESIRED_TREES) {
            nearbyTreeRadius--;
        } else if (nearbyTreeRadius < myType.sensorRadius - 0.001 && numTrees < (int)(DESIRED_TREES * 0.9)) {
            nearbyTreeRadius++;
        }
    }

    private static void updateNearbyBullets() throws GameActionException {
        nearbyBullets = rc.senseNearbyBullets(nearbyBulletRadius);
        int numBullets = nearbyBullets.length;
        if (numBullets > DESIRED_BULLETS) {
            nearbyBulletRadius--;
        } else if (nearbyBulletRadius < myType.bulletSightRadius - 0.001 && numBullets < (int)(DESIRED_BULLETS * 0.9)) {
            nearbyBulletRadius++;
        }
    }

    public static void processNearbyRobots() throws GameActionException {
        nearestEnemy = null;
        float minEnemyDist = 99999999;
        nearestEnemyLumberjack = null;
        float minEnemyLumberjackDist = 99999999;
        nearestEnemyShooter = null;
        float minEnemyShooterDist = 99999999;
        nearestEnemyGardener = null;
        float minEnemyGardenerDist = 99999999;
        int numIters = Math.min(nearbyRobots.length, DESIRED_ROBOTS);
        for (int i = 0; i < numIters; i++) {
            RobotInfo robot = nearbyRobots[i];
            if (robot.getTeam() == enemyTeam) {
                float dist = myLoc.distanceTo(robot.getLocation()) - robot.getRadius();
                if (dist < minEnemyDist) {
                    nearestEnemy = robot;
                    minEnemyDist = dist;
                }
                if (robot.type == RobotType.LUMBERJACK) {
                    if (dist < minEnemyLumberjackDist) {
                        nearestEnemyLumberjack = robot;
                        minEnemyLumberjackDist = dist;
                    }
                } else if (robot.type == RobotType.SOLDIER || robot.type == RobotType.TANK || robot.type == RobotType.SCOUT) {
                    if (dist < minEnemyShooterDist) {
                        nearestEnemyShooter = robot;
                        minEnemyShooterDist = dist;
                    }
                } else if (robot.type == RobotType.GARDENER) {
                    if (dist < minEnemyGardenerDist) {
                        nearestEnemyGardener = robot;
                        minEnemyGardenerDist = dist;
                    }
                }
            }
        }
    }

    public static void processNearbyTrees() throws GameActionException {
        float minDist = Float.POSITIVE_INFINITY;
        float minRobotTreeDist = Float.POSITIVE_INFINITY;
        float minFriendlyDist = Float.POSITIVE_INFINITY;
        float minUnfriendlyDist = Float.POSITIVE_INFINITY;
        float minFriendlyHealth = Float.POSITIVE_INFINITY;
        nearestTree = null;
        nearestRobotTree = null;
        nearestFriendlyTree = null;
        nearestUnfriendlyTree = null;
        lowestFriendlyTree = null;
        int numIters = Math.min(nearbyTrees.length, DESIRED_TREES);
        for (int i = 0; i < numIters; i++) {
            TreeInfo tree = nearbyTrees[i];
            float dist = myLoc.distanceTo(tree.getLocation()) - tree.radius;
            if (dist < minDist) {
                nearestTree = tree;
                minDist = dist;
            }
            if (tree.team == myTeam) {
                if (dist < minFriendlyDist) {
                    nearestFriendlyTree = tree;
                    minFriendlyDist = dist;
                }
                if (tree.health < minFriendlyHealth && tree.location.distanceTo(myLoc) <= 3) {
                    lowestFriendlyTree = tree;
                    minFriendlyHealth = tree.health;
                }
            } else {
                if (dist < minUnfriendlyDist) {
                    nearestUnfriendlyTree = tree;
                    minUnfriendlyDist = dist;
                    if (tree.containedRobot != null) {
                    	nearestRobotTree = tree;
                    	minRobotTreeDist = dist;
                    }
                }
            }
        }
    }

    public static void processNearbyBullets() throws GameActionException {
        int numIters = Math.min(nearbyBullets.length, DESIRED_BULLETS);
        bulletsToAvoid = new BulletInfo[numIters];
        numBulletsToAvoid = 0;
        for (int i = 0; i < numIters; i++) {
        	BulletInfo bullet = nearbyBullets[i];
        	if (willCollideWith(bullet, myLoc, myType.bodyRadius + myType.strideRadius)) {
        		bulletsToAvoid[numBulletsToAvoid] = bullet;
        		numBulletsToAvoid++;
        	}
        }
    }

    public static void tryToShake() throws GameActionException {
    	float radius = 1 + myType.bodyRadius;
    	if (nearbyTrees.length > 0) {
    		int tryMax = 3;
    		int tried = 0;
    		for (TreeInfo t: nearbyTrees) {
    			if (t.containedBullets < 1) continue;
    			if (++tried >= tryMax) break;
    			if (rc.canShake(t.ID)) rc.shake(t.ID);
    			if (t.location.distanceTo(myLoc) > radius) break;
    		}
    	}
    }
    
    public static boolean kiteEnemy(RobotInfo enemy, float strafeDist) throws GameActionException {
    	debugTick(1);
    	boolean moved = rc.hasMoved();
    	if (moved) {
    		System.out.println("Already moved!");
    		return moved;
    	} 
    	if (enemy.type == RobotType.LUMBERJACK){ // ensure we avoid lumberjack strikes
    		strafeDist = Math.min(1.5f, strafeDist); 
    	}
    	if (enemy.type == RobotType.ARCHON || enemy.type == RobotType.GARDENER){ // dont kite gardeners or archons
    		return false; 
    	}
    	debugTick(2);
    	float exclude = strafeDist + myType.bodyRadius + enemy.type.bodyRadius;
    	float curDist = myLoc.distanceTo(enemy.location);
    	Direction toEnemy = myLoc.directionTo(enemy.location);
    	float stride = myType.strideRadius;

    	float difference = Math.abs(curDist - exclude);

    	if (stride < difference) { // if I cannot move beyond the border line...
    		if (curDist > exclude) { // if already outside exclusion zone, strafe to the side
    			debugTick(3);
    			if (rc.canMove(toEnemy.rotateLeftDegrees(90), stride)) { // try Left
    				moved = tryMoveElseLeftRight(toEnemy.rotateLeftDegrees(90), stride);
    			}
    			else if (rc.canMove(toEnemy.rotateRightDegrees(90), stride)) { // try Right
    				moved = tryMoveElseLeftRight(toEnemy.rotateRightDegrees(90), stride);
    			}
    			else {
    				return false;
    			}

    		}
    		else { // if inside exclusion zone
    			debugTick(4);
    			// simply move out.
    			moved = tryMoveElseLeftRight(toEnemy.opposite(), stride);
    		}
    	}
    	else { // if I can reach beyond the border line...
    		if (curDist > exclude) { // if already outside exclusion zone
    			debugTick(5);
    			if (rc.canMove(toEnemy.rotateLeftDegrees(90), stride)) { // try Left
    				moved = tryMoveElseLeftRight(toEnemy.rotateLeftDegrees(90), stride);
    			}
    			else if (rc.canMove(toEnemy.rotateRightDegrees(90), stride)) { // try Right
    				moved = tryMoveElseLeftRight(toEnemy.rotateRightDegrees(90), stride);
    			}
    			else {
    				return false;
    			}
    		}
    		else { // if inside exclusion zone
    			debugTick(6);
    			moved = tryMoveElseLeftRight(toEnemy.opposite(), exclude - curDist);
    			if (!moved) {
    				moved = tryMoveElseLeftRight(toEnemy.opposite(), stride);
    			}
    		}
    	}
    	
    	debugTick(7);
    	return moved;
    }

    public static MapLocation[] getMyArchonLocations() throws GameActionException {
        int numArchons = rc.readBroadcast(NUM_ARCHONS_CHANNEL);
        if (numArchons > ARCHON_LOCATION_TABLE_NUM_ENTRIES) {
            System.out.println("More than 3 archons detected!!!");
            numArchons = ARCHON_LOCATION_TABLE_NUM_ENTRIES;
        }
        MapLocation[] archonLocations = new MapLocation[numArchons];
        for (int i = 0; i < numArchons; i++) {
            float x = Float.intBitsToFloat(rc.readBroadcast(ARCHON_LOCATION_TABLE_CHANNEL + (ARCHON_LOCATION_TABLE_ENTRY_SIZE*i)));
            float y = Float.intBitsToFloat(rc.readBroadcast(ARCHON_LOCATION_TABLE_CHANNEL + (ARCHON_LOCATION_TABLE_ENTRY_SIZE*i) + 1));
            archonLocations[i] = new MapLocation(x, y);
        }
        return archonLocations;
    }

    public static float minDistBetween(MapLocation a, MapLocation[] bs) {
        float minDist = Float.POSITIVE_INFINITY;
        for (MapLocation b : bs) {
            float dist = a.distanceTo(b);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    public static RobotInfo getNearestEnemy() {
        return nearestEnemy;
    }

    public static RobotInfo getNearestEnemyLumberjack() {
        return nearestEnemyLumberjack;
    }

    public static RobotInfo getNearestEnemyShooter() {
        return nearestEnemyShooter;
    }

    public static RobotInfo getNearestEnemyGardener() {
        return nearestEnemyGardener;
    }

    public static TreeInfo getNearestTree() {
        return nearestTree;
    }
    
    public static TreeInfo getNearestRobotTree() {
        return nearestRobotTree;
    }

    public static TreeInfo getNearestFriendlyTree() {
        return nearestFriendlyTree;
    }

    public static TreeInfo getNearestUnfriendlyTree() {
        return nearestUnfriendlyTree;
    }

    public static TreeInfo getLowestFriendlyTree() {
        return lowestFriendlyTree;
    }

    public static BulletInfo[] getBulletsToAvoid() {
        return bulletsToAvoid;
    }

    public static boolean tryMoveExact(MapLocation loc) throws GameActionException {
        if (rc.canMove(loc)) {
            rc.move(loc);
            myLoc = loc;
            return true;
        } else {
            MapLocation fudge = loc.add(Direction.getSouth(), 0.001f);
            if (rc.canMove(fudge)) {
                rc.move(fudge);
                myLoc = fudge;
                return true;
            } else {
                fudge = loc.add(Direction.getEast(), 0.001f);
                if (rc.canMove(fudge)) {
                    rc.move(fudge);
                    myLoc = fudge;
                    return true;
                } else {
                    fudge = loc.add(Direction.getNorth(), 0.001f);
                    if (rc.canMove(fudge)) {
                        rc.move(fudge);
                        myLoc = fudge;
                        return true;
                    } else {
                        fudge = loc.add(Direction.getWest(), 0.001f);
                        if (rc.canMove(fudge)) {
                            rc.move(fudge);
                            myLoc = fudge;
                            return true;
                        } else {
                            System.out.println("Failed to move");
                            return false;
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    public static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }
    
    static Direction[] usefulDirections = { // every 16th of a circle
    		new Direction((float) 0.0),
    		new Direction((float) 22.5),
    		new Direction((float) 45.0),
    		new Direction((float) 67.5),
    		new Direction((float) 90.0),
    		new Direction((float) 112.5),
    		new Direction((float) 135.0),
    		new Direction((float) 157.5),
    		new Direction((float) 180.0),
    		new Direction((float) 202.5),
    		new Direction((float) 225.0),
    		new Direction((float) 247.5),
    		new Direction((float) 270.0),
    		new Direction((float) 292.5),
    		new Direction((float) 315.0),
    		new Direction((float) 337.5)
    };
    
    // replacement for RandomDirection that helps to get useful angles
    public static Direction usefulRandomDir() {
    	int nextI = (int) (16*Math.random());
    	return usefulDirections[nextI];
    }
    
    //Given a Direction, it returns the Direction after a 180 degree turn
    public static Direction turn180(Direction currDir) {
    	return new Direction((float) (currDir.radians + Math.PI % (2 * Math.PI)));
    }

    public static boolean tryMoveElseBack(MapLocation loc) throws GameActionException {
        return tryMoveElseBack(myLoc.directionTo(loc), myLoc.distanceTo(loc));
    }

    public static boolean tryMoveElseBack(Direction dir) throws GameActionException {
        return tryMoveElseBack(dir, myType.strideRadius);
    }

    public static boolean tryMoveElseBack(Direction dir, float dist) throws GameActionException {
        float currentStride = dist;
        while (currentStride > 0.1) {
            MapLocation newLoc = myLoc.add(dir, currentStride);
            if (rc.canMove(dir, currentStride)) {
                if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                    rc.move(dir, currentStride);
                    myLoc = newLoc;
                    return true;
                }
            }
            currentStride -= 0.2;
        }
        return false;
    }

    public static boolean tryMoveElseLeftRight(MapLocation loc) throws GameActionException {
        return tryMoveElseLeftRight(myLoc.directionTo(loc), myLoc.distanceTo(loc));
    }

    public static boolean tryMoveElseLeftRight(Direction dir) throws GameActionException {
        return tryMoveElseLeftRight(dir, myType.strideRadius);
    }

    public static boolean tryMoveElseLeftRight(Direction dir, float dist) throws GameActionException {
        return tryMoveElseLeftRight(dir, dist, 30, 5);
    }

    public static boolean tryMoveElseLeftRight(MapLocation loc, float degreeOffset, int checksPerSide) throws GameActionException {
        return tryMoveElseLeftRight(myLoc.directionTo(loc), myLoc.distanceTo(loc), degreeOffset, checksPerSide);
    }

    public static boolean tryMoveElseLeftRight(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
        return tryMoveElseLeftRight(dir, myType.strideRadius, degreeOffset, checksPerSide);
    }

    public static boolean tryMoveElseLeftRight(Direction dir, float dist, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        MapLocation newLoc = myLoc.add(dir, dist);
        if (rc.canMove(dir, dist)) {
            if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                rc.move(dir, dist);
                myLoc = newLoc;
                return true;
            }
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            Direction newDir = dir.rotateLeftDegrees(degreeOffset*currentCheck);
            newLoc = myLoc.add(newDir, dist);
            if(rc.canMove(newDir, dist)) {
                if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                    rc.move(newDir, dist);
                    myLoc = newLoc;
                    return true;
                }
            }

            // Try the offset on the right side
            newDir = dir.rotateRightDegrees(degreeOffset*currentCheck);
            newLoc = myLoc.add(newDir, dist);
            if(rc.canMove(newDir, dist)) {
                if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                    rc.move(newDir, dist);
                    myLoc = newLoc;
                    return true;
                }
            }

            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }


    public static boolean tryHireGardener(Direction dir) throws GameActionException {
        return tryHireGardener(dir, 20, 8);
    }

    public static boolean tryHireGardener(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
        // First, try intended direction
        if (rc.canHireGardener(dir)) {
            rc.hireGardener(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            Direction newDir = dir.rotateLeftDegrees(degreeOffset*currentCheck);
            if (rc.canHireGardener(newDir)) {
                rc.hireGardener(newDir);
                return true;
            }

            // Try the offset on the right side
            newDir = dir.rotateRightDegrees(degreeOffset*currentCheck);
            if (rc.canHireGardener(newDir)) {
                rc.hireGardener(newDir);
                return true;
            }

            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    public static boolean tryMoveElseBackExcludeCircle(MapLocation loc, MapLocation excludeLoc, float excludeR) throws GameActionException {
        return tryMoveElseBackExcludeCircle(myLoc.directionTo(loc), myLoc.distanceTo(loc), excludeLoc, excludeR);
    }

    public static boolean tryMoveElseBackExcludeCircle(Direction dir, MapLocation excludeLoc, float excludeR) throws GameActionException {
        return tryMoveElseBackExcludeCircle(dir, myType.strideRadius, excludeLoc, excludeR);
    }

    public static boolean tryMoveElseBackExcludeCircle(Direction dir, float dist, MapLocation excludeLoc, float excludeR) throws GameActionException {
        float currentStride = dist;
        while (currentStride > 0.1) {
            MapLocation newLoc = myLoc.add(dir, currentStride);
            if (rc.canMove(dir, currentStride)) {
                if (newLoc.distanceTo(excludeLoc) > excludeR) {
                    if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                        rc.move(dir, currentStride);
                        myLoc = newLoc;
                        return true;
                    }
                }
            }
            currentStride -= 0.2;
        }
        if (myLoc.distanceTo(excludeLoc) > excludeR) {
            boolean success = tryMoveDistFrom(excludeLoc, excludeR);
            if (success) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryMoveElseLeftRightExcludeCircle(MapLocation loc, MapLocation excludeLoc, float excludeR) throws GameActionException {
        return tryMoveElseLeftRightExcludeCircle(myLoc.directionTo(loc), myLoc.distanceTo(loc), excludeLoc, excludeR);
    }

    public static boolean tryMoveElseLeftRightExcludeCircle(Direction dir, MapLocation excludeLoc, float excludeR) throws GameActionException {
        return tryMoveElseLeftRightExcludeCircle(dir, myType.strideRadius, excludeLoc, excludeR);
    }

    public static boolean tryMoveElseLeftRightExcludeCircle(Direction dir, float dist, MapLocation excludeLoc, float excludeR) throws GameActionException {
        return tryMoveElseLeftRightExcludeCircle(dir, dist, excludeLoc, excludeR, 30, 5);
    }

    public static boolean tryMoveElseLeftRightExcludeCircle(MapLocation loc, MapLocation excludeLoc, float excludeR, float degreeOffset, int checksPerSide) throws GameActionException {
        return tryMoveElseLeftRightExcludeCircle(myLoc.directionTo(loc), myLoc.distanceTo(loc), excludeLoc, excludeR, degreeOffset, checksPerSide);
    }

    public static boolean tryMoveElseLeftRightExcludeCircle(Direction dir, MapLocation excludeLoc, float excludeR, float degreeOffset, int checksPerSide) throws GameActionException {
        return tryMoveElseLeftRightExcludeCircle(dir, myType.strideRadius, excludeLoc, excludeR, degreeOffset, checksPerSide);
    }

    public static boolean tryMoveElseLeftRightExcludeCircle(Direction dir, float dist, MapLocation excludeLoc, float excludeR, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        MapLocation newLoc = myLoc.add(dir, dist);
        if (rc.canMove(dir, dist)) {
            if (newLoc.distanceTo(excludeLoc) > excludeR) {
                if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                    rc.move(dir, dist);
                    myLoc = newLoc;
                    return true;
                }
            }
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            Direction newDir = dir.rotateLeftDegrees(degreeOffset*currentCheck);
            newLoc = myLoc.add(newDir, dist);
            if(rc.canMove(newDir, dist)) {
                if (newLoc.distanceTo(excludeLoc) > excludeR) {
                    if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                        rc.move(newDir, dist);
                        myLoc = newLoc;
                        return true;
                    }
                }
            }

            // Try the offset on the right side
            newDir = dir.rotateRightDegrees(degreeOffset*currentCheck);
            newLoc = myLoc.add(newDir, dist);
            if(rc.canMove(newDir, dist)) {
                if (newLoc.distanceTo(excludeLoc) > excludeR) {
                    if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                        rc.move(newDir, dist);
                        myLoc = newLoc;
                        return true;
                    }
                }
            }

            // No move performed, try slightly further
            currentCheck++;
        }
        if (myLoc.distanceTo(excludeLoc) <= excludeR) {
            boolean success = tryMoveDistFrom(excludeLoc, excludeR);
            if (success) {
                return true;
            }
        }

        // A move never happened, so return false.
        return false;
    }

    public static boolean tryMoveDistFrom(MapLocation loc, float r) throws GameActionException {
        boolean success;
        if (myLoc.distanceTo(loc) - myType.strideRadius >= r) {
            // go towards
            success = tryMoveElseLeftRight(myLoc.directionTo(loc));
            if (success) {
                return true;
            }
            success = tryMoveElseBack(myLoc.directionTo(loc));
            if (success) {
                return true;
            }
        } else if (myLoc.distanceTo(loc) + myType.strideRadius <= r) {
            // go away
            success = tryMoveElseLeftRight(loc.directionTo(myLoc));
            if (success) {
                return true;
            }
            success = tryMoveElseBack(loc.directionTo(myLoc));
            if (success) {
                return true;
            }
        } else {
            // go to intersection
            MapLocation[] intersections = Geometry.getCircleIntersections(myLoc, myType.strideRadius, loc, r);
            if (intersections.length >= 1) {
                MapLocation target = intersections[circleClockwise ? 1 : 0];
                if (myLoc.distanceTo(target) < myType.strideRadius) {
                    success = tryMoveElseBack(target);
                    if (!success) {
                        success = tryMoveElseLeftRight(target, 15, 5);
                    }
                } else {
                    success = tryMoveElseBack(myLoc.directionTo(target));
                    if (!success) {
                        success = tryMoveElseLeftRight(myLoc.directionTo(target), 15, 5);
                    }
                }
                if (success) {
                    return true;
                } else {
                    circleClockwise = !circleClockwise;
                    target = intersections[circleClockwise ? 1 : 0];
                    if (myLoc.distanceTo(target) < myType.strideRadius) {
                        success = tryMoveElseBack(target);
                    } else {
                        success = tryMoveElseBack(myLoc.directionTo(target));
                    }
                    if (success) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean tryMoveDistFromExcludeCircle(MapLocation targetLoc, float targetR, MapLocation excludeLoc, float excludeR) throws GameActionException {
        boolean success;
        if (myLoc.distanceTo(targetLoc) - myType.strideRadius >= targetR) {
            // go towards
            success = tryMoveElseLeftRightExcludeCircle(myLoc.directionTo(targetLoc), excludeLoc, excludeR);
            if (success) {
                return true;
            }
            success = tryMoveElseBackExcludeCircle(myLoc.directionTo(targetLoc), excludeLoc, excludeR);
            if (success) {
                return true;
            }
        } else if (myLoc.distanceTo(targetLoc) + myType.strideRadius <= targetR) {
            // go away
            success = tryMoveElseLeftRightExcludeCircle(targetLoc.directionTo(myLoc), excludeLoc, excludeR);
            if (success) {
                return true;
            }
            success = tryMoveElseBackExcludeCircle(targetLoc.directionTo(myLoc), excludeLoc, excludeR);
            if (success) {
                return true;
            }
        } else {
            // go to intersection
            MapLocation[] intersections = Geometry.getCircleIntersections(myLoc, myType.strideRadius, targetLoc, targetR);
            if (intersections.length >= 1) {
                MapLocation target = intersections[circleClockwise ? 1 : 0];
                if (myLoc.distanceTo(target) < myType.strideRadius) {
                    success = tryMoveElseBackExcludeCircle(target, excludeLoc, excludeR);
                    if (!success) {
                        success = tryMoveElseLeftRightExcludeCircle(target, excludeLoc, excludeR, 15, 5);
                    }
                } else {
                    success = tryMoveElseBackExcludeCircle(myLoc.directionTo(target), excludeLoc, excludeR);
                    if (!success) {
                        success = tryMoveElseLeftRightExcludeCircle(myLoc.directionTo(target), excludeLoc, excludeR, 15, 5);
                    }
                }
                if (success) {
                    return true;
                } else {
                    circleClockwise = !circleClockwise;
                    target = intersections[circleClockwise ? 1 : 0];
                    if (myLoc.distanceTo(target) < myType.strideRadius) {
                        success = tryMoveElseBackExcludeCircle(target, excludeLoc, excludeR);
                    } else {
                        success = tryMoveElseBackExcludeCircle(myLoc.directionTo(target), excludeLoc, excludeR);
                    }
                    if (success) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean willCollideWith(BulletInfo bullet, MapLocation loc, float r) {
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;
        MapLocation bulletDestination = bulletLocation.add(propagationDirection, bullet.speed);

        MapLocation[] intersections = Geometry.getCircleLineSegmentIntersections(loc, r, bulletLocation, bulletDestination);
        boolean intersects = intersections.length > 0;
        return intersects;
    }
    
    public static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation loc = myLoc; 
        float r = myType.bodyRadius;
    	Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;
        MapLocation bulletDestination = bulletLocation.add(propagationDirection, bullet.speed);

        MapLocation[] intersections = Geometry.getCircleLineSegmentIntersections(loc, r, bulletLocation, bulletDestination);
        boolean intersects = intersections.length > 0;
        return intersects;
    }

    public static boolean willCollideWith(BulletInfo[] bullets, MapLocation loc, float r) {
        for (BulletInfo bullet : bullets) {
            if (bullet == null) {
                break;
            } else {
                if (willCollideWith(bullet, loc, r)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static boolean willCollideWithMe(BulletInfo[] bullets) {
    	MapLocation loc = myLoc; 
        float r = myType.bodyRadius;
    	for (BulletInfo bullet : bullets) {
            if (bullet == null) {
                break;
            } else {
                if (willCollideWith(bullet, loc, r)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // This ignores trees and enemy robots
    public static boolean hasLineOfSightFF(MapLocation target) throws GameActionException {
        MapLocation start = myLoc.add(myLoc.directionTo(target), myType.bodyRadius);
        MapLocation center = new MapLocation((start.x + target.x) / 2f, (start.y + target.y) / 2f);
        float r = start.distanceTo(target) / 2f;
        for (RobotInfo robot : nearbyRobots) {
        	if (robot.team == rc.getTeam().opponent()){
        		continue;
        	}
            MapLocation itemLoc = robot.location;
            float itemR = robot.type.bodyRadius;
            if (center.distanceTo(itemLoc) <= r + itemR) {
                if (itemLoc.distanceTo(target) <= itemR) {
                    // This is the target, do nothing
                } else {
                    MapLocation[] intersections = Geometry.getCircleLineSegmentIntersections(itemLoc, itemR, start, target);
                    if (intersections.length > 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean hasLineOfSight(MapLocation target) throws GameActionException {
        MapLocation start = myLoc.add(myLoc.directionTo(target), myType.bodyRadius);
        MapLocation center = new MapLocation((start.x + target.x) / 2f, (start.y + target.y) / 2f);
        float r = start.distanceTo(target) / 2f;
        for (RobotInfo robot : nearbyRobots) {
            MapLocation itemLoc = robot.location;
            float itemR = robot.type.bodyRadius;
            if (center.distanceTo(itemLoc) <= r + itemR) {
                if (itemLoc.distanceTo(target) <= itemR) {
                    // This is the target, do nothing
                } else {
                    MapLocation[] intersections = Geometry.getCircleLineSegmentIntersections(itemLoc, itemR, start, target);
                    if (intersections.length > 0) {
                        return false;
                    }
                }
            }
        }
        for (TreeInfo tree : nearbyTrees) {
            MapLocation itemLoc = tree.location;
            float itemR = tree.radius;
            if (center.distanceTo(itemLoc) <= r + itemR) {
                if (itemLoc.distanceTo(target) <= itemR) {
                    // This is the target, do nothing
                } else {
                    MapLocation[] intersections = Geometry.getCircleLineSegmentIntersections(itemLoc, itemR, start, target);
                    if (intersections.length > 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static int[] readBroadcastArray(int channelStart, int length) throws GameActionException {
        int[] retval = new int[length];
        for (int i = 0; i < length; i++) {
            retval[i] = rc.readBroadcast(channelStart + i);
        }
        return retval;
    }

    public static void writeBroadcastArray(int channelStart, int[] arr) throws GameActionException {
        for (int i = 0; i < arr.length; i++) {
            rc.broadcast(channelStart + i, arr[i]);
        }
    }

    public static MapBounds getMapBounds() throws GameActionException {
        //BOUNDS_TABLE_CHANNEL=4, maxXc=5, CHANNEL_BOUND_INNER_SOUTH=6, CHANNEL_BOUND_INNER_NORTH=7;
        MapBounds bounds = MapBounds.deserialize(readBroadcastArray(BOUNDS_TABLE_CHANNEL, 8));

        if (bounds.getOuterBound(MapBounds.WEST) == 0 && bounds.getOuterBound(MapBounds.EAST) == 0) {
            bounds = new MapBounds();
            bounds.updateInnerBound(MapBounds.WEST, myLoc.x - myType.bodyRadius);
            bounds.updateInnerBound(MapBounds.EAST, myLoc.x + myType.bodyRadius);
            bounds.updateInnerBound(MapBounds.SOUTH, myLoc.y - myType.bodyRadius);
            bounds.updateInnerBound(MapBounds.NORTH, myLoc.y + myType.bodyRadius);
            for (MapLocation loc : myInitialArchonLocations) {
                bounds.updateInnerBound(MapBounds.WEST, loc.x - RobotType.ARCHON.bodyRadius);
                bounds.updateInnerBound(MapBounds.EAST, loc.x + RobotType.ARCHON.bodyRadius);
                bounds.updateInnerBound(MapBounds.SOUTH, loc.y - RobotType.ARCHON.bodyRadius);
                bounds.updateInnerBound(MapBounds.NORTH, loc.y + RobotType.ARCHON.bodyRadius);
            }
            for (MapLocation loc : enemyInitialArchonLocations) {
                bounds.updateInnerBound(MapBounds.WEST, loc.x - RobotType.ARCHON.bodyRadius);
                bounds.updateInnerBound(MapBounds.EAST, loc.x + RobotType.ARCHON.bodyRadius);
                bounds.updateInnerBound(MapBounds.SOUTH, loc.y - RobotType.ARCHON.bodyRadius);
                bounds.updateInnerBound(MapBounds.NORTH, loc.y + RobotType.ARCHON.bodyRadius);
            }
        }

        //Debug
        MapLocation knownNE = new MapLocation(bounds.getInnerBound(MapBounds.EAST), bounds.getInnerBound(MapBounds.NORTH));
        MapLocation knownSE = new MapLocation(bounds.getInnerBound(MapBounds.EAST), bounds.getInnerBound(MapBounds.SOUTH));
        MapLocation knownNW = new MapLocation(bounds.getInnerBound(MapBounds.WEST), bounds.getInnerBound(MapBounds.NORTH));
        MapLocation knownSW = new MapLocation(bounds.getInnerBound(MapBounds.WEST), bounds.getInnerBound(MapBounds.SOUTH));
		
        int r = 0; int g = 255; int b = 255;
        rc.setIndicatorLine(knownNE, knownNW, r, g, b);
        rc.setIndicatorLine(knownNE, knownSE, r, g, b);
        rc.setIndicatorLine(knownSW, knownSE, r, g, b);
        rc.setIndicatorLine(knownSW, knownNW, r, g, b);
        

        return bounds;

    }

    public static MapLocation[] mapBST (MapLocation inner, MapLocation outer) throws GameActionException {

        float i = outer.distanceTo(inner) / 2;
        Direction away = outer.directionTo(inner);
        MapLocation mid = outer.add(away, i);

        int c = 0;

        while (i >= 0.0005) { // this is the precision I choose to get
            c+=1;
            if (rc.onTheMap(mid)) {
                inner = mid;
                i = inner.distanceTo(outer) / 2;
                mid = mid.subtract(away, i);
            }
            else {
                outer = mid;
                i = inner.distanceTo(outer) / 2;
                mid = mid.add(away, i);
            }

            if (c > 15) {
                System.out.println("narrowBounds timed out");
                break;
            }

        }

        return new MapLocation[]{inner, outer};
    }

    public static void updateMapBounds(MapBounds bounds) throws GameActionException {
        MapLocation[] senseLocs = new MapLocation[4];
        senseLocs[MapBounds.NORTH] = myLoc.add(Direction.getNorth(), myType.sensorRadius - 0.001f); // maxY sensed
        senseLocs[MapBounds.EAST] = myLoc.add(Direction.getEast(), myType.sensorRadius - 0.001f); // maxX sensed;
        senseLocs[MapBounds.SOUTH] = myLoc.add(Direction.getSouth(), myType.sensorRadius - 0.001f); // minY sensed;
        senseLocs[MapBounds.WEST] = myLoc.add(Direction.getWest(), myType.sensorRadius - 0.001f); // minX sensed;

        for (int dirOrd = 0; dirOrd < 4; dirOrd++) {
            MapLocation senseLoc = senseLocs[dirOrd];
            if (!bounds.bounds[dirOrd].valid()) {
                System.out.println("Detected invalid bounds! " + bounds.bounds[dirOrd].toString());
            }
            if(!rc.onTheMap(senseLoc)) {
                bounds.updateInnerBound(dirOrd, myLoc.add(MapBounds.dirFromOrd(dirOrd), myType.bodyRadius));
                bounds.updateOuterBound(dirOrd, senseLoc);
                MapLocation[] eastBounds = mapBST(bounds.getInnerBoundLoc(dirOrd, myLoc), bounds.getOuterBoundLoc(dirOrd, myLoc));
                bounds.updateInnerBound(dirOrd, eastBounds[0]);
                bounds.updateOuterBound(dirOrd, eastBounds[1]);
            } else {
                bounds.updateInnerBound(dirOrd, senseLoc);
            }
            if (!bounds.bounds[dirOrd].valid()) {
                System.out.println("Created invalid bounds! " + bounds.bounds[dirOrd].toString());
            }
        }

        writeBroadcastArray(BOUNDS_TABLE_CHANNEL, bounds.serialize());
    }

    public static final int FARM_TABLE_ENTRY_EXISTS_MASK = 0x1;
    public static final int FARM_TABLE_ENTRY_GARDENER_MASK = 0x2;
    public static final int FARM_TABLE_ENTRY_LUMBERJACK_MASK = 0x4;
    public static final int FARM_TABLE_ENTRY_FULL_MASK = 0x8;

    public static int createFarmTableEntry() throws GameActionException{
        int farmTableCount = rc.readBroadcast(FARM_TABLE_COUNT_CHANNEL);
        int farmNum = farmTableCount;
        if (farmNum >= FARM_TABLE_NUM_ENTRIES) {
            System.out.println("Farm table overflow!");
            return -1;
        }
        rc.broadcast(FARM_TABLE_COUNT_CHANNEL, farmTableCount + 1);
        writeFarmTableEntry(farmNum, myLoc, true, false, false);
        return farmNum;
    }

    public static void writeFarmTableEntry(int farmNum, MapLocation loc, boolean gardenerAlive, boolean lumberjackAlive) throws GameActionException {
        int farmTableCount = rc.readBroadcast(FARM_TABLE_COUNT_CHANNEL);
        if (farmNum >= farmTableCount) {
            System.out.println("Farm number invalid!");
            return;
        }
        int farmTableEntryChannel = FARM_TABLE_CHANNEL + (farmNum * FARM_TABLE_ENTRY_SIZE);
        int xChannel = farmTableEntryChannel;
        int yChannel = farmTableEntryChannel + 1;
        int flagsChannel = farmTableEntryChannel + 2;
        rc.broadcast(xChannel, Float.floatToIntBits(loc.x));
        rc.broadcast(yChannel, Float.floatToIntBits(loc.y));
        int flags = rc.readBroadcast(flagsChannel);
        flags = flags | FARM_TABLE_ENTRY_EXISTS_MASK;
        if (gardenerAlive) {
            flags = flags | FARM_TABLE_ENTRY_GARDENER_MASK;
        }
        if (lumberjackAlive) {
            flags = flags | FARM_TABLE_ENTRY_LUMBERJACK_MASK;
        }
        rc.broadcast(flagsChannel, flags);
    }

    public static void writeFarmTableEntry(int farmNum, MapLocation loc, boolean gardenerAlive, boolean lumberjackAlive, boolean full) throws GameActionException {
        int farmTableCount = rc.readBroadcast(FARM_TABLE_COUNT_CHANNEL);
        if (farmNum >= farmTableCount) {
            System.out.println("Farm number invalid!");
            return;
        }
        int farmTableEntryChannel = FARM_TABLE_CHANNEL + (farmNum * FARM_TABLE_ENTRY_SIZE);
        int xChannel = farmTableEntryChannel;
        int yChannel = farmTableEntryChannel + 1;
        int flagsChannel = farmTableEntryChannel + 2;
        rc.broadcast(xChannel, Float.floatToIntBits(loc.x));
        rc.broadcast(yChannel, Float.floatToIntBits(loc.y));
        int flags = rc.readBroadcast(flagsChannel);
        flags = flags | FARM_TABLE_ENTRY_EXISTS_MASK;
        if (gardenerAlive) {
            flags = flags | FARM_TABLE_ENTRY_GARDENER_MASK;
        }
        if (lumberjackAlive) {
            flags = flags | FARM_TABLE_ENTRY_LUMBERJACK_MASK;
        }
        if (full) {
            flags = flags | FARM_TABLE_ENTRY_FULL_MASK;
        } else {
            flags = flags & ~FARM_TABLE_ENTRY_FULL_MASK;
        }
        rc.broadcast(flagsChannel, flags);
    }

    public static MapLocation readFarmTableEntryLocation(int farmNum) throws GameActionException {
        int farmTableEntryChannel = FARM_TABLE_CHANNEL + (farmNum * FARM_TABLE_ENTRY_SIZE);
        int xChannel = farmTableEntryChannel;
        int yChannel = farmTableEntryChannel + 1;
        float x = Float.intBitsToFloat(rc.readBroadcast(xChannel));
        float y = Float.intBitsToFloat(rc.readBroadcast(yChannel));
        return new MapLocation(x, y);
    }

    public static int readFarmTableEntryFlags(int farmNum) throws GameActionException {
        int farmTableEntryChannel = FARM_TABLE_CHANNEL + (farmNum * FARM_TABLE_ENTRY_SIZE);
        int flagsChannel = farmTableEntryChannel + 2;
        int flags = rc.readBroadcast(flagsChannel);
        return flags;
    }

    public static void resetFarmTableEntryFlags(int farmNum) throws GameActionException {
        int farmTableEntryChannel = FARM_TABLE_CHANNEL + (farmNum * FARM_TABLE_ENTRY_SIZE);
        int flagsChannel = farmTableEntryChannel + 2;
        int flags = rc.readBroadcast(flagsChannel);
        flags = flags & (FARM_TABLE_ENTRY_EXISTS_MASK | FARM_TABLE_ENTRY_FULL_MASK);
        rc.broadcast(flagsChannel, flags);
    }

    public static int getFarmTableEntryCount() throws GameActionException {
        return rc.readBroadcast(FARM_TABLE_COUNT_CHANNEL);
    }

    public static int numFarmTableEntriesFull() throws GameActionException {
        int farmCount = getFarmTableEntryCount();
        int fullCount = 0;
        for (int farmNum = 0; farmNum < farmCount; farmNum++) {
            int flags = readFarmTableEntryFlags(farmNum);
            if ((flags & FARM_TABLE_ENTRY_FULL_MASK) != 0) {
                fullCount++;
            }
        }
        return fullCount;
    }

    public static void addLumberjackJob(MapLocation loc) throws GameActionException {
        addLumberjackJob(loc, -1);
    }

    public static void addLumberjackJob(MapLocation loc, int farmNum) throws GameActionException {
        int begin = rc.readBroadcast(LJ_JOBS_TABLE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(LJ_JOBS_TABLE_COUNT_CHANNEL);
        if (count >= LJ_JOBS_TABLE_NUM_ENTRIES) {
            System.out.println("Lumberjack job table overflow!");
            return;
        }
        int entryIndex = (begin + count) % LJ_JOBS_TABLE_NUM_ENTRIES;
        int entryChannel = LJ_JOBS_TABLE_CHANNEL + (entryIndex * LJ_JOBS_TABLE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        int farmNumChannel = entryChannel + 2;
        rc.broadcast(xChannel, Float.floatToIntBits(loc.x));
        rc.broadcast(yChannel, Float.floatToIntBits(loc.y));
        rc.broadcast(farmNumChannel, farmNum);
        count++;
        rc.broadcast(LJ_JOBS_TABLE_COUNT_CHANNEL, count);
    }

    public static int popLumberjackJobFarmNum() throws GameActionException {
        int begin = rc.readBroadcast(LJ_JOBS_TABLE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(LJ_JOBS_TABLE_COUNT_CHANNEL);
        if (count <= 0) {
            return -1;
        }
        int entryChannel = LJ_JOBS_TABLE_CHANNEL + (begin * LJ_JOBS_TABLE_ENTRY_SIZE);
        //int xChannel = begin;
        //int yChannel = begin + 1;
        int farmNumChannel = entryChannel + 2;
        int farmNum = rc.readBroadcast(farmNumChannel);
        begin = (begin + 1) % LJ_JOBS_TABLE_NUM_ENTRIES;
        rc.broadcast(LJ_JOBS_TABLE_BEGIN_CHANNEL, begin);
        count = count - 1;
        rc.broadcast(LJ_JOBS_TABLE_COUNT_CHANNEL, count);
        return farmNum;
    }

    public static boolean[] getFarmTableHasLumberjackJob() throws GameActionException {
        int farmTableCount = rc.readBroadcast(FARM_TABLE_COUNT_CHANNEL);
        boolean[] farmTableHasLumberjackJob = new boolean[farmTableCount];
        int begin = rc.readBroadcast(LJ_JOBS_TABLE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(LJ_JOBS_TABLE_COUNT_CHANNEL);
        for (int i = 0; i < count; i++) {
            int ljIndex = (begin + i) % LJ_JOBS_TABLE_NUM_ENTRIES;
            int entryChannel = LJ_JOBS_TABLE_CHANNEL + (ljIndex * LJ_JOBS_TABLE_ENTRY_SIZE);
            int farmNumChannel = entryChannel + 2;
            int farmNum = rc.readBroadcast(farmNumChannel);
            farmTableHasLumberjackJob[farmNum] = true;
        }
        return farmTableHasLumberjackJob;
    }

    public static RobotType peekBuildQueue1() throws GameActionException {
        int begin = rc.readBroadcast(BUILD_QUEUE_1_BEGIN_CHANNEL);
        int count = rc.readBroadcast(BUILD_QUEUE_1_COUNT_CHANNEL);
        if (count < 1) {
            return null;
        }
        int itemChannel = BUILD_QUEUE_1_CHANNEL + begin;
        int robotTypeNum = rc.readBroadcast(itemChannel);
        return RobotType.values()[robotTypeNum];
    }

    public static RobotType peekBuildQueue2() throws GameActionException {
        int begin = rc.readBroadcast(BUILD_QUEUE_2_BEGIN_CHANNEL);
        int count = rc.readBroadcast(BUILD_QUEUE_2_COUNT_CHANNEL);
        if (count <= 0) {
            return null;
        }
        int entryChannel = BUILD_QUEUE_2_CHANNEL + begin;
        int robotTypeNum = rc.readBroadcast(entryChannel);
        return RobotType.values()[robotTypeNum];
    }

    public static void addBuildQueue1(RobotType rt) throws GameActionException {
        int begin = rc.readBroadcast(BUILD_QUEUE_1_BEGIN_CHANNEL);
        int count = rc.readBroadcast(BUILD_QUEUE_1_COUNT_CHANNEL);
        if (count >= BUILD_QUEUE_1_LENGTH) {
            System.out.println("Build queue 1 overflow!");
            return;
        }
        int entryIndex = (begin + count) % BUILD_QUEUE_1_LENGTH;
        int entryChannel = BUILD_QUEUE_1_CHANNEL + entryIndex;
        rc.broadcast(entryChannel, rt.ordinal());
        count++;
        rc.broadcast(BUILD_QUEUE_1_COUNT_CHANNEL, count);
    }

    public static void addBuildQueue2(RobotType rt) throws GameActionException {
        int begin = rc.readBroadcast(BUILD_QUEUE_2_BEGIN_CHANNEL);
        int count = rc.readBroadcast(BUILD_QUEUE_2_COUNT_CHANNEL);
        if (count >= BUILD_QUEUE_2_LENGTH) {
            System.out.println("Build queue 2 overflow!");
            return;
        }
        int entryIndex = (begin + count) % BUILD_QUEUE_2_LENGTH;
        int entryChannel = BUILD_QUEUE_2_CHANNEL + entryIndex;
        rc.broadcast(entryChannel, rt.ordinal());
        count++;
        rc.broadcast(BUILD_QUEUE_2_COUNT_CHANNEL, count);
    }

    public static RobotType popBuildQueue1() throws GameActionException {
        int begin = rc.readBroadcast(BUILD_QUEUE_1_BEGIN_CHANNEL);
        int count = rc.readBroadcast(BUILD_QUEUE_1_COUNT_CHANNEL);
        if (count <= 0) {
            return null;
        }
        int entryChannel = BUILD_QUEUE_1_CHANNEL + begin;
        int rtNum = rc.readBroadcast(entryChannel);
        begin = (begin + 1) % BUILD_QUEUE_1_LENGTH;
        rc.broadcast(BUILD_QUEUE_1_BEGIN_CHANNEL, begin);
        count = count - 1;
        rc.broadcast(BUILD_QUEUE_1_COUNT_CHANNEL, count);
        return RobotType.values()[rtNum];
    }

    public static RobotType popBuildQueue2() throws GameActionException {
        int begin = rc.readBroadcast(BUILD_QUEUE_2_BEGIN_CHANNEL);
        int count = rc.readBroadcast(BUILD_QUEUE_2_COUNT_CHANNEL);
        if (count <= 0) {
            return null;
        }
        int entryChannel = BUILD_QUEUE_2_CHANNEL + begin;
        int rtNum = rc.readBroadcast(entryChannel);
        begin = (begin + 1) % BUILD_QUEUE_2_LENGTH;
        rc.broadcast(BUILD_QUEUE_2_BEGIN_CHANNEL, begin);
        count = count - 1;
        rc.broadcast(BUILD_QUEUE_2_COUNT_CHANNEL, count);
        return RobotType.values()[rtNum];
    }

    public static void setGlobalDefaultBuild(RobotType type) throws GameActionException {
        if (type == null) {
            rc.broadcast(GLOBAL_DEFAULT_BUILD_CHANNEL, -1);
        } else {
            rc.broadcast(GLOBAL_DEFAULT_BUILD_CHANNEL, type.ordinal());
        }
    }

    public static RobotType getGlobalDefaultBuild() throws GameActionException {
        int robotNum = rc.readBroadcast(GLOBAL_DEFAULT_BUILD_CHANNEL);
        if (robotNum < 0) {
            return null;
        } else {
            return RobotType.values()[robotNum];
        }
    }

    public static void debugTick(int id) {
        int currentRoundNum = rc.getRoundNum();
        int bytecodes = Clock.getBytecodeNum();
        debugBytecodesList[id] = bytecodes;
        numDebugBytecodes = Math.max(numDebugBytecodes, id+1);
        if (currentRoundNum != roundNum) {
            if (!debugTripped) {
                debugTripped = true;
                System.out.println("Detected over-bytecodes!!!");
                System.out.println("Round changed before tick " + id);
                for (int i = 0; i < numDebugBytecodes - 1; i++) {
                    System.out.println(i + ": " + debugBytecodesList[i]);
                }
                System.out.println((numDebugBytecodes - 1) + ": " + debugBytecodesList[numDebugBytecodes - 1] + " + " + (currentRoundNum - roundNum) + " rounds");
            } else {
                System.out.println(id + ": " + bytecodes + " + " + (currentRoundNum - roundNum) + " rounds");
            }
        }
    }

    public static void sendAttackLocation(MapLocation loc) throws GameActionException {
        if (loc == null) {
            rc.broadcast(ATTACK_LOCATION_EXISTS_CHANNEL, 0);
        } else {
            rc.broadcast(ATTACK_LOCATION_EXISTS_CHANNEL, 1);
            rc.broadcast(ATTACK_LOCATION_X_CHANNEL, Float.floatToIntBits(loc.x));
            rc.broadcast(ATTACK_LOCATION_Y_CHANNEL, Float.floatToIntBits(loc.y));
        }
    }

    public static MapLocation queryAttackLocation() throws GameActionException {
        boolean attackLocExists = rc.readBroadcast(ATTACK_LOCATION_EXISTS_CHANNEL) > 0;
        if (attackLocExists) {
            float x = Float.intBitsToFloat(rc.readBroadcast(ATTACK_LOCATION_X_CHANNEL));
            float y = Float.intBitsToFloat(rc.readBroadcast(ATTACK_LOCATION_Y_CHANNEL));
            return new MapLocation(x, y);
        } else {
            return null;
        }
    }

    public static void sendAttackFinished() throws GameActionException {
        rc.broadcast(ATTACK_FINISHED_CHANNEL, 1);
    }

    public static boolean popAttackFinished() throws GameActionException {
        boolean finished = rc.readBroadcast(ATTACK_FINISHED_CHANNEL) > 0;
        rc.broadcast(ATTACK_FINISHED_CHANNEL, 0);
        return finished;
    }

    public static void sendScoutMode(ScoutMode mode, boolean override) throws GameActionException {
        rc.broadcast(NEW_SCOUT_MODE_CHANNEL, mode.ordinal());
        if (override) {
            rc.broadcast(OVERRIDE_SCOUT_MODE_CHANNEL, mode.ordinal());
        } else {
            rc.broadcast(OVERRIDE_SCOUT_MODE_CHANNEL, -1);
        }
    }

    public static ScoutMode queryScoutMode(ScoutMode mode) throws GameActionException {
        int overrideModeOrd = rc.readBroadcast(OVERRIDE_SCOUT_MODE_CHANNEL);
        if (overrideModeOrd < 0) {
            // No override, so only change mode if no current mode
            if (mode == null) {
                // No current mode, so change to new mode
                int newModeOrd = rc.readBroadcast(NEW_SCOUT_MODE_CHANNEL);
                mode = ScoutMode.values()[newModeOrd];
            }
        } else {
            // Mode override
            mode = ScoutMode.values()[overrideModeOrd];
        }
        return mode;
    }

    public static void setInitialBuildQueue1(RobotType[] initialBuildQueue1) {
        RobotGlobal.initialBuildQueue1 = initialBuildQueue1;
    }

    public static void setInitialBuildQueue2(RobotType[] initialBuildQueue2) {
        RobotGlobal.initialBuildQueue2 = initialBuildQueue2;
    }

    public static void setInitialDefaultBuild(RobotType initialDefaultBuild) {
        RobotGlobal.initialDefaultBuild = initialDefaultBuild;
    }

    public static void initializeBuildQueue1() throws GameActionException {
        for (RobotType rt : initialBuildQueue1) {
            addBuildQueue1(rt);
        }
    }

    public static void initializeBuildQueue2() throws GameActionException {
        for (RobotType rt : initialBuildQueue2) {
            addBuildQueue2(rt);
        }
    }

    public static void initializeDefaultBuild() throws GameActionException {
        setGlobalDefaultBuild(initialDefaultBuild);
    }

    public static void setGardenerSchedule(GardenerSchedule schedule) {
        RobotGlobal.gardenerSchedule = schedule;
    }

    public static GardenerSchedule getGardenerSchedule() {
        return gardenerSchedule;
    }

    public static void setGardenerScheduleN(int n) {
        RobotGlobal.gardenerScheduleN = n;
    }

    public static int getGardenerScheduleN() {
        return gardenerScheduleN;
    }

    public static void setExperimental(boolean experimental) {
        RobotGlobal.experimental = experimental;
    }

    public static boolean getExperimental() {
        return experimental;
    }

    /*

                RangeList exclude = new RangeList(false);

                // Sense robots
                RobotInfo[] robots = rc.senseNearbyRobots();
                for (RobotInfo robot : robots) {
                    MapLocation robotLoc = robot.getLocation();
                    float radius = myType.bodyRadius + robot.getType().bodyRadius;
                    float[] radiusIntersections = Geometry.getCircleIntersections(myLoc, myType.strideRadius, robotLoc, radius);
                    if (radiusIntersections.length == 2) {
                        exclude.add(radiusIntersections[0], radiusIntersections[1]);
                    }
                }

                // Sense trees
                TreeInfo[] trees = rc.senseNearbyTrees();
                for (TreeInfo tree : trees) {
                    MapLocation robotLoc = tree.getLocation();
                    float radius = myType.bodyRadius + tree.getRadius();
                    float[] radiusIntersections = Geometry.getCircleIntersections(myLoc, myType.strideRadius, robotLoc, radius);
                    if (radiusIntersections.length == 2) {
                        exclude.add(radiusIntersections[0], radiusIntersections[1]);
                    }
                }

                // Sense bullets
                BulletInfo[] bullets = rc.senseNearbyBullets();
                Direction[] moveDirs = new Direction[3];
                for (int strideNum = 1; strideNum <= 3; strideNum++) {
                    for (BulletInfo bullet : bullets) {
                        MapLocation bulletStart = bullet.getLocation();
                        Direction bulletDir = bullet.getDir();
                        float bulletSpeed = bullet.getSpeed();
                        MapLocation bulletEnd = bulletStart.add(bulletDir, bulletSpeed * strideNum);
                        float radius = myType.bodyRadius;
                        float[] startRadiusIntersections = Geometry.getCircleIntersections(myLoc, myType.strideRadius * strideNum, bulletStart, radius);
                        float[] endRadiusIntersections = Geometry.getCircleIntersections(myLoc, myType.strideRadius * strideNum, bulletEnd, radius);
                        MapLocation bulletLineLeftStart = bulletStart.add(bulletDir.rotateLeftDegrees(90), radius);
                        MapLocation bulletLineLeftEnd = bulletLineLeftStart.add(bulletDir, bulletSpeed);
                        MapLocation bulletLineRightStart = bulletStart.add(bulletDir.rotateRightDegrees(90), radius);
                        MapLocation bulletLineRightEnd = bulletLineRightStart.add(bulletDir, bulletSpeed);
                        MapLocation[] poly = new MapLocation[]{
                                bulletLineLeftStart,
                                bulletLineLeftEnd,
                                bulletLineRightStart,
                                bulletLineRightEnd
                        };
                        float[][] polyIntersectionPairs = Geometry.getCirclePolygonIntersectionPairs(myLoc, myType.strideRadius * strideNum, poly);
                        if (startRadiusIntersections.length == 2) {
                            exclude.add(startRadiusIntersections[0], startRadiusIntersections[1]);
                            //System.out.println("A Happened");
                        }
                        if (endRadiusIntersections.length == 2) {
                            exclude.add(endRadiusIntersections[0], endRadiusIntersections[1]);
                            //System.out.println("B Happened");
                        }
                        for (float[] polyIntersectionPair : polyIntersectionPairs) {
                            exclude.add(polyIntersectionPair[0], polyIntersectionPair[1]);
                            //System.out.println("C Happened");
                        }
                    }
                    float closest = exclude.closest(attackDir.radians);
                    Direction moveDir;
                    if (java.lang.Float.isNaN(closest)) {
                        moveDir = null;
                    } else {
                        moveDir = new Direction(closest);
                    }
                    moveDirs[strideNum-1] = moveDir;
                }

                RangeList.Range current = exclude.getFirst();
                float swept = 0;
                while (current != null) {
                    float running = current.start;
                    boolean stopTime = false;
                    while (!stopTime) {
                        float next = (running + 0.1f) % 2*(float)Math.PI;
                        if (RangeList.isInside(current.stop, running, next)) {
                            next = current.stop;
                            stopTime = true;
                        }
                        if (next > running) {
                            swept += next - running;
                        } else {
                            swept += next + 2*(float)Math.PI - running;
                        }
                        rc.setIndicatorLine(myLoc.add(new Direction(running), myType.strideRadius), myLoc.add(new Direction(next), myType.strideRadius), 255,0,0);
                        running = next;
                    }

                    current = current.next;
                    if (current == exclude.getFirst()) {
                        break;
                    }
                }

                System.out.println(swept);
                if (swept > 6) {
                    System.out.println("High sweep");
                    System.out.println(exclude);
                }

                rc.setIndicatorDot(myLoc.add(attackDir, myType.strideRadius), 0, 255, 0);

                Direction farthestDir = null;
                float farthestDist = -1;
                for (Direction moveDir : moveDirs) {
                    if (moveDir != null) {
                        float dist = moveDir.radiansBetween(attackDir);
                        if (dist > farthestDist) {
                            farthestDir = moveDir;
                            farthestDist = dist;
                        }
                    }
                }

                rc.setIndicatorDot(myLoc.add(farthestDir, myType.strideRadius), 0, 0, 255);

                // Move
                if (farthestDir != null) {
                    boolean success = tryMove(myLoc.add(farthestDir, myType.strideRadius));
                    if (!success) {
                        System.out.println("Exclude: " + exclude);
                        System.out.println("Move: " + exclude.closest(attackDir.radians));
                    }
                } else {
                    System.out.println("Null move");
                    System.out.println("Exclude: " + exclude);
                }

     */
}
