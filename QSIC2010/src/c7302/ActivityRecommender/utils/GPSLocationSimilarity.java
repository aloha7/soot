package c7302.ActivityRecommender.utils;

import hku.cs.seg.experiment.core.ExecutionTimer;

import java.math.*;
import jcolibri.exception.NoApplicableSimilarityFunctionException;
import jcolibri.method.retrieve.NNretrieval.similarity.LocalSimilarityFunction;

/**
 * This function returns the similarity of GPS coordinates in HK. sim(x,y)=1-
 * Euclidean distance/60KM assuming 60KM is the longest between any two points
 * in Hong Kong
 * 
 * Now it works with Number values.
 */
public class GPSLocationSimilarity implements LocalSimilarityFunction {

        final double DEGREES_TO_RADIANS = ( Math.PI/180.0 );
        final double EARTH_RADIUS = 6371.0;
        final double MAX_DISTANCE_HK = 60.0;
        
    public double computeDistance(Object paramObject1, Object paramObject2) {
    	
    	
    	
        String[] loc1 = ((String)paramObject1).split(",");
        String[] loc2 = ((String)paramObject2).split(",");
        
		double long1 = Double.parseDouble(loc1[0]);
		double lat1 = Double.parseDouble(loc1[1]);
		double long2 = Double.parseDouble(loc2[0]);
		double lat2 = Double.parseDouble(loc2[1]);        
        
    	long1 = long1 * DEGREES_TO_RADIANS;
    	lat1 = lat1 * DEGREES_TO_RADIANS;
    	long2 = long2 * DEGREES_TO_RADIANS;
    	lat2 = lat2 * DEGREES_TO_RADIANS;
    	
        double dLong = long2 - long1;
        double dLat = lat2 - lat1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLong / 2) * Math.sin(dLong / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS * c;
        
    	
        
        return distance;
    }

	public double compute(Object paramObject1, Object paramObject2)
			throws NoApplicableSimilarityFunctionException {
		ExecutionTimer.me().startCounter("siml_comp");

        try{
            String[] loc1 = ((String)paramObject1).split(",");
            String[] loc2 = ((String)paramObject2).split(",");
			
			double distance = computeDistance(paramObject1, paramObject2);
//            double similarity = Math.pow(distance / MAX_DISTANCE_HK, 0.25);
            double similarity = 1 - Math.pow(distance / MAX_DISTANCE_HK, 0.25);
           
            return similarity;
        }
        catch (Exception ex){
            throw new NoApplicableSimilarityFunctionException(ex);
        }
        finally {
        	ExecutionTimer.me().endCounter("siml_comp");
        }
	}

	public boolean isApplicable(Object paramObject1, Object paramObject2) {
		// TODO Auto-generated method stub
            try {
                double similarity = compute(paramObject1, paramObject2);
                return (similarity >= 0 && similarity <= 1);
            }
            catch (Exception ex){
                return false;
            }
	}
}
