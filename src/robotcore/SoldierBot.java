package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;

public class SoldierBot extends RobotGlobal {

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
            } catch (Exception e) {
                System.out.println("Soldier: exception during turn");
                e.printStackTrace();
            }
        }
    }

    public static void turn() throws GameActionException {
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

        processNearbyRobots();
        processNearbyBullets();
        RobotInfo nearestEnemy = getNearestEnemy();
        Direction goDir = myLoc.directionTo(enemyInitialArchonLocations[0]);
        if (nearestEnemy != null) {
            goDir = myLoc.directionTo(nearestEnemy.location);
        }

        boolean moved = tryMoveElseLeftRight(goDir);
        if (!moved) {
            moved = tryMoveElseBack(goDir);
        }

        if (nearestEnemy != null) {
            if (rc.canFireSingleShot()) {
                rc.fireSingleShot(myLoc.directionTo(nearestEnemy.location));
            }
        }

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
