package robotcore;

import battlecode.common.*;

public class ProposedFarm {

    // Farm geometry constants
    static final float hexFarmRadius = 3f;

    // Farm placement data
    private MapLocation farmCenter = null;
    private Direction buildDirection = null;
    private MapLocation buildLoc = null;
    private MapLocation constructionZone = null;
    private MapLocation[] treeLocs = new MapLocation[5];
    private MapLocation[] treePlantingLocs = new MapLocation[5];
    private Direction[] treeDirections = new Direction[5];

    public ProposedFarm(MapLocation center, Direction buildDir) {
        this.farmCenter = center;
        this.buildDirection = buildDir;
        Direction firstMove = buildDir.rotateLeftDegrees(60);

        Direction treeDir = firstMove;
        Direction moveDir;
        float spOff = GameConstants.GENERAL_SPAWN_OFFSET;
        
        this.buildLoc = center;
        this.constructionZone = farmCenter.add(buildDirection, 2 + spOff);
        this.treePlantingLocs[0] = center;
        this.treeLocs[0] = center.add(firstMove, 2);
        this.treeDirections[0] = treeDir;

        for(int T = 1; T < 5; T++) {
            MapLocation curLoc = this.treePlantingLocs[T-1];
            treeDir = firstMove.rotateLeftDegrees(60*T);

            this.treePlantingLocs[T] = center;
            this.treeLocs[T] = this.treePlantingLocs[T].add(treeDir, 2 + spOff);
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

    public void drawFarm(RobotController rc) throws GameActionException { 
    	// draws all tree locations in green
        for (MapLocation t: treeLocs) {
            if (t != null && rc.onTheMap(t)) rc.setIndicatorDot(t, 0, 200, 0);
        }
    }
}
