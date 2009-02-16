package ccr.app;

public class Coordinates {

    public double x = 0.0, y = 0.0;

    public Coordinates(double _x, double _y) {

        x = _x;
        y = _y;
    }

    public static double calDist(Coordinates l1, Coordinates l2) {

        double dist = (l2.x - l1.x) * (l2.x - l1.x) + (l2.y - l1.y) * (l2.y - l1.y);
        dist = Math.sqrt(dist);

        return dist;
    }

    public static double calDist(double x1, double y1, double x2, double y2) {

        double dist = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        dist = Math.sqrt(dist);

        return dist;
    }
    
    //2009-2-15
    public String toString(){
    	return "x=" + x + ",y=" +y;
    }

}
