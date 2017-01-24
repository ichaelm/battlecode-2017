package robotcore;

import battlecode.common.*;

public class CommMap {

    // member variables

    private static RobotController rc;
    private static MapLocation origin;
    private static MapBounds bounds;

    // broadcast array constants

    private static final int MAP_ORIGIN_X_CHANNEL = 5000;
    private static final int MAP_ORIGIN_Y_CHANNEL = 5001;
    private static final int MAP_CHANNEL = 5002;
    private static final int MAP_ENTRY_SIZE = 1;
    private static final int MAP_SIZE_A = 51;
    private static final int MAP_SIZE_B = 59;
    private static final int MAP_NUM_ENTRIES = MAP_SIZE_A * MAP_SIZE_B;
    private static final int MAP_LENGTH = MAP_ENTRY_SIZE * MAP_NUM_ENTRIES;

    // bitmasks

    private static final int MAP_CELL_EXPLORED_MASK = 0x80000000;
    private static final int MAP_CELL_CLEAR_MASK = 0x40000000;

    // geometry constants

    public static final float CELL_RADIUS = 1f;
    public static final float CELL_RESOLUTION = 2f;
    public static final Direction aDir = Direction.getEast(); // Algorithm depends on aDir being east!!!
    public static final Direction bDir = Direction.getEast().rotateLeftDegrees(60);

    // whole map operations

    public static void sendOrigin(MapLocation origin) throws GameActionException {
        CommMap.origin = origin;
        rc.broadcast(MAP_ORIGIN_X_CHANNEL, Float.floatToIntBits(origin.x));
        rc.broadcast(MAP_ORIGIN_Y_CHANNEL, Float.floatToIntBits(origin.y));
    }

    public static void setRC(RobotController rc) {
        CommMap.rc = rc;
    }

    public static void refresh(MapBounds bounds) throws GameActionException {
        float x = Float.intBitsToFloat(rc.readBroadcast(MAP_ORIGIN_X_CHANNEL));
        float y = Float.intBitsToFloat(rc.readBroadcast(MAP_ORIGIN_Y_CHANNEL));
        origin = new MapLocation(x, y);
        CommMap.bounds = bounds;
    }

    // channel math utilities

    public enum HexDir {
        DIR_A(1,0),
        DIR_B(0,1),
        DIR_NA_B(-1,1),
        DIR_NA(-1,0),
        DIR_NB(0,-1),
        DIR_A_NB(1,-1);

        public final int dA;
        public final int dB;

        HexDir(int dA, int dB) {
            this.dA = dA;
            this.dB = dB;
        }
    }

    public static HexDir toHexDir(Direction dir) {
        int degrees30 = (int)dir.getAngleDegrees() + 30;
        int quotient = (((degrees30 / 60) % 6) + 6) % 6;
        return HexDir.values()[quotient];
    }

    public static class HexCoord {

        public final int a;
        public final int b;

        public HexCoord(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HexCoord hexCoord = (HexCoord) o;

            if (a != hexCoord.a) return false;
            return b == hexCoord.b;
        }

        @Override
        public int hashCode() {
            int result = a;
            result = 31 * result + b;
            return result;
        }

        public static class Iterator implements java.util.Iterator<HexCoord> {
            private final int centerA;
            private final int centerB;
            private final int minR;
            private final int maxR;
            private int a;
            private int b;
            private int r;

            private Iterator(int centerA, int centerB, int r) {
                this(centerA, centerB, r, r);
            }

            private Iterator(int centerA, int centerB, int minR, int maxR) {
                this.centerA = centerA;
                this.centerB = centerB;
                this.minR = minR;
                this.maxR = maxR;
                a = minR;
                b = 0;
                r = minR;
            }

            @Override
            public boolean hasNext() {
                return r <= maxR;
            }

            @Override
            public HexCoord next() {
                // Don't check if has next
                int aToReturn = a;
                int bToReturn = b;
                if (a == 0 && b == 0) { // If this is the center point
                    r++;
                    a++;
                } else if ((a == r) && (b == -1)) { // If this is the last point at radius r
                    r++;
                    a++;
                    b++;
                } else if ((a == r) && (b != 0)) { // Else if we are on the 6th leg
                    b++;
                } else if (b == -r) { // Else if we are on the 5th leg
                    a++;
                } else if (a + b == -r) { // Else if we are on the 4th leg
                    a++;
                    b--;
                } else if (a == -r) { // Else if we are on the 3rd leg
                    b--;
                } else if (b == r) { // Else if we are on the 2nd leg
                    a--;
                } else /*if (a + b == r)*/ { // Else if we are on the 1st leg (assumed for speed)
                    a--;
                    b++;
                }
                return new HexCoord(centerA + aToReturn, centerB + bToReturn);
            }
        }

        public static class ArcIterator implements java.util.Iterator<HexCoord> {
            private final int centerA;
            private final int centerB;
            private final int endA;
            private final int endB;
            private final int r;
            private int a;
            private int b;
            private boolean notDone = true;

            private ArcIterator(int centerA, int centerB, int r, Direction dir) {
                this.centerA = centerA;
                this.centerB = centerB;
                this.r = r;
                Direction startDir = dir.rotateRightDegrees(90);
                HexDir startHexDir = toHexDir(startDir);
                a = startHexDir.dA * r;
                b = startHexDir.dB * r;
                endA = -a;
                endB = -b;
            }

            @Override
            public boolean hasNext() {
                return notDone;
            }

            @Override
            public HexCoord next() {
                // Don't check if has next
                int aToReturn = a;
                int bToReturn = b;
                if ((a == endA) && (b == endB)) { // If this is the last point
                    notDone = false;
                } else if ((a == r) && (b != 0)) { // Else if we are on the 6th leg
                    b++;
                } else if (b == -r) { // Else if we are on the 5th leg
                    a++;
                } else if (a + b == -r) { // Else if we are on the 4th leg
                    a++;
                    b--;
                } else if (a == -r) { // Else if we are on the 3rd leg
                    b--;
                } else if (b == r) { // Else if we are on the 2nd leg
                    a--;
                } else /*if (a + b == r)*/ { // Else if we are on the 1st leg (assumed for speed)
                    a--;
                    b++;
                }
                return new HexCoord(centerA + aToReturn, centerB + bToReturn);
            }
        }
    }

    public static HexCoord.Iterator hexBandIterator(int centerA, int centerB, int minR, int maxR) {
        return new HexCoord.Iterator(centerA, centerB, minR, maxR);
    }

    public static HexCoord.Iterator hexPerimeterIterator(int centerA, int centerB, int r) {
        return new HexCoord.Iterator(centerA, centerB, r, r);
    }

    public static HexCoord.Iterator hexFullIterator(int centerA, int centerB, int r) {
        return new HexCoord.Iterator(centerA, centerB, 0, r);
    }

    public static HexCoord.ArcIterator hexArcIterator(int centerA, int centerB, int r, Direction dir) {
        return new HexCoord.ArcIterator(centerA, centerB, r, dir);
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

    public static float[] exactHexCoord(MapLocation loc) {
        float bExact = (loc.y - origin.y) / bDir.getDeltaY(CELL_RESOLUTION);
        float aExact = (loc.x - (origin.x + bDir.getDeltaX(bExact * CELL_RESOLUTION))) / aDir.getDeltaX(CELL_RESOLUTION);
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
        HexCoord coord = nearestHexCoord(loc);
        return commCell(coord.a, coord.b);
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
