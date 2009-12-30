package ccr.app;

import java.io.*;
import java.util.*;

import ccr.help.FileOperator;

public class CCRScenarios {

    public static int SCENARIO_NUM = 2;  // Two scenarios

    public static int POS_NUM = 8;  // Eight positions in each scenario

    private static Coordinates posActLocs[][] = new Coordinates[SCENARIO_NUM][POS_NUM];  // Actual locations of positions

    private static Vector posEstLocs[][] = new Vector[SCENARIO_NUM][POS_NUM];  // Estimated locations of positions

    private Random rand;

    static {

        // Initialize all actual locations of positions in scenario 0
        posActLocs[0][0] = new Coordinates(0.5, 0.5);
        posActLocs[0][1] = new Coordinates(1.5, 0.5);
        posActLocs[0][2] = new Coordinates(0.5, 1.5);
        posActLocs[0][3] = new Coordinates(1.5, 1.5);
        posActLocs[0][4] = new Coordinates(0.5, 2.5);
        posActLocs[0][5] = new Coordinates(1.5, 2.5);
        posActLocs[0][6] = new Coordinates(0.5, 3.5);
        posActLocs[0][7] = new Coordinates(1.5, 3.5);

        // Initialize all actual locations of positions in scenario 1
        posActLocs[1][0] = new Coordinates(0.3, 0.5);
        posActLocs[1][1] = new Coordinates(0.3, 2.5);
        posActLocs[1][2] = new Coordinates(1.0, 0.5);
        posActLocs[1][3] = new Coordinates(1.0, 1.5);
        posActLocs[1][4] = new Coordinates(1.0, 2.5);
        posActLocs[1][5] = new Coordinates(1.0, 3.5);
        posActLocs[1][6] = new Coordinates(1.9, 1.5);
        posActLocs[1][7] = new Coordinates(1.9, 2.5);

        // Initialize all estimated locations of positions in scenario 0
        for (int i = 0; i < POS_NUM; i++) {
            posEstLocs[0][i] = new Vector();
            try {
//            	2009-09-04: 
            	BufferedReader br = new BufferedReader(new FileReader("data/scenario 0/input/" + i + ".txt"));            	
//            	System.out.println(new File("data/scenario 1/input/" + i + ".txt").getAbsolutePath());
//            	File dataFile = FileOperator.getFile(System.getProperty("java.class.path"), "data/scenario 0/input/" + i + ".txt");
//                BufferedReader br = new BufferedReader(new FileReader(dataFile));
                String line = br.readLine();
                while (line != null) {
                    StringTokenizer st = new StringTokenizer(line);
                    double x = Double.parseDouble(st.nextToken());
                    double y = Double.parseDouble(st.nextToken());
                    posEstLocs[0][i].add(new Coordinates(x, y));
                    line = br.readLine();
                }
                br.close();
            } catch (IOException e) {
                System.out.println(e);
                System.exit(1);
            }
        }

        // Initialize all estimated locations of positions in scenario 1
        for (int i = 0; i < POS_NUM; i++) {
            posEstLocs[1][i] = new Vector();
            try {
            	//2009-09-04: 
              BufferedReader br = new BufferedReader(new FileReader("data/scenario 1/input/" + i + ".txt"));
//              System.out.println(new File("data/scenario 1/input/" + i + ".txt").getAbsolutePath());
//            	File dataFile = FileOperator.getFile(System.getProperty("java.class.path"), "data/scenario 0/input/" + i + ".txt");
//                BufferedReader br = new BufferedReader(new FileReader(dataFile));

                String line = br.readLine();
                while (line != null) {
                    StringTokenizer st = new StringTokenizer(line);
                    double x = Double.parseDouble(st.nextToken());
                    double y = Double.parseDouble(st.nextToken());
                    posEstLocs[1][i].add(new Coordinates(x, y));
                    line = br.readLine();
                }
                br.close();
            } catch (IOException e) {
                System.out.println(e);
                System.exit(1);
            }
        }
    }
    
    public CCRScenarios(int seed) {
    	
    	rand = new Random(seed);
    }

    public Coordinates getActLoc(int sid, int pos) {

        Coordinates result = null;

        if (sid >= 0 && sid < SCENARIO_NUM && pos >= 0 && pos < POS_NUM) {
            result = posActLocs[sid][pos];
        } else {
            System.out.println("incorrect scenario (" + sid + ") or position (" + pos + ")");
        }

        return result;
    }

    public Coordinates getEstLoc(int sid, int pos) {

        Coordinates result = null;

        if (sid >= 0 && sid < SCENARIO_NUM && pos >= 0 && pos < POS_NUM) {
            // Experimentation
            int r = rand.nextInt(posEstLocs[sid][pos].size());
         //   System.out.print("r=" + r);
            //int r = 10;
            // ***
            result = (Coordinates) posEstLocs[sid][pos].get(r);
        } else {
            System.out.println("incorrect scenario (" + sid + ") or position (" + pos + ")");
        }

        return result;
    }

}
