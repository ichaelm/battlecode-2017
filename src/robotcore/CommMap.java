package robotcore;

import battlecode.common.*;

public class CommMap {

    // member variables

    private static RobotController rc;
    private static MapLocation origin;
    private static MapBounds bounds;

    // broadcast array constants

    private static final int MAP_ORIGIN_X_CHANNEL = 0;
    private static final int MAP_ORIGIN_Y_CHANNEL = 1;
    private static final int MAP_CHANNEL = 2;
    private static final int MAP_ENTRY_SIZE = 1;
    private static final int MAP_SIZE_A = 51;
    private static final int MAP_SIZE_B = 59;
    private static final int MAP_NUM_ENTRIES = MAP_SIZE_A * MAP_SIZE_B;
    private static final int MAP_LENGTH = MAP_ENTRY_SIZE * MAP_NUM_ENTRIES;

    // bitmasks

    private static final int MAP_CELL_EXPLORED_MASK = 0x80000000;
    private static final int MAP_CELL_CLEAR_MASK = 0x40000000;

    // geometry constants

    private static final float CELL_RADIUS = 1f;
    private static final float CELL_RESOLUTION = 2f;
    private static final Direction aDir = Direction.getEast(); // Algorithm depends on aDir being east!!!
    private static final Direction bDir = Direction.getEast().rotateLeftDegrees(60);

    // whole map operations

    public static void initialize(RobotController rc, MapLocation origin) throws GameActionException {
        CommMap.rc = rc;
        CommMap.origin = origin;
        rc.broadcast(MAP_ORIGIN_X_CHANNEL, Float.floatToIntBits(origin.x));
        rc.broadcast(MAP_ORIGIN_Y_CHANNEL, Float.floatToIntBits(origin.y));
    }

    public static void refresh(MapBounds bounds) throws GameActionException {
        float x = Float.intBitsToFloat(rc.readBroadcast(MAP_ORIGIN_X_CHANNEL));
        float y = Float.intBitsToFloat(rc.readBroadcast(MAP_ORIGIN_Y_CHANNEL));
        origin = new MapLocation(x, y);
        CommMap.bounds = bounds;
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
        wrappedB -= minB;
        wrappedB = ((wrappedB % MAP_SIZE_B) + MAP_SIZE_B) % MAP_SIZE_B;
        wrappedB += minB;
        return wrappedB;
    }

    private static int unwrapAGivenB(int wrappedA, int b) {
        int minA = minAOnKnownMapGivenB(b);
        wrappedA -= minA;
        wrappedA = ((wrappedA % MAP_SIZE_A) + MAP_SIZE_A) % MAP_SIZE_A;
        wrappedA += minA;
        return wrappedA;
    }

    private static int[] entryIndexToHexCoord(int entryIndex) {
        int wrappedB = entryIndex / MAP_SIZE_A;
        int wrappedA = entryIndex % MAP_SIZE_A;
        int b = unwrapB(wrappedB);
        int a = unwrapAGivenB(wrappedA, b);
        return new int[]{a, b};
    }

    // coordinate math utilities

    public static boolean hexCoordOverlapsKnownMap(int a, int b) {
        return b <= maxBOnKnownMap() && a <= maxAOnKnownMapGivenB(b);
    }

    public static int maxBOnKnownMap() {
        float maxBExact = ((bounds.getInnerBound(MapBounds.NORTH) - origin.y) + CELL_RADIUS) / (bDir.getDeltaY(CELL_RESOLUTION));
        return (int)Math.floor(maxBExact);
    }

    public static int minBOnKnownMap() {
        float minBExact = ((bounds.getInnerBound(MapBounds.SOUTH) - origin.y) - CELL_RADIUS) / (bDir.getDeltaY(CELL_RESOLUTION));
        return (int)Math.ceil(minBExact);
    }

    public static int maxAOnKnownMapGivenB(int b) {
        b = unwrapB(b);
        MapLocation start = origin.add(bDir, b);
        float maxAExact = ((bounds.getInnerBound(MapBounds.EAST) - start.x) + CELL_RADIUS) / CELL_RESOLUTION;
        return (int)Math.floor(maxAExact);
    }

    public static int minAOnKnownMapGivenB(int b) {
        b = unwrapB(b);
        MapLocation start = origin.add(bDir, b);
        float minAExact = ((bounds.getInnerBound(MapBounds.WEST) - start.x) - CELL_RADIUS) / CELL_RESOLUTION;
        return (int)Math.ceil(minAExact);
    }

    public static MapLocation hexCoordToLoc(int a, int b) {
        return origin.add(aDir, a * CELL_RESOLUTION).add(bDir, b * CELL_RESOLUTION);
    }

    public static int[] nearestHexCoord(MapLocation loc) {
        float bExact = (loc.y - origin.y) / bDir.getDeltaY(1);
        float aExact = (loc.x - (origin.x + bDir.getDeltaX(bExact)));
        int aFloor = (int)Math.floor(aExact);
        int aCeil = (int)Math.ceil(aExact);
        int bFloor = (int)Math.floor(bExact);
        int bCeil = (int)Math.ceil(bExact);
        int[][] coords = new int[][]{
                {aFloor, bFloor},
                {aFloor, bCeil},
                {aCeil, bFloor},
                {aCeil, bCeil},
        };
        float[] dists = new float[]{
                loc.distanceTo(hexCoordToLoc(aFloor, bFloor)),
                loc.distanceTo(hexCoordToLoc(aFloor, bCeil)),
                loc.distanceTo(hexCoordToLoc(aCeil, bFloor)),
                loc.distanceTo(hexCoordToLoc(aCeil, bCeil))
        };
        float minDist = 9999999;
        int[] minDistCoord = null;
        for (int i = 0; i < 4; i++) {
            float dist = dists[i];
            if (dist < minDist) {
                minDist = dist;
                minDistCoord = coords[i];
            }
        }
        return minDistCoord;
    }

    // CommCell interface

    public static class CommCell {

        private final int channel;

        public CommCell(int channel) {
            this.channel = channel;
        }

        public boolean queryExplored() throws GameActionException {
            return (rc.readBroadcast(channel) & MAP_CELL_EXPLORED_MASK) != 0;
        }

        public boolean queryClear() throws GameActionException {
            return (rc.readBroadcast(channel) & MAP_CELL_CLEAR_MASK) != 0;
        }

        public void sendExplored(boolean explored) throws GameActionException {
            int flags = rc.readBroadcast(channel);
            if (explored) {
                flags = flags & MAP_CELL_EXPLORED_MASK;
            } else {
                flags = flags | ~MAP_CELL_EXPLORED_MASK;
            }
            rc.broadcast(channel, flags);
        }

        public void sendClear(boolean clear) throws GameActionException {
            int flags = rc.readBroadcast(channel);
            if (clear) {
                flags = flags & MAP_CELL_CLEAR_MASK;
            } else {
                flags = flags | ~MAP_CELL_CLEAR_MASK;
            }
            rc.broadcast(channel, flags);
        }
    }

    public static CommCell getNearestCommCell(MapLocation loc) {
        int[] coord = nearestHexCoord(loc);
        return commCell(coord[0], coord[1]);
    }

    public static CommCell commCell(int a, int b) {
        int entryIndex = hexCoordToEntryIndex(a, b);
        int entryChannel = MAP_CHANNEL + (entryIndex * MAP_ENTRY_SIZE);
        return new CommCell(entryChannel);
    }

    // Cell interface

    public static class Cell {

        private int a;
        private int b;
        private MapLocation loc;
        private boolean explored;
        private boolean clear;

        public Cell(int a, int b, MapLocation loc, boolean explored, boolean clear) {
            this.a = a;
            this.b = b;
            this.loc = loc;
            this.explored = explored;
            this.clear = clear;
        }

        public boolean isExplored() {
            return explored;
        }

        public void setExplored(boolean explored) {
            this.explored = explored;
        }

        public boolean isClear() {
            return clear;
        }

        public void setClear(boolean clear) {
            this.clear = clear;
        }

        public int getA() {
            return a;
        }

        public int getB() {
            return b;
        }

        public MapLocation getLoc() {
            return loc;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Cell at hexCoord (");
            sb.append(a);
            sb.append(", ");
            sb.append(b);
            sb.append(") located at ");
            sb.append(loc);
            sb.append(" is ");
            if (!explored) {
                sb.append("not ");
            }
            sb.append("explored and is ");
            if (!clear) {
                sb.append("not ");
            }
            sb.append("clear.");
            return sb.toString();
        }
    }

    public static Cell queryNearestCell(MapLocation loc) throws GameActionException {
        int[] coord = nearestHexCoord(loc);
        return queryCell(coord[0], coord[1]);
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
