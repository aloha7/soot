package jcolibri.method.maintenance;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import jcolibri.cbrcore.CBRCase;
import jcolibri.exception.InitializingException;
import jcolibri.method.reuse.classification.KNNClassificationConfig;

/**
 * Computes the competence model for a given case base.
 * 
 * @author Lisa Cummins
 * @author Derek Bridge
 * 22/05/07
 */
public class CompetenceModel {

	private Map coverageSets;
	
	private Map reachabilitySets;

	private Map liabilitySets;
	
	/**
	 * Computes the competence model for the given cases using
	 * the given solves function.
	 * @param solves the function to use to find which cases
	 * solve a query case.
	 * @param knnConfig
	 * @param cases the cases for which the competence model 
	 * is being computed.
	 */
	public void computeCompetenceModel(SolvesFunction solves, KNNClassificationConfig knnConfig, Collection cases)
	{	
		coverageSets = new HashMap();
		reachabilitySets = new HashMap();
		liabilitySets = new HashMap();
		
		for(Object on: cases)
		{	
			CBRCase q = (CBRCase)on;
			solves.setCasesThatSolveAndMisclassifyQ(q, cases, knnConfig);
			Collection solveQ = (Collection)solves.getCasesThatSolvedQuery();
			Collection misclassifyQ = (Collection)solves.getCasesThatMisclassifiedQuery();
			Collection reachabilitySet = new LinkedList();
			if(solveQ != null)
			{	for(Object om : solveQ)
				{	
					CBRCase c = (CBRCase)om;
					reachabilitySet.add(c);
					Collection coverageSet = (Collection)coverageSets.get(c);
					if(coverageSet == null)
					{	coverageSet = new LinkedList();
					}
					coverageSet.add(q);
					coverageSets.put(c, coverageSet);
				}
				reachabilitySets.put(q, reachabilitySet);
			}
			
			if(misclassifyQ != null)
			{	for(Object ot: misclassifyQ)
				{
					CBRCase c = (CBRCase)ot;
					Collection liabilitySet = (Collection)liabilitySets.get(c);
					if(liabilitySet == null)
					{	liabilitySet = new LinkedList();
					}
					liabilitySet.add(q);
					liabilitySets.put(c, liabilitySet);
				}
			}
		}
	}
	
	/**
	 * Returns the coverage set of the given case.
	 * @param c the case whose coverage set is being retrieved.
	 * @return the coverage set of c.
	 * @throws InitializingException Indicates that the competence
	 * model has not yet been computed.
	 */
	public Collection getCoverageSet(CBRCase c) throws InitializingException 
	{	if (coverageSets == null)
			throw new InitializingException();
		return (Collection)coverageSets.get(c);
	}
	
	/**
	 * Returns the reachability set of the given case.
	 * @param c the case whose reachability set is being retrieved.
	 * @return the reachability set of c.
	 * @throws InitializingException Indicates that the competence
	 * model has not yet been computed.
	 */
	public Collection getReachabilitySet(CBRCase c) throws InitializingException 
	{	if (reachabilitySets == null)
			throw new InitializingException();
		return (Collection)reachabilitySets.get(c);
	}
	
	/**
	 * Returns the liability set of the given case.
	 * @param c the case whose liability set is being retrieved.
	 * @return the liability set of c.
	 * @throws InitializingException Indicates that the competence
	 * model has not yet been computed.
	 */
	public Collection getLiabilitySet(CBRCase c) throws InitializingException 
	{	if (liabilitySets == null)
			throw new InitializingException();
		return (Collection)liabilitySets.get(c);
	}
	
	/**
	 * Returns the coverage sets of the case base.
	 * @return the coverage sets of the case base.
	 */
	public Map getCoverageSets()
	{	return coverageSets;
	}
	
	/**
	 * Returns the reachability sets of the case base.
	 * @return the reachability sets of the case base.
	 */
	public Map getReachabilitySets()
	{	return reachabilitySets;
	}
	
	/**
	 * Returns the liability sets of the case base.
	 * @return the liability sets of the case base.
	 */
	public Map getLiabilitySets()
	{	return liabilitySets;
	}
}