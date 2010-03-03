package jcolibri.method.maintenance.algorithms;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import jcolibri.cbrcore.CBRCase;
import jcolibri.exception.InitializingException;
import jcolibri.method.maintenance.AbstractCaseBaseEditMethod;
import jcolibri.method.maintenance.CompetenceModel;
import jcolibri.method.maintenance.solvesFunctions.CBESolvesFunction;
import jcolibri.method.reuse.classification.KNNClassificationConfig;

import org.apache.commons.logging.LogFactory;

/**
 * Provides the ability to run the RC case base editing algorithm 
 * on a case base to eliminate redundancy.
 * 
 * @author Lisa Cummins
 * @author Derek Bridge
 * 18/05/07
 */
public class RCRedundancyRemoval extends AbstractCaseBaseEditMethod {
	
	/**
	 * Simulates the RC case base editing algorithm, returning the cases
	 * that would be deleted by the algorithm.
	 * @param cases The group of cases on which to perform editing.
	 * @param simConfig The similarity configuration for these cases.
	 * @return the list of cases that would be deleted by the 
	 * RC algorithm.
	 */
	public Collection retrieveCasesToDelete(Collection cases, KNNClassificationConfig simConfig) 
	{	/* 
		 * RC Algorithm:
		 *	
		 * T: Original training cases
		 * CM: Competence Model
		 * RC(c): Sum_c' E CoverageSet(C) (1/|ReachabilitySet(c')|)
		 * 
		 * Edit(T,CM,RC):
		 * 
		 * R-Set = RENN(T) {that is, repeated ENN}
		 * (Not included here, RENN performed separately)
		 * E-Set = {}
		 * While R-Set is not empty
		 * 		c = Next case in R-Set according to RC
		 * 		E-Set = E-Set U {c}
		 * 		R-Set = R-Set – CoverageSet(c)
		 * 		Update(CM)
		 * EndWhile
		 * 
		 * Return (E-Set)
		 */
	    	jcolibri.util.ProgressController.init(this.getClass(),"RC Redundancy Removal",jcolibri.util.ProgressController.UNKNOWN_STEPS);
		List localCases = new LinkedList();
		for(Object on: cases)
		{
			CBRCase c = (CBRCase)on;
			localCases.add(c);
		}
			
		CompetenceModel sc = new CompetenceModel();
		
		LinkedList keepCases = new LinkedList();
		
		while(localCases.size() > 0)
		{	double topRCScore = 0.0;
			CBRCase topRCCase = null;

			sc.computeCompetenceModel(new CBESolvesFunction(), simConfig, localCases);
			
			try
			{   for(Object om: localCases)
			    {	
					CBRCase c = (CBRCase)om;
					double rcScore = 0.0;
			    	Collection cCov = sc.getCoverageSet(c);
			    	for(Object ot: cCov)
			    	{
			    		CBRCase c1 = (CBRCase)ot;
			    		rcScore += (1/(double)sc.getReachabilitySet(c1).size());
			    	}
			    	if(rcScore > topRCScore)
			    	{	topRCScore = rcScore;
			    		topRCCase = c;
			    	}
			    }
			    
			    keepCases.add(topRCCase);
			    
			    Collection cSet = sc.getCoverageSet(topRCCase);
			    List toRemove = new LinkedList();
			    for(Object o1: cSet)
			    {
			    	CBRCase c = (CBRCase)o1;
			    	toRemove.add(c);
			    }
			    localCases.removeAll(toRemove);
			} catch (InitializingException e)
			{   LogFactory.getLog(this.getClass()).error(e);
			}
			jcolibri.util.ProgressController.step(this.getClass());
		}
		
		//Add all cases that are not being kept to the list of deleted cases
		List allCasesToBeRemoved = new LinkedList();
		for(Object o2: cases)
		{	
			CBRCase c = (CBRCase)o2;
			if(!keepCases.contains(c))
				allCasesToBeRemoved.add(c);
		}
		jcolibri.util.ProgressController.finish(this.getClass());
		return allCasesToBeRemoved;
	}
}