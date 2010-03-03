/**
 * DisplayCasesIfSimilarity.java
 * jCOLIBRI2 framework. 
 * @author Juan A. Recio-García.
 * GAIA - Group for Artificial Intelligence Applications
 * http://gaia.fdi.ucm.es
 * 31/10/2007
 */
package jcolibri.extensions.recommendation.conditionals;

import java.util.Collection;

import jcolibri.method.retrieve.RetrievalResult;

/**
 * Conditional method that returns true if the retrieved cases
 * have a minimum similarity
 * 
 * @author Juan A. Recio-Garcia
 * @author Developed at University College Cork (Ireland) in collaboration with Derek Bridge.
 * @version 1.0
 *
 */
public class DisplayCasesIfSimilarity
{
    /**
     * Checks if cases should be displayed checking the minimum similarity of all them.
     * @param retrieval set of cases
     * @param minSimil min similarity value
     * @return true if all them have a higher similarity than minSimil.
     */
    public static boolean displayCasesIfAllHaveMinimumSimilarity(Collection retrieval, double minSimil)
    {
	for(Object o: retrieval){
		RetrievalResult rr = (RetrievalResult)o;
		if(rr.getEval() < minSimil)
			return false;
	}
	    
	return true;
    }
    
    /**
     * Checks if cases should be displayed checking the minimum similarity of them.
     * This method returns true if just one case has a higher similarity.
     * @param retrieval set of cases
     * @param minSimil min similarity value
     * @return true if any of them has a higher similarity than minSimil.
     */
    public static boolean displayCasesIfAnyHaveMinimumSimilarity(Collection retrieval, double minSimil)
    {
	for(Object o: retrieval){
		RetrievalResult rr = (RetrievalResult)o;
		if(rr.getEval() >= minSimil)
			return true;
	}
	    
	return false;
    }
}
