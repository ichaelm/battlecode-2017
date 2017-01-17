package robotcore;

import battlecode.common.*;
import robotcore.RobotGlobal;

public class TankBot extends RobotGlobal {

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
            } catch (Exception e) {
                System.out.println("Tank: exception during turn");
                e.printStackTrace();
            }
        }
    }

    public static void turn() throws GameActionException {
    	tryToShake();
        // Generate a random direction
        Direction dir = randomDirection();

        // Move randomly
        tryMoveElseLeftRight(randomDirection());

        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();
    }
}
