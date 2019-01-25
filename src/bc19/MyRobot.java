package bc19;
import bc19.node;
import bc19.nodeMap;
import java.util.Stack;
import java.util.ArrayList;

public class MyRobot extends BCAbstractRobot {

   /*
   Note: the variables posX and posY always refer to the actual poition on the map (as in map[posY][posX]
       the variables dx and dy always refer to the distance from me
       (me is the variable for the robot currently running)
    */

    private int step = -1;

    //Globals
    private int tradeKarbon = 0;
    private int tradeFuel = 0;

    private int friendlyCastles = 0;
    private int enemyCastles = 0;

    private int mapLength;
    private int clusterRadius = 6;
    private int castleClusterRadius = 6;

    private int adjacentBroadcastingRaduis = 4; //The raduis to be used for braodcasting to adjacent units

    private int maxKarbonite = 20;
    private int maxFuel = 100;

    private int UNKNOWN = -1;
    private int MINE = 0;
    private int RETURN_RESOURCE = 1;
    private int SPAWN_ADJACENT = 2;
    private int SPAWN_NEARBY = 3;
    private int LISTEN = 4;
    private int TRAVEL = 5;
    private int SPAWN_TRAVELER = 6;
    private int BUILD_CHURCH = 7;

    private int goal = UNKNOWN;

    //Unit Variables
    int destinationX;
    int destinationY;

    //Pilgrim Variables
    private boolean stationaryUnit = true;
    private int homeCastle; //A direction
    private Robot homeCastleUnit;
    private int homeCastleUnitX;
    private int homeCastleUnitY;
    private int resourceX;
    private int resourceY;

    //Use these as one tile steps in a direction
    private int[] NORTH = {0, -1};
    private int[] NORTHEAST = {1, -1};
    private int[] EAST = {1, 0};
    private int[] SOUTHEAST = {1, 1};
    private int[] SOUTH = {0, 1};
    private int[] SOUTHWEST = {-1, 1};
    private int[] WEST = {-1, 0};
    private int[] NORTHWEST = {-1, -1};

    private int[] STATIONARY = {0, 0};

    //Call this array to cycle through all directions easily
    private int[][] myDirections = {NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST};

    //Pathfinding Directions
    private int[] DOUBLE_NORTH = {0, -2};
    private int[] DOUBLE_EAST = {2, 0};
    private int[] DOUBLE_SOUTH = {0, 2};
    private int[] DOUBLE_WEST = {-2, 0};

    private int[][] pathDiagonal = {DOUBLE_NORTH, NORTHEAST, DOUBLE_EAST, SOUTHEAST, DOUBLE_SOUTH, SOUTHWEST, DOUBLE_WEST, NORTHWEST};

    private Robot[] robots; //At the beginning of each turn this array will be filled with all robots visible to me
    private ArrayList clusterX;
    private ArrayList clusterY;
    private ArrayList clusterSize;
    private ArrayList clusterId;
    //Clusters don't count if they are near a castle

    //Castle variables
    private ArrayList adjacentResource;
    private ArrayList nearbyResourceX;
    private ArrayList nearbyResourceY;

    private ArrayList friendlyCastlesX;
    private ArrayList friendlyCastlesY;
    private ArrayList friendlyCastlesId;
    private ArrayList enemyCastlesX;
    private ArrayList enemyCastlesY;

    private boolean symmetry;
    private boolean loneCastle = false;

    private int rivalCastleX;
    private int rivalCastleY;
    private int distanceFromRival;

    private boolean HORIZONTAL = true;
    private boolean VERTICAL = false;

    private int myTeam;

    //Turn ---------------------------------------------

    public Action turn() {
        robots = getVisibleRobots();
        step++;

        if (me.unit == SPECS.CASTLE) {
            //First Turn
            if (step == 0) {
                log("Let's go");
                initializeCastle();
                determineSymmetry();

                findClusters();
                removeClustersNearCastle();

                //for(int i = 0; i < clusterY.size(); i++) { //List Clusters
                //log("Cluster: (" + String.valueOf(clusterX.get(i)) + "," + String.valueOf(clusterY.get(i)) + ")");
                //}

                findAdjacentResources();
                findNearbyResources();

                broadcastLocationX();

                initialScan();

            } else if (step == 1) {
                determineFriendlyCastlesX();
            } else if (step == 2) {
                broadcastLocationY();
            } else if (step == 3) {
                determineFriendlyCastlesY();
                determineEnemyCastles();
                removeClustersNearCastles();
                log("Clusters: " + String.valueOf(clusterY.size()));
            }

            else { // Every Turn ------------------------------
                for(int i = 0; i < robots.length; i++)
                {
                    int message = robots[i].castle_talk;
                    if(message == 0 || robots[i] == me)
                        continue;
                    if(robots[i].unit == SPECS.CASTLE) {
                        log("Receiving a message");
                        for (int j = 0; j < clusterId.size(); j++) {
                            if (message == (int) clusterId.get(j)) {
                                log("The message recieved is " + String.valueOf(message) + " removing cluster number " + String.valueOf(j));
                                clusterX.remove(j);
                                clusterY.remove(j);
                                clusterSize.remove(j);
                                clusterId.remove(j);
                            }
                        }
                    }
                }
            }

            // Goals -----------------------------------

            if (goal == SPAWN_ADJACENT) //Spawn pilgrims on resources adjacent to this castle
            {
                if (adjacentResource.size() < 1)
                    goal = SPAWN_NEARBY;
                else if (canBuild(SPECS.PILGRIM)) {
                    int direction = findOpenBuildSpot((int) adjacentResource.get(0));
                    if (direction > -1) {
                        adjacentResource.remove(0);
                        //log("Building pilgrim for adjacent resource");
                        return buildUnit(SPECS.PILGRIM, myDirections[direction][0], myDirections[direction][1]);
                    }
                }
            }
            if (goal == SPAWN_NEARBY) //Spawn a pilgrim to handle resources within a radius of the castle
            {
                if (nearbyResourceX.size() < 1) {
                    if (step >= 4)
                        goal = SPAWN_TRAVELER;
                } else if (canBuild(SPECS.PILGRIM)) {
                    int direction = directionTo((int) nearbyResourceX.get(0), (int) nearbyResourceY.get(0));
                    direction = findOpenBuildSpot(direction);
                    if (direction > -1) {
                        broadcast(adjacentBroadcastingRaduis, (int) nearbyResourceX.get(0), (int) nearbyResourceY.get(0));
                        nearbyResourceX.remove(0);
                        nearbyResourceY.remove(0);
                        //log("Building a pilgrim for nearby resource");
                        return buildUnit(SPECS.PILGRIM, myDirections[direction][0], myDirections[direction][1]);
                    }
                }
            }
            if (goal == SPAWN_TRAVELER) {
                if (clusterX.size() < 1)
                    goal = UNKNOWN;
                else if (canBuild(SPECS.PILGRIM)) {
                    //log("Building traveler");
                    int direction = directionTo((int) clusterX.get(0), (int) clusterY.get(0));
                    direction = findOpenBuildSpot(direction);
                    if (direction > -1) {
                        broadcast(adjacentBroadcastingRaduis, (int) clusterX.get(0), (int) clusterY.get(0)); //Fix radius
                        clusterX.remove(0);
                        clusterY.remove(0);
                        clusterSize.remove(0);
                        log("Sending traveler. Broadcasting: " + String.valueOf(clusterId.get(0)));
                        castleTalk((int)clusterId.get(0));
                        clusterId.remove(0);
                        //log("Building a pilgrim for nearby resource");
                        return buildUnit(SPECS.PILGRIM, myDirections[direction][0], myDirections[direction][1]);
                    }
                }
            }

            return proposeTrade(tradeKarbon, tradeFuel); //If the castle can't find anything to do, it ends its turn by proposing a trade
        }

        // Church ---------------------------------------------------------------------------

        else if (me.unit == SPECS.CHURCH) {
            if (step == 0) //First Turn
            {
                initializeChurch();
                findAdjacentResources();
                findNearbyResources();
            }

            if (goal == SPAWN_ADJACENT) //Spawn pilgrims on resources adjacent to this church
            {
                if (adjacentResource.size() < 1)
                    goal = SPAWN_NEARBY;
                else if (canBuild(SPECS.PILGRIM)) {
                    int direction = findOpenBuildSpot((int) adjacentResource.get(0));
                    if (direction > -1) {
                        adjacentResource.remove(0);
                        return buildUnit(SPECS.PILGRIM, myDirections[direction][0], myDirections[direction][1]);
                    }
                }
            }
            if (goal == SPAWN_NEARBY) //Spawn a pilgrim to handle resources within a radius of the church
            {
                if (nearbyResourceX.size() < 1) {
                    goal = UNKNOWN;
                } else if (canBuild(SPECS.PILGRIM)) {
                    int direction = directionTo((int) nearbyResourceX.get(0), (int) nearbyResourceY.get(0));
                    direction = findOpenBuildSpot(direction);
                    if (direction > -1) {
                        broadcast(adjacentBroadcastingRaduis, (int) nearbyResourceX.get(0), (int) nearbyResourceY.get(0));
                        nearbyResourceX.remove(0);
                        nearbyResourceY.remove(0);
                        //log("Building a pilgrim for nearby resource");
                        return buildUnit(SPECS.PILGRIM, myDirections[direction][0], myDirections[direction][1]);
                    }
                }
            }
        }

        // Pilgrim --------------------------------------------------------------------------

        else if (me.unit == SPECS.PILGRIM) {
            if (step == 0) //First Turn
            {
                initializePilgrim();
            }


            if (hasMaxResource() && goal != BUILD_CHURCH) {
                goal = RETURN_RESOURCE;
            }
            if (homeCastle == -1) {
                homeCastle = findAdjacentCastle();
                homeCastleUnit = findAdjacentCastleUnit();
                homeCastleUnitX = homeCastleUnit.x;
                homeCastleUnitY = homeCastleUnit.y;
            }

            if (goal == LISTEN) {
                int[] temp = decodeSignal(homeCastleUnit);
                resourceX = temp[0];
                resourceY = temp[1];
                if (distanceBetween(me.x, me.y, resourceX, resourceY) <= castleClusterRadius) {
                    goal = MINE;
                    //log("I'm a nearby");
                } else {
                    log("I'm a traveler");
                    goal = TRAVEL;
                }
            }
            if (goal == MINE) {
                if (stationaryUnit)
                    return mine();
                else {
                    if (onDestination())
                        return mine();
                    else {
                        int[] s = moveTowards(resourceX, resourceY);
                        return move(s[0], s[1]);
                    }
                }
            } else if (goal == RETURN_RESOURCE) {
                if (stationaryUnit) {
                    goal = MINE;
                    return give(myDirections[homeCastle][0], myDirections[homeCastle][1], me.karbonite, me.fuel);
                } else {
                    if (isAdjacentTo(homeCastleUnit) > -1) {
                        goal = MINE;
                        return give(myDirections[isAdjacentTo(homeCastleUnit)][0], myDirections[isAdjacentTo(homeCastleUnit)][1], me.karbonite, me.fuel);
                    } else {
                        int[] s = moveTowards(homeCastleUnitX, homeCastleUnitY);
                        return move(s[0], s[1]);
                    }
                }
            } else if (goal == TRAVEL) {
                if (onDestination()) {
                    goal = BUILD_CHURCH;
                } else {
                    int[] s = moveTowards(resourceX, resourceY);
                    return move(s[0], s[1]);
                }
            }
            if (goal == BUILD_CHURCH) {
                if (canBuild(SPECS.CHURCH)) {
                    int direction = findChurchBuildSpot(1);
                    goal = MINE;
                    stationaryUnit = true;
                    homeCastleUnit = null;
                    homeCastleUnitX = -1;
                    homeCastleUnitY = -1;
                    homeCastle = -1;
                    if (direction > -1)
                        return buildUnit(SPECS.CHURCH, myDirections[direction][0], myDirections[direction][1]);
                    else
                        log("No spots to build church");
                } else {
                    return mine();
                }
            }
        }

        // Crusader ------------------------------------------------------

        else if (me.unit == SPECS.CRUSADER) {

        }

        // Prophet --------------------------------------------------------

        else if (me.unit == SPECS.PROPHET) {
            return move(1, 1);
        }

        // Preacher -------------------------------------------------------

        else if (me.unit == SPECS.PREACHER) {

        } else {
            log("This unit is not a known unit");
        }
    }


    //Methods --------------------

    //Does an initial scan for a castle, not knowing where the other castles are
    void initialScan() {
        if(robots.length < 2) {
            log("Lone Castle.");
            loneCastle = true;
        }
        distanceFromRival = distanceBetween(me.x, me.y, rivalCastleX, rivalCastleY);
        log(String.valueOf(distanceFromRival) + " tiles from rival castle");
        if (symmetry == VERTICAL) {
            rivalCastleX = mapLength - me.x - 1;
            rivalCastleY = me.y;
        } else {
            rivalCastleX = me.x;
            rivalCastleY = mapLength - me.y - 1;
        }
    }

    //Returns a direction to build a church is (empty and no resource)
    int findChurchBuildSpot(int pref) {
        if (isEmpty(myDirections[pref % myDirections.length][0], myDirections[pref % myDirections.length][1])
                && !hasResource(myDirections[pref % myDirections.length][0], myDirections[pref % myDirections.length][1]))
            return pref % myDirections.length;

        for (int i = 1; i < 5; i++) {
            if (isEmpty(myDirections[(pref + i) % myDirections.length][0], myDirections[(pref + i) % myDirections.length][1])
                    && !hasResource(myDirections[(pref + i) % myDirections.length][0], myDirections[(pref + i) % myDirections.length][1])) //Clockwise
                return (pref + i) % myDirections.length;

            pref += myDirections.length;

            if (isEmpty(myDirections[(pref - i) % myDirections.length][0], myDirections[(pref - i) % myDirections.length][1])
                    && !hasResource(myDirections[(pref - i) % myDirections.length][0], myDirections[(pref - i) % myDirections.length][1])) //Counterclockwise
                return (pref - i) % myDirections.length;
            pref -= myDirections.length;
        }
        log("No open positions");
        return -1;
    }

    //Returns if a pilgrim is on its correct resource
    boolean onDestination() {
        return me.x == resourceX && me.y == resourceY;
    }

    //Returns if there is enough resources to build the unit in question
    boolean canBuild(int unit) {
        if (unit == SPECS.PILGRIM)
            return (karbonite >= 10) && (fuel > 52);
        else if (unit == SPECS.CRUSADER)
            return (karbonite >= 15) && (fuel > 50);
        else if (unit == SPECS.PROPHET)
            return (karbonite >= 25) && (fuel > 50);
        else if (unit == SPECS.PREACHER)
            return (karbonite >= 30) && (fuel > 50);
        else if (unit == SPECS.CHURCH)
            return (karbonite >= 50 && (fuel > 200));
        return false; //If not a unit
    }

    //Returns an array of the correct movement option to eventually attain posX and posY
    int[] moveTowards(int posX, int posY) {
        int[] s = new int[2];
        if (me.unit == SPECS.PILGRIM || me.unit == SPECS.PROPHET || me.unit == SPECS.PREACHER) {
            if (distanceBetween(me.x, me.y, posX, posY) <= 2) {
                if (!isEmpty(posX - me.x, posY - me.y)) {
                    int direction = findOpenBuildSpot(directionTo(posX, posY));
                    if (direction > -1) {
                        s[0] = myDirections[direction][0]; //Move adjacent if occupied
                        s[1] = myDirections[direction][1];
                    } else {
                        s = STATIONARY;
                    }
                } else {
                    s[0] = posX - me.x;
                    s[1] = posY - me.y;
                }
            } else {
                int direction = directionTo(posX, posY);
                s = tryMove(direction);
            }
            return s;
        }
    }

    //Finds the closest-to-optimal direction as an array
    int[] tryMove(int pref) {
        if (isEmpty(pathDiagonal[pref][0], pathDiagonal[pref][1]))
            return pathDiagonal[pref];
        if (pref % 2 == 1) { //if diagonal
            pref += pathDiagonal.length;
            for (int i = 1; i < 5; i++) {
                if (isEmpty(pathDiagonal[(pref + i) % pathDiagonal.length][0], pathDiagonal[(pref + i) % pathDiagonal.length][1]))
                    return pathDiagonal[(pref + i) % pathDiagonal.length];

                if (isEmpty(pathDiagonal[(pref - i) % pathDiagonal.length][0], pathDiagonal[(pref - i) % pathDiagonal.length][1]))
                    return pathDiagonal[(pref - i) % pathDiagonal.length];
            }
            pref -= pathDiagonal.length;
        }
        int[] s = new int[2];
        int direction = findOpenBuildSpot(pref);
        if (direction > -1) {
            s[0] = myDirections[direction][0];
            s[1] = myDirections[direction][1];
        } else {
            s = STATIONARY;
        }
        return s;
    }

    //Broadcasts 2 numbers between 0 and 63 inclusive
    void broadcast(int radius_squared, int message1, int message2) {
        //log("Broadcasting: " + String.valueOf(message1) + "," + String.valueOf(message2));
        signal(message2 * 64 + message1, radius_squared);
    }

    //Interprets a signal into 2 numbers
    int[] decodeSignal(Robot sender) {
        int[] signal = new int[2];
        int value = sender.signal;
        signal[0] = value % 64;
        signal[1] = (int) value / 64;
        log("Decoding Signal: " + String.valueOf(signal[0]) + "," + String.valueOf(signal[1]));
        return signal;
    }

    //Bradcast Location on castle talk. broadcasts 64 if me.x == 0
    void broadcastLocationX() {
        castleTalk(me.x);
        if (me.x == 0)
            castleTalk(64);
    }

    //Bradcast Location on castle talk. braodcasts 64 if me.y == 0
    void broadcastLocationY() {
        castleTalk(me.y);
        if (me.y == 0)
            castleTalk(64);
    }

    //Learn the X position of all castles
    void determineFriendlyCastlesX() {
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].castle_talk > 0 && robots[i].team == myTeam) {
                if (robots[i].castle_talk == 64)
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
    void determineFriendlyCastlesY() {
        for (int h = 0; h < friendlyCastlesId.size(); h++) {
            for (int i = 0; i < robots.length; i++) {
                if (robots[i].id == (int) friendlyCastlesId.get(h)) {
                    if (robots[i].castle_talk == 64)
                        friendlyCastlesY.add(0);
                    else
                        friendlyCastlesY.add(robots[i].castle_talk);
                    break;
                }
            }
        }
        broadcastLocationY();
    }

    //Calculates the locations of all enemy castles
    void determineEnemyCastles() {
        for (int i = 0; i < friendlyCastlesX.size(); i++) {
            if (symmetry == VERTICAL) {
                enemyCastlesX.add(mapLength - (int) friendlyCastlesX.get(i) - 1);
                enemyCastlesY.add((int) friendlyCastlesY.get(i));
            } else {
                enemyCastlesX.add((int) friendlyCastlesX.get(i));
                enemyCastlesY.add(mapLength - (int) friendlyCastlesY.get(i) - 1);
            }
            //log("Enemy castle at: " + String.valueOf((int)enemyCastlesX.get(i)) + "," + String.valueOf((int)enemyCastlesY.get(i)));
        }
    }

    //Records if the map is horizontally or vertically symmetric.
    //Depending on the map it is possible this method will fail but it is not likely.
    void determineSymmetry() {
        for (int i = 0; i < mapLength / 2; i++) {
            if (map[i][i] != map[mapLength - 1 - i][mapLength - 1 - i]) {
                if (map[i][mapLength - 1 - i] == map[i][i]) {
                    symmetry = VERTICAL;
                    //log("Symmetry: VERTICAL");
                } else {
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
    void findNearbyResources() {
        for (int i = 0; i < 2 * castleClusterRadius + 1; i++) {
            int myHeight = 2 * i + 1;
            if (i > castleClusterRadius + 1)
                myHeight = 2 * (2 * castleClusterRadius - i) + 1;
            for (int j = 0; j < myHeight; j++) {
                if (offMap(i - castleClusterRadius + me.x, j - (myHeight - 1) / 2 + me.y))
                    continue;
                if (isAdjacentToTile(i - castleClusterRadius, j - (myHeight - 1) / 2)) {
                    continue; //Don't count adjacent resources as they are already in the adjacent tile array
                }
                if (hasResource(i - castleClusterRadius, j - (myHeight - 1) / 2)) {
                    nearbyResourceX.add(i - castleClusterRadius + me.x);
                    nearbyResourceY.add(j - (myHeight - 1) / 2 + me.y);
                }
            }
        }
    }

    //Checks to see if there is a resource dx, dy from me
    boolean hasResource(int dx, int dy) {
        if (offMap(me.x + dx, me.y + dy))
            return false;
        return karboniteMap[me.y + dy][me.x + dx] || fuelMap[me.y + dy][me.x + dx];
    }

    //Checks if tile posX, posY is off map
    boolean offMap(int posX, int posY) {
        if (posX >= mapLength || posY >= mapLength || posX < 0 || posY < 0)
            return true;
        return false;
    }

    //Checks to see if the tile dx and dy away from me is passable.
    boolean isPassable(int dx, int dy) {
        if (offMap(me.x + dx, me.y + dy))
            return false;
        return map[me.y + dy][me.x + dx];
    }

    //Checks to see if the tile dx and dy away from me is occupied by another robot
    boolean isOccupied(int dx, int dy) {
        int posX = me.x + dx;
        int posY = me.y + dy;

        if (offMap(posX, posY))
            return false;

        for (int i = 0; i < robots.length; i++) {
            if (robots[i].x == posX && robots[i].y == posY) {
                return true;
            }
        }
        return false;
    }

    //Checks to see if the tile is empty (no mountains or robots there)
    boolean isEmpty(int dx, int dy) {
        return isPassable(dx, dy) && !isOccupied(dx, dy);
    }

    //Cycles through all tiles adjacent to me and returns the direction of an open spot to build
    //The pref in the preffered direction to build in
    //The int returned in the myDirections index of the correct direction
    int findOpenBuildSpot(int pref) {
        if (isEmpty(myDirections[pref % myDirections.length][0], myDirections[pref % myDirections.length][1]))
            return pref % myDirections.length;

        for (int i = 1; i < 5; i++) {
            if (isEmpty(myDirections[(pref + i) % myDirections.length][0], myDirections[(pref + i) % myDirections.length][1])) //Clockwise
                return (pref + i) % myDirections.length;

            pref += myDirections.length;

            if (isEmpty(myDirections[(pref - i) % myDirections.length][0], myDirections[(pref - i) % myDirections.length][1])) //Counterclockwise
                return (pref - i) % myDirections.length;
            pref -= myDirections.length;
        }
        log("No open positions");
        return -1;
    }

    //Returns the distance between two tiles
    int distanceBetween(int x1, int y1, int x2, int y2) {
        return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }

    //Returns the distance me is from another robot
    public int distanceFrom(Robot r) {
        return Math.abs(r.x - me.x) + Math.abs(r.y - me.y);
    }

    //Return the direction a robots is in if adjacent. Otherwise returns -1
    int isAdjacentTo(Robot r) {
        for (int i = 0; i < myDirections.length; i++) {
            if (r.x == me.x + myDirections[i][0] && r.y == me.y + myDirections[i][1])
                return i;
        }
        return -1;
    }

    //Checks if tile dx, dy away is adjacent to me
    boolean isAdjacentToTile(int dx, int dy) {
        if (Math.abs(dx) < 2 && Math.abs(dy) < 2)
            return true;
        return false;
    }

    //The init function for castles
    void initializeCastle() {
        myTeam = me.team;
        mapLength = map.length;
        goal = SPAWN_ADJACENT;
        clusterX = new ArrayList();
        clusterY = new ArrayList();
        clusterSize = new ArrayList();
        clusterId = new ArrayList();
        adjacentResource = new ArrayList();
        nearbyResourceX = new ArrayList();
        nearbyResourceY = new ArrayList();
        friendlyCastlesX = new ArrayList();
        friendlyCastlesY = new ArrayList();
        friendlyCastlesId = new ArrayList();
        enemyCastlesX = new ArrayList();
        enemyCastlesY = new ArrayList();
    }

    //The init function for churches
    void initializeChurch() {
        myTeam = me.team;
        mapLength = map.length;
        goal = SPAWN_ADJACENT;
        adjacentResource = new ArrayList();
        nearbyResourceX = new ArrayList();
        nearbyResourceY = new ArrayList();
    }

    //The init function for pilgrims
    void initializePilgrim() {
        myTeam = me.team;
        mapLength = map.length;
        homeCastle = findAdjacentCastle();
        homeCastleUnit = findAdjacentCastleUnit();
        homeCastleUnitX = homeCastleUnit.x;
        homeCastleUnitY = homeCastleUnit.y;
        if (!isOnResource()) {
            stationaryUnit = false;
            goal = LISTEN;
        } else
            goal = MINE;
    }

    int findAdjacentCastle() {
        for (int i = 0; i < robots.length; i++) {
            if (robots[i].unit == SPECS.CASTLE || robots[i].unit == SPECS.CHURCH) {
                int direction = isAdjacentTo(robots[i]);
                if (direction > -1)
                    return direction;
            }
        }
        return -1;
    }

    Robot findAdjacentCastleUnit() {
        for (int i = 0; i < robots.length; i++) {
            if ((robots[i].unit == SPECS.CASTLE || robots[i].unit == SPECS.CHURCH) && isAdjacentTo(robots[i]) > -1) {
                return robots[i];
            }
        }
        return null;
    }

    //Scans the map and finds the location of clusters on the map (prioritizes karbonite)
    void findClusters() {
        for (int i = 0; i < mapLength; i++) {
            for (int j = 0; j < mapLength; j++) {
                if (karboniteMap[j][i]) {
                    if (!isClusterNearby(i, j)) {
                        clusterX.add(i);
                        clusterY.add(j);
                        clusterSize.add(1);
                        clusterId.add(clusterId.size() + 1);
                    }
                }
            }
        }
    }

    //Returns if there is already a cluster point close to a resource deopt to prevent double clustering
    boolean isClusterNearby(int x, int y) {
        for (int i = 0; i < clusterX.size(); i++) {
            if (distanceBetween(x, y, (int) clusterX.get(i), (int) clusterY.get(i)) < clusterRadius) {
                clusterSize.set(i, (int)clusterSize.get(i) + 1);
                return true;
            }
        }
        return false;
    }

    //A function to remove any clusters too close to me (a castle) because the castle will automatically handle
    //havesting resources near itself and dosn't need to mark it as a cluster
    //Note: this method does not remove clusters near other castles so as of right now it may end up sending
    //Pilgrims to other castles as if it were a cluster
    void removeClustersNearCastle() {
        for (int i = 0; i < clusterX.size(); i++) {
            if (distanceBetween(me.x, me.y, (int) clusterX.get(i), (int) clusterY.get(i)) < castleClusterRadius
                    || distanceBetween(rivalCastleX, rivalCastleY, (int) clusterX.get(i), (int) clusterY.get(i)) < castleClusterRadius) {
                clusterX.remove(i);
                clusterY.remove(i);
                clusterSize.remove(i);
                clusterId.remove(i);
                i--;
            }
        }
    }

    //Same as above but does it for all other castles
    void removeClustersNearCastles() {
        for (int h = 0; h < friendlyCastles; h++) {
            if (me.id == (int) friendlyCastlesId.get(h))
                continue;
            for (int i = 0; i < clusterX.size(); i++) {
                if (distanceBetween((int) friendlyCastlesX.get(h), (int) friendlyCastlesY.get(h), (int) clusterX.get(i), (int) clusterY.get(i)) < castleClusterRadius) {
                    clusterX.remove(i);
                    clusterY.remove(i);
                    clusterSize.remove(i);
                    clusterId.remove(i);
                    i--;
                }
            }
        }
        for (int h = 0; h < enemyCastles; h++) {
            for (int i = 0; i < clusterX.size(); i++) {
                if (distanceBetween((int) enemyCastlesX.get(h), (int) enemyCastlesY.get(h), (int) clusterX.get(i), (int) clusterY.get(i)) < castleClusterRadius) {
                    clusterX.remove(i);
                    clusterY.remove(i);
                    clusterSize.remove(i);
                    clusterId.remove(i);
                    i--;
                }
            }
        }
    }

    //Adds any adjacent resources to a castle or church to its adjacent array
    void findAdjacentResources() {
        //log("Finding Adjacent Resources");
        for (int i = 0; i < myDirections.length; i++) {
            if (offMap(me.x + myDirections[i][0], me.y + myDirections[i][1]))
                continue; //Skips spots off the map
            if (karboniteMap[me.y + myDirections[i][1]][me.x + myDirections[i][0]] && !isOccupied(myDirections[i][0], myDirections[i][1])) {
                adjacentResource.add(i);
            }
        }
        for (int i = 0; i < myDirections.length; i++) {
            if (offMap(me.x + myDirections[i][0], me.y + myDirections[i][1]))
                continue; //Skips spots off the map
            if (fuelMap[me.y + myDirections[i][1]][me.x + myDirections[i][0]] && !isOccupied(myDirections[i][0], myDirections[i][1])) {
                adjacentResource.add(i);
            }
        }
    }

    //Checks to see if unit is standing on a resource
    boolean isOnResource() {
        return karboniteMap[me.y][me.x] || fuelMap[me.y][me.x];
    }

    //Checks to see if unit is naxed out on a resource
    boolean hasMaxResource() {
        return me.karbonite == maxKarbonite || me.fuel == maxFuel;
    }

    //Returns the direction to a specific position from the robot. Returns NORTH if the robot is already on the spot
    int directionTo(int posX, int posY) {
        int i;
        if (posX < me.x) {
            if (posY < me.y)
                i = 7; //NORTHWEST
            else if (posY > me.y)
                i = 5; //SOUTHWEST
            else
                i = 6; //WEST
        } else if (posX > me.x) {
            if (posY > me.y)
                i = 3; //SOUTHEAST
            else if (posY < me.y)
                i = 1; //NORTHEAST
            else
                i = 2; //EAST
        } else {
            if (posY > me.y)
                i = 4; //SOUTH
            else if (posY < me.y)
                i = 0; //NORTH
            else {
                log("DirectionTo not found.");
                i = -1;
            }
        }
        return i;
    }



    //this will be the list of movements that will be returned from pathfinder
    Stack<Integer> directionStack;

    //List<Object> openList = new



    //convert the map into a nodal representation
    //pass in what type you want to move towards -- will return movement object



   /*
   // A* Search Algorithm
1.  Initialize the open list
2.  Initialize the closed list
put the starting node on the open
list (you can leave its f at zero)



3.  while the open list is not empty
a) find the node with the least f on
  the open list, call it "q"



b) pop q off the open list



c) generate q's 8 successors and set their
  parents to q


d) for each successor
   i) if successor is the goal, stop search
     successor.g = q.g + distance between
                         successor and q
     successor.h = distance from goal to
     successor (This can be done using many
     ways, we will discuss three heuristics-
     Manhattan, Diagonal and Euclidean
     Heuristics)


     successor.f = successor.g + successor.h


   ii) if a node with the same position as
       successor is in the OPEN list which has a
      lower f than successor, skip this successor


   iii) if a node with the same position as
       successor  is in the CLOSED list which has
       a lower f than successor, skip this successor
       otherwise, add  the node to the open list
end (for loop)


e) push q on the closed list
end (while loop)
    */

    public void findPath(int[][] stop)
    {
        node tempNode; //temporary node storage
        node selectedNode; //node with the smallest f value in openList


        ArrayList openList = new ArrayList();
        ArrayList closedList = new ArrayList();


        //create starting node and put it in the openList -- leave f = 0


        node startNode = new node();
        startNode.x = me.x;
        startNode.y = me.y;
        openList.add(startNode);


        //try
        //{
        while (openList.isEmpty() == false)
        {
            //find element in list that has the smallest f value
            int selectedIndex;


            //go through all of the elements in the list and select index of target node
            for (int i = 0; i < openList.size(); i++)
            {
                int smallestF = 0;
                tempNode = (node)openList.get(i);


                if ((tempNode.f < smallestF ) || (i == 0))
                {
                    selectedIndex = i;
                    smallestF = tempNode.f;
                }
            }


            //pop target element out of list
            selectedNode = (node)openList.get(selectedIndex);
            openList.remove(selectedIndex); //remove the selectedNode from the list




            //generate the 8 successors to the selected node
              /*
              for (int j = 0; j < 8; j++)
              {
                  node newNode = new node();
                  newNode.x = selectedNode.x - 1;
                  newNode.y = selectedNode.y + 1;


                  //need to find all adjacent blocks, check if they are valid blocks FIRST
              }
              */

        }
        //}
    }
}







