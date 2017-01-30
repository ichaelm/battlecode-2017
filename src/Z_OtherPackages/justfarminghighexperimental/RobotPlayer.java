package justfarminghighexperimental;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import robotcore.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            RobotGlobal.init(rc);
            RobotGlobal.setGardenerSchedule(RobotGlobal.GardenerSchedule.ONCE_EVERY_N_ROUNDS);
            RobotGlobal.setGardenerScheduleN(150);
            RobotGlobal.setExperimental(true);
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
