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
        debugTick(1);
        processNearbyRobots();
        debugTick(2);
        processNearbyBullets();
        debugTick(3);
        RobotInfo nearestEnemy = getNearestEnemy();
        debugTick(4);
        int a = 0;
        a = roundNum >=  1 ? (int) (Math.random()*enemyInitialArchonLocations.length) : a;
        rc.setIndicatorDot(enemyInitialArchonLocations[a], 255, 0, 255);
        Direction goDir = myLoc.directionTo(enemyInitialArchonLocations[a]);
        goDir = rc.canMove(goDir) ? goDir: randomDirection();
        if (nearestEnemy != null) {
            goDir = myLoc.directionTo(nearestEnemy.location);
        }

        debugTick(5);
        boolean moved = tryMoveElseLeftRight(goDir);
        debugTick(6);
        if (!moved) {
            moved = tryMoveElseBack(goDir);
        }
        debugTick(7);

        if (nearestEnemy != null) {
            if (rc.canFireSingleShot()) {
                rc.fireSingleShot(myLoc.directionTo(nearestEnemy.location));
            }
        }
        debugTick(8);

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
