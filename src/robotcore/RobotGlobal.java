package robotcore;

import battlecode.common.*;

public strictfp class RobotGlobal {
    public static RobotController rc;

    // Channel constants
    public static final int CHANNEL_ARCHON_COUNTER = 0;
    public static final int CHANNEL_ARCHON_LOCATION_ARRAY_START = CHANNEL_ARCHON_COUNTER + 1;
    public static final int CHANNEL_ARCHON_LOCATION_ARRAY_LENGTH = 6;
    public static final int CHANNEL_FARM_LOCATION_ARRAY_START = CHANNEL_ARCHON_LOCATION_ARRAY_START + CHANNEL_ARCHON_LOCATION_ARRAY_LENGTH;
    public static final int CHANNEL_FARM_LOCATION_ARRAY_LENGTH = 100;
    public static final int CHANNEL_BOUND_INNER_WEST = CHANNEL_FARM_LOCATION_ARRAY_START + CHANNEL_FARM_LOCATION_ARRAY_LENGTH;
    public static final int CHANNEL_BOUND_INNER_EAST = CHANNEL_BOUND_INNER_WEST + 1;
    public static final int CHANNEL_BOUND_INNER_SOUTH = CHANNEL_BOUND_INNER_EAST + 1;
    public static final int CHANNEL_BOUND_INNER_NORTH = CHANNEL_BOUND_INNER_SOUTH + 1;
    public static final int CHANNEL_BOUND_OUTER_WEST = CHANNEL_BOUND_INNER_NORTH + 1;
    public static final int CHANNEL_BOUND_OUTER_EAST = CHANNEL_BOUND_OUTER_WEST + 1;
    public static final int CHANNEL_BOUND_OUTER_SOUTH = CHANNEL_BOUND_OUTER_EAST + 1;
    public static final int CHANNEL_BOUND_OUTER_NORTH = CHANNEL_BOUND_OUTER_SOUTH + 1;

    // Performance constants
    public static final int DESIRED_ROBOTS = 20;
    public static final int DESIRED_TREES = 20;
    public static final int DESIRED_BULLETS = 20;

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
    private static TreeInfo nearestTree = null;
    private static TreeInfo nearestFriendlyTree = null;
    private static TreeInfo lowestFriendlyTree = null;
    private static BulletInfo[] bulletsToAvoid = new BulletInfo[0];
    private static int numBulletsToAvoid = 0;
    private static RobotType buildOrder;

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
        if (!neverUpdated && newRoundNum - roundNum != 1) {
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
        float minDist = 99999999;
        nearestEnemy = null;
        int numIters = Math.min(nearbyRobots.length, DESIRED_ROBOTS);
        for (int i = 0; i < numIters; i++) {
            RobotInfo robot = nearbyRobots[i];
            if (robot.getTeam() == enemyTeam) {
                float dist = myLoc.distanceTo(robot.getLocation()) - robot.getRadius();
                if (dist < minDist) {
                    nearestEnemy = robot;
                    minDist = dist;
                }
            }
        }
    }

    public static void processNearbyTrees() throws GameActionException {
        float minDist = Float.POSITIVE_INFINITY;
        float minFriendlyDist = Float.POSITIVE_INFINITY;
        float minFriendlyHealth = Float.POSITIVE_INFINITY;
        nearestTree = null;
        nearestFriendlyTree = null;
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

    public static MapLocation[] getMyArchonLocations() throws GameActionException {
        int numArchons = rc.readBroadcast(CHANNEL_ARCHON_COUNTER);
        MapLocation[] archonLocations = new MapLocation[numArchons];
        for (int i = 0; i < numArchons; i++) {
            float x = Float.intBitsToFloat(rc.readBroadcast(CHANNEL_ARCHON_LOCATION_ARRAY_START + (2*i)));
            float y = Float.intBitsToFloat(rc.readBroadcast(CHANNEL_ARCHON_LOCATION_ARRAY_START + (2*i) + 1));
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

    public static TreeInfo getNearestTree() {
        return nearestTree;
    }

    public static TreeInfo getNearestFriendlyTree() {
        return nearestFriendlyTree;
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

    public static void setBuildOrder(RobotType type) {
        buildOrder = type;
    }

    public static RobotType getBuildOrder() {
        return buildOrder;
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    public static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    public static boolean tryMoveElseBack(Direction dir) throws GameActionException {
        float currentStride = myType.strideRadius;
        while (currentStride > 0.1) {
            MapLocation newLoc = myLoc.add(dir, currentStride);
            if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                if (rc.canMove(newLoc)) {
                    rc.move(newLoc);
                    myLoc = newLoc;
                    return true;
                }
            }
            currentStride -= 0.2;
        }
        return false;
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryMoveElseLeftRight(Direction dir) throws GameActionException {
        return tryMoveElseLeftRight(dir,15,11);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    public static boolean tryMoveElseLeftRight(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        MapLocation newLoc = myLoc.add(dir, myType.strideRadius);
        if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
            if (rc.canMove(newLoc)) {
                rc.move(newLoc);
                myLoc = newLoc;
                return true;
            }
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            newLoc = myLoc.add(dir.rotateLeftDegrees(degreeOffset*currentCheck), myType.strideRadius);
            if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                if(rc.canMove(newLoc)) {
                    rc.move(newLoc);
                    myLoc = newLoc;
                    return true;
                }
            }

            // Try the offset on the right side
            newLoc = myLoc.add(dir.rotateLeftDegrees(degreeOffset*currentCheck), myType.strideRadius);
            if (!willCollideWith(bulletsToAvoid, newLoc, myType.bodyRadius)) {
                if(rc.canMove(newLoc)) {
                    rc.move(newLoc);
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

    public static boolean willCollideWith(BulletInfo bullet, MapLocation loc, float r) {

        // Get relevant bullet information
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
        //CHANNEL_BOUND_INNER_WEST=4, maxXc=5, CHANNEL_BOUND_INNER_SOUTH=6, CHANNEL_BOUND_INNER_NORTH=7;
        MapBounds bounds = MapBounds.deserialize(readBroadcastArray(CHANNEL_BOUND_INNER_WEST, 8));

        if (bounds.getOuterBound(MapBounds.WEST) == 0 && bounds.getOuterBound(MapBounds.EAST) == 0) {
            bounds = new MapBounds();
            bounds.updateInnerBound(MapBounds.WEST, myLoc.x - myType.bodyRadius);
            bounds.updateInnerBound(MapBounds.EAST, myLoc.x + myType.bodyRadius);
            bounds.updateInnerBound(MapBounds.SOUTH, myLoc.y - myType.bodyRadius);
            bounds.updateInnerBound(MapBounds.NORTH, myLoc.y + myType.bodyRadius);
        }

        //Debug
        /*
        MapLocation knownNE = new MapLocation(bounds.getInnerBound(MapBounds.EAST), bounds.getInnerBound(MapBounds.NORTH));
        MapLocation knownSE = new MapLocation(bounds.getInnerBound(MapBounds.EAST), bounds.getInnerBound(MapBounds.SOUTH));
        MapLocation knownNW = new MapLocation(bounds.getInnerBound(MapBounds.WEST), bounds.getInnerBound(MapBounds.NORTH));
        MapLocation knownSW = new MapLocation(bounds.getInnerBound(MapBounds.WEST), bounds.getInnerBound(MapBounds.SOUTH));

        int r = 0; int g = 255; int b = 100;
        rc.setIndicatorLine(knownNE, knownNW, r, g, b);
        rc.setIndicatorLine(knownNE, knownSE, r, g, b);
        rc.setIndicatorLine(knownSW, knownSE, r, g, b);
        rc.setIndicatorLine(knownSW, knownNW, r, g, b);
        */

        return bounds;

    }

    public static MapLocation[] narrowBounds (MapLocation inner, MapLocation outer) throws GameActionException {

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
            if(!rc.onTheMap(senseLoc)) {
                bounds.updateInnerBound(dirOrd, myLoc.add(MapBounds.dirFromOrd(dirOrd), myType.bodyRadius));
                bounds.updateOuterBound(dirOrd, senseLoc);
                MapLocation[] eastBounds = narrowBounds(bounds.getInnerBoundLoc(dirOrd, myLoc), bounds.getOuterBoundLoc(dirOrd, myLoc));
                bounds.updateInnerBound(dirOrd, eastBounds[0]);
                bounds.updateOuterBound(dirOrd, eastBounds[1]);
            } else {
                bounds.updateInnerBound(dirOrd, senseLoc);
            }
        }

        writeBroadcastArray(CHANNEL_BOUND_INNER_WEST, bounds.serialize());
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
