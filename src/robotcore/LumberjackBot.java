package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;

public class LumberjackBot extends RobotGlobal {

    public static void loop() {
        while (true) {
            try {
                update();
            } catch (Exception e) {
                System.out.println("Archon: exception during update");
                e.printStackTrace();
            }
            try {
                turn();
            } catch (Exception e) {
                System.out.println("Archon: exception during turn");
                e.printStackTrace();
            }
        }
    }

    public static void turn() throws GameActionException {
        processNearbyRobots();
        processNearbyBullets();
        RobotInfo nearestEnemy = getNearestEnemy();
        Direction goDir = myLoc.directionTo(enemyInitialArchonLocations[0]);
        boolean enemyInRange = false;
        if (nearestEnemy != null) {
            goDir = myLoc.directionTo(nearestEnemy.location);
            float distToEnemy = myLoc.distanceTo(nearestEnemy.location);
            enemyInRange = distToEnemy <= myType.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS + nearestEnemy.type.bodyRadius;
        }

        boolean moved = tryMoveElseLeftRight(goDir);
        if (!moved) {
            moved = tryMoveElseBack(goDir);
        }

        boolean attacked = false;
        if (enemyInRange) {
            if (rc.canStrike()) {
                rc.strike();
                attacked = true;
            }
        }
        if (!attacked) {
            processNearbyTrees();
            TreeInfo nearestTree = getNearestTree();
            boolean treeInRange = false;
            if (nearestTree != null) {
                float distToTree = myLoc.distanceTo(nearestTree.location);
                treeInRange = distToTree <= myType.bodyRadius + myType.strideRadius + nearestTree.radius;
            }
            if (treeInRange) {
                if (rc.canShake(nearestTree.ID)) {
                    rc.shake(nearestTree.ID);
                }
                if (rc.canChop(nearestTree.ID)) {
                    rc.chop(nearestTree.ID);
                    attacked = true;
                }
            }
        }

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
