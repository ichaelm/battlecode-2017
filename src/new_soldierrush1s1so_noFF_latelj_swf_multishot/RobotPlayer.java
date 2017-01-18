package new_soldierrush1s1so_noFF_latelj_swf_multishot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import robotcore.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            RobotGlobal.init(rc);
            RobotGlobal.setInitialBuildQueue1(new RobotType[]{
                    RobotType.SCOUT,
                    RobotType.SOLDIER
            });
            RobotGlobal.setInitialDefaultBuild(RobotType.SOLDIER);
            RobotGlobal.setGardenerSchedule(RobotGlobal.GardenerSchedule.WHEN_FULL);
            RobotGlobal.friendlyFireOn = false;
            RobotGlobal.setLateLumberjacks(true);
            RobotGlobal.setScoutWhenFull(true);
            RobotGlobal.useTriad = true;
            RobotGlobal.usePentad = true;
        } catch (Exception e) {
            System.out.println("Exception during global init");
            e.printStackTrace();
        }

        switch (RobotGlobal.myType) {
            case ARCHON:
                NewArchonBot.loop();
                break;
            case GARDENER:
                NewGardenerBot.loop();
                break;
            case SOLDIER:
                NewSoldierBot.loop();
                break;
            case LUMBERJACK:
                NewLumberjackBot.loop();
                break;
            case SCOUT:
                NewScoutBot.loop();
                break;
            case TANK:
                NewTankBot.loop();
                break;
        }
	}

}