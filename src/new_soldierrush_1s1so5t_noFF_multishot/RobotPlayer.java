package new_soldierrush_1s1so5t_noFF_multishot;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import robotcore.*;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        try {
            RobotGlobal.init(rc);
            RobotGlobal.setInitialBuildQueue1(new RobotType[]{
            		RobotType.SOLDIER,
            		RobotType.SCOUT
            });
            RobotGlobal.setInitialBuildQueue2(new RobotType[]{
            		RobotType.TANK,
                    RobotType.TANK,
                    RobotType.TANK,
                    RobotType.TANK,
                    RobotType.TANK
            });
            RobotGlobal.setInitialDefaultBuild(RobotType.SOLDIER);
            RobotGlobal.setGardenerSchedule(RobotGlobal.GardenerSchedule.ONCE_EVERY_N_ROUNDS);
            RobotGlobal.setGardenerScheduleN(150);
            RobotGlobal.friendlyFireOn = false;
            RobotGlobal.useTriad = true;
            RobotGlobal.usePentad = true;
            
        } catch (Exception e) {
            System.out.println("Exception during global init");
            e.printStackTrace();
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
