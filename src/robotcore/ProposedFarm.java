package robotcore;

import battlecode.common.*;

public class ProposedFarm {

    // Farm geometry constants
    static final float hexFarmRadius = 3f;
    static final float tankFarmRadius = 4f;

    // Farm placement data
    private MapLocation farmCenter = null;
    private Direction buildDirection = null;
    private MapLocation buildLoc = null;
    private MapLocation constructionZone = null;
    private MapLocation[] treeLocs = new MapLocation[5];
    private MapLocation[] treePlantingLocs = new MapLocation[5];
    private Direction[] treeDirections = new Direction[5];
    private boolean tankFarm = false;

    public ProposedFarm(MapLocation center, Direction buildDir) {
        this.farmCenter = center;
        this.buildDirection = buildDir;
        float spOff = GameConstants.GENERAL_SPAWN_OFFSET;
        
        this.buildLoc = center;
        this.constructionZone = farmCenter.add(buildDirection, 2 + spOff);

        for(int T = 0; T < 5; T++) {
            Direction treeDir = buildDir.rotateLeftDegrees(60*(T+1));
            this.treePlantingLocs[T] = center;
            this.treeLocs[T] = center.add(treeDir, 2 + spOff);
            this.treeDirections[T] = treeDir;
        }
    }
    
    public ProposedFarm(MapLocation center, Direction buildDir, boolean tankFarm) {
    	this.tankFarm = tankFarm;
    	this.farmCenter = center;
		this.buildDirection = buildDir;
		float spOff = GameConstants.GENERAL_SPAWN_OFFSET;
		this.buildLoc = center;
		//this.constructionZone = farmCenter.add(buildDirection, 2 + spOff);
		
    	if (!tankFarm) {
    		for(int T = 0; T < 5; T++) {
    			Direction treeDir = buildDir.rotateLeftDegrees(60*(T+1));
    			this.treePlantingLocs[T] = center;
    			this.treeLocs[T] = center.add(treeDir, 2 + spOff);
    			this.treeDirections[T] = treeDir;
    		}
    	} else {
    		this.buildDirection = buildDir.rotateRightDegrees(30);
    		
    		for(int T = 0; T < 4; T++) {
    			Direction treeDir = buildDir.rotateLeftDegrees(60*(T+1));
    			this.treePlantingLocs[T] = center;
    			this.treeLocs[T] = center.add(treeDir, 2 + spOff);
    			this.treeDirections[T] = treeDir;
    		}
    	}
    	
    	this.constructionZone = farmCenter.add(buildDirection, 2 + spOff);
    	
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
    
    public void changeFarmType(boolean tankF) {
    	if (this.tankFarm == tankF) {
    		return;
    	} else {
    		this.tankFarm = tankF;
    		
    		MapLocation center = this.farmCenter;
    		Direction buildDir = this.buildDirection;
    		
    		float spOff = GameConstants.GENERAL_SPAWN_OFFSET;
    		this.buildLoc = center;
    		//this.constructionZone = farmCenter.add(buildDirection, 2 + spOff);
    		
        	if (!tankFarm) {
        		buildDirection = buildDirection.rotateLeftDegrees(30);
        		buildDir = this.buildDirection;
        		
        		for(int T = 0; T < 5; T++) {
        			Direction treeDir = buildDir.rotateLeftDegrees(60*(T+1));
        			this.treePlantingLocs[T] = center;
        			this.treeLocs[T] = center.add(treeDir, 2 + spOff);
        			this.treeDirections[T] = treeDir;
        		}
        	} else {
        		this.buildDirection = buildDir.rotateRightDegrees(30);
        		
        		for(int T = 0; T < 4; T++) {
        			Direction treeDir = buildDir.rotateLeftDegrees(60*(T+1));
        			this.treePlantingLocs[T] = center;
        			this.treeLocs[T] = center.add(treeDir, 2 + spOff);
        			this.treeDirections[T] = treeDir;
        		}
        	}
        	
        	this.constructionZone = farmCenter.add(buildDirection, 2 + spOff);
    	}
    }

    public void debug_drawFarm(RobotController rc) throws GameActionException {
        if (RobotGlobal.debug) {
            // draws all tree locations in green
            for (MapLocation t: treeLocs) {
                if (t != null && rc.onTheMap(t)) {
                    RobotGlobal.debug_dot(t, 0, 200, 0);
                }
            }
        }
    }
}
