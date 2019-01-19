package bc19;

public class MyRobot extends BCAbstractRobot {
    int step = -1;

    int tradeKarbon = 0;
    int tradeFuel = 0;

    //Use these as one tile steps in a direction
    int[] NORTH = {0, -1};
    int[] NORTHEAST = {1, -1};
    int[] EAST = {1, 0};
    int[] SOUTHEAST = {1, 1};
    int[] SOUTH = {0, 1};
    int[] SOUTHWEST = {-1, 1};
    int[] WEST = {-1, 0};
    int[] NORTHWEST = {-1, -1};

    //Call this array to cycle through all directions easily
    int[][] myDirections = {NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST};

    Robot[] robots; //At the beginning of each turn this array will be filled with all robots visible to me

    public Action turn() {
        robots = getVisibleRobots();
        step++;

        if(me.unit == SPECS.CASTLE) {

            int pref = findOpenBuildSpot(0);
            if(pref > -1) {
                log("Building Unit");
                return buildUnit(SPECS.PILGRIM, myDirections[pref][0], myDirections[pref][1]);
            }
            return proposeTrade(tradeKarbon, tradeFuel); //If the castle can't find anything to do, it ends its turn by proposing a trade
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
            log("This unit is not a known unit");
        }
    }


    //Methods


    //Checks to see if the tile dx and dy away from me is passable.
    public boolean isPassable(int dx, int dy) {
        return map[me.y + dy][me.x + dx];
    }

    //Checks to see if the tile dx and dy away from me is occupied by another robot
    public boolean isOccupied(int dx, int dy) {
        int posX = me.x + dx;
        int posY = me.y + dy;

        for(int i = 0; i < robots.length; i++) {
            if(robots[i].x == posX && robots[i].y == posY) {
                return true;
            }
        }
        return false;
    }

    //Checks to see if the tile is empty (no mountains or robots there)
    public boolean isEmpty(int dx, int dy) {
        return isPassable(dx, dy) && !isOccupied(dx, dy);
    }

    //Cycles through all tiles adjacent to me and returns the direction of an open spot to build
    //The pref in the preffered direction to build in
    //The int returned in the myDirections index of the correct direction
    public int findOpenBuildSpot(int pref) {
        if(isEmpty(myDirections[pref%myDirections.length][0], myDirections[pref%myDirections.length][1]))
            return pref%myDirections.length;

        for(int i = 1; i < 5; i++) {
            if(isEmpty(myDirections[(pref + i)%myDirections.length][0], myDirections[(pref + i)%myDirections.length][1])) //Clockwise
                return (pref + i)%myDirections.length;

            pref += myDirections.length;

            if(isEmpty(myDirections[(pref - i)%myDirections.length][0], myDirections[(pref - i)%myDirections.length][1])) //Counterclockwise
                return (pref - i)%myDirections.length;
            pref -= myDirections.length;
        }
        log("No open positions");
        return -1;
    }

    //Returns the distance between two tiles
    public int distanceBetween(int x1, int y1, int x2, int y2) {
        return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }

    //Returns the distance me is from another robot
    public int distanceFrom(Robot r) {
        return Math.abs(r.x - me.x) + Math.abs(r.y - me.y);
    }
}













