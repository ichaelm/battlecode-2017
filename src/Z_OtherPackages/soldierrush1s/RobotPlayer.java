package soldierrush1s;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import robotcore.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            RobotGlobal.init(rc);
            RobotGlobal.setInitialBuildQueue1(new RobotType[]{
                    RobotType.SCOUT
            });
            RobotGlobal.setInitialDefaultBuild(RobotType.SOLDIER);
            RobotGlobal.setGardenerSchedule(RobotGlobal.GardenerSchedule.WHEN_FULL);
        } catch (Exception e) {
            RobotGlobal.debug_exception(e, "init");
        }

        switch (RobotGlobal.myType) {
            case ARCHON:
                ArchonBot.loop();
                break;
            case GARDENER:
                GardenerBot.loop();
                break;
            case SOLDIER:
                SoldierBot.loop();
                break;
            case LUMBERJACK:
                LumberjackBot.loop();
                break;
            case SCOUT:
                ScoutBot.loop();
                break;
            case TANK:
                TankBot.loop();
                break;
        }
	}

}
