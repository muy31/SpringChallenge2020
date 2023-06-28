import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Grab the pellets as fast as you can!
 **/
class Player {
    static double[][] mapInt;
    static char[][] map;
    static ArrayList<int[]> bigPellets;
    static ArrayList<int[]> littlePellets;
    static boolean[][] trappable;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int width = in.nextInt(); // size of the grid
        int height = in.nextInt(); // top left corner is (x=0, y=0)
        map = new char[height][width];
        mapInt = new double[height][width];
        trappable = new boolean[height][width];
        ArrayList<int[]> someInfo = new ArrayList<int[]>();
        Pac.initialize(height, width);

        if (in.hasNextLine()) { in.nextLine(); }
        for (int r = 0; r < height; r++) {
            String row = in.nextLine(); // one line of the grid: space " " is floor, pound "#" is wall
            for(int c = 0; c < row.length(); c++){
                map[r][c] = row.charAt(c);
            }
        }

        for (int r = 0; r < height; r++) {
            for(int c = 0; c < width; c++){
                if(map[r][c] == ' '){
                    if(AI.findNeighbors(r, c, map).size() == 1){
                        trappable[r][c] = true;
                    }
                }
            }
        }

        for(int i = 0; i < 10; i++){
            for (int r = 0; r < height; r++) {
                for(int c = 0; c < width; c++){
                    if(map[r][c] == ' '){
                        ArrayList<int[]> neighbors = AI.findNeighbors(r,c,map);
                        boolean add = false;
                        if(neighbors.size() == 2){
                            for(int[] loc: neighbors){
                                if(trappable[loc[0]][loc[1]]){
                                    add = true;
                                }
                            }
                            if(add){
                                trappable[r][c] = true;
                            }
                        }
                    }
                }
            }
        }

        for(boolean[] tt: trappable){
            for(boolean t: tt){
                if(t){
                    System.err.print("T");
                }else{
                    System.err.print("F");
                }
            }
            System.err.println();
        }


        AI.intializeAllDistances(height, width);
        // game loop
        while (true) {
            int myScore = in.nextInt();
            int opponentScore = in.nextInt();
            int visiblePacCount = in.nextInt(); // all your pacs and enemy pacs in sight
            mapInt = new double[height][width];
            AI.linesofSight = new boolean[height][width];
            //Pac Info
            Pac.myPacs = new ArrayList<Pac>();
            Pac.visibleEnemyPacs = new ArrayList<Pac>();
            //This resets my pac objects so that it doesn't try to call a dead Pac
            for(int i = 0; i < 5; i++){ Pac.pacs[i] = null;}

            //Get info for all visible Pacs
            for (int i = 0; i < visiblePacCount; i++) {
                int pacId = in.nextInt(); // pac number (unique within a team)
                boolean mine = in.nextInt() != 0; // true if this pac is yours
                int x = in.nextInt(); // position in the grid
                int y = in.nextInt(); // position in the grid
                String typeId = in.next(); // unused in wood leagues
                int speedTurnsLeft = in.nextInt(); // unused in wood leagues
                int abilityCooldown = in.nextInt(); // unused in wood leagues

                if(!mine){ pacId += 5;}

                if(typeId.equals("DEAD")){ Pac.pacs[pacId] = null;}

                if(Pac.pacs[pacId] == null && !typeId.equals("DEAD")){
                    Pac pp = new Pac(pacId, mine, x, y, typeId, speedTurnsLeft, abilityCooldown);
                    if(pacId >= 5){
                        Pac.visibleEnemyPacs.add(pp);
                    }else{
                        AI.LineOfSight(y, x);
                    }
                }else if(!typeId.equals("DEAD")){
                    Pac.pacs[pacId].updateInfo(pacId, mine, x, y, typeId, speedTurnsLeft, abilityCooldown);
                    if(pacId >= 5){
                        Pac.visibleEnemyPacs.add(Pac.pacs[pacId]);
                    }
                }
            }

            boolean[] hold = new boolean[10];

            //System.err.println(Pac.myPacs);
            //Some info?, determines that a collision happened when attempting to move
            for(int[] info: someInfo){
                //If it is null, then pac died :(
                if(Pac.pacs[info[0]] != null){
                    if(Pac.pacs[info[0]].x == info[1] && Pac.pacs[info[0]].y == info[2] && !(Pac.pacs[info[0]].x == info[3] && Pac.pacs[info[0]].y == info[4])){
                        //then collision happened and pac ended up at same place
                        Pac.pacs[info[0]].switchTo = AI.loseAgainst(Pac.pacs[info[0]].typeId);
                        //hold[info[0]] = true;
                    }
                }
            }


            //Pellet info
            bigPellets = new ArrayList<int[]>();
            littlePellets = new ArrayList<int[]>();

            
            
            int visiblePelletCount = in.nextInt(); // all pellets in sight
            for (int i = 0; i < visiblePelletCount; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int value = in.nextInt(); // amount of points this pellet is worth
                mapInt[y][x] = (double) value;
                int[] a = {y, x};
                if(value == 10.0){
                    bigPellets.add(a);
                }else{
                    littlePellets.add(a);
                }
            }

            //Goves a score to unknown squares
            for(int r = 0; r < Player.map.length; r++){
                for(int c = 0; c < Player.map[r].length; c++){
                    if(Player.map[r][c] == ' ' && Player.mapInt[r][c] == 0.0 && !AI.linesofSight[r][c]){
                        if(!Pac.locationsAllPacsBeen[r][c]){
                            Player.mapInt[r][c] = 0.8;
                        }
                    }
                    if(AI.linesofSight[r][c] && Player.mapInt[r][c] == 0.0){
                        Pac.locationsAllPacsBeen[r][c] = true;
                    }
                }
            }

            //Make a new move function
            AI.tryFindPaths();

            String turnMoves = "";
            //If attempted to move, pacId + true, else false
            someInfo = new ArrayList<int[]>();

            for(Pac p: Pac.myPacs){
                if(p.speed){
                    turnMoves += "SPEED "+p.pacId+"| ";
                }else if(p.switcha){
                    turnMoves += "SWITCH "+p.pacId+" "+p.switchTo+"| ";
                }else{
                    if(!hold[p.pacId]){
                        turnMoves += "MOVE "+p.pacId+" "+p.nextX+ " "+p.nextY+" ("+p.choiceX +","+p.choiceY+")| ";
                    }else{
                        turnMoves += "MOVE "+p.pacId+" "+p.x+ " "+p.y+" (HOLD)|";
                    }
                    
                    int[] info = {p.pacId, p.x, p.y, p.nextX, p.nextY};
                    someInfo.add(info);
                }
                
            }
            
            System.out.println(turnMoves);
        }
    }
}

class AI{

    static int[][][][] distances;
    static boolean[][] linesofSight;

    static void intializeAllDistances(int h, int w){
        distances = new int[h][w][h][w];
        for(int row = 0; row < distances.length; row++){
            for(int col = 0; col < distances[row].length; col++){
                distances[row][col] = distanceMap(row, col, Player.map);
            }
        }
    }

    static void tryFindPaths(){
        //Doesn't take long to find pellets and remove dupes...
        for(Pac p: Pac.myPacs){
            PacMethods.convertToScoreMap(p);
            PacMethods.acknowledgeDanger(p);
            PacMethods.findClosestPellet(p, 0);
        }
        removeTargetDuplicates(true);

        for(Pac p: Pac.myPacs){
            int[] bestMove = evaluateDist(p, AI.distances[p.choiceY][p.choiceX][p.y][p.x]);
            p.nextX = bestMove[1];
            p.nextY = bestMove[0];
        }
        AI.toStopMotion();
        for(Pac p: Pac.myPacs){
            PacMethods.checkAbilities(p);
        }
    }

    static void LineOfSight(int r, int c){
        int rr = r;
        int cc = c;
        if(Player.map[rr][cc] == ' '){
            for(int dir = 0; dir < 4; dir++){
                rr = r;
                cc = c;
                while(true){
                    linesofSight[rr][cc] = true;
                    int[] next = AI.findNeighborDir(rr, cc, Player.map, dir);
                    if(next == null){
                        break;
                    }
                    rr = next[0];
                    cc = next[1];
                    if(rr == r && cc == c){
                        break;
                    }
                }
            }
        }
    }

    static int[] evaluateDist(Pac p, int spacelength){
        
        //find all locations within two spaces of Pac (takes 1.5 ms)
        ArrayList<int[]> locations = new ArrayList<int[]>();
        for(int r = 0; r < Player.map.length; r++){
            for(int c = 0; c < Player.map[r].length; c++){
                if(Player.map[r][c] == ' '){
                    if(distances[r][c][p.y][p.x] <= spacelength + 1 && !(r == p.y && c == p.x)){
                        int[] a = {r, c};
                        locations.add(a);
                    }
                }
            }
        }
        //System.err.println("Location is good!");

        //find all movepaths that maximize pellet score in the next two moves
        ArrayList<int[]> bestScores = new ArrayList<int[]>();
        double bestScore = -10000.0;
        for(int[] loc : locations){
            double score = evaluateScore(loc[0], loc[1], p);
            //System.err.println("A " +score);
            if(score > bestScore){
                bestScores = new ArrayList<int[]>();
                bestScores.add(loc);
                bestScore = score;
            }else if(score == bestScore){
                bestScores.add(loc);
                bestScore = score;
            }
        }
        /*
        for(int[] loc: bestScores){
            System.err.println(p.pacId + " End: " + Arrays.toString(loc));
        }
        */

        //use path whose outcome is closest to choiceX,Y
        int[] best = null;
        int Cdist = Integer.MAX_VALUE;
        for(int[] loc: bestScores){
            if(AI.distances[loc[0]][loc[1]][p.choiceY][p.choiceX] < Cdist){
                Cdist = AI.distances[loc[0]][loc[1]][p.choiceY][p.choiceX];
                best = loc;
            }
        }
        System.err.println(Arrays.toString(best));
        return best;
    }

    static ArrayList<int[]> findPath(int startR,int startC,int endR,int endC){
        ArrayList<int[]> path = new ArrayList<int[]>();
        int rr = startR;
        int cc = startC;

        while(!(rr == endR && cc == endC)){
            ArrayList<int[]> neighs = findNeighbors(rr,cc,Player.map);
            int Cdist = distances[rr][cc][endR][endC];
            for(int[] loc: neighs){
                if(distances[loc[0]][loc[1]][endR][endC] < Cdist){
                    Cdist = distances[loc[0]][loc[1]][endR][endC];
                    rr = loc[0];
                    cc = loc[1];
                }
            }
            int[] a = {rr, cc};
            path.add(a);
        }

        return path;
    }

    //Evaluates score gained by moving from (px,py) to (c,r)
    static double evaluateScore(int r, int c, Pac p){
        ArrayList<int[]> path = findPath(p.y, p.x, r, c);
        double score = 0;
        for(int[] loc : path){
           score += p.scoreMap[loc[0]][loc[1]]; 
        }
        return score;
    }

    static boolean canTrap(Pac p, Pac pp){
        if(Player.trappable[pp.y][pp.x] && AI.distances[p.y][p.x][pp.y][pp.x] <= 3){
                return true;
        }
        return false;
    }

    //Attempts to prevent choice duplication, thus enhancing collision avoidance (has bugs)
    static void removeTargetDuplicates(boolean start){

        for(Pac p1: Pac.myPacs){
            if(start){
                p1.noGo = new ArrayList<String>();
            }
            for(Pac p2: Pac.myPacs){
                if(start){
                    p2.noGo = new ArrayList<String>();
                }
                if(p1 != p2){
                    //If choices are the same, choose the one w/ smallest distance to pellet
                    if(AI.distances[p1.choiceY][p1.choiceX][p2.choiceY][p2.choiceX] <= 4){
                        if(distances[p1.choiceY][p1.choiceX][p1.y][p1.x] <= distances[p2.choiceY][p2.choiceX][p2.y][p2.x]){
                            p2.noGo.add(p2.choiceY+","+p2.choiceX);
                            p2.choiceX = -1;
                            p2.choiceY = -1;
                            //finds pellet not including the one already chosen
                            PacMethods.findClosestPellet(p2, 0);
                        }else{
                            p1.noGo.add(p1.choiceY+","+p1.choiceX);
                            p1.choiceX = -1;
                            p1.choiceY = -1;
                            //finds pellet not including the one already chosen
                            PacMethods.findClosestPellet(p1, 0);
                        }
                    }
                }
            }
        }

        boolean dupes = false;
        for(Pac p1: Pac.myPacs){
            for(Pac p2: Pac.myPacs){
                if(p1 != p2){
                    if(p1.choiceX == p2.choiceX && p1.choiceY == p2.choiceY){ dupes = true; break;}
                }
            }
        }

        if(dupes){
            removeTargetDuplicates(false);
        }
    }

    //If two or more pacs are moving towards the same square, stop one to let the other pass
    static void toStopMotion(){
        String places = "";
        for(Pac p: Pac.myPacs){
            PacMethods.findNextPlaceActually(p);
            if(places.indexOf("|"+p.acNextX + " "+ p.acNextY+"|") != -1){
                p.nextX = p.x;
                p.nextY = p.y;
            }
            places += "|"+p.acNextX + " "+ p.acNextY+"|";
        }
    }

    //Returns what type will defeat
    static String winAgainst(String type){
        if(type.equals("PAPER")){
            return "ROCK";
        }else if(type.equals("ROCK")){
            return "SCISSORS";
        }else{
            return "PAPER";
        }
    }

    static String loseAgainst(String type){
        if(type.equals("PAPER")){
            return "SCISSORS";
        }else if(type.equals("ROCK")){
            return "PAPER";
        }else{
            return "ROCK";
        }
    }

    //Precalcs all distances
    public static int[][] distanceMap(int R1, int C1, char[][] map){
        if(map[R1][C1] == ' '){
            int[][] distances = new int[map.length][map[0].length];
            int start = 1;
            ArrayList<int[]> neighbors = new ArrayList<int[]>();
            int[] a = {R1, C1};
            neighbors.add(a);
            //loop
            while(!neighbors.isEmpty()){
                ArrayList<int[]> nextNeighs = new ArrayList<int[]>();
                for(int[] loc: neighbors){
                    if(distances[loc[0]][loc[1]] == 0){
                        distances[loc[0]][loc[1]] = start;
                        nextNeighs.addAll(findNeighbors(loc[0], loc[1], map));
                    }
                }
                start++;
                neighbors.clear();
                neighbors.addAll(nextNeighs);
            }
            return distances; 
        }else{
            return null;
        }
    }

    static int[] findNeighborDir(int R, int C, char[][] map, int dir){
        if(map[R][C] == '#'){
            return null;
        }else{
            if(dir == 0){
                if(map[(R - 1 + map.length)%map.length][C] == ' '){
                    int[] a = {(R - 1 + map.length)%map.length, C};
                    return a;
                }
            }else if(dir == 1){
                if(map[(R + 1)%map.length][C] == ' '){
                    int[] a = {(R + 1)%map.length, C};
                    return a;
                }
            }else if(dir == 2){
                if(map[R][(C - 1 + map[R].length)%map[R].length] == ' '){
                    int[] a = {R, (C - 1 + map[R].length)%map[R].length};
                    return a;
                }
            }else{
                if(map[R][(C + 1)%map[R].length] == ' '){
                    int[] a = {R, (C + 1)%map[R].length};
                    return a;
                }
            }
            return null;
        }
    }

    static ArrayList<int[]> findNeighbors(int R, int C, char[][] map){
        ArrayList<int[]> neighbors = new ArrayList<int[]>();
        if(map[R][C] == ' '){
            if(map[(R - 1 + map.length)%map.length][C] == ' '){
                int[] a = {(R - 1 + map.length)%map.length, C};
                neighbors.add(a);
            }
        }

        if(map[R][C] == ' '){
            if(map[(R + 1)%map.length][C] == ' '){
                int[] a = {(R + 1)%map.length, C};
                neighbors.add(a);
            }
        }

        if(map[R][C] == ' '){
            if(map[R][(C - 1 + map[R].length)%map[R].length] == ' '){
                int[] a = {R, (C - 1 + map[R].length)%map[R].length};
                neighbors.add(a);
            }
        }

        if(map[R][C] == ' '){
            if(map[R][(C + 1)%map[R].length] == ' '){
                int[] a = {R, (C + 1)%map[R].length};
                neighbors.add(a);
            }
        }
        return neighbors;
    }
}

class PacMethods {

    static void convertToScoreMap(Pac p){
        p.scoreMap = new double[Player.map.length][Player.map[0].length];
        for(int r = 0; r < Player.map.length; r++){
            for(int c = 0; c < Player.map[0].length; c++){
                p.scoreMap[r][c] = Player.mapInt[r][c];
            }
        }
    }
    
    //Finds the closest satisfactory location at least bounddist steps away for a Pac to aim for
    static void findClosestPellet(Pac p, int bounddist){
        p.choiceX = p.x;
        p.choiceY = p.y;
        int Cdist = Integer.MAX_VALUE;

        //Creates array from all known pellet positions
        ArrayList<int[]> bb = new ArrayList<int[]>();
        bb.addAll(Player.bigPellets);
        bb.addAll(Player.littlePellets);

        for(int[] pellRC : bb){
            int dist = AI.distances[pellRC[0]][pellRC[1]][p.y][p.x];
            if(Player.mapInt[pellRC[0]][pellRC[1]] == 10.0){
                dist -= 100;
            }
            if(dist < Cdist && !p.noGo.contains(pellRC[0]+","+pellRC[1]) && AI.distances[pellRC[0]][pellRC[1]][p.y][p.x] >= bounddist + 1){
                p.choiceX = pellRC[1];
                p.choiceY = pellRC[0];
                Cdist = dist;
            }
        }

        //BB turned out to be empty, or no places satisfied conditions
        if(p.choiceX == p.x && p.choiceY == p.y){
            int[] target = closestPlaceNeverBeenTo(bounddist, p.y,p.x, p.noGo);
            p.choiceX = target[1];
            p.choiceY = target[0];
        }
    }

    static void findClosestBig(Pac p, int bounddist){
        p.choiceX = p.x;
        p.choiceY = p.y;
        int Cdist = Integer.MAX_VALUE;

        //Creates array from all known big pellet
        for(int[] pellRC : Player.bigPellets){
            int dist = AI.distances[pellRC[0]][pellRC[1]][p.y][p.x];
            if(Player.mapInt[pellRC[0]][pellRC[1]] == 10.0){
                dist -= 100;
            }
            if(dist < Cdist && !p.noGo.contains(pellRC[0]+","+pellRC[1]) && AI.distances[pellRC[0]][pellRC[1]][p.y][p.x] >= bounddist + 1){
                p.choiceX = pellRC[1];
                p.choiceY = pellRC[0];
                Cdist = dist;
            }
        }
        //Empty or not satisfactory location
        if(p.choiceX == p.x && p.choiceY == p.y){
            //System.err.println(Pac.locationsAllPacsBeen[1][27]);
            int[] target = closestPlaceNeverBeenTo(bounddist, p.y,p.x, p.noGo);
            p.choiceX = target[1];
            p.choiceY = target[0];
        }
    }

    //Finds all locations where pac believes no other pac has been
    static ArrayList<int[]> placesNeverBeenTo(int bounddist, int py, int px, ArrayList<String> noGo){
        ArrayList<int[]> placesNone = new ArrayList<int[]>();
        for(int r = 0; r < Player.map.length; r++){
            for(int c = 0; c < Player.map[0].length; c++){
                if(Pac.locationsAllPacsBeen[r][c] == false && Player.map[r][c] == ' ' && !noGo.contains(r+","+c) && AI.distances[r][c][py][px] >= bounddist + 1){
                    int[] a = {r, c};
                    placesNone.add(a);
                }
            }
        }
        return placesNone;
    }

    //Chooses closest location where pac believes no other pac has been
    static int[] closestPlaceNeverBeenTo(int bounddist, int py, int px, ArrayList<String> noGo){
        ArrayList<int[]> placesNone = placesNeverBeenTo(bounddist, py, px, noGo);
        int Cdist = Integer.MAX_VALUE;
        int[] target = null;

        for(int[] loc: placesNone){
            if(AI.distances[loc[0]][loc[1]][py][px] < Cdist){
                Cdist = AI.distances[loc[0]][loc[1]][py][px];
                target = loc;
            }
        }
        return target;
    }

    //Based on environment conditions, chooses what to switch to if switch is possible
    static void acknowledgeDanger(Pac p){
        ArrayList<Pac> closePacs = new ArrayList<Pac>();
        for(Pac pp : Pac.visibleEnemyPacs){
            //Three spaces or less from my location
            if(p.speedTurnsLeft > 0){
                if(AI.distances[pp.y][pp.x][p.acNextY][p.acNextX] <= 3){
                    closePacs.add(pp);
                } 
            }else{
                if(AI.distances[pp.y][pp.x][p.acNextY][p.acNextX] <= 2){
                    closePacs.add(pp);
                } 
            } 

        }

        for(Pac pp: closePacs){
            ArrayList<int[]> neighbors = AI.findNeighbors(pp.y,pp.x,Player.map);
            if(p.typeId == pp.typeId){
                System.err.println(p.pacId + " ??????");
                if(pp.abilityCooldown == 0){
                    //dangerous
                    if(p.abilityCooldown == 0){
                        p.scoreMap[pp.y][pp.x] -= 0.5;
                        for(int[] loc: neighbors){
                            p.scoreMap[loc[0]][loc[1]] -= 0.5;
                        }
                    }else{
                        for(int[] loc: neighbors){
                            p.scoreMap[loc[0]][loc[1]]--;
                        }
                        p.scoreMap[pp.y][pp.x]--;
                    }
                }else{
                    //not dangerous
                    p.scoreMap[pp.y][pp.x] -= 0.5;
                    for(int[] loc: neighbors){
                        p.scoreMap[loc[0]][loc[1]] -= 0.5;
                    }

                    /*
                    if(p.abilityCooldown == 0){
                        p.scoreMap[pp.y][pp.x] += 1;
                        for(int[] loc: neighbors){
                            p.scoreMap[loc[0]][loc[1]] += 0.5;
                        }
                    }
                    */
                }
            //would lose against opponent 
            }else if(p.typeId.equals(AI.winAgainst(pp.typeId))){
                System.err.println(p.pacId + " RUN");
                if(pp.abilityCooldown == 0){
                    if(p.abilityCooldown == 0){
                        p.scoreMap[pp.y][pp.x] -= 1.5;
                        for(int[] loc: neighbors){
                            p.scoreMap[loc[0]][loc[1]] -= 0.5;
                        }
                    }else{
                        p.scoreMap[pp.y][pp.x] -= 2;
                        for(int[] loc: neighbors){
                            p.scoreMap[loc[0]][loc[1]] -= 2;
                        }
                    }
                }else{
                    
                    if(p.abilityCooldown == 0){
                        p.scoreMap[pp.y][pp.x] += 0.5;
                        for(int[] loc: neighbors){
                            p.scoreMap[loc[0]][loc[1]] += 0.5;
                        }
                    }else{
                    
                        p.scoreMap[pp.y][pp.x] -= 2;
                        for(int[] loc: neighbors){
                            p.scoreMap[loc[0]][loc[1]] -= 2.5;
                        }
                    }
                }
            //would win against opponent
            }else if(p.typeId.equals(AI.loseAgainst(pp.typeId))){
                if(AI.canTrap(p, pp)){
                    System.err.println(p.pacId + " MUNCH");
                    if(pp.abilityCooldown == 0){
                        if(p.abilityCooldown == 0){
                            p.scoreMap[pp.y][pp.x] += 1.5;
                            for(int[] loc: neighbors){
                                p.scoreMap[loc[0]][loc[1]] += 1.0;
                            }
                        }else{
                            p.scoreMap[pp.y][pp.x] -= 0.5;
                            for(int[] loc: neighbors){
                                p.scoreMap[loc[0]][loc[1]] -= 0.5;
                            }
                        }
                    }else{
                        if(p.abilityCooldown == 0){
                            p.scoreMap[pp.y][pp.x] += 5;
                            for(int[] loc: neighbors){
                                p.scoreMap[loc[0]][loc[1]] += 1;
                            }
                        }else{
                            p.scoreMap[pp.y][pp.x] += 5;
                            for(int[] loc: neighbors){
                                p.scoreMap[loc[0]][loc[1]] += 1;
                            }
                        }
                    }
                }else{
                    System.err.println(p.pacId + " MUNCH");
                    if(pp.abilityCooldown == 0){
                        if(p.abilityCooldown == 0){
                            p.scoreMap[pp.y][pp.x] += 1.0;
                            for(int[] loc: neighbors){
                                p.scoreMap[loc[0]][loc[1]] += 1.0;
                            }
                        }else{
                            p.scoreMap[pp.y][pp.x] -= 0.5;
                            for(int[] loc: neighbors){
                                p.scoreMap[loc[0]][loc[1]] -= 0.5;
                            }
                        }
                    }else{
                        if(p.abilityCooldown == 0){
                            p.scoreMap[pp.y][pp.x] += 1;
                            for(int[] loc: neighbors){
                                p.scoreMap[loc[0]][loc[1]] += 1;
                            }
                        }else{
                            p.scoreMap[pp.y][pp.x] += 0.5;
                            for(int[] loc: neighbors){
                                p.scoreMap[loc[0]][loc[1]] += 0.5;
                            }
                        }
                    }
                }
            }
        }
    }

    static void whatToSwitchTo(Pac p){
        ArrayList<Pac> closePacs = new ArrayList<Pac>();
        for(Pac pp : Pac.visibleEnemyPacs){
            //Three spaces or less from my location
            if(p.speedTurnsLeft > 0){
                if(AI.distances[pp.y][pp.x][p.acNextY][p.acNextX] <= 3){
                    closePacs.add(pp);
                } 
            }else{
                if(AI.distances[pp.y][pp.x][p.acNextY][p.acNextX] <= 2){
                    closePacs.add(pp);
                } 
            } 
        }

        int Cdist = Integer.MAX_VALUE;
        Pac pp = null;
        for(Pac pr: closePacs){
            int dist = AI.distances[pr.y][pr.x][p.acNextY][p.acNextX];
            if(dist < Cdist){
                pp = p;
                Cdist = dist;
            }
        }

        if(pp != null){
            if(p.typeId == pp.typeId){
                if(pp.abilityCooldown == 0){
                    //dangerous
                    p.switchTo = AI.winAgainst(p.typeId);
                }else{
                    //not dangerous
                    p.switchTo = AI.loseAgainst(pp.typeId);
                }
            //would lose against opponent 
            }else if(p.typeId.equals(AI.winAgainst(pp.typeId))){
                if(pp.abilityCooldown == 0){
                    //dangerous
                    p.switchTo = pp.typeId;
                }else{
                    //not dangerous
                    p.switchTo = AI.loseAgainst(pp.typeId);
                }
            //would win against opponent
            }else if(p.typeId.equals(AI.loseAgainst(pp.typeId))){
                if(pp.abilityCooldown == 0){
                    //dangerous
                    p.switchTo = AI.winAgainst(p.typeId);
                }
            }
        }
    }

    static void checkAbilities(Pac p){
        if(p.abilityCooldown == 0){
            PacMethods.whatToSwitchTo(p);
            if(p.switchTo == null){
                p.speed = true;
            }else if(p.switchTo != null){
                p.switcha = true;
            }
        }
    }

    static void findNextPlaceActually(Pac p){
        ArrayList<int[]> neigh = AI.findNeighbors(p.y,p.x,Player.map);
        int dist = Integer.MAX_VALUE;
        for(int[] loc: neigh){
            if(AI.distances[loc[0]][loc[1]][p.nextY][p.nextX] < dist){
                dist = AI.distances[loc[0]][loc[1]][p.nextY][p.nextX];
                p.acNextX = loc[1];
                p.acNextY = loc[0];
            }
        }
    }
}


class Pac{
    int pacId;
    boolean mine;
    int x;
    int y;
    String typeId;
    int speedTurnsLeft;
    int abilityCooldown;

    static Pac[] pacs;
    static boolean[][] locationsAllPacsBeen;
    static ArrayList<Pac> myPacs;
    static ArrayList<Pac> visibleEnemyPacs;

    double[][] scoreMap;

    //Location pac is honestly aiming for
    int choiceX;
    int choiceY;

    //Move location that is actually outputted (in direction of choiceX,Y)
    int nextX;
    int nextY;

    //The exact next square pac will be on if it were to move towards nextX,Y, given it is different from px,y
    int acNextX;
    int acNextY;

    //Type to switch to if switch may be necessary
    String switchTo;

    //Should I use speed or switch>
    boolean speed;
    boolean switcha;

    //Places this pac is not allowed to choose (each string is in the form "r,c")
    ArrayList<String> noGo;

    //Initializes static variables not meant to change in length 
    public static void initialize(int rows, int cols){
        pacs = new Pac[10];
        locationsAllPacsBeen = new boolean[rows][cols];
    }

    public Pac(int Id, boolean owned, int x, int y, String type, int speedTurns, int ability){
        this.pacId = Id;
        this.mine = owned;
        this.x = x;
        this.y = y;
        this.typeId = type;
        this.speedTurnsLeft = speedTurns;
        this.abilityCooldown = ability;
        noGo = new ArrayList<String>();
        locationsAllPacsBeen[y][x] = true;
        pacs[Id] = this;
        ArrayList<int[]> neg = AI.findNeighbors(y, x, Player.map);

        if(pacId < 5){
            myPacs.add(this);
            Player.mapInt[y][x]-= 1;
            for(int[] loc : neg){
                Player.mapInt[loc[0]][loc[1]] -= 0.8;
            }
        }else {
            visibleEnemyPacs.add(this);
        }
    }

    public void updateInfo(int Id, boolean owned, int x, int y, String type, int speedTurns, int ability){
        Player.mapInt[y][x]-=2;
        this.pacId = Id;
        this.mine = owned;
        this.x = x;
        this.y = y;
        this.typeId = type;
        this.speedTurnsLeft = speedTurns;
        this.abilityCooldown = ability;
        locationsAllPacsBeen[y][x] = true;
        if(pacId < 5){
            Player.mapInt[y][x]-= 1;
        }
    }
}
