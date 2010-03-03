/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package c7302.ActivityRecommender.utils;

import hku.cs.seg.experiment.core.ExecutionTimer;
import jcolibri.exception.NoApplicableSimilarityFunctionException;
import jcolibri.method.retrieve.NNretrieval.similarity.LocalSimilarityFunction;

public class StringSetSimilarity implements LocalSimilarityFunction {

    final String SPLITTING_REGEX = "[,;]";

    public double compute(Object paramObject1, Object paramObject2)
            throws NoApplicableSimilarityFunctionException {
    	ExecutionTimer.me().startCounter("siml_comp");
        try {       	
            String[] strs1 = ((String)paramObject1).split(SPLITTING_REGEX);
            String[] strs2 = ((String)paramObject2).split(SPLITTING_REGEX);

            int total = 0;
            int sat = 0;
            
            
            for (int i = 0; i < strs2.length; i++) {
            	String str2 = strs2[i];
                if (str2.length() == 0) continue;
                total ++;
                for (int j = 0; j < strs1.length; j++) {
                	String str1 = strs1[j];
                    if (str2.equalsIgnoreCase(str1)) {
                        sat ++;
                        break;
                    }
                }
            }

            double similarity = (total == 0? 0 : ((double)sat) / total);
           
            return similarity;
        }
        catch (Exception ex) {
            throw new NoApplicableSimilarityFunctionException(ex);
        }
        finally {
        
        	ExecutionTimer.me().endCounter("siml_comp");
        }
    }

    public boolean isApplicable(Object paramObject1, Object paramObject2) {
        if (paramObject1 == null || paramObject1.getClass() != String.class) return false;
        if (paramObject2 == null || paramObject2.getClass() != String.class) return false;
        return true;
    }

}
