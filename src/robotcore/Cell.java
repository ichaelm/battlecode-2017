package robotcore;

import battlecode.common.*;

public class Cell {

    public final int a;
    public final int b;
    public final MapLocation loc;
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
