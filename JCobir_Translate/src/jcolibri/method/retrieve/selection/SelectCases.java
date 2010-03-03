/**
 * SelectCases.java
 * jCOLIBRI2 framework. 
 * @author Juan A. Recio-García.
 * GAIA - Group for Artificial Intelligence Applications
 * http://gaia.fdi.ucm.es
 * 24/11/2007
 */
package jcolibri.method.retrieve.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import jcolibri.cbrcore.CBRCase;
import jcolibri.method.retrieve.RetrievalResult;

/**
 * Class that stores the selectAll and selectTopK methods.
 * 
 * @author Juan A. Recio-Garcia
 * @author Developed at University College Cork (Ireland) in collaboration with Derek Bridge.
 * @version 1.0
 */
public class SelectCases
{    
    /**
     * Selects all cases
     * @param cases to select
     * @return all cases
     */
    public static Collection selectAll(Collection cases)
    {
	Collection res = new ArrayList();
	for(Object on: cases){
		RetrievalResult rr = (RetrievalResult)on;
		res.add(rr.get_case());
	}
		
	return res;
    }
    
    /**
     * Selects top K cases
     * @param cases to select
     * @param k is the number of csaes to select
     * @return top k cases
     */
    public static Collection selectTopK(Collection cases, int k)
    {
	ArrayList res = new ArrayList();
	Iterator cIter  =cases.iterator(); 
	for(int c=0; c<k && c<cases.size(); c++)
	    res.add(((RetrievalResult)cIter.next()).get_case());
	return res;    
    }
    
    /**
     * Selects all cases but returns them into RetrievalResult objects
     * @param cases to select
     * @return all cases into RetrievalResult objects
     */
    public static Collection selectAllRR(Collection cases)
    {
	return cases;
    }
    
    /**
     * Selects top k cases but returns them into RetrievalResult objects
     * @param cases to select
     * @return top k cases into RetrievalResult objects
     */
    public static Collection selectTopKRR(Collection cases, int k)
    {
	ArrayList res = new ArrayList();
	Iterator cIter  =cases.iterator(); 
	for(int c=0; c<k && c<cases.size(); c++)
	    res.add((RetrievalResult)cIter.next());
	return res;    
    }
}
