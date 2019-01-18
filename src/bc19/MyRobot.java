package bc19;

//import java.util.Arrays;

public class MyRobot extends BCAbstractRobot {
    int step = -1;

    int tradeKarbon = 0;
    int tradeFuel = 0;

    public Action turn() {
        step++;

        if(me.unit == SPECS.CASTLE) {
            int[][] visibleMap = getVisibleRobotMap();
            if(isEmpty(1,1) && !isOccupied(1, 1)) {
                log("Creating a pilgrim to the south east");
                return buildUnit(SPECS.PILGRIM, 1, 1);
            }
            return proposeTrade(tradeKarbon, tradeFuel);
        }

        else if(me.unit == SPECS.CHURCH) {

        }

        else if(me.unit == SPECS.PILGRIM) {
            //return move(1, 1);
        }

        else if(me.unit == SPECS.CRUSADER) {

        }

        else if(me.unit == SPECS.PROPHET) {

        }

        else if(me.unit == SPECS.PREACHER) {

        }

        else {
            log("This unit is not a knows unit");
        }
    }

    public boolean isEmpty(int dx, int dy) {
        //log("passsable: " + String.valueOf(map[me.x + dx][me.y + dy]));
        return map[me.x + dx][me.y + dy];
    }

    public boolean isOccupied(int dx, int dy) {
        int posX = me.x + dx;
        int posY = me.y + dy;
        Robot[] robots = getVisibleRobots();

        for(int i = 0; i < robots.length; i++) {
            if(robots[i].x == posX && robots[i].y == posY) {
                //log("it's occupied");
                return true;
            }
        }
        return false;
    }

    public Point findOpenBuildSpot() {
    }
}













