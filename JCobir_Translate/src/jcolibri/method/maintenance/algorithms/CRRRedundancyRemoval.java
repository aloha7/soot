package jcolibri.method.maintenance.algorithms;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jcolibri.cbrcore.CBRCase;
import jcolibri.exception.InitializingException;
import jcolibri.method.maintenance.AbstractCaseBaseEditMethod;
import jcolibri.method.maintenance.CaseResult;
import jcolibri.method.maintenance.CompetenceModel;
import jcolibri.method.maintenance.solvesFunctions.CBESolvesFunction;
import jcolibri.method.reuse.classification.KNNClassificationConfig;

import org.apache.commons.logging.LogFactory;

/**
 * Provides the ability to run the CRR case base editing algorithm 
 * on a case base to eliminate redundancy.
 * 
 * @author Lisa Cummins
 * @author Derek Bridge
 * 18/05/07
 */
public class CRRRedundancyRemoval extends AbstractCaseBaseEditMethod {

	/**
	 * Simulates the CRR case base editing algorithm, returning the cases
	 * that would be deleted by the algorithm.
	 * @param cases The group of cases on which to perform editing.
	 * @param simConfig The similarity configuration for these cases.
	 * @return the list of cases that would be deleted by the 
	 * CRR algorithm.
	 */
	public List retrieveCasesToDelete(Collection cases, KNNClassificationConfig simConfig)
	{	/*
		 * Conservative Redundancy Removal(CRR) Algorithm:
		 * T, Training Set
		 * 
		 * Build case-base competence model
		 * For each c in T
		 * 		CSet(c) = Coverage Set of c
		 * End-For
		 * 
		 * Remove redundant cases from case-base
		 * ESet = {}, (Edited Set)
		 * TSet = T sorted in ascending order of CSet(c) size
		 * c = first case in TSet
		 * 
		 * While TSet != {}
		 * 		ESet = ESet + {c}
		 * 		TSet = TSet – CSet(c)
		 * 		c = next case in TSet
		 * End-While
		 * 
		 * Return TSet
		 */
	    	jcolibri.util.ProgressController.init(this.getClass(), "Conservative Redundancy Removal(CRR)", jcolibri.util.ProgressController.UNKNOWN_STEPS);
		List localCases = new LinkedList();
		for(Object on: cases)
		{
			CBRCase c = (CBRCase)on;
			localCases.add(c);
		}
		
		CompetenceModel sc = new CompetenceModel();
		sc.computeCompetenceModel(new CBESolvesFunction(), simConfig, localCases);
		//Map<CBRCase, Collection<CBRCase>> coverageSets = sc.getCoverageSets();

		LinkedList tSet = new LinkedList();
		
		List caseCoverageSetSizes = new LinkedList();
		for(Object on: localCases)
		{	
			CBRCase c = (CBRCase)on;
			tSet.add(c);
			Collection currCoverageSet = null;
			try
			{   currCoverageSet = sc.getCoverageSet(c);
			    caseCoverageSetSizes.add(new CaseResult(c, currCoverageSet.size()));
			} catch (InitializingException e)
			{   LogFactory.getLog(this.getClass()).error(e);
			}
			jcolibri.util.ProgressController.step(this.getClass());
		}
		
		caseCoverageSetSizes = CaseResult.sortResults(true, caseCoverageSetSizes);
		List newCases = new LinkedList();
		List allCasesToBeRemoved = new LinkedList();
		
		while(caseCoverageSetSizes.size() > 0)
		{	List removeThese = new LinkedList();
		    	CBRCase c = ((CaseResult)caseCoverageSetSizes.get(0)).getCase();
			newCases.add(c);
			try
			{   Collection cCoverageSet = sc.getCoverageSet(c);
			    for(Iterator cIter = cCoverageSet.iterator(); cIter.hasNext(); )
			    {	CBRCase removed = (CBRCase)cIter.next();
			    	if(!removed.equals(c) && !allCasesToBeRemoved.contains(removed))
			    	{	allCasesToBeRemoved.add(removed);
			    	}
			    	removeThese.add(removed);
			    }
			    ListIterator iter = null;
			    for(iter = caseCoverageSetSizes.listIterator(); iter.hasNext() && removeThese.size() > 0; )
			    {	CaseResult cResult = (CaseResult)iter.next();
			        	if(removeThese.contains(cResult.getCase()))
			    	{	iter.remove();
			    		removeThese.remove(cResult.getCase());
			    	}
			    }
			    caseCoverageSetSizes = CaseResult.sortResults(true, caseCoverageSetSizes);
			} catch (InitializingException e)
			{   LogFactory.getLog(this.getClass()).error(e);
			}
			jcolibri.util.ProgressController.step(this.getClass());
		}	
		jcolibri.util.ProgressController.finish(this.getClass());
		return allCasesToBeRemoved;
	}
}
