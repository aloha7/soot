package hku.cs.seg.experiment.qsic2010.fun;

import c7302.ActivityRecommender.utils.GPSLocationSimilarity;

public class TrialMain {
	
	public void assign(int a){
		int b = 0;
		if(a > 1){
			b = 1;
		}else if( a == 1){
			b = 2; 
		}else {
			b = 3;
		}
		
		int i = 0;
		while(i < 10){
			i ++;
		}
		
		for(int k = 0; k < 10; k ++){
			b = 2;
		}
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GPSLocationSimilarity sim = new GPSLocationSimilarity();
		System.out.println(sim.computeDistance("114.184678000000,22.2824380000000", "114.20539569952108,22.291835862529183"));
//		System.out.println(sim.computeDistance("114.16950345039368,22.277743214103282", "114.18127942085266,22.281332141508372"));

	}

}
