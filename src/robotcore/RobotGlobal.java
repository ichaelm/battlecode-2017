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

    public static class CommMap {

        // member variables

        private static MapLocation commMapOrigin;

        // geometry constants

        public static final float CELL_RADIUS = 1f;
        public static final float CELL_RESOLUTION = 2f;
        public static final Direction aDir = Direction.getEast(); // Algorithm depends on aDir being east!!!
        public static final Direction bDir = Direction.getEast().rotateLeftDegrees(60);

        // bitmasks

        private static final int MAP_CELL_EXPLORED_MASK = 0x80000000;
        private static final int MAP_CELL_CLEAR_MASK = 0x40000000;

        // whole map operations

        public static void sendOrigin(MapLocation origin) throws GameActionException {
            CommMap.commMapOrigin = origin;
            rc.broadcast(MAP_ORIGIN_X_CHANNEL, Float.floatToIntBits(origin.x));
            rc.broadcast(MAP_ORIGIN_Y_CHANNEL, Float.floatToIntBits(origin.y));
        }

        public static void queryOrigin() throws GameActionException {
            float x = Float.intBitsToFloat(rc.readBroadcast(MAP_ORIGIN_X_CHANNEL));
            float y = Float.intBitsToFloat(rc.readBroadcast(MAP_ORIGIN_Y_CHANNEL));
            commMapOrigin = new MapLocation(x, y);
        }

        // channel math utilities

        private static int wrapA(int a) {
            return ((a % MAP_SIZE_A) + MAP_SIZE_A) % MAP_SIZE_A;
        }

        private static int wrapB(int b) {
            return ((b % MAP_SIZE_B) + MAP_SIZE_B) % MAP_SIZE_B;
        }

        private static int hexCoordToEntryIndex(int a, int b) {
            int wrappedA = wrapA(a);
            int wrappedB = wrapB(b);
            return (wrappedB * MAP_SIZE_A) + wrappedA;
        }

        private static int unwrapB(int wrappedB) {
            int minB = minBOnKnownMap();
            int minBquotient = Math.floorDiv(minB, MAP_SIZE_B);
            wrappedB += (minBquotient * MAP_SIZE_B);
            if (wrappedB < minB) {
                wrappedB += MAP_SIZE_B;
            }
            return wrappedB;
        }

        private static int unwrapAGivenB(int wrappedA, int b) {
            int minA = minAOnKnownMapGivenB(b);
            int minAquotient = Math.floorDiv(minA, MAP_SIZE_A);
            wrappedA += (minAquotient * MAP_SIZE_A);
            if (wrappedA < minA) {
                wrappedA += MAP_SIZE_A;
            }
            return wrappedA;
        }

        private static HexCoord entryIndexToHexCoord(int entryIndex) {
            int wrappedB = entryIndex / MAP_SIZE_A;
            int wrappedA = entryIndex % MAP_SIZE_A;
            int b = unwrapB(wrappedB);
            int a = unwrapAGivenB(wrappedA, b);
            return new HexCoord(a, b);
        }

        // coordinate math utilities

        public static boolean hexCoordOverlapsKnownMap(int a, int b) {
            return b <= maxBOnKnownMap() && a <= maxAOnKnownMapGivenB(b);
        }

        public static int maxBOnKnownMap() {
            float maxBExact = ((knownMapBounds.getInnerBound(MapBounds.NORTH) - commMapOrigin.y) + CELL_RADIUS) / (bDir.getDeltaY(CELL_RESOLUTION));
            return (int)Math.floor(maxBExact);
        }

        public static int minBOnKnownMap() {
            float minBExact = ((knownMapBounds.getInnerBound(MapBounds.SOUTH) - commMapOrigin.y) - CELL_RADIUS) / (bDir.getDeltaY(CELL_RESOLUTION));
            return (int)Math.ceil(minBExact);
        }

        public static int maxAOnKnownMapGivenB(int b) {
            b = unwrapB(b);
            MapLocation start = commMapOrigin.add(bDir, b);
            float maxAExact = ((knownMapBounds.getInnerBound(MapBounds.EAST) - start.x) + CELL_RADIUS) / CELL_RESOLUTION;
            return (int)Math.floor(maxAExact);
        }

        public static int minAOnKnownMapGivenB(int b) {
            b = unwrapB(b);
            MapLocation start = commMapOrigin.add(bDir, b);
            float minAExact = ((knownMapBounds.getInnerBound(MapBounds.WEST) - start.x) - CELL_RADIUS) / CELL_RESOLUTION;
            return (int)Math.ceil(minAExact);
        }

        public static MapLocation hexCoordToLoc(int a, int b) {
            return commMapOrigin.add(aDir, a * CELL_RESOLUTION).add(bDir, b * CELL_RESOLUTION);
        }

        public static float[] exactHexCoord(MapLocation loc) {
            float bExact = (loc.y - commMapOrigin.y) / bDir.getDeltaY(CELL_RESOLUTION);
            float aExact = (loc.x - (commMapOrigin.x + bDir.getDeltaX(bExact * CELL_RESOLUTION))) / aDir.getDeltaX(CELL_RESOLUTION);
            return new float[]{aExact, bExact};
        }

        public static HexCoord nearestHexCoord(MapLocation loc) {
            float[] exactCoord = exactHexCoord(loc);
            int aFloor = (int)Math.floor(exactCoord[0]);
            int aCeil = (int)Math.ceil(exactCoord[0]);
            int bFloor = (int)Math.floor(exactCoord[1]);
            int bCeil = (int)Math.ceil(exactCoord[1]);
            HexCoord[] coords = new HexCoord[]{
                    new HexCoord(aFloor, bFloor),
                    new HexCoord(aFloor, bCeil),
                    new HexCoord(aCeil, bFloor),
                    new HexCoord(aCeil, bCeil),
            };
            float[] dists = new float[]{
                    loc.distanceTo(hexCoordToLoc(aFloor, bFloor)),
                    loc.distanceTo(hexCoordToLoc(aFloor, bCeil)),
                    loc.distanceTo(hexCoordToLoc(aCeil, bFloor)),
                    loc.distanceTo(hexCoordToLoc(aCeil, bCeil))
            };
            float minDist = 9999999;
            HexCoord minDistCoord = null;
            for (int i = 0; i < 4; i++) {
                float dist = dists[i];
                if (dist < minDist) {
                    minDist = dist;
                    minDistCoord = coords[i];
                }
            }
            return minDistCoord;
        }

        public static int[] circleHexBounds(MapLocation loc, float r) {
            r = r * 1.15470053838f;
            float[] exactHexCoord = exactHexCoord(loc);
            float minAExact = exactHexCoord[0] - (r / CELL_RESOLUTION);
            float maxAExact = exactHexCoord[0] + (r / CELL_RESOLUTION);
            float minBExact = exactHexCoord[1] - (r / CELL_RESOLUTION);
            float maxBExact = exactHexCoord[1] + (r / CELL_RESOLUTION);
            float minSumExact = exactHexCoord[0] + exactHexCoord[1] - (r / CELL_RESOLUTION);
            float maxSumExact = exactHexCoord[0] + exactHexCoord[1] + (r / CELL_RESOLUTION);
            int minA = (int)Math.ceil(minAExact);
            int maxA = (int)Math.floor(maxAExact);
            int minB = (int)Math.ceil(minBExact);
            int maxB = (int)Math.floor(maxBExact);
            int minSum = (int)Math.ceil(minSumExact);
            int maxSum = (int)Math.floor(maxSumExact);
            return new int[]{minA, maxA, minB, maxB, minSum, maxSum};
        }

        // Cell interface

        public static Cell queryNearestCell(MapLocation loc) throws GameActionException {
            HexCoord coord = nearestHexCoord(loc);
            return queryCell(coord.a, coord.b);
        }

        public static Cell queryCell(int a, int b) throws GameActionException {
            MapLocation loc = hexCoordToLoc(a, b);
            int entryIndex = hexCoordToEntryIndex(a, b);
            int entryChannel = MAP_CHANNEL + (entryIndex * MAP_ENTRY_SIZE);
            int flags = rc.readBroadcast(entryChannel);
            boolean explored = (flags & MAP_CELL_EXPLORED_MASK) != 0;
            boolean clear = (flags & MAP_CELL_CLEAR_MASK) != 0;
            return new Cell(a, b, loc, explored, clear);
        }

        public static void sendCell(Cell cell) throws GameActionException {
            int entryIndex = hexCoordToEntryIndex(cell.a, cell.b);
            int entryChannel = MAP_CHANNEL + (entryIndex * MAP_ENTRY_SIZE);
            int flags = 0;
            if (cell.isExplored()) {
                flags = flags | MAP_CELL_EXPLORED_MASK;
            }
            if (cell.isClear()) {
                flags = flags | MAP_CELL_CLEAR_MASK;
            }
            rc.broadcast(entryChannel, flags);
        }
    }

    public static class CommArrayQueue {

        private final int channel;
        private final int numElements;
        private final int elementSize;
        private final int startChannel;
        private final int countChannel;

        public CommArrayQueue(int channel, int elementSize, int numElements, int startChannel, int countChannel) {
            this.channel = channel;
            this.numElements = numElements;
            this.elementSize = elementSize;
            this.startChannel = startChannel;
            this.countChannel = countChannel;
        }

        public int count() throws GameActionException {
            return rc.readBroadcast(countChannel);
        }

        public int[] peek() throws GameActionException {
            int begin = rc.readBroadcast(startChannel);
            int count = rc.readBroadcast(countChannel);
            if (count <= 0) {
                return null;
            }
            int itemIndex = begin;
            int itemChannel = channel + (itemIndex * elementSize);
            return readBroadcastArray(itemChannel, elementSize);
        }

        public int[] peek(int i) throws GameActionException {
            int begin = rc.readBroadcast(startChannel);
            int count = rc.readBroadcast(countChannel);
            if (i >= count) {
                return null;
            }
            int itemIndex = (begin + i) % numElements;
            int itemChannel = channel + (itemIndex * elementSize);
            return readBroadcastArray(itemChannel, elementSize);
        }

        public void add(int[] item) throws GameActionException {
            int begin = rc.readBroadcast(startChannel);
            int count = rc.readBroadcast(countChannel);
            if (count >= numElements) {
                debug_print("Array queue overflow!");
                return;
            }
            int itemIndex = (begin + count) % numElements;
            int itemChannel = channel + itemIndex;
            writeBroadcastArray(itemChannel, item);
            count++;
            rc.broadcast(countChannel, count);
        }

        public int[] pop() throws GameActionException {
            int begin = rc.readBroadcast(startChannel);
            int count = rc.readBroadcast(countChannel);
            if (count <= 0) {
                return null;
            }
            int itemIndex = begin;
            int itemChannel = channel + (itemIndex * elementSize);
            int[] item = readBroadcastArray(itemChannel, elementSize);
            begin = (begin + 1) % numElements;
            rc.broadcast(startChannel, begin);
            count = count - 1;
            rc.broadcast(countChannel, count);
            return item;
        }

        public void remove() throws GameActionException {
            int begin = rc.readBroadcast(startChannel);
            int count = rc.readBroadcast(countChannel);
            if (count > 0) {
                begin = (begin + 1) % numElements;
                rc.broadcast(startChannel, begin);
                count = count - 1;
                rc.broadcast(countChannel, count);
            } else {
                debug_print("Array queue removed with no elements!");
            }
        }

        public void clear() throws GameActionException {
            rc.broadcast(countChannel, 0);
        }
    }

    public static class FarmTableEntry {
        private static final int EXPLORED_MASK = 0x1;
        private static final int GARDENER_REGISTER_MASK = 0x2;
        private static final int LUMBERJACK_REGISTER_MASK = 0x4;
        private static final int FULL_MASK = 0x8;
        private static final int GARDENER_STORE_MASK = 0x10;
        private static final int LUMBERJACK_STORE_MASK = 0x20;
        private static final int READY_MASK = 0x40;
        private static final int CLEAR_MASK = 0x80;
        private static final int ACTIVE_MASK = 0x100;
        private static final int ARRIVED_MASK = 0x200;
        private static final int BUILD_DIR_MASK = 0x3C00;
        private static final int BUILD_DIR_OFFSET = 10;

        private int flags;

        public FarmTableEntry() {
            this(0);
        }

        public FarmTableEntry(int flags) {
            this.flags = flags;
        }

        public void setExplored() {
            flags |= EXPLORED_MASK;
        }

        public void resetExplored() {
            flags &= ~EXPLORED_MASK;
        }

        public boolean isExplored() {
            return (flags & EXPLORED_MASK) != 0;
        }

        public void setFull() {
            flags |= FULL_MASK;
        }

        public void resetFull() {
            flags &= ~FULL_MASK;
        }

        public boolean isFull() {
            return (flags & FULL_MASK) != 0;
        }

        public void setReady() {
            flags |= READY_MASK;
        }

        public void resetReady() {
            flags &= ~READY_MASK;
        }

        public boolean isReady() {
            return (flags & READY_MASK) != 0;
        }

        public void setClear() {
            flags |= CLEAR_MASK;
        }

        public void resetClear() {
            flags &= ~CLEAR_MASK;
        }

        public boolean isClear() {
            return (flags & CLEAR_MASK) != 0;
        }

        public void setActive() {
            flags |= ACTIVE_MASK;
        }

        public void resetActive() {
            flags &= ~ACTIVE_MASK;
        }

        public boolean isActive() {
            return (flags & ACTIVE_MASK) != 0;
        }

        public void setArrived() {
            flags |= ARRIVED_MASK;
        }

        public void resetArrived() {
            flags &= ~ARRIVED_MASK;
        }

        public boolean isArrived() {
            return (flags & ARRIVED_MASK) != 0;
        }

        public void setBuildDir(int buildDir) {
            flags &= ~BUILD_DIR_MASK;
            flags |= (buildDir << BUILD_DIR_OFFSET) & BUILD_DIR_MASK;
        }

        public int getBuildDir() {
            return (flags & BUILD_DIR_MASK) >> BUILD_DIR_OFFSET;
        }

        public boolean hasGardenerRegistered() {
            return (flags & GARDENER_REGISTER_MASK) != 0;
        }

        public boolean hasGardenerStored() {
            return (flags & GARDENER_STORE_MASK) != 0;
        }

        public void registerGardener() {
            flags |= GARDENER_REGISTER_MASK;
        }

        public boolean hasLumberjackRegistered() {
            return (flags & LUMBERJACK_REGISTER_MASK) != 0;
        }

        public boolean hasLumberjackStored() {
            return (flags & LUMBERJACK_STORE_MASK) != 0;
        }

        public void registerLumberjack() {
            flags |= LUMBERJACK_REGISTER_MASK;
        }

        public void storeRegisters() {
            if (hasGardenerRegistered()) {
                flags |= GARDENER_STORE_MASK;
            }
            if (hasLumberjackRegistered()) {
                flags |= LUMBERJACK_STORE_MASK;
            }
            flags &= ~(GARDENER_REGISTER_MASK | LUMBERJACK_REGISTER_MASK);
        }
    }


    public enum GardenerSchedule {ONCE_EVERY_N_ROUNDS, WHEN_FULL}

    public enum ScoutMode {HARASS, COLLECT}

    public static RobotController rc;

    // Channel constants
    public static final int EXEC_ROUND_CHANNEL = 0;
    public static final int ARCHON_COUNTER_CHANNEL = EXEC_ROUND_CHANNEL + 1;
    public static final int NUM_ARCHONS_CHANNEL = ARCHON_COUNTER_CHANNEL + 1;
    public static final int ARCHON_LOCATION_TABLE_CHANNEL = NUM_ARCHONS_CHANNEL + 1;
    public static final int ARCHON_LOCATION_TABLE_ENTRY_SIZE = 2;
    public static final int ARCHON_LOCATION_TABLE_NUM_ENTRIES = 3;
    public static final int ARCHON_LOCATION_TABLE_LENGTH = ARCHON_LOCATION_TABLE_ENTRY_SIZE * ARCHON_LOCATION_TABLE_NUM_ENTRIES;
    public static final int FARM_TABLE_CHANNEL = ARCHON_LOCATION_TABLE_CHANNEL + ARCHON_LOCATION_TABLE_LENGTH;
    public static final int FARM_TABLE_ENTRY_SIZE = 1;
    public static final int FARM_TABLE_NUM_ENTRIES = 64;
    public static final int FARM_TABLE_LENGTH = FARM_TABLE_ENTRY_SIZE * FARM_TABLE_NUM_ENTRIES;
    public static final int FARMS_NEED_G_LIST_CHANNEL = FARM_TABLE_CHANNEL + FARM_TABLE_LENGTH;
    public static final int FARMS_NEED_G_LIST_LENGTH = 64;
    public static final int FARMS_NEED_G_LIST_START_CHANNEL = FARMS_NEED_G_LIST_CHANNEL + FARMS_NEED_G_LIST_LENGTH;
    public static final int FARMS_NEED_G_LIST_COUNT_CHANNEL = FARMS_NEED_G_LIST_START_CHANNEL + 1;
    public static final int FARMS_NEED_LJ_LIST_CHANNEL = FARMS_NEED_G_LIST_COUNT_CHANNEL + 1;
    public static final int FARMS_NEED_LJ_LIST_LENGTH = 64;
    public static final int FARMS_NEED_LJ_LIST_START_CHANNEL = FARMS_NEED_LJ_LIST_CHANNEL + FARMS_NEED_LJ_LIST_LENGTH;
    public static final int FARMS_NEED_LJ_LIST_COUNT_CHANNEL = FARMS_NEED_LJ_LIST_START_CHANNEL + 1;
    public static final int FARMS_EXPLORED_LIST_CHANNEL = FARMS_NEED_LJ_LIST_COUNT_CHANNEL + 1;
    public static final int FARMS_EXPLORED_LIST_LENGTH = 64;
    public static final int FARMS_EXPLORED_LIST_START_CHANNEL = FARMS_EXPLORED_LIST_CHANNEL + FARMS_EXPLORED_LIST_LENGTH;
    public static final int FARMS_EXPLORED_LIST_COUNT_CHANNEL = FARMS_EXPLORED_LIST_START_CHANNEL + 1;
    public static final int FARMS_ACTIVE_LIST_CHANNEL = FARMS_EXPLORED_LIST_COUNT_CHANNEL + 1;
    public static final int FARMS_ACTIVE_LIST_LENGTH = 64;
    public static final int FARMS_ACTIVE_LIST_START_CHANNEL = FARMS_ACTIVE_LIST_CHANNEL + FARMS_ACTIVE_LIST_LENGTH;
    public static final int FARMS_ACTIVE_LIST_COUNT_CHANNEL = FARMS_ACTIVE_LIST_START_CHANNEL + 1;
    public static final int BOUNDS_TABLE_CHANNEL = FARMS_ACTIVE_LIST_COUNT_CHANNEL + 1; // IN_W, IN_E, IN_S, IN_N, OUT_W, OUT_E, OUT_S, OUT_N
    public static final int BOUNDS_TABLE_LENGTH = 8;
    public static CommArrayQueue gardenerJobsQueue = new CommArrayQueue(FARMS_NEED_G_LIST_CHANNEL, 1, FARMS_NEED_G_LIST_LENGTH, FARMS_NEED_G_LIST_START_CHANNEL, FARMS_NEED_G_LIST_COUNT_CHANNEL);
    public static CommArrayQueue lumberjackJobsQueue = new CommArrayQueue(FARMS_NEED_LJ_LIST_CHANNEL, 1, FARMS_NEED_LJ_LIST_LENGTH, FARMS_NEED_LJ_LIST_START_CHANNEL, FARMS_NEED_LJ_LIST_COUNT_CHANNEL);
    public static CommArrayQueue exploredFarmsQueue = new CommArrayQueue(FARMS_EXPLORED_LIST_CHANNEL, 1, FARMS_EXPLORED_LIST_LENGTH, FARMS_EXPLORED_LIST_START_CHANNEL, FARMS_EXPLORED_LIST_COUNT_CHANNEL);
    public static CommArrayQueue activeFarmsQueue = new CommArrayQueue(FARMS_ACTIVE_LIST_CHANNEL, 1, FARMS_ACTIVE_LIST_LENGTH, FARMS_ACTIVE_LIST_START_CHANNEL, FARMS_ACTIVE_LIST_COUNT_CHANNEL);
    public static final int BUILD_QUEUE_1_CHANNEL = BOUNDS_TABLE_CHANNEL + BOUNDS_TABLE_LENGTH;
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
    public static final int ATTACK_LOCATION_QUEUE_CHANNEL = WHICH_ARCHON_MAKES_GARDENERS_CHANNEL + 1;
    public static final int ATTACK_LOCATION_QUEUE_ENTRY_SIZE = 2;
    public static final int ATTACK_LOCATION_QUEUE_NUM_ENTRIES = 5;
    public static final int ATTACK_LOCATION_QUEUE_LENGTH = ATTACK_LOCATION_QUEUE_ENTRY_SIZE * ATTACK_LOCATION_QUEUE_NUM_ENTRIES;
    public static final int ATTACK_LOCATION_QUEUE_BEGIN_CHANNEL = ATTACK_LOCATION_QUEUE_CHANNEL + ATTACK_LOCATION_QUEUE_LENGTH;
    public static final int ATTACK_LOCATION_QUEUE_COUNT_CHANNEL = ATTACK_LOCATION_QUEUE_BEGIN_CHANNEL + 1;
    public static final int DEFEND_LOCATION_QUEUE_CHANNEL = ATTACK_LOCATION_QUEUE_COUNT_CHANNEL + 1;
    public static final int DEFEND_LOCATION_QUEUE_ENTRY_SIZE = 2;
    public static final int DEFEND_LOCATION_QUEUE_NUM_ENTRIES = 5;
    public static final int DEFEND_LOCATION_QUEUE_LENGTH = DEFEND_LOCATION_QUEUE_ENTRY_SIZE * DEFEND_LOCATION_QUEUE_NUM_ENTRIES;
    public static final int DEFEND_LOCATION_QUEUE_BEGIN_CHANNEL = DEFEND_LOCATION_QUEUE_CHANNEL + DEFEND_LOCATION_QUEUE_LENGTH;
    public static final int DEFEND_LOCATION_QUEUE_COUNT_CHANNEL = DEFEND_LOCATION_QUEUE_BEGIN_CHANNEL + 1;
    public static final int NEW_SCOUT_MODE_CHANNEL = DEFEND_LOCATION_QUEUE_COUNT_CHANNEL + 1;
    public static final int OVERRIDE_SCOUT_MODE_CHANNEL = NEW_SCOUT_MODE_CHANNEL + 1;
    private static final int MAP_ORIGIN_X_CHANNEL = OVERRIDE_SCOUT_MODE_CHANNEL + 1;
    private static final int MAP_ORIGIN_Y_CHANNEL = MAP_ORIGIN_X_CHANNEL + 1;
    private static final int MAP_CHANNEL = MAP_ORIGIN_Y_CHANNEL + 1;
    private static final int MAP_ENTRY_SIZE = 1;
    private static final int MAP_SIZE_A = 51;
    private static final int MAP_SIZE_B = 59;
    private static final int MAP_NUM_ENTRIES = MAP_SIZE_A * MAP_SIZE_B;
    private static final int MAP_LENGTH = MAP_ENTRY_SIZE * MAP_NUM_ENTRIES;
    private static final int FIRST_FARM_EXISTS_CHANNEL = MAP_CHANNEL + MAP_LENGTH;
    private static final int FIRST_FARM_LOC_X_CHANNEL = FIRST_FARM_EXISTS_CHANNEL + 1;
    private static final int FIRST_FARM_LOC_Y_CHANNEL = FIRST_FARM_LOC_X_CHANNEL + 1;
    private static final int CURRENT_FARM_NUM_X_CHANNEL = FIRST_FARM_LOC_Y_CHANNEL + 1;
    private static final int CURRENT_FARM_NUM_Y_CHANNEL = CURRENT_FARM_NUM_X_CHANNEL + 1;
    public static final int GARDENER_COUNTER_CHANNEL = CURRENT_FARM_NUM_Y_CHANNEL + 1;
    public static final int LUMBERJACK_COUNTER_CHANNEL = GARDENER_COUNTER_CHANNEL + 1;
    public static final int SCOUT_COUNTER_CHANNEL = LUMBERJACK_COUNTER_CHANNEL + 1;
    public static final int SOLDIER_COUNTER_CHANNEL = SCOUT_COUNTER_CHANNEL + 1;
    public static final int TANK_COUNTER_CHANNEL = SOLDIER_COUNTER_CHANNEL + 1;
    public static final int GARDENER_NUM_CHANNEL = TANK_COUNTER_CHANNEL + 1;
    public static final int LUMBERJACK_NUM_CHANNEL = GARDENER_NUM_CHANNEL + 1;
    public static final int SCOUT_NUM_CHANNEL = LUMBERJACK_NUM_CHANNEL + 1;
    public static final int SOLDIER_NUM_CHANNEL = SCOUT_NUM_CHANNEL + 1;
    public static final int TANK_NUM_CHANNEL = SOLDIER_NUM_CHANNEL + 1;
    public static final int GARDENER_BUILT_CHANNEL = TANK_NUM_CHANNEL + 1;
    public static final int LUMBERJACK_BUILT_CHANNEL = GARDENER_BUILT_CHANNEL + 1;
    public static final int SCOUT_BUILT_CHANNEL = LUMBERJACK_BUILT_CHANNEL + 1;
    public static final int SOLDIER_BUILT_CHANNEL = SCOUT_BUILT_CHANNEL + 1;
    public static final int TANK_BUILT_CHANNEL = SOLDIER_BUILT_CHANNEL + 1;
    public static final int CAN_PLANT_COUNTER_CHANNEL = TANK_BUILT_CHANNEL + 1;
    public static final int CAN_PLANT_NUM_CHANNEL = CAN_PLANT_COUNTER_CHANNEL + 1;

    // Performance constants
    public static final int DESIRED_ROBOTS = 20;
    public static final int DESIRED_TREES = 20;
    public static final int DESIRED_BULLETS = 20;
    public static final float FARM_DIST = 8f;
    
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
    public static HexCoord myHexCoord;
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
    public static float vpCost = 7.5f;

    // Results of further processing
    private static RobotInfo nearestEnemy = null;
    private static RobotInfo nearestEnemyLumberjack = null;
    private static RobotInfo nearestEnemyShooter = null;
    private static RobotInfo nearestEnemyGardener = null;
    private static RobotInfo nearestEnemyHostile = null;
    private static RobotInfo nearestEnemyNonHostile = null;
    private static TreeInfo nearestTree = null;
    private static TreeInfo nearestRobotTree = null;
    private static TreeInfo nearestFriendlyTree = null;
    private static TreeInfo nearestUnfriendlyTree = null;
    private static TreeInfo lowestFriendlyTree = null;
    private static BulletInfo[] bulletsToAvoid = new BulletInfo[0];
    private static int numBulletsToAvoid = 0;
    public static boolean isLeader = false;
    public static int birthTurn = -1;
    public static boolean firstTurn = true;

    // Special stored values
    private static boolean circleClockwise = true;
    private static int[] debugBytecodesList = new int[100];
    private static int numDebugBytecodes = 0;
    private static boolean debugTripped = false;
    private static GardenerSchedule gardenerSchedule = GardenerSchedule.ONCE_EVERY_N_ROUNDS;
    private static int gardenerScheduleN = -1;
    private static boolean experimental = false;
    private static boolean lateLumberjacks = false;
    private static boolean scoutWhenFull = false;
    private static RobotType[] initialBuildQueue1 = new RobotType[0];
    private static int initialBuildQueue1index = 0;
    private static RobotType[] initialBuildQueue2 = new RobotType[0];
    private static RobotType initialDefaultBuild = null;
    private static HexCoord lastScoutHexCoord;
    private static HexCoord[] toScoutCoordBuffer = new HexCoord[20];
    private static int toScoutCoordBufferStart = 0;
    private static int toScoutCoordBufferCount = 0;
    private static boolean useFarmGrid = false;
    public static boolean debugExceptions = true;
    public static boolean debug = false;

    // Configuration for Offensive Units
    public static boolean useTriad = false;
    public static boolean usePentad = false;
    public static float triadDist = 2.75f;
    public static float pentadDist = 2.25f;
    public static boolean friendlyFireOn = true;
    public static boolean prioritizeRobotTrees = false;
    public static boolean kiteEnemyLumberjacks = true;
    public static float avoidRadius = 1.1f;
    public static boolean kite = false;
    public static boolean kiteSoldiers = false;
    public static boolean kiteScouts = false;
    public static boolean kiteTanks = false;
    public static float attackCircleStart = 15f;
    public static float attackCircleChange = 0.125f;
    public static boolean ceaseFire = false;
    
    // Configuration for Gardeners Units
    public static boolean earlyLumberjacks = false;
    public static int earlyLJRounds = 500;
    

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
            debug_print("Restarted the same turn!");
        } else if (!neverUpdated && newRoundNum - roundNum != 1) {
            debug_print("Skipped a turn!");
        }
        roundNum = newRoundNum;
        if (birthTurn == -1) {
            birthTurn = roundNum;
        } else {
            firstTurn = false;
        }
        victoryPoints = rc.getTeamVictoryPoints();
        treeCount = rc.getTreeCount();
        teamBullets = rc.getTeamBullets();
        vpCost = rc.getVictoryPointCost();

        updateNearbyRobots();
        updateNearbyTrees();
        updateNearbyBullets();

        knownMapBounds = getMapBounds();
        updateMapBounds(knownMapBounds);

        CommMap.queryOrigin();
        myHexCoord = CommMap.nearestHexCoord(myLoc);

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
        nearestEnemyHostile = null;
        float minEnemyHostileDist = 99999999;
        nearestEnemyNonHostile = null;
        float minEnemyNonHostileDist = 99999999;
        int numIters = Math.min(nearbyRobots.length, DESIRED_ROBOTS);
        for (int i = 0; i < numIters; i++) {
            RobotInfo robot = nearbyRobots[i];
            if (robot.getTeam() == enemyTeam) {
                float dist = myLoc.distanceTo(robot.getLocation()) - robot.getRadius();
                if (dist < minEnemyDist) {
                    nearestEnemy = robot;
                    minEnemyDist = dist;
                }
                if (robot.type == RobotType.LUMBERJACK || robot.type == RobotType.SOLDIER || robot.type == RobotType.TANK || robot.type == RobotType.SCOUT) {
                    if (dist < minEnemyHostileDist) {
                        nearestEnemyHostile = robot;
                        minEnemyHostileDist = dist;
                    }
                    if (robot.type == RobotType.LUMBERJACK) {
                        if (dist < minEnemyLumberjackDist) {
                            nearestEnemyLumberjack = robot;
                            minEnemyLumberjackDist = dist;
                        }
                    } else {
                        if (dist < minEnemyShooterDist) {
                            nearestEnemyShooter = robot;
                            minEnemyShooterDist = dist;
                        }
                    }
                } else {
                    if (dist < minEnemyNonHostileDist) {
                        nearestEnemyNonHostile = robot;
                        minEnemyNonHostileDist = dist;
                    }
                    if (robot.type == RobotType.GARDENER) {
                        if (dist < minEnemyGardenerDist) {
                            nearestEnemyGardener = robot;
                            minEnemyGardenerDist = dist;
                        }
                    }
                }
            }
        }
    }

    public static void processNearbyTrees() throws GameActionException {
        boolean explore = queryFirstFarmExists();
        int farmNum = -1;
        MapLocation farmToExplore = null;
        boolean farmReady = true;
        boolean farmClear = true;
        boolean[] buildDirsBlocked = new boolean[16];
        if (explore) {
            farmNum = queryCurrentFarmNum();
            debug_print("current farm num = " + farmNum);
            farmToExplore = null;
            if (canExploreFarm(farmNum)) {
                farmToExplore = farmNumToLoc(farmNum);
                debug_print("can explore at " + farmToExplore);
            } else {
                debug_print("can't explore");
            }
        }
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
            if (farmToExplore != null) {
                float farmTreeDist = farmToExplore.distanceTo(tree.getLocation()) - tree.radius;
                if (farmTreeDist < ProposedFarm.hexFarmRadius + 2.05f) {
                    for (int iDir = 0; iDir < 16; iDir++) {
                        MapLocation checkLoc = farmToExplore.add(usefulDirections[iDir], 4);
                        if (rc.isCircleOccupiedExceptByThisRobot(checkLoc, 1)) {
                            buildDirsBlocked[iDir] = true;
                        }
                    }
                    if (farmTreeDist < ProposedFarm.hexFarmRadius) {
                        farmClear = false;
                        if (farmTreeDist < RobotType.GARDENER.bodyRadius) {
                            farmReady = false;
                        }
                    }
                }
            }
        }
        if (farmToExplore != null) {
            sendFarmExploredInfo(farmNum, farmReady, farmClear, buildDirsBlocked);
            incrementCurrentFarmLoc();
            exploredFarmsQueue.add(new int[]{farmNum});
        }
    }
    
    public static RobotInfo[] nearbyFriendlyGardeners() { // locate friendly gardeners
    	RobotInfo[] nearbyFriendly = rc.senseNearbyRobots(-1, rc.getTeam());
    	int len = nearbyFriendly.length;
    	
    	if (len < 1) {
    		debug_print("No nearby friendlies!");
    		return null;
    	}
    	
    	RobotInfo[] fgArray = new RobotInfo[len];
    	int fgI = 0;
    	for (int i = 0; i < len; i++) {
    		RobotInfo fr = nearbyFriendly[i];
    		if (fr.type == RobotType.GARDENER) { // if gardener
    			fgArray[fgI] = fr; // store in fgArray
    			fgI ++;
    		}
    		
    		//debug_print("FGs: " + fgI);
    	}
    	
    	if (fgArray.length < 1) return null;
    	
    	return fgArray;
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

    public static void elections() throws GameActionException {
        int lastExecRound = rc.readBroadcast(EXEC_ROUND_CHANNEL);
        if (roundNum == lastExecRound) {
            isLeader = false;
        } else if (roundNum > lastExecRound) {
            isLeader = true;
            debug_print("I am the leader!");
        } else {
            debug_print("Elections error");
        }
        rc.broadcast(EXEC_ROUND_CHANNEL, roundNum);
    }

    public static void leadIfLeader() throws GameActionException {
        if (isLeader) {
            lead();
        }
    }

    public static void lead() throws GameActionException {
        leaderVP();
        if (firstTurn) {
            for (MapLocation attackLoc : enemyInitialArchonLocations) {
                addAttackLocation(attackLoc);
            }
        }
        sendScoutMode(ScoutMode.HARASS, false);


        MapLocation[] myArchonLocations;
        if (firstTurn) {
            myArchonLocations = myInitialArchonLocations;
        } else {
            myArchonLocations = getMyArchonLocations();
        }
        int maxMinDistArchon = -1;
        float maxMinDist = -1f;
        for (int i = 0; i < myArchonLocations.length; i++) {
            MapLocation loc = myArchonLocations[i];
            float minDist = minDistBetween(loc, enemyInitialArchonLocations);
            if (minDist > maxMinDist) {
                maxMinDist = minDist;
                maxMinDistArchon = i;
            }
        }
        rc.broadcast(WHICH_ARCHON_MAKES_GARDENERS_CHANNEL, maxMinDistArchon);

        leaderStoreCounters();

        leaderClearOrders(); // clears build orders and farming orders
        leaderManageActiveFarms(); // sends farming jobs
        leaderOrderBuilds(); // sends build orders (reads farming jobs)
    }

    private static void leaderOrderBuilds() throws GameActionException {
        float bulletsLeft = teamBullets;

        while (initialBuildQueue1index < initialBuildQueue1.length) {
            RobotType buildOrder = initialBuildQueue1[initialBuildQueue1index];
            if (bulletsLeft >= buildOrder.bulletCost) {
                addBuildQueue1(buildOrder);
                bulletsLeft -= buildOrder.bulletCost;
            } else {
                break;
            }
            initialBuildQueue1index++;
        }

        int numLumberjackJobs = lumberjackJobsQueue.count();
        for (int i = 0; i < numLumberjackJobs; i++) {
            RobotType buildOrder = RobotType.LUMBERJACK;
            if (bulletsLeft >= buildOrder.bulletCost) {
                addBuildQueue1(buildOrder);
                bulletsLeft -= buildOrder.bulletCost;
            } else {
                break;
            }
        }

        /*
        int numGardenerJobs = gardenerJobsQueue.count();
        for (int i = 0; i < numGardenerJobs; i++) {
            RobotType buildOrder = RobotType.GARDENER;
            if (bulletsLeft >= buildOrder.bulletCost) {
                addBuildQueue1(buildOrder);
                bulletsLeft -= buildOrder.bulletCost;
            } else {
                break;
            }
        }
        */

        if (initialDefaultBuild != null) {
            while (bulletsLeft >= initialDefaultBuild.bulletCost) {
                addBuildQueue1(initialDefaultBuild);
                bulletsLeft -= initialDefaultBuild.bulletCost;
            }
        }
    }

    public static void registerArchon() throws GameActionException {
        rc.broadcast(ARCHON_COUNTER_CHANNEL, rc.readBroadcast(ARCHON_COUNTER_CHANNEL) + 1);
    }
    
    public static void registerGardener() throws GameActionException {
        rc.broadcast(GARDENER_COUNTER_CHANNEL, rc.readBroadcast(GARDENER_COUNTER_CHANNEL) + 1);
    }

    public static void registerLumberjack() throws GameActionException {
        rc.broadcast(LUMBERJACK_COUNTER_CHANNEL, rc.readBroadcast(LUMBERJACK_COUNTER_CHANNEL) + 1);
    }

    public static void registerSoldier() throws GameActionException {
        rc.broadcast(SOLDIER_COUNTER_CHANNEL, rc.readBroadcast(SOLDIER_COUNTER_CHANNEL) + 1);
    }

    public static void registerTank() throws GameActionException {
        rc.broadcast(TANK_COUNTER_CHANNEL, rc.readBroadcast(TANK_COUNTER_CHANNEL) + 1);
    }

    public static void registerScout() throws GameActionException {
        rc.broadcast(SCOUT_COUNTER_CHANNEL, rc.readBroadcast(SCOUT_COUNTER_CHANNEL) + 1);
    }

    public static void leaderStoreCounters() throws GameActionException {
        rc.broadcast(NUM_ARCHONS_CHANNEL, rc.readBroadcast(ARCHON_COUNTER_CHANNEL));
        rc.broadcast(ARCHON_COUNTER_CHANNEL, 0);

        rc.broadcast(GARDENER_NUM_CHANNEL, rc.readBroadcast(GARDENER_COUNTER_CHANNEL));
        rc.broadcast(GARDENER_COUNTER_CHANNEL, 0);

        rc.broadcast(LUMBERJACK_NUM_CHANNEL, rc.readBroadcast(LUMBERJACK_COUNTER_CHANNEL));
        rc.broadcast(LUMBERJACK_COUNTER_CHANNEL, 0);

        rc.broadcast(SCOUT_NUM_CHANNEL, rc.readBroadcast(SCOUT_COUNTER_CHANNEL));
        rc.broadcast(SCOUT_COUNTER_CHANNEL, 0);

        rc.broadcast(SOLDIER_NUM_CHANNEL, rc.readBroadcast(SOLDIER_COUNTER_CHANNEL));
        rc.broadcast(SOLDIER_COUNTER_CHANNEL, 0);

        rc.broadcast(TANK_NUM_CHANNEL, rc.readBroadcast(TANK_COUNTER_CHANNEL));
        rc.broadcast(TANK_COUNTER_CHANNEL, 0);

        rc.broadcast(CAN_PLANT_NUM_CHANNEL, rc.readBroadcast(CAN_PLANT_COUNTER_CHANNEL));
        rc.broadcast(CAN_PLANT_COUNTER_CHANNEL, 0);
    }



    public static boolean canExploreFarm(int farmNum) throws GameActionException {
        return myLoc.distanceTo(farmNumToLoc(farmNum)) + ProposedFarm.hexFarmRadius + 2.05 <= myType.sensorRadius;
    }

    public static boolean queryFirstFarmExists() throws GameActionException {
        return rc.readBroadcast(FIRST_FARM_EXISTS_CHANNEL) != 0;
    }

    public static void sendFirstFarm(MapLocation loc) throws GameActionException {
        rc.broadcast(FIRST_FARM_EXISTS_CHANNEL, 1);
        rc.broadcast(FIRST_FARM_LOC_X_CHANNEL, Float.floatToIntBits(loc.x));
        rc.broadcast(FIRST_FARM_LOC_Y_CHANNEL, Float.floatToIntBits(loc.y));
    }

    public static MapLocation queryFirstFarmLoc() throws GameActionException {
        float x = Float.intBitsToFloat(rc.readBroadcast(FIRST_FARM_LOC_X_CHANNEL));
        float y = Float.intBitsToFloat(rc.readBroadcast(FIRST_FARM_LOC_Y_CHANNEL));
        return new MapLocation(x, y);
    }

    public static void incrementCurrentFarmLoc() throws GameActionException {
        int x = rc.readBroadcast(CURRENT_FARM_NUM_X_CHANNEL);
        int y = rc.readBroadcast(CURRENT_FARM_NUM_Y_CHANNEL);
        if (x == 0 && y == 0) {
            x++;
        } else {
            int sum = x + y;
            int diff = x - y;
            if (diff < 0) {
                if (sum < 0) {
                    y++;
                } else {
                    x++;
                }
            } else {
                if (sum > 0) {
                    y--;
                } else {
                    x--;
                }
            }
        }
        rc.broadcast(CURRENT_FARM_NUM_X_CHANNEL, x);
        rc.broadcast(CURRENT_FARM_NUM_Y_CHANNEL, y);
    }

    public static boolean farmLocsExhausted() throws GameActionException {
        boolean retval = queryCurrentFarmNum() < 0;
        if (retval) {
            debug_print("Farm locs exhausted");
        }
        return retval;
    }

    public static void makeCurrentFarmLocOnMap() throws GameActionException {
        MapLocation currentFarmLoc = queryCurrentFarmLoc();
        while (currentFarmLoc != null && !farmLocsExhausted() && !knownMapBounds.isInsideOuter(currentFarmLoc)) {
            incrementCurrentFarmLoc();
            currentFarmLoc = queryCurrentFarmLoc();
        }
    }

    public static MapLocation queryCurrentFarmLoc() throws GameActionException {
        if (farmLocsExhausted()) {
            return null;
        }
        MapLocation firstFarmLoc = queryFirstFarmLoc();
        int x = rc.readBroadcast(CURRENT_FARM_NUM_X_CHANNEL);
        int y = rc.readBroadcast(CURRENT_FARM_NUM_Y_CHANNEL);
        return firstFarmLoc.translate(x * FARM_DIST, y * FARM_DIST);
    }

    public static int queryCurrentFarmNum() throws GameActionException {
        int x = rc.readBroadcast(CURRENT_FARM_NUM_X_CHANNEL);
        int y = rc.readBroadcast(CURRENT_FARM_NUM_Y_CHANNEL);
        int farmNum = farmOffsetToNum(x, y);
        if (farmNum < 0 || farmNum >= 64) {
            debug_print("Farmnum error: x = " + x + ", y = " + y);
            return -1;
        }
        return farmNum;
    }

    public static int[] farmNumToOffset(int num) {
        int y = num / 8;
        int x = num % 8;
        y = y - 4;
        x = x - 4;
        return new int[]{x, y};
    }

    public static int farmOffsetToNum(int x, int y) {
        return ((y + 4) * 8) + (x + 4);
    }

    public static MapLocation farmNumToLoc(int num) throws GameActionException {
        int[] offset = farmNumToOffset(num);
        int x = offset[0];
        int y = offset[1];
        MapLocation firstFarmLoc = queryFirstFarmLoc();
        return firstFarmLoc.translate(x * FARM_DIST, y * FARM_DIST);
    }

    private static void toScoutCoordBuffer_add(HexCoord elem) {
        int index = (toScoutCoordBufferStart + toScoutCoordBufferCount) % toScoutCoordBuffer.length;
        toScoutCoordBuffer[index] = elem;
        if (toScoutCoordBufferCount >= toScoutCoordBuffer.length) {
            toScoutCoordBufferStart = (toScoutCoordBufferStart + 1) % toScoutCoordBuffer.length;
        } else {
            toScoutCoordBufferCount++;
        }
    }

    private static HexCoord toScoutCoordBuffer_peek() {
        return toScoutCoordBuffer[toScoutCoordBufferStart];
    }

    private static HexCoord toScoutCoordBuffer_pop() {
        if (toScoutCoordBufferCount <= 0) {
            return null;
        }
        toScoutCoordBufferCount--;
        int index = toScoutCoordBufferStart;
        toScoutCoordBufferStart = (toScoutCoordBufferStart + 1) % toScoutCoordBuffer.length;
        return toScoutCoordBuffer[index];
    }

    public static void scoutHex() throws GameActionException {
        int maxSteps = (int)Math.floor(((myType.sensorRadius - 1) / (2f /*/ 1.15470053838f*/)) /*+ 1.15470053838f*/);
        if (lastScoutHexCoord == null) {
            int minA = myHexCoord.a - maxSteps;
            int maxA = myHexCoord.a + maxSteps;
            int minB = myHexCoord.b - maxSteps;
            int maxB = myHexCoord.b + maxSteps;
            int minSum = myHexCoord.a + myHexCoord.b - maxSteps;
            int maxSum = myHexCoord.a + myHexCoord.b + maxSteps;

            for (int a = minA; a <= maxA; a++) {
                for (int b = Math.max(minB, (minSum - a)); b <= maxB && b <= (maxSum - a); b++) {
                    MapLocation loc = CommMap.hexCoordToLoc(a, b);
                    if (loc.distanceTo(RobotGlobal.myLoc) <= myType.sensorRadius - 1) {
                        if (rc.onTheMap(loc)) {
                            Cell c = CommMap.queryCell(a, b);
                            if (c.isExplored()) {
                                debug_dot(loc, 0, 0, 255);
                            } else {
                                c.setExplored(true);
                                if (rc.senseTreeAtLocation(loc) == null) {
                                    debug_dot(loc, 0, 255, 0);
                                    c.setClear(true);
                                } else {
                                    debug_dot(loc, 255, 0, 0);
                                    c.setClear(false);
                                }
                                CommMap.sendCell(c);
                            }
                        }
                    } else {
                        debug_dot(loc, 255, 255, 255);
                    }
                }
            }
        } else if (!myHexCoord.equals(lastScoutHexCoord)) {
            Direction dir = CommMap.hexCoordToLoc(lastScoutHexCoord.a, lastScoutHexCoord.b).directionTo(CommMap.hexCoordToLoc(myHexCoord.a, myHexCoord.b));
            HexCoord.ArcIterator it = HexCoord.hexArcIterator(myHexCoord.a, myHexCoord.b, maxSteps, dir);
            while (it.hasNext()) {
                HexCoord hc = it.next();
                MapLocation loc = CommMap.hexCoordToLoc(hc.a, hc.b);
                if (loc.distanceTo(RobotGlobal.myLoc) <= myType.sensorRadius - 1) {
                    if (rc.onTheMap(loc)) {
                        Cell c = CommMap.queryCell(hc.a, hc.b);
                        if (c.isExplored()) {
                            debug_dot(loc, 0, 0, 255);
                        } else {
                            c.setExplored(true);
                            if (rc.senseTreeAtLocation(loc) == null) {
                                debug_dot(loc, 0, 255, 0);
                                c.setClear(true);
                            } else {
                                debug_dot(loc, 255, 0, 0);
                                c.setClear(false);
                            }
                            CommMap.sendCell(c);
                        }
                    }
                } else {
                    debug_dot(loc, 255, 255, 255);
                    toScoutCoordBuffer_add(hc);
                }
            }
        }
        lastScoutHexCoord = myHexCoord;
        HexCoord leftoverCoord = toScoutCoordBuffer_pop();
        if (leftoverCoord != null) {
            MapLocation loc = CommMap.hexCoordToLoc(leftoverCoord.a, leftoverCoord.b);
            if (loc.distanceTo(RobotGlobal.myLoc) <= myType.sensorRadius - 1) {
                if (rc.onTheMap(loc)) {
                    Cell c = CommMap.queryCell(leftoverCoord.a, leftoverCoord.b);
                    if (c.isExplored()) {
                        debug_dot(loc, 0, 0, 255);
                    } else {
                        c.setExplored(true);
                        if (rc.senseTreeAtLocation(loc) == null) {
                            debug_dot(loc, 0, 255, 0);
                            c.setClear(true);
                        } else {
                            debug_dot(loc, 255, 0, 0);
                            c.setClear(false);
                        }
                        CommMap.sendCell(c);
                    }
                }
            } else {
                debug_dot(loc, 255, 255, 255);
                toScoutCoordBuffer_add(leftoverCoord);
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
    	debug_tick(1);
    	boolean moved = rc.hasMoved();
    	if (moved) {
    		debug_print("Already moved!");
    		return moved;
    	} 
    	if (enemy.type == RobotType.LUMBERJACK){ // ensure we avoid lumberjack strikes
    		strafeDist = Math.min(1.5f, strafeDist); 
    	}
    	if (enemy.type == RobotType.SOLDIER){ // ensure we avoid lumberjack strikes
    		if (!kiteSoldiers) return false;
    	}
    	if (enemy.type == RobotType.SCOUT){ // ensure we avoid lumberjack strikes
    		if (!kiteScouts) return false;
    	}
    	if (enemy.type == RobotType.TANK){ // ensure we avoid lumberjack strikes
    		if (!kiteTanks) return false;
    	}
    	if (enemy.type == RobotType.ARCHON || enemy.type == RobotType.GARDENER){ // dont kite gardeners or archons
    		return false; 
    	}
    	debug_tick(2);
    	float exclude = strafeDist + myType.bodyRadius + enemy.type.bodyRadius;
    	float curDist = myLoc.distanceTo(enemy.location);
    	Direction toEnemy = myLoc.directionTo(enemy.location);
    	float stride = myType.strideRadius;

    	float difference = Math.abs(curDist - exclude);

    	if (stride < difference) { // if I cannot move beyond the border line...
    		if (curDist > exclude) { // if already outside exclusion zone, strafe to the side
    			debug_tick(3);
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
    			debug_tick(4);
    			// simply move out.
    			moved = tryMoveElseLeftRight(toEnemy.opposite(), stride);
    		}
    	}
    	else { // if I can reach beyond the border line...
    		if (curDist > exclude) { // if already outside exclusion zone
    			debug_tick(5);
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
    			debug_tick(6);
    			moved = tryMoveElseLeftRight(toEnemy.opposite(), exclude - curDist);
    			if (!moved) {
    				moved = tryMoveElseLeftRight(toEnemy.opposite(), stride);
    			}
    		}
    	}
    	
    	debug_tick(7);
    	return moved;
    }

    public static MapLocation[] getMyArchonLocations() throws GameActionException {
        int numArchons = rc.readBroadcast(NUM_ARCHONS_CHANNEL);
        if (numArchons > ARCHON_LOCATION_TABLE_NUM_ENTRIES) {
            debug_print("More than 3 archons detected!!!");
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

    public static RobotInfo getNearestEnemyHostile() {
        return nearestEnemyHostile;
    }

    public static RobotInfo getNearestEnemyNonHostile() {
        return nearestEnemyNonHostile;
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
                            debug_print("Failed to move");
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
    	if (rc.onTheMap(newLoc)) { 
    		if (rc.canMove(dir, dist)) {
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
            
            if (!rc.onTheMap(newLoc)) { continue; }
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
            if (!rc.onTheMap(newLoc)) { continue; }
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

    public static boolean tryBuildRobot(RobotType type, Direction dir) throws GameActionException {
        return tryBuildRobot(type, dir, 15, 11);
    }

    public static boolean tryBuildRobot(RobotType type, Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
        // First, try intended direction
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            Direction newDir = dir.rotateLeftDegrees(degreeOffset*currentCheck);
            if (rc.canBuildRobot(type, newDir)) {
                rc.buildRobot(type, newDir);
                return true;
            }

            // Try the offset on the right side
            newDir = dir.rotateRightDegrees(degreeOffset*currentCheck);
            if (rc.canBuildRobot(type, newDir)) {
                rc.buildRobot(type, newDir);
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
                } else {
                    success = tryMoveElseLeftRight(myLoc.directionTo(target), 15, 3);
                    if (!success) {
                        success = tryMoveElseBack(myLoc.directionTo(target));
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
                        debug_line(myLoc, target, 255, 0, 0);
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
                } else if (itemLoc.distanceTo(myLoc) <= itemR) {
                    // This is overlapping my center, do nothing
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
    
    // Will be used to see if tank barrage will hit friendly farms or other things out of sensor range
    public static boolean willConeOfFireHitCircle(MapLocation target, MapLocation center, float radius) throws GameActionException {
    	// Start with the line from tank to target
    	MapLocation[] intersections = Geometry.getCircleLineSegmentIntersections(center, radius, myLoc, target);
    	if (intersections.length >= 1) {
    		debug_dot(intersections[0], 88, 88, 88);
    		debug_print("Intersection detected!");
    		return true;
    	}
    	
    	// Calculate offsets forming the cone 'base' 
    	float offsetDistMax = 2.5f;
    	Direction targetDir = myLoc.directionTo(target);
    	Direction dirA = targetDir.rotateLeftDegrees(90);
    	Direction dirB = targetDir.rotateRightDegrees(90);
    	MapLocation offsetLocA = target.add(dirA, offsetDistMax); // cone base point 1
    	MapLocation offsetLocB = target.add(dirB, offsetDistMax); // cone base point 2
    	//getCircleLineSegmentIntersections(MapLocation center, float r, MapLocation lineA, MapLocation lineB)
    	intersections = Geometry.getCircleLineSegmentIntersections(center, radius, myLoc, offsetLocA);
    	if (intersections.length >= 1) {
    		debug_dot(intersections[0], 88, 88, 88);
    		debug_print("Intersection detected!");
    		return true;
    	}
    	intersections = Geometry.getCircleLineSegmentIntersections(center, radius, myLoc, offsetLocB);
    	if (intersections.length >= 1) {
    		debug_dot(intersections[0], 88, 88, 88);
    		debug_print("Intersection detected!");
    		return true;
    	}
    	
    	return false;
    }
    
    public static boolean leaderVP() throws GameActionException { // Donation strategy
    	int VPtoWin = GameConstants.VICTORY_POINTS_TO_WIN - victoryPoints;
    	
    	if(teamBullets >= vpCost * VPtoWin) {
    	    rc.donate(teamBullets);
            teamBullets = 0f;
            return true;
        }
        if(rc.getRoundLimit() - rc.getRoundNum() < 2) {
        	rc.donate(teamBullets);
        	teamBullets = 0f;
        	return true;
        }
        
        // if not under attack
        if (peekDefendLocation() == null) {
        	 if (teamBullets > vpCost*5) {
        	     float toDonate = (float) (vpCost * Math.floor(rc.getTreeCount()/15));
                 rc.donate(toDonate);
        	     teamBullets -= toDonate;
                 return true;
             }
        }
        
        if (teamBullets > 1500) {
        	rc.donate(vpCost);
            teamBullets -= vpCost;
         	return true;
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
        
        MapLocation knownNE = bounds.getInnerCornerLoc(MapBounds.NORTH, MapBounds.EAST);
        MapLocation knownSE = bounds.getInnerCornerLoc(MapBounds.SOUTH, MapBounds.EAST);
        MapLocation knownNW = bounds.getInnerCornerLoc(MapBounds.NORTH, MapBounds.WEST);
        MapLocation knownSW = bounds.getInnerCornerLoc(MapBounds.SOUTH, MapBounds.WEST);
		
        int r = 0; int g = 255; int b = 0;;
        if (myTeam == Team.A) r = 255;
        if (myTeam == Team.B) b = 255;
        
        debug_line(knownNE, knownNW, r, g, b);
        debug_line(knownNE, knownSE, r, g, b);
        debug_line(knownSW, knownSE, r, g, b);
        debug_line(knownSW, knownNW, r, g, b);
        

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
                debug_print("narrowBounds timed out");
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
                debug_print("Detected invalid bounds! " + bounds.bounds[dirOrd].toString());
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
                debug_print("Created invalid bounds! " + bounds.bounds[dirOrd].toString());
            }
        }

        writeBroadcastArray(BOUNDS_TABLE_CHANNEL, bounds.serialize());
    }

    public static void writeFarmTableEntry(int farmNum, FarmTableEntry e) throws GameActionException {
        int entryChannel = FARM_TABLE_CHANNEL + (FARM_TABLE_ENTRY_SIZE * farmNum);
        rc.broadcast(entryChannel, e.flags);
    }

    public static FarmTableEntry readFarmTableEntry(int farmNum) throws GameActionException {
        int entryChannel = FARM_TABLE_CHANNEL + (FARM_TABLE_ENTRY_SIZE * farmNum);
        return new FarmTableEntry(rc.readBroadcast(entryChannel));
    }

    public static void storeFarmTableEntry(int farmNum) throws GameActionException {
        FarmTableEntry e = readFarmTableEntry(farmNum);
        e.storeRegisters();
        writeFarmTableEntry(farmNum, e);
    }

    public static void leaderClearOrders() throws GameActionException {
        // Clear gardener jobs queue
        gardenerJobsQueue.clear();
        // Clear lumberjack jobs queue
        lumberjackJobsQueue.clear();
        // Clear build queues
        clearBuildQueue1();
        clearBuildQueue2();
    }

    public static void sendFarmExploredInfo(int farmNum, boolean ready, boolean clear, boolean[] buildDirsBlocked) throws GameActionException {
        FarmTableEntry e = readFarmTableEntry(farmNum);
        e.setExplored();
        int longestRangeStart = -1;
        int longestRangeLength = 0;
        int rangeStart = -1;
        int rangeLength = 0;
        boolean inRange = false;
        int firstBlocked = -1;
        for (int i = 0; i < 32 && i <= (firstBlocked + 16); i++) {
            int iWrap = i % 16;
            if (inRange) {
                if (buildDirsBlocked[iWrap]) {
                    if (firstBlocked == -1) {
                        firstBlocked = iWrap;
                    }
                    if (rangeLength > longestRangeLength) {
                        longestRangeStart = rangeStart;
                        longestRangeLength = rangeLength;
                    }
                } else {
                    rangeLength++;
                }
            } else {
                if (buildDirsBlocked[iWrap]) {
                    if (firstBlocked == -1) {
                        firstBlocked = iWrap;
                    }
                    // move along
                } else {
                    rangeStart = iWrap;
                    rangeLength = 1;
                }
            }
        }
        int idealDirection = 8;
        if (longestRangeLength == 0) {
            clear = false;
        } else {
            idealDirection = longestRangeStart + (longestRangeLength / 2);
        }
        if (ready) {
            e.setReady();
        }
        if (clear) {
            e.setClear();
        }
        e.setBuildDir(idealDirection);
        writeFarmTableEntry(farmNum, e);
    }

    public static void activateFarm() throws GameActionException {
        int[] farmIndexArr = exploredFarmsQueue.pop();
        activeFarmsQueue.add(farmIndexArr);
        int farmIndex = farmIndexArr[0];
        FarmTableEntry e = readFarmTableEntry(farmIndex);
        e.setActive();
        writeFarmTableEntry(farmIndex, e);
    }

    public static void leaderManageActiveFarms() throws GameActionException {
        int numActiveFarms = activeFarmsQueue.count();
        for (int i = 0; i < numActiveFarms; i++) {
            int activeFarmIndex = activeFarmsQueue.peek(i)[0];
            FarmTableEntry e = readFarmTableEntry(activeFarmIndex);
            e.storeRegisters();
            if (!e.isReady() && !e.hasLumberjackStored()) { // If farm is not ready, and does not have a lumberjack
                // Farm needs lumberjack
                lumberjackJobsQueue.add(new int[]{activeFarmIndex});
                debug_print("Request lumberjack");
            } else if (e.isReady() && !e.hasGardenerStored()) { // If the farm is ready, and does not have a gardener
                // Farm needs gardener
                gardenerJobsQueue.add(new int[]{activeFarmIndex});
                debug_print("Request gardener");
            } else if (e.hasGardenerStored() && !e.isClear() && !e.hasLumberjackStored()) { // If the farm has a gardener but is not clear and has no lumberjack
                // Farm needs lumberjack
                lumberjackJobsQueue.add(new int[]{activeFarmIndex});
                debug_print("Request lumberjack");
            }
        }
    }

	public static MapLocation[] getAllFarmLocs() throws GameActionException {
		int fCount = activeFarmsQueue.count();
		if (fCount < 1) return null;
		MapLocation[] farmLocs = new MapLocation[fCount];

		for (int i = 0; i < fCount; i++) {
		    int fNum = activeFarmsQueue.peek(i)[0];
			farmLocs[i] = farmNumToLoc(fNum);
			//debug_print("Farm # " + fNum + " is at " + farmLocs[fNum]);
		}
		
		return farmLocs;
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
            debug_print("Build queue 1 overflow!");
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
            debug_print("Build queue 2 overflow!");
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

    public static void clearBuildQueue1() throws GameActionException {
        rc.broadcast(BUILD_QUEUE_1_COUNT_CHANNEL, 0);
    }

    public static void clearBuildQueue2() throws GameActionException {
        rc.broadcast(BUILD_QUEUE_2_COUNT_CHANNEL, 0);
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

    public static void debug_tick(int id) {
        if (debug) {
            int currentRoundNum = rc.getRoundNum();
            int bytecodes = Clock.getBytecodeNum();
            debugBytecodesList[id] = bytecodes;
            numDebugBytecodes = Math.max(numDebugBytecodes, id+1);
            if (currentRoundNum != roundNum) {
                if (!debugTripped) {
                    debugTripped = true;
                    debug_print("Detected over-bytecodes!!!");
                    debug_print("Round changed before tick " + id);
                    for (int i = 0; i < numDebugBytecodes - 1; i++) {
                        debug_print(i + ": " + debugBytecodesList[i]);
                    }
                    debug_print((numDebugBytecodes - 1) + ": " + debugBytecodesList[numDebugBytecodes - 1] + " + " + (currentRoundNum - roundNum) + " rounds");
                } else {
                    debug_print(id + ": " + bytecodes + " + " + (currentRoundNum - roundNum) + " rounds");
                }
            }
        }
    }

    public static void debug_print(String s) {
        if (debug) {
            System.out.println(s);
        }
    }

    public static void debug_print(Object o) {
        if (debug) {
            System.out.println(o);
        }
    }

    public static void debug_exception(Exception e) {
        if (debugExceptions) {
            e.printStackTrace();
        }
    }

    public static void debug_exception(Exception e, String ctx) {
        if (debugExceptions) {
            System.out.println("Exception context: " + ctx);
            e.printStackTrace();
        }
    }

    public static void debug_dot(MapLocation loc, int r, int g, int b) throws GameActionException {
        if (debug) {
            rc.setIndicatorDot(loc, r, g, b);
        }
    }

    public static void debug_line(MapLocation start, MapLocation end, int r, int g, int b) throws GameActionException {
        if (debug) {
            rc.setIndicatorLine(start, end, r, g, b);
        }
    }

    public static void addAttackLocation(MapLocation loc) throws GameActionException {
        int begin = rc.readBroadcast(ATTACK_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(ATTACK_LOCATION_QUEUE_COUNT_CHANNEL);
        if (count >= ATTACK_LOCATION_QUEUE_NUM_ENTRIES) {
            debug_print("Attack location queue overflow!");
            return;
        }
        int entryIndex = (begin + count) % ATTACK_LOCATION_QUEUE_NUM_ENTRIES;
        int entryChannel = ATTACK_LOCATION_QUEUE_CHANNEL + (entryIndex * ATTACK_LOCATION_QUEUE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        rc.broadcast(xChannel, Float.floatToIntBits(loc.x));
        rc.broadcast(yChannel, Float.floatToIntBits(loc.y));
        count++;
        rc.broadcast(ATTACK_LOCATION_QUEUE_COUNT_CHANNEL, count);
    }

    public static void addAttackLocationFirst(MapLocation loc) throws GameActionException {
        int begin = rc.readBroadcast(ATTACK_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(ATTACK_LOCATION_QUEUE_COUNT_CHANNEL);
        if (count >= ATTACK_LOCATION_QUEUE_NUM_ENTRIES) {
            debug_print("Attack location queue overflow!");
            return;
        }
        begin = ((begin - 1) + ATTACK_LOCATION_QUEUE_NUM_ENTRIES) % ATTACK_LOCATION_QUEUE_NUM_ENTRIES;
        count = (count + 1) % ATTACK_LOCATION_QUEUE_NUM_ENTRIES;
        int entryIndex = begin;
        int entryChannel = ATTACK_LOCATION_QUEUE_CHANNEL + (entryIndex * ATTACK_LOCATION_QUEUE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        rc.broadcast(xChannel, Float.floatToIntBits(loc.x));
        rc.broadcast(yChannel, Float.floatToIntBits(loc.y));
        rc.broadcast(ATTACK_LOCATION_QUEUE_BEGIN_CHANNEL, begin);
        rc.broadcast(ATTACK_LOCATION_QUEUE_COUNT_CHANNEL, count);
    }

    public static MapLocation peekAttackLocation() throws GameActionException {
        int begin = rc.readBroadcast(ATTACK_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(ATTACK_LOCATION_QUEUE_COUNT_CHANNEL);
        if (count <= 0) {
            return null;
        }
        int entryIndex = begin;
        int entryChannel = ATTACK_LOCATION_QUEUE_CHANNEL + (entryIndex * ATTACK_LOCATION_QUEUE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        float x = Float.intBitsToFloat(rc.readBroadcast(xChannel));
        float y = Float.intBitsToFloat(rc.readBroadcast(yChannel));
        return new MapLocation(x, y);
    }

    public static void updateAttackLocation(MapLocation loc) throws GameActionException {
        updateAttackLocation(loc, 0);
    }

    public static void updateAttackLocation(MapLocation loc, int which) throws GameActionException {
        int begin = rc.readBroadcast(ATTACK_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(ATTACK_LOCATION_QUEUE_COUNT_CHANNEL);
        if (count <= which) {
            debug_print("Tried to update nonexistant attack location!");
            return;
        }
        int entryIndex = (begin + which) % ATTACK_LOCATION_QUEUE_NUM_ENTRIES;;
        int entryChannel = ATTACK_LOCATION_QUEUE_CHANNEL + (entryIndex * ATTACK_LOCATION_QUEUE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        rc.broadcast(xChannel, Float.floatToIntBits(loc.x));
        rc.broadcast(yChannel, Float.floatToIntBits(loc.y));
    }

    public static MapLocation popAttackLocation() throws GameActionException {
        int begin = rc.readBroadcast(ATTACK_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(ATTACK_LOCATION_QUEUE_COUNT_CHANNEL);
        if (count <= 0) {
            return null;
        }
        int entryIndex = begin;
        int entryChannel = ATTACK_LOCATION_QUEUE_CHANNEL + (entryIndex * ATTACK_LOCATION_QUEUE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        float x = Float.intBitsToFloat(rc.readBroadcast(xChannel));
        float y = Float.intBitsToFloat(rc.readBroadcast(yChannel));
        begin = (begin + 1) % ATTACK_LOCATION_QUEUE_NUM_ENTRIES;
        rc.broadcast(ATTACK_LOCATION_QUEUE_BEGIN_CHANNEL, begin);
        count = count - 1;
        rc.broadcast(ATTACK_LOCATION_QUEUE_COUNT_CHANNEL, count);
        return new MapLocation(x, y);
    }

    public static int whichAttackLocation(MapLocation loc) throws GameActionException {
        int begin = rc.readBroadcast(ATTACK_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(ATTACK_LOCATION_QUEUE_COUNT_CHANNEL);
        for (int i = 0; i < count; i++) {
            int entryIndex = (begin + i) % ATTACK_LOCATION_QUEUE_NUM_ENTRIES;
            int entryChannel = ATTACK_LOCATION_QUEUE_CHANNEL + (entryIndex * ATTACK_LOCATION_QUEUE_ENTRY_SIZE);
            int xChannel = entryChannel;
            int yChannel = entryChannel + 1;
            float x = Float.intBitsToFloat(rc.readBroadcast(xChannel));
            float y = Float.intBitsToFloat(rc.readBroadcast(yChannel));
            MapLocation foundLoc = new MapLocation(x, y);
            if (foundLoc.distanceTo(loc) < 4f) {
                return i;
            }
        }
        return -1;
    }

    public static void addDefendLocation(MapLocation loc) throws GameActionException {
        int begin = rc.readBroadcast(DEFEND_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(DEFEND_LOCATION_QUEUE_COUNT_CHANNEL);
        if (count >= DEFEND_LOCATION_QUEUE_NUM_ENTRIES) {
            debug_print("Defend location queue overflow!");
            return;
        }
        int entryIndex = (begin + count) % DEFEND_LOCATION_QUEUE_NUM_ENTRIES;
        int entryChannel = DEFEND_LOCATION_QUEUE_CHANNEL + (entryIndex * DEFEND_LOCATION_QUEUE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        rc.broadcast(xChannel, Float.floatToIntBits(loc.x));
        rc.broadcast(yChannel, Float.floatToIntBits(loc.y));
        count++;
        rc.broadcast(DEFEND_LOCATION_QUEUE_COUNT_CHANNEL, count);
    }

    public static void addDefendLocationFirst(MapLocation loc) throws GameActionException {
        int begin = rc.readBroadcast(DEFEND_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(DEFEND_LOCATION_QUEUE_COUNT_CHANNEL);
        if (count >= DEFEND_LOCATION_QUEUE_NUM_ENTRIES) {
            debug_print("Defend location queue overflow!");
            return;
        }
        begin = ((begin - 1) + DEFEND_LOCATION_QUEUE_NUM_ENTRIES) % DEFEND_LOCATION_QUEUE_NUM_ENTRIES;
        count = (count + 1) % DEFEND_LOCATION_QUEUE_NUM_ENTRIES;
        int entryIndex = begin;
        int entryChannel = DEFEND_LOCATION_QUEUE_CHANNEL + (entryIndex * DEFEND_LOCATION_QUEUE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        rc.broadcast(xChannel, Float.floatToIntBits(loc.x));
        rc.broadcast(yChannel, Float.floatToIntBits(loc.y));
        rc.broadcast(DEFEND_LOCATION_QUEUE_BEGIN_CHANNEL, begin);
        rc.broadcast(DEFEND_LOCATION_QUEUE_COUNT_CHANNEL, count);
    }

    public static MapLocation peekDefendLocation() throws GameActionException {
        int begin = rc.readBroadcast(DEFEND_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(DEFEND_LOCATION_QUEUE_COUNT_CHANNEL);
        if (count <= 0) {
            return null;
        }
        int entryIndex = begin;
        int entryChannel = DEFEND_LOCATION_QUEUE_CHANNEL + (entryIndex * DEFEND_LOCATION_QUEUE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        float x = Float.intBitsToFloat(rc.readBroadcast(xChannel));
        float y = Float.intBitsToFloat(rc.readBroadcast(yChannel));
        return new MapLocation(x, y);
    }

    public static void updateDefendLocation(MapLocation loc) throws GameActionException {
        updateDefendLocation(loc, 0);
    }

    public static void updateDefendLocation(MapLocation loc, int which) throws GameActionException {
        int begin = rc.readBroadcast(DEFEND_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(DEFEND_LOCATION_QUEUE_COUNT_CHANNEL);
        if (count <= which) {
            debug_print("Tried to update nonexistant defend location!");
            return;
        }
        int entryIndex = (begin + which) % DEFEND_LOCATION_QUEUE_NUM_ENTRIES;;
        int entryChannel = DEFEND_LOCATION_QUEUE_CHANNEL + (entryIndex * DEFEND_LOCATION_QUEUE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        rc.broadcast(xChannel, Float.floatToIntBits(loc.x));
        rc.broadcast(yChannel, Float.floatToIntBits(loc.y));
    }



    public static MapLocation popDefendLocation() throws GameActionException {
        int begin = rc.readBroadcast(DEFEND_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(DEFEND_LOCATION_QUEUE_COUNT_CHANNEL);
        if (count <= 0) {
            return null;
        }
        int entryIndex = begin;
        int entryChannel = DEFEND_LOCATION_QUEUE_CHANNEL + (entryIndex * DEFEND_LOCATION_QUEUE_ENTRY_SIZE);
        int xChannel = entryChannel;
        int yChannel = entryChannel + 1;
        float x = Float.intBitsToFloat(rc.readBroadcast(xChannel));
        float y = Float.intBitsToFloat(rc.readBroadcast(yChannel));
        begin = (begin + 1) % DEFEND_LOCATION_QUEUE_NUM_ENTRIES;
        rc.broadcast(DEFEND_LOCATION_QUEUE_BEGIN_CHANNEL, begin);
        count = count - 1;
        rc.broadcast(DEFEND_LOCATION_QUEUE_COUNT_CHANNEL, count);
        return new MapLocation(x, y);
    }

    public static int whichDefendLocation(MapLocation loc) throws GameActionException {
        int begin = rc.readBroadcast(DEFEND_LOCATION_QUEUE_BEGIN_CHANNEL);
        int count = rc.readBroadcast(DEFEND_LOCATION_QUEUE_COUNT_CHANNEL);
        for (int i = 0; i < count; i++) {
            int entryIndex = (begin + i) % DEFEND_LOCATION_QUEUE_NUM_ENTRIES;
            int entryChannel = DEFEND_LOCATION_QUEUE_CHANNEL + (entryIndex * DEFEND_LOCATION_QUEUE_ENTRY_SIZE);
            int xChannel = entryChannel;
            int yChannel = entryChannel + 1;
            float x = Float.intBitsToFloat(rc.readBroadcast(xChannel));
            float y = Float.intBitsToFloat(rc.readBroadcast(yChannel));
            MapLocation foundLoc = new MapLocation(x, y);
            if (foundLoc.distanceTo(loc) < 4f) {
                return i;
            }
        }
        return -1;
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

    public static boolean getLateLumberjacks() {
        return lateLumberjacks;
    }

    public static void setLateLumberjacks(boolean lateLumberjacks) {
        RobotGlobal.lateLumberjacks = lateLumberjacks;
    }

    public static boolean getScoutWhenFull() {
        return scoutWhenFull;
    }

    public static void setScoutWhenFull(boolean scoutWhenFull) {
        RobotGlobal.scoutWhenFull = scoutWhenFull;
    }

    public static boolean getUseFarmGrid() {
        return useFarmGrid;
    }

    public static void setUseFarmGrid(boolean useFarmGrid) {
        RobotGlobal.useFarmGrid = useFarmGrid;
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
                            //debug_print("A Happened");
                        }
                        if (endRadiusIntersections.length == 2) {
                            exclude.add(endRadiusIntersections[0], endRadiusIntersections[1]);
                            //debug_print("B Happened");
                        }
                        for (float[] polyIntersectionPair : polyIntersectionPairs) {
                            exclude.add(polyIntersectionPair[0], polyIntersectionPair[1]);
                            //debug_print("C Happened");
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
                        debug_line(myLoc.add(new Direction(running), myType.strideRadius), myLoc.add(new Direction(next), myType.strideRadius), 255,0,0);
                        running = next;
                    }

                    current = current.next;
                    if (current == exclude.getFirst()) {
                        break;
                    }
                }

                debug_print(swept);
                if (swept > 6) {
                    debug_print("High sweep");
                    debug_print(exclude);
                }

                debug_dot(myLoc.add(attackDir, myType.strideRadius), 0, 255, 0);

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

                debug_dot(myLoc.add(farthestDir, myType.strideRadius), 0, 0, 255);

                // Move
                if (farthestDir != null) {
                    boolean success = tryMove(myLoc.add(farthestDir, myType.strideRadius));
                    if (!success) {
                        debug_print("Exclude: " + exclude);
                        debug_print("Move: " + exclude.closest(attackDir.radians));
                    }
                } else {
                    debug_print("Null move");
                    debug_print("Exclude: " + exclude);
                }

     */
}
