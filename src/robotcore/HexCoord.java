package robotcore;

import battlecode.common.*;

public class HexCoord {

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

    // member variables

    public final int a;
    public final int b;

    public HexCoord(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public static HexDir toHexDir(Direction dir) {
        int degrees30 = (int)dir.getAngleDegrees() + 30;
        int quotient = (((degrees30 / 60) % 6) + 6) % 6;
        return HexDir.values()[quotient];
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HexCoord hexCoord = (HexCoord) o;

        return a == hexCoord.a && b == hexCoord.b;
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