package robotcore;

import battlecode.common.*;
import robotcore.*;

public class ArchonBot extends RobotGlobal {

    static boolean hasBuiltGardner = false;
    static boolean shouldBuild = false;
    static boolean plenty = true;

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
        RobotType currentBuildOrder = getBuildOrder();
        float currentBuildOrderCost = currentBuildOrder.bulletCost;
        boolean newShouldBuild = false;

        if (teamBullets < currentBuildOrderCost) {
            plenty = false;
        }

        // Generate a random direction
        Direction dir = randomDirection();

        if (!hasBuiltGardner) {
            // Build a gardener in this direction
            if (rc.canHireGardener(dir)) {
                rc.hireGardener(dir);
                hasBuiltGardner = true;
            }
        } else {
            // Check if build has been missed
            if (teamBullets >= currentBuildOrderCost && !plenty) {
                if (shouldBuild) {
                    // Build has been missed!
                    // Build a gardner
                    if (rc.canHireGardener(dir)) {
                        rc.hireGardener(dir);
                        hasBuiltGardner = true;
                    }
                } else {
                    // Build should happen this turn
                    newShouldBuild = true;
                }
            }
        }

        shouldBuild = newShouldBuild;

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
