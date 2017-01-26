package robotcore;

import battlecode.common.*;


public class TankBot extends RobotGlobal {
	static final int iNORTH = 0;
    static final int iEAST = 1;
    static final int iSOUTH = 2;
    static final int iWEST = 3;
    
    static MapLocation nearestToMe = null;
    static MapLocation nearestToEnemy = null;
    static MapLocation furthestFromMe = null;
    static MapLocation furthestFromEnemy = null;
	
	static Direction goDir;
	static boolean firstTurn = true;
	static int goCount = 0;
	static RobotInfo nearestEnemy = null; 
	static MapLocation attackLoc = null;
	static boolean friendlyFireOn = false;
	static boolean wasAttacking = false;
	static int attackRound = 0;
	
	
	public static Direction offsetTarget(MapLocation target) throws GameActionException { // gives a random angle offset for shooting
		debugTick(6);
		float offsetDistMax = 2.5f;
		MapLocation newTarget = null;
		int c = 0;
		debugTick(7);
		while (newTarget == null) {
			c++;
			debugTick(7+c);
			try{
				Direction dir = myLoc.directionTo(target);
				
				if (c > 5) {
					dir = myLoc.directionTo(target);
					newTarget = target;
					break;
				}

				if (Math.random() > 0.5) dir = dir.rotateLeftDegrees(90);
				else dir = dir.rotateRightDegrees(90);
				
				float offsetDist = (float) (Math.random()*offsetDistMax);

				newTarget = target.add(dir, offsetDist);
				

				
			} catch (Exception e) {
				System.out.println("blah: " + e.getMessage());
				e.getMessage();
			}

		}
		debugTick(15);
		rc.setIndicatorDot(newTarget, 225, 100, 0);
		return myLoc.directionTo(newTarget);
	}
	
	public static MapLocation parseMap() {
		if (attackLoc == null) return null;
		MapLocation knownNE = knownMapBounds.getInnerCornerLoc(MapBounds.NORTH, MapBounds.EAST);
        MapLocation knownSE = knownMapBounds.getInnerCornerLoc(MapBounds.SOUTH, MapBounds.EAST);
        MapLocation knownNW = knownMapBounds.getInnerCornerLoc(MapBounds.NORTH, MapBounds.WEST);
        MapLocation knownSW = knownMapBounds.getInnerCornerLoc(MapBounds.SOUTH, MapBounds.WEST);
        
        float distToTarget = myLoc.distanceTo(attackLoc);
		
        float knownHeight = knownMapBounds.getInnerBound(iNORTH) - knownMapBounds.getInnerBound(iSOUTH);
        float knownWidth = knownMapBounds.getInnerBound(iEAST) - knownMapBounds.getInnerBound(iWEST);
        float avgY = knownMapBounds.getInnerBound(iSOUTH) + knownHeight * 0.5f;
        float avgX = knownMapBounds.getInnerBound(iWEST) + knownWidth * 0.5f;
        
        MapLocation knownMidNorth = new MapLocation(avgX, knownMapBounds.getInnerBound(iNORTH));
        MapLocation knownMidSouth = new MapLocation(avgX, knownMapBounds.getInnerBound(iSOUTH));
        MapLocation knownMidEast = new MapLocation(knownMapBounds.getInnerBound(iEAST), avgY);
        MapLocation knownMidWest = new MapLocation(knownMapBounds.getInnerBound(iWEST), avgY);
        
        MapLocation[] spots = new MapLocation[] {
        		knownNE.add(Direction.SOUTH, 2).add(Direction.WEST, 2),
        		knownSE.add(Direction.NORTH, 2).add(Direction.WEST, 2),
        		knownNW.add(Direction.SOUTH, 2).add(Direction.EAST, 2),
        		knownSW.add(Direction.NORTH, 2).add(Direction.EAST, 2),
        		knownMidNorth.add(Direction.SOUTH, 2),
        		knownMidWest.add(Direction.EAST, 2),
        		knownMidSouth.add(Direction.NORTH, 2),
        		knownMidEast.add(Direction.WEST, 2)
        };
        
        
        float minDistToMe = Float.POSITIVE_INFINITY;
        float minDistToEnemy = Float.POSITIVE_INFINITY;
        float maxDistToMe = 0;
        float maxDistToEnemy = 0;
        
        for (MapLocation l: spots) {
        	float dM = l.distanceTo(myLoc);
        	float dE = l.distanceTo(attackLoc);
        	
        	if (dM < minDistToMe) {
        		nearestToMe = l;
        	}
        	if (dE < minDistToEnemy) {
        		nearestToEnemy = l;
        	}
        	
        	minDistToMe = Math.min(dM, minDistToMe);
        	minDistToEnemy = Math.min(dE, minDistToEnemy);
        	
        	if (dM > maxDistToMe) {
        		furthestFromMe = l;
        	}
        	if (dE > maxDistToEnemy) {
        		furthestFromEnemy = l;
        	}
        	
        	maxDistToMe = Math.max(dM, maxDistToMe);
        	maxDistToEnemy = Math.max(dE, maxDistToEnemy);
        }
        
        int r = (int) (Math.random()*spots.length);
        return spots[r];
	}
	/**
	public static MapLocation newAttackLocation() throws GameActionException {
		MapLocation prev = attackLoc;
		MapLocation curr = attackLoc;
		while (prev.equals(curr)){
			curr = popAttackLocation();
			if (curr == null){
				return null;
			}
		}
		
		return curr;
	}
	**/
    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                System.out.println("Tank: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
                Clock.yield();
            } catch (Exception e) {
                System.out.println("Tank: exception during turn");
                e.printStackTrace();
            }
        }
    }
    
    
    public static void turn() throws GameActionException {
		VP();
		processNearbyRobots();
		processNearbyBullets();
		processNearbyTrees();
		parseMap();
		tryToShake();
        if (firstTurn) {
            goDir = randomDirection();
        }
        boolean shoot = true;

        debugTick(1);
        
        if (attackLoc != null) {
        	if (attackLoc.equals(peekAttackLocation())){
            	wasAttacking = true;
            	attackRound ++;
            } else { 
            	wasAttacking = false;
            	attackRound = 0;
            }
        } else { 
        	wasAttacking = false;
        	attackRound = 0;
        }
        
        attackLoc = peekAttackLocation();
        nearestEnemy = getNearestEnemy();
        
        boolean moved = false;
        boolean attacked = false;
        boolean treeInRange = false;
        boolean robotTreeInRange = false;
        Direction robotTreeDir = null;
        Direction treeDir = null;
        TreeInfo nearestTree = getNearestUnfriendlyTree();
        TreeInfo nearestRobotTree = getNearestRobotTree();
        
        debugTick(2);
        if (nearestTree != null) {
            float distToTree = myLoc.distanceTo(nearestTree.location);
            treeDir = myLoc.directionTo(nearestTree.location);
            treeInRange = distToTree <= myType.bodyRadius + myType.strideRadius + nearestTree.radius;
        }
        if (nearestRobotTree != null) { // in case we want to mark these
            float distToRobotTree = myLoc.distanceTo(nearestRobotTree.location);
            robotTreeDir = myLoc.directionTo(nearestRobotTree.location);
            robotTreeInRange = distToRobotTree <= myType.bodyRadius + myType.strideRadius + nearestRobotTree.radius;
        }

        if (nearestEnemy != null) {
        	goDir = myLoc.directionTo(nearestEnemy.location);
        	if (!friendlyFireOn) {
        		shoot = hasLineOfSightFF(nearestEnemy.location); // if this tank is to avoid FriendlyFire
        	}
        } else if (attackLoc != null) { // firing line code
        	debugTick(3);
        	//System.out.println("attRound: " + attackRound);
        	float attackRadius = attackCircleStart - (attackRound * attackCircleChange); // Radius for the firing line
        	
        	debugTick(4);
        	
        	MapLocation firingLineSpot = attackLoc.add(attackLoc.directionTo(myLoc), attackRadius); // Location on the line 
        	
        	MapLocation barrageLoc = parseMap();
        	rc.setIndicatorDot(barrageLoc, 0, 0, 0);
        	
        	rc.setIndicatorDot(firingLineSpot, 222, 222, 222);
        	goDir = myLoc.directionTo(firingLineSpot);
        	
        	debugTick(5);
        	
        	rc.setIndicatorDot(attackLoc, 255, 0, 0);

        	if (!rc.hasMoved()) { moved = tryMoveElseLeftRight(myLoc.directionTo(barrageLoc)); } // <<<<<<<<<<
        	
        	Direction shootAt = offsetTarget(attackLoc);
        	debugTick(12);
        	if (rc.canFireSingleShot() && shoot) {
        		rc.fireSingleShot(shootAt);
        	}
        	debugTick(16);
        	if (myLoc.distanceTo(attackLoc) < myType.sensorRadius) {
        		//newAttackLocation();
        		popAttackLocation();
        		
        	}
        	if (attackRound > 100) { // if bombardment has been long enough, switch targets
        		//newAttackLocation();
        		attackLoc = null;
        	}
        	
        }

        if (nearestEnemy == null && attackLoc == null) {
        	if (treeInRange) { 			// Body-attack the nearest tree
        		rc.setIndicatorDot(nearestTree.location, 100, 100, 100);
        		
        		if (!moved && rc.canMove(treeDir)) rc.move(treeDir);
        		else moved = false;
        	}
        	else {
        		if (!moved) {
        			moved = tryMoveElseLeftRight(goDir, 15, 2);
        		}
        	}
        } else {
        	if (!moved) {
    			moved = tryMoveElseLeftRight(goDir, 15, 2);
    		}
        }
        if (!moved) {
        	moved = tryMoveElseBack(goDir);
        	
        	if (!moved) {
        		goDir = randomDirection();
        	}
        }

        if (nearestEnemy != null) {
        	Direction atEnemy = myLoc.directionTo(nearestEnemy.location);
        	float dist = nearestEnemy.location.distanceTo(myLoc); 
        	if (nearestEnemy.type.bodyRadius > 1) dist --; // this prevents tanks from avoiding pentads on tanks and archons
        	if (shoot) { // if this Tank is to avoid FriendlyFire
        		if (usePentad && rc.canFirePentadShot() && dist < pentadDist + 1) { // if Tank shoots, canFire becomes false
        			rc.firePentadShot(atEnemy);
        		}
        		else if (useTriad && rc.canFireTriadShot() && dist < triadDist + 1) {
                	rc.fireTriadShot(atEnemy);
                }
        		else if (rc.canFireSingleShot()) {
            		rc.fireSingleShot(atEnemy);
            	}	
            }
        }
        
        RobotInfo nearestHostile = getNearestEnemyHostile();
        RobotInfo nearestNonHostile = getNearestEnemyNonHostile();

        // Update attack and defend locations
        if (nearestHostile != null) {
            int whichDefendLoc = whichDefendLocation(nearestHostile.location);
            if (whichDefendLoc >= 0) {
                updateDefendLocation(nearestHostile.location, whichDefendLoc);
            } else {
                addDefendLocationFirst(nearestHostile.location);
            }
        }
        if (nearestNonHostile != null) {
            int whichAttackLoc = whichAttackLocation(nearestNonHostile.location);
            if (whichAttackLoc >= 0) {
                updateAttackLocation(nearestNonHostile.location, whichAttackLoc);
            } else {
                addAttackLocationFirst(nearestNonHostile.location);
            }
        }
        
        firstTurn = false;
    }
}
