package jcolibri.method.reuse.classification;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jcolibri.extensions.classification.ClassificationSolution;
import jcolibri.method.retrieve.RetrievalResult;

/**
 * Provides the ability to classify a query by predicting its
 * solution from supplied cases. Classification is done by 
 * majority voting, so the predicted class is the one that
 * has the highest number of votes.
 * 
 * @author Derek Bridge
 * @author Lisa Cummins
 * 16/05/07
 */
public class MajorityVotingMethod extends AbstractKNNClassificationMethod
{

    /**
     * Predicts the class that has the highest number of votes
     * among the k most similar cases.
     * If several classes receive the same highest vote, the class that
     * has the lowest hash code is taken as the prediction. 
     * @param cases
     *            an ordered list of cases along with similarity scores.
     * @return Returns the predicted solution.
     */
    public ClassificationSolution getPredictedSolution(Collection cases)
    {
        Map votes = new HashMap();
        Map values = new HashMap();
        
        for (Object on: cases)
        {
        	RetrievalResult result = (RetrievalResult)on;
            ClassificationSolution solution = (ClassificationSolution)result.get_case().getSolution();
            
            Object classif = solution.getClassification();
            
            if (votes.containsKey(classif))
            {
                votes.put(classif, (Integer)votes.get(classif) + 1);
            }
            else
            {
                votes.put(classif, 1);
            }
            values.put(classif, solution);
        }
        
        int highestVoteSoFar = 0;
        Object predictedClass = null;
        for (Object om : votes.entrySet())
        {
        	Map.Entry e = (Map.Entry)om;
            if ((Integer)e.getValue() >= highestVoteSoFar)
            {
                highestVoteSoFar = (Integer)e.getValue();
                predictedClass = (Object)e.getKey();
            }
        }
        
        
        return (ClassificationSolution)values.get(predictedClass);
    }
}
