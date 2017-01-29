package robotcore;

import battlecode.common.*;


public class SoldierBot extends RobotGlobal {
    static Direction goDir;
    static MapLocation attackLoc = null;
    static boolean wasAttacking = false;
	static int attackRound = 0;

    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                System.out.println("Soldier: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
                Clock.yield();
            } catch (Exception e) {
                System.out.println("Soldier: exception during turn");
                e.printStackTrace();
            }
        }
    }



    public static void turn() throws GameActionException {
        processNearbyRobots();
        processNearbyBullets();
        processNearbyTrees();
        tryToShake();
        elections();

        registerSoldier();

        if (firstTurn) {
            goDir = randomDirection();
        }

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

        MapLocation attackLoc = peekAttackLocation();
        MapLocation defendLoc = peekDefendLocation();
        RobotInfo nearestHostile = getNearestEnemyHostile();
        RobotInfo nearestNonHostile = getNearestEnemyNonHostile();
        boolean moved = false;
        boolean shoot = true;
        debugTick(0);
        
        MapLocation selectedGardenerLoc = null;
        RobotInfo selectedGardenerInfo = getNearestEnemyGardener();
		if (selectedGardenerInfo != null) {
			selectedGardenerLoc = selectedGardenerInfo.location;
		}

        if (nearestHostile != null) { // If there is a nearby hostile enemy
            // Move towards it or kite it
            Direction atHostile = myLoc.directionTo(nearestHostile.location);
            if (kite) {
                moved = kiteEnemy(nearestHostile, avoidRadius);
            } else {
                moved = tryMoveElseLeftRight(atHostile, 15, 8);
            }
            atHostile = myLoc.directionTo(nearestHostile.location);

            if (!friendlyFireOn) {
        		shoot = hasLineOfSightFF(nearestHostile.location); // if this soldier is to avoid FriendlyFire
        	}
            // Shoot at it if close enough
            float dist = nearestHostile.location.distanceTo(myLoc);
            if (shoot) { // if this soldier is to avoid FriendlyFire
                if (usePentad && rc.canFirePentadShot() && dist < pentadDist) { // if soldier shoots, canFire becomes false
                    rc.firePentadShot(atHostile);
                }
                else if (useTriad && rc.canFireTriadShot() && dist < triadDist) {
                    rc.fireTriadShot(atHostile);
                }
                else if (rc.canFireSingleShot() && dist < 3.5f) { // TODO: factor out
                    rc.fireSingleShot(atHostile);
                }
            }

        } else if (defendLoc != null) { // Otherwise, if there is a location to defend
            // Move towards it
            Direction defendDir = myLoc.directionTo(defendLoc);
            moved = tryMoveElseLeftRight(defendDir, 15, 4);

            // If I'm close to the defend target, I already know there's no hostile, so pop it
            if (myLoc.distanceTo(defendLoc) < myType.bodyRadius * 2) {
                popDefendLocation();
            }
        } else if (attackLoc != null) { // Otherwise, if there is a location to attack
            // Move towards it
            Direction attackDir = myLoc.directionTo(attackLoc);
            moved = tryMoveElseLeftRight(attackDir, 15, 4);

            // If I'm close to the attack target, I already know there's no non-hostile, so pop it
            if (myLoc.distanceTo(attackLoc) < myType.bodyRadius * 2) {
                popAttackLocation();
            }
        } else { // Otherwise, bounce around
            moved = tryMoveElseBack(goDir);
            if (!moved) {
                goDir = randomDirection();
                moved = tryMoveElseBack(goDir);
            }
        }

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
        	if (!friendlyFireOn) {
        		shoot = hasLineOfSightFF(nearestNonHostile.location); // if this soldier is to avoid FriendlyFire
        	}
        	if (selectedGardenerInfo != null && shoot) {
        		Direction atGard = myLoc.directionTo(selectedGardenerLoc);
        		float dist = myLoc.distanceTo(selectedGardenerLoc);
        		
        		if (usePentad && rc.canFirePentadShot() && dist < pentadDist) { // if soldier shoots, canFire becomes false
                    rc.firePentadShot(atGard);
                }
                else if (useTriad && rc.canFireTriadShot() && dist < triadDist) {
                    rc.fireTriadShot(atGard);
                }
                else if (rc.canFireSingleShot()) {
                    rc.fireSingleShot(atGard);
                }
        	}
        	
        	if (attackRound > 200 && shoot) {
        		System.out.println("Attacking non-hostiles!");
        		Direction atThing = myLoc.directionTo(nearestNonHostile.location);
        		float dist = myLoc.distanceTo(nearestNonHostile.location);
        		if (usePentad && rc.canFirePentadShot() && dist < pentadDist) { // if soldier shoots, canFire becomes false
                    rc.firePentadShot(atThing);
                }
                else if (useTriad && rc.canFireTriadShot() && dist < triadDist) {
                    rc.fireTriadShot(atThing);
                }
                else if (rc.canFireSingleShot()) {
                    rc.fireSingleShot(atThing);
                }
        	}
        	
            int whichAttackLoc = whichAttackLocation(nearestNonHostile.location);
            if (whichAttackLoc >= 0) {
                updateAttackLocation(nearestNonHostile.location, whichAttackLoc);
            } else {
                addAttackLocationFirst(nearestNonHostile.location);
            }
        }
    }

   

}
