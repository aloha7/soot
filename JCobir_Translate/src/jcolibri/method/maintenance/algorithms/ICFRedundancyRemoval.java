package jcolibri.method.maintenance.algorithms;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jcolibri.cbrcore.CBRCase;
import jcolibri.method.maintenance.AbstractCaseBaseEditMethod;
import jcolibri.method.maintenance.CompetenceModel;
import jcolibri.method.maintenance.solvesFunctions.ICFSolvesFunction;
import jcolibri.method.reuse.classification.KNNClassificationConfig;

/**
 * Provides the ability to run the ICF case base editing algorithm 
 * on a case base to eliminate redundancy.
 * 
 * @author Lisa Cummins
 * @author Derek Bridge
 * 18/05/07
 */
public class ICFRedundancyRemoval extends AbstractCaseBaseEditMethod {
	
	/**
	 * Simulates the ICF case base editing algorithm, returning the cases
	 * that would be deleted by the algorithm.
	 * @param cases The group of cases on which to perform editing.
	 * @param simConfig The similarity configuration for these cases.
	 * @return the list of cases that would be deleted by the 
	 * ICF algorithm.
	 */
	public Collection retrieveCasesToDelete(Collection cases, KNNClassificationConfig simConfig)
	{	/* ICF Algorithm:
		 * T: Training Set
		 * 
		 * Run RENN on T
		 * (Not included here, RENN performed seperately)
		 *
		 * Repeat
		 * 		For all x E T do
		 * 			compute reachable(x)
		 * 			compute coverage(x)
		 * 		End-For
		 * 		progress = false
		 * 		For all x E T do
		 * 			If |reachable(x)| > |coverage(x)| then
		 * 				flag x for removal
		 * 				process = true
		 * 			End-If
		 * 		End-For
		 * 		For all x E T do	
		 * 			If x flagged for removal then
		 * 				T = T - {x}
		 * 			End-If
		 * 		End-For
		 * Until not progress
		 * 
		 * Return T
		 */
		jcolibri.util.ProgressController.init(this.getClass(),"ICF Redundancy Removal",jcolibri.util.ProgressController.UNKNOWN_STEPS);
		List localCases = new LinkedList();
		for(Object on: cases)
		{	
			CBRCase c = (CBRCase)on;
			localCases.add(c);
		}

		CompetenceModel sc = new CompetenceModel();
		Map coverageSets = null, reachabilitySets = null;
		List allCasesToBeRemoved = new LinkedList();
	
		boolean changes = true;
		while(changes)
		{	changes = false;
			List casesToBeRemoved = new LinkedList();
			
			sc.computeCompetenceModel(new ICFSolvesFunction(), simConfig, localCases);
			coverageSets = sc.getCoverageSets();
			reachabilitySets = sc.getReachabilitySets();
	
			for(Object om: localCases)
			{	
				CBRCase c = (CBRCase)om;
				Collection coverageSet = (Collection)coverageSets.get(c);
				Collection reachabilitySet = (Collection)reachabilitySets.get(c);
				if(reachabilitySet.size() > coverageSet.size())
				{	casesToBeRemoved.add(c);
					changes = true;
				}
			}
	
			allCasesToBeRemoved.addAll(casesToBeRemoved);
			localCases.removeAll(casesToBeRemoved);
			jcolibri.util.ProgressController.step(this.getClass());
		}
		jcolibri.util.ProgressController.finish(this.getClass());
		return allCasesToBeRemoved;
	}	
}