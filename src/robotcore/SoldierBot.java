package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;



public class SoldierBot extends RobotGlobal {
    static Direction goDir;
    static boolean firstTurn = true;

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
        VP();
        processNearbyRobots();
        processNearbyBullets();
        processNearbyTrees();
        tryToShake();
        if (firstTurn) {
            goDir = randomDirection();
        }

        MapLocation attackLoc = peekAttackLocation();
        MapLocation defendLoc = peekDefendLocation();
        RobotInfo nearestHostile = getNearestEnemyHostile();
        RobotInfo nearestNonHostile = getNearestEnemyNonHostile();
        boolean moved = false;
        boolean shoot = true;
        debugTick(0);
        

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
            int whichAttackLoc = whichAttackLocation(nearestNonHostile.location);
            if (whichAttackLoc >= 0) {
                updateAttackLocation(nearestNonHostile.location, whichAttackLoc);
            } else {
                addAttackLocationFirst(nearestNonHostile.location);
            }
        }

        firstTurn = false;
    }

    void doesNothing(){
   	 /*
       int moveMode = 0; // 0 is approach, 1 is kite lumberjack, 2 is run from lumberjack
       Direction moveDir = null;

       MapLocation myLocation = rc.getLocation();
       MapLocation closestEnemyLoc = null;
       float closestEnemyDist = 9999999;

       // See if there are any nearby enemy robots
       RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

       for (RobotInfo robot : robots) {
           MapLocation robotLocation = robot.getLocation();
           float dist = robot.getLocation().distanceTo(myLocation);
           if (dist < closestEnemyDist) {
               closestEnemyLoc = robotLocation;
               closestEnemyDist = dist;
           }
           RobotType type = robot.getType();
           if (type == RobotType.LUMBERJACK) {
               if (dist <= GameConstants.LUMBERJACK_STRIKE_RADIUS + RobotType.LUMBERJACK.bodyRadius) {
                   moveMode = 2; // run
                   moveDir = robotLocation.directionTo(myLocation);
               } else if (dist <= GameConstants.LUMBERJACK_STRIKE_RADIUS + RobotType.LUMBERJACK.bodyRadius + RobotType.SOLDIER.strideRadius) {
                   moveMode = 1; // kite
                   moveDir = robotLocation.directionTo(myLocation).rotateLeftDegrees(90);
               }
           }
       }
       if (moveMode == 0) {
           if (closestEnemyLoc == null) {
               moveDir = randomDirection();
           } else {
               moveDir = myLocation.directionTo(closestEnemyLoc);
           }
       }
   	  */

    }

}
