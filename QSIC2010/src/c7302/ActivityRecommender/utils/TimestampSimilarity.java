package c7302.ActivityRecommender.utils;

import hku.cs.seg.experiment.core.ExecutionTimer;

import java.sql.Timestamp;
import jcolibri.exception.NoApplicableSimilarityFunctionException;
import jcolibri.method.retrieve.NNretrieval.similarity.LocalSimilarityFunction;

/**
 * This function returns the cyclic similarity of two timestamps in a day. first
 * convert them into time of day
 * 
 */

public class TimestampSimilarity implements LocalSimilarityFunction {

        final long MILLION_SECOND_PER_HALFDAY = 12 * 3600 * 1000;
        final long MILLION_SECOND_PER_DAY = MILLION_SECOND_PER_HALFDAY * 2;

	public TimestampSimilarity() {

	}

	public double compute(Object paramObject1, Object paramObject2)
			throws NoApplicableSimilarityFunctionException {
    	ExecutionTimer.me().startCounter("siml_comp");
        try {
            Timestamp ts1 = (Timestamp) paramObject1;
            Timestamp ts2 = (Timestamp) paramObject2;

            long diff = Math.abs((ts1.getTime() % MILLION_SECOND_PER_DAY) - (ts2.getTime() % MILLION_SECOND_PER_DAY));

            if (diff >= MILLION_SECOND_PER_HALFDAY) {
                diff = MILLION_SECOND_PER_HALFDAY - diff;
            }

            double similarity = 1.0 - ((double)diff) / MILLION_SECOND_PER_HALFDAY;

            return similarity;
        }
        catch (Exception ex) {
            throw new NoApplicableSimilarityFunctionException(ex);
        }
        finally {
        	ExecutionTimer.me().startCounter("siml_comp");
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
