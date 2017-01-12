package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;

public class GardenerBot extends RobotGlobal {

    static Direction goDir;

    public static void loop() {
        goDir = randomDirection();
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
        processNearbyBullets();
        if (roundNum % 20 == 1) {
            goDir = randomDirection();
        }
        Direction buildDir = randomDirection();

        RobotType currentBuildOrder = getBuildOrder();

        // Randomly attempt to build a soldier or lumberjack in this direction
        if (rc.canBuildRobot(currentBuildOrder, buildDir)) {
            rc.buildRobot(currentBuildOrder, buildDir);
        }

        // Move randomly
        tryMoveElseLeftRight(randomDirection());

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
