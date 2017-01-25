package sprint_robotcore;

import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class MapBounds {
    public static final int NORTH = 0;
    public static final int EAST = 1;
    public static final int SOUTH = 2;
    public static final int WEST = 3;

    public Range[] bounds;

    public MapBounds() {
        this.bounds = new Range[]{
                new Range(0, 600),
                new Range(0, 600),
                new Range(0, 600),
                new Range(0, 600)
        };
    }

    public MapBounds(Range[] bounds) {
        this.bounds = bounds;
    }

    public float getInnerBound(int direction) {
        if (directionIsPositive(direction)) {
            return bounds[direction].min;
        } else {
            return bounds[direction].max;
        }
    }

    public float getOuterBound(int direction) {
        if (directionIsPositive(direction)) {
            return bounds[direction].max;
        } else {
            return bounds[direction].min;
        }
    }

    public MapLocation getInnerBoundLoc(int direction, MapLocation loc) {
        if (directionIsX(direction)) {
            return new MapLocation(getInnerBound(direction), loc.y);
        } else {
            return new MapLocation(loc.x, getInnerBound(direction));
        }
    }

    public MapLocation getOuterBoundLoc(int direction, MapLocation loc) {
        if (directionIsX(direction)) {
            return new MapLocation(getOuterBound(direction), loc.y);
        } else {
            return new MapLocation(loc.x, getOuterBound(direction));
        }
    }

    public boolean updateInnerBound(int direction, float innerBound) {
        if (directionIsPositive(direction)) {
            return bounds[direction].updateMin(innerBound);
        } else {
            return bounds[direction].updateMax(innerBound);
        }
    }

    public boolean updateOuterBound(int direction, float outerBound) {
        if (directionIsPositive(direction)) {
            return bounds[direction].updateMax(outerBound);
        } else {
            return bounds[direction].updateMin(outerBound);
        }
    }

    public boolean updateInnerBound(int direction, MapLocation innerBoundLoc) {
        float innerBound;
        if (directionIsX(direction)) {
            innerBound = innerBoundLoc.x;
        } else {
            innerBound = innerBoundLoc.y;
        }
        return updateInnerBound(direction, innerBound);
    }

    public boolean updateOuterBound(int direction, MapLocation outerBoundLoc) {
        float outerBound;
        if (directionIsX(direction)) {
            outerBound = outerBoundLoc.x;
        } else {
            outerBound = outerBoundLoc.y;
        }
        return updateOuterBound(direction, outerBound);
    }

    public static Direction dirFromOrd(int dirOrd) {
        if (dirOrd == NORTH) {
            return Direction.getNorth();
        } else if (dirOrd == EAST) {
            return Direction.getEast();
        } else if (dirOrd == SOUTH) {
            return Direction.getSouth();
        } else if (dirOrd == WEST) {
            return Direction.getWest();
        } else {
            return null;
        }
    }

    public static MapBounds deserialize(int[] arr) {
        float westInner = Float.intBitsToFloat(arr[0]);
        float eastInner = Float.intBitsToFloat(arr[1]);
        float southInner = Float.intBitsToFloat(arr[2]);
        float northInner = Float.intBitsToFloat(arr[3]);
        float westOuter = Float.intBitsToFloat(arr[4]);
        float eastOuter = Float.intBitsToFloat(arr[5]);
        float southOuter = Float.intBitsToFloat(arr[6]);
        float northOuter = Float.intBitsToFloat(arr[7]);

        Range[] bounds = new Range[]{
                new Range(northInner,northOuter),
                new Range(eastInner,eastOuter),
                new Range(southOuter,southInner),
                new Range(westOuter,westInner)
        };
        return new MapBounds(bounds);
    }

    public int[] serialize() {
        int[] retval = new int[]{
                Float.floatToIntBits(getInnerBound(WEST)),
                Float.floatToIntBits(getInnerBound(EAST)),
                Float.floatToIntBits(getInnerBound(SOUTH)),
                Float.floatToIntBits(getInnerBound(NORTH)),
                Float.floatToIntBits(getOuterBound(WEST)),
                Float.floatToIntBits(getOuterBound(EAST)),
                Float.floatToIntBits(getOuterBound(SOUTH)),
                Float.floatToIntBits(getOuterBound(NORTH)),
        };

        return retval;
    }

    private boolean directionIsPositive(int direction) {
        return (direction == NORTH) || (direction == EAST);
    }

    private boolean directionIsX(int direction) {
        return (direction == EAST) || (direction == WEST);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MapBounds{");

        sb.append("NORTH=");
        sb.append(bounds[NORTH]);

        sb.append(",");

        sb.append("EAST=");
        sb.append(bounds[EAST]);

        sb.append(",");

        sb.append("SOUTH=");
        sb.append(bounds[SOUTH]);

        sb.append(",");

        sb.append("WEST=");
        sb.append(bounds[WEST]);

        sb.append("}");

        return sb.toString();
    }
}
