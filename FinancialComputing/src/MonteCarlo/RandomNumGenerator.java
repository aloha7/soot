package MonteCarlo;

import java.util.ArrayList;

public class RandomNumGenerator {
	public static ArrayList numSeq = new ArrayList();

	/**generate a sequence of random numbers with a given length  
	 * 
	 * @param length
	 * @return
	 */
	public static ArrayList genRandomNumSequence(int length) {
		ArrayList path = new ArrayList();
		for(int i = 0; i < length; i ++){
			path.add(BoxMuller());
		}
		
		//for testing purpose
//		path.add(-0.455371721);
//		path.add(0.835858055);
//		path.add(-1.300297453);
//		path.add(0.32408914);
//		path.add(2.580365738);
//		path.add(0.867100103);
//		path.add(-0.428714404);
//		path.add(0.857485704);
//		path.add(0.360733866);
//		path.add(2.774300791);
//		path.add(-0.943638153);
//		path.add(-0.126074567);
		
		return path;
	}

	
	
	private static double BoxMuller(){
		double r, x, y;
		
		do{
			x = Math.random() * 2 - 1;
			y = Math.random() * 2 - 1;
			r = Math.pow(x, 2) + Math.pow(y, 2);
		}while(r > 1 || r ==0);
		return x * Math.sqrt(-2*Math.log(r)/r);
	}
	
	private static double AlternativeBoxMuller(){
		double  x, y;
		x = Math.random();
		y = Math.random();
		return Math.sqrt(-2*Math.log(x))* Math.sin(2*Math.PI*y);
	}
}
