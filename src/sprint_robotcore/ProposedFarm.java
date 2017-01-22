package sprint_robotcore;

import battlecode.common.*;

public class ProposedFarm {

    // Farm geometry constants
    static final float octEdge = (float) 0.4693;
    static final float octDiag = (float) 0.6132;
    static final float octagonFarmRadius = (float) 3.625;

    // Farm placement data
    private MapLocation farmCenter = null;
    private Direction buildDirection = null;
    private MapLocation buildLoc = null;
    private MapLocation constructionZone = null;
    private MapLocation[] treeLocs = new MapLocation[7];
    private MapLocation[] treePlantingLocs = new MapLocation[7];
    private Direction[] treeDirections = new Direction[7];

    public ProposedFarm(MapLocation center, Direction buildDir) {
        this.farmCenter = center;
        this.buildDirection = buildDir;
        Direction firstMove = buildDir.rotateLeftDegrees(45);

        Direction treeDir = firstMove;
        Direction moveDir;

        this.buildLoc = center.add(buildDir, octDiag);
        this.constructionZone = farmCenter.add(buildDirection, octDiag + 2 + GameConstants.GENERAL_SPAWN_OFFSET);
        this.treePlantingLocs[0] = center.add(firstMove, octDiag);
        this.treeLocs[0] = center.add(firstMove, octDiag + 2);
        this.treeDirections[0] = treeDir;

        for(int T = 1; T < 7; T++) {
            MapLocation curLoc = this.treePlantingLocs[T-1];
            treeDir = firstMove.rotateLeftDegrees(45*T);
            moveDir = treeDir.rotateLeftDegrees((float) 67.5);

            this.treePlantingLocs[T] = curLoc.add(moveDir, octEdge);
            this.treeLocs[T] = this.treePlantingLocs[T].add(treeDir, 2 + GameConstants.GENERAL_SPAWN_OFFSET);
            this.treeDirections[T] = treeDir;
        }
    }

    public MapLocation getFarmCenter() {
        return farmCenter;
    }

    public Direction getBuildDirection() {
        return buildDirection;
    }

    public MapLocation getBuildLoc() {
        return buildLoc;
    }
    public MapLocation getConstructionZone() {
        return constructionZone;
    }

    public MapLocation[] getTreeLocs() {
        return treeLocs;
    }

    public MapLocation[] getTreePlantingLocs() {
        return treePlantingLocs;
    }

    public Direction[] getTreeDirections() {
        return treeDirections;
    }

    public void drawFarm(RobotController rc) throws GameActionException { // draws all tree locations in green
        for (MapLocation t: treeLocs) {
            if (t != null && rc.onTheMap(t)) rc.setIndicatorDot(t, 0, 200, 0);
        }
    }
}
