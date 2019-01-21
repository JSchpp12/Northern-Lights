package bc19;
import java.util.ArrayList;

public class MyRobot extends BCAbstractRobot {

    /*
    Note: the variables posX and posY always refer to the actual poition on the map (as in map[posY][posX]
        the variables dx and dy always refer to the distance from me
        (me is the variable for the robot currently running)
     */

    int step = -1;

    //Globals
    int tradeKarbon = 0;
    int tradeFuel = 0;

    int friendlyCastles = 0;
    int enemyCastles = 0;

    int mapLength;
    int clusterRadius = 4;
    int castleClusterRadius = 6;

    int maxKarbonite = 20;
    int maxFuel = 100;

    int UNKOWN = -1;
    int MINE = 0;
    int RETURN_RESOURCE = 1;
    int SPAWN_ADJACENT = 2;
    int SPAWN_NEARBY = 3;

    int goal = UNKOWN;

    //Unit Variables
    int destinationX;
    int destinationY;

    //Pilgrim Variables
    boolean stationaryUnit = false;
    int homeCastle; //A direction

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
    ArrayList clusterX;
    ArrayList clusterY;
    //Clusters don't count if they are near a castle

    //Castle variables
    ArrayList adjacentResource;
    ArrayList nearbyResourceX;
    ArrayList nearbyResourceY;

    ArrayList friendlyCastlesX;
    ArrayList friendlyCastlesY;
    ArrayList friendlyCastlesId;
    ArrayList enemyCastlesX;
    ArrayList enemyCastlesY;

    boolean symmetry;

    boolean HORIZONTAL = true;
    boolean VERTICAL = false;

    int myTeam;

    //Turn ---------------------------------------------

    public Action turn() {
        robots = getVisibleRobots();
        step++;

        if(me.unit == SPECS.CASTLE) {

            //First Turn
            if(step == 0) {
                log("Let's go");
                initializeCastle();
                determineSymmetry();

                findClusters();
                removeClustersNearCastle();

                for(int i = 0; i < clusterY.size(); i++) { //List Clusters
                    //log("Cluster: (" + String.valueOf(clusterX.get(i)) + "," + String.valueOf(clusterY.get(i)) + ")");
                }

                findAdjacentResources();
                findNearbyResources();

                broadcastLocationX();
            } else if(step == 1) {
                determineFriendlyCastlesX();
            } else if(step == 2) {
                broadcastLocationY();
            } else if(step == 3) {
                determineFriendlyCastlesY();
                determineEnemyCastles();
                removeClustersNearCastles();
                log("Clusters: " + String.valueOf(clusterY.size()));
            }

            // Goals -----------------------------------

            if(goal == SPAWN_ADJACENT) { //Spawn pilgrims on resources adjacent to this castle
                if(adjacentResource.size() < 1)
                    goal = SPAWN_NEARBY;
                else {
                    int direction = findOpenBuildSpot((int)adjacentResource.get(0));
                    adjacentResource.remove(0);
                    //log("Building pilgrim for adjacent resource");
                    return buildUnit(SPECS.PILGRIM, myDirections[direction][0], myDirections[direction][1]);
                }
            }
            if(goal == SPAWN_NEARBY) { //Spawn a pilgrim to handle resources within a radius of the castle
                if(nearbyResourceX.size() < 1)
                    goal = UNKOWN;
                else {
                    int direction = directionTo((int)nearbyResourceX.get(0), (int)nearbyResourceY.get(0));
                    direction = findOpenBuildSpot(direction);
                    nearbyResourceX.remove(0);
                    nearbyResourceY.remove(0);
                    //log("Building a pilgrim for nearby resource");
                    return buildUnit(SPECS.PILGRIM, myDirections[direction][0], myDirections[direction][1]);
                }
            }

            return proposeTrade(tradeKarbon, tradeFuel); //If the castle can't find anything to do, it ends its turn by proposing a trade
        }

        // Church ---------------------------------------------------------------------------

        else if(me.unit == SPECS.CHURCH) {

        }

        else if(me.unit == SPECS.PILGRIM) {
            if(step == 0) { //First Turn
                initializePilgrim();
            }

            if(goal == UNKOWN) {
                if(isOnResource())
                    goal = MINE;
            }
            if(hasMaxResource()) {
                goal = RETURN_RESOURCE;
            }

            if(goal == MINE) {
                //log("Mining");
                return mine();
            }
            else if(goal == RETURN_RESOURCE) {
                //log("Giving back resource");
                goal = MINE;
                return give(myDirections[homeCastle][0], myDirections[homeCastle][1], me.karbonite, me.fuel);
            }
        }

        else if(me.unit == SPECS.CRUSADER) {

        }

        else if(me.unit == SPECS.PROPHET) {
            return move(1, 1);
        }

        else if(me.unit == SPECS.PREACHER) {

        }

        else {
            log("This unit is not a known unit");
        }
    }


    //Methods --------------------

    //Bradcast Location on castle talk. broadcasts 64 if me.x == 0
    public void broadcastLocationX() {
        castleTalk(me.x);
        if(me.x == 0)
            castleTalk(64);
    }

    //Bradcast Location on castle talk. braodcasts 64 if me.y == 0
    public void broadcastLocationY() {
        castleTalk(me.y);
        if(me.y == 0)
            castleTalk(64);
    }

    //Learn the X position of all castles
    public void determineFriendlyCastlesX() {
        for(int i = 0; i < robots.length; i++) {
            if(robots[i].castle_talk > 0 && robots[i].team == myTeam) {
                if(robots[i].castle_talk == 64)
                    friendlyCastlesX.add(0);
                else
                    friendlyCastlesX.add(robots[i].castle_talk);
                friendlyCastlesId.add(robots[i].id);
                friendlyCastles++;
                enemyCastles++;
            }
        }
        broadcastLocationX();
    }

    //Learn the Y position of all castles
    public void determineFriendlyCastlesY() {
        for(int h = 0; h < friendlyCastlesId.size(); h++) {
            for(int i = 0; i < robots.length; i++) {
                if(robots[i].id == (int)friendlyCastlesId.get(h)) {
                    if(robots[i].castle_talk == 64)
                        friendlyCastlesY.add(0);
                    else
                        friendlyCastlesY.add(robots[i].castle_talk);
                    break;
                }
            }
        }
        broadcastLocationY();
    }

    //Learn the locations of all enemy castles
    public void determineEnemyCastles() {
        for(int i = 0; i < friendlyCastlesX.size(); i++) {
            if(symmetry == VERTICAL) {
                enemyCastlesX.add(mapLength - (int) friendlyCastlesX.get(i) - 1);
                enemyCastlesY.add((int)friendlyCastlesY.get(i));
            } else {
                enemyCastlesX.add((int) friendlyCastlesX.get(i));
                enemyCastlesY.add(mapLength - (int) friendlyCastlesY.get(i) - 1);
            }
            log("Enemy castle at: " + String.valueOf((int)enemyCastlesX.get(i)) + "," + String.valueOf((int)
                    enemyCastlesY.get(i)));
        }
    }

    //Records if the map is horizontally or vertically symmetric.
    //Depending on the map it is possible this method will fail but it is not likely.
    public void determineSymmetry() {
        for(int i = 0; i < mapLength/2; i++) {
            if(map[i][i] != map[mapLength - 1 - i][mapLength - 1 - i]) {
                if(map[i][mapLength - 1 - i] == map[i][i]) {
                    symmetry = VERTICAL;
                    //log("Symmetry: VERTICAL");
                }
                else {
                    symmetry = HORIZONTAL;
                    //log("Symmetry: HORIZONTAL");
                }
                return;
            }
        }
        symmetry = VERTICAL;
        log("Symmetry not found. Assuming VERTICAL");
    }

    //Locates Resources within a radius of a castle or church
    public void findNearbyResources() {
        for(int i = 0; i < 2*castleClusterRadius + 1; i++) {
            int myHeight = 2*i + 1;
            if(i > castleClusterRadius + 1)
                myHeight = 2*(2*castleClusterRadius - i) + 1;
            for(int j = 0; j < myHeight; j++) {
                if(offMap(i - castleClusterRadius + me.x, j - (myHeight - 1)/2 + me.y))
                    continue;
                if(isAdjacentToTile(i - castleClusterRadius, j - (myHeight - 1)/2)) {
                    continue; //Don't count adjacent resources as they are already in the adjacent tile array
                }
                if(hasResource(i - castleClusterRadius, j - (myHeight - 1)/2)) {
                    nearbyResourceX.add(i - castleClusterRadius + me.x);
                    nearbyResourceY.add(j - (myHeight - 1)/2 + me.y);
                }
            }
        }
    }

    //Checks to see if there is a resource dx, dy from me
    public boolean hasResource(int dx, int dy) {
        if(offMap(me.x + dx, me.y + dy))
            return false;
        return karboniteMap[me.y + dy][me.x + dx] || fuelMap[me.y + dy][me.x + dx];
    }

    //Checks if tile posX, posY is off map
    public boolean offMap(int posX, int posY) {
        if(posX >= mapLength || posY >= mapLength || posX < 0 || posY < 0)
            return true;
        return false;
    }

    //Checks to see if the tile dx and dy away from me is passable.
    public boolean isPassable(int dx, int dy) {
        if(offMap(me.x + dx, me.y + dy))
            return false;
        return map[me.y + dy][me.x + dx];
    }

    //Checks to see if the tile dx and dy away from me is occupied by another robot
    public boolean isOccupied(int dx, int dy) {
        int posX = me.x + dx;
        int posY = me.y + dy;

        if(offMap(posX, posY))
            return false;

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

    //Return the direction a robots is in if adjacent. Otherwise returns -1
    public int isAdjacentTo(Robot r) {
        for(int i = 0; i < myDirections.length; i++) {
            if(r.x == me.x + myDirections[i][0] && r.y == me.y + myDirections[i][1])
                return i;
        }
        return -1;
    }

    //Checks if tile dx, dy away is adjacent to me
    public boolean isAdjacentToTile(int dx, int dy) {
        if(Math.abs(dx) < 2 && Math.abs(dy) < 2)
            return true;
        return false;
    }

    //The init function for castles
    public void initializeCastle() {
        myTeam = me.team;
        mapLength = map.length;
        goal = SPAWN_NEARBY;
        clusterX = new ArrayList();
        clusterY = new ArrayList();
        adjacentResource = new ArrayList();
        nearbyResourceX = new ArrayList();
        nearbyResourceY = new ArrayList();
        friendlyCastlesX = new ArrayList();
        friendlyCastlesY = new ArrayList();
        friendlyCastlesId = new ArrayList();
        enemyCastlesX = new ArrayList();
        enemyCastlesY = new ArrayList();
    }

    //The init function for pilgrims
    public void initializePilgrim() {
        homeCastle = findAdjacentCastle();
    }

    public int findAdjacentCastle() {
        for(int i = 0; i < robots.length; i++) {
            if(robots[i].unit == SPECS.CASTLE) {
                int direction = isAdjacentTo(robots[i]);
                if(direction > -1)
                    return direction;
            }
        }
        return -1;
    }

    //Scans the map and finds the location of clusters on the map (prioritizes karbonite)
    public void findClusters() {
        for(int i = 0; i < mapLength; i++) {
            for(int j = 0; j < mapLength; j++) {
                if(karboniteMap[j][i]) {
                    if (!isClusterNearby(i, j)) {
                        clusterX.add(i);
                        clusterY.add(j);
                    }
                }
            }
        }
    }

    //Returns if there is already a cluster point close to a resource deopt to prevent double clustering
    public boolean isClusterNearby(int x, int y) {
        for(int i = 0; i < clusterX.size(); i++) {
            if(distanceBetween(x, y, (int) clusterX.get(i), (int) clusterY.get(i)) < clusterRadius)
                return true;
        }
        return false;
    }

    //A function to remove any clusters too close to me (a castle) because the castle will automatically handle
    //havesting resources near itself and dosn't need to mark it as a cluster
    //Note: this method does not remove clusters near other castles so as of right now it may end up sending
    //Pilgrims to other castles as if it were a cluster
    public void removeClustersNearCastle() {
        for(int i = 0; i < clusterX.size(); i++) {
            if(distanceBetween(me.x, me.y, (int) clusterX.get(i), (int) clusterY.get(i)) < castleClusterRadius) {
                clusterX.remove(i);
                clusterY.remove(i);
                i--;
            }
        }
    }

    //Same as above but does it for all other castles
    public void removeClustersNearCastles() {
        for(int h = 0; h < friendlyCastles; h++) {
            if(me.id == (int)friendlyCastlesId.get(h))
                continue;
            for(int i = 0; i < clusterX.size(); i++) {
                if(distanceBetween((int)friendlyCastlesX.get(h), (int)friendlyCastlesY.get(h), (int)clusterX.get(i), (int)clusterY.get(i)) < castleClusterRadius) {
                    clusterX.remove(i);
                    clusterY.remove(i);
                    i--;
                }
            }
        }
        for(int h = 0; h < enemyCastles; h++) {
            for(int i = 0; i < clusterX.size(); i++) {
                if(distanceBetween((int)enemyCastlesX.get(h), (int)enemyCastlesY.get(h), (int)clusterX.get(i), (int)clusterY.get(i)) < castleClusterRadius) {
                    clusterX.remove(i);
                    clusterY.remove(i);
                    i--;
                }
            }
        }
    }

    //Adds any adjacent resources to a castle or church to its adjacent array
    public void findAdjacentResources() {
        //log("Finding Adjacent Resources");
        for(int i = 0; i < myDirections.length; i++) {
            if(offMap(me.x + myDirections[i][0], me.y + myDirections[i][1]))
                continue; //Skips spots off the map
            if(karboniteMap[me.y + myDirections[i][1]][me.x + myDirections[i][0]]) {
                adjacentResource.add(i);
            }
        }
        for(int i = 0; i < myDirections.length; i++) {
            if(offMap(me.x + myDirections[i][0], me.y + myDirections[i][1]))
                continue; //Skips spots off the map
            if(fuelMap[me.y + myDirections[i][1]][me.x + myDirections[i][0]]) {
                adjacentResource.add(i);
            }
        }
    }

    //Checks to see if unit is standing on a resource
    public boolean isOnResource() {
        return karboniteMap[me.y][me.x] || fuelMap[me.y][me.x];
    }

    //Checks to see if unit is naxed out on a resource
    public boolean hasMaxResource() {
        return me.karbonite == maxKarbonite || me.fuel == maxFuel;
    }

    //Returns the direction to a specific position from the robot. Returns NORTH if the robot is already on the spot
    public int directionTo(int posX, int posY) {
        if(posX < me.x) {
            if(posY < me.y)
                return 7; //NORTHWEST
            else if(posY > me.y)
                return 5; //SOUTHWEST
            else
                return 6; //WEST
        } else if(posX > me.x) {
            if(posY > me.y)
                return 3; //SOUTHEAST
            else if(posY < me.y)
                return 1; //NORTHEAST
            else
                return 2; //EAST
        } else {
            if(posY > me.y)
                return 4; //SOUTH
            else
                return 0; //NORTH
        }
    }
}













