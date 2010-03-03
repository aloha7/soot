/**
 * NNretrievalMethod.java
 * jCOLIBRI2 framework. 
 * @author Juan A. Recio-García.
 * GAIA - Group for Artificial Intelligence Applications
 * http://gaia.fdi.ucm.es
 * 03/01/2007
 */
package jcolibri.method.retrieve.NNretrieval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jcolibri.cbrcore.CBRCase;
import jcolibri.cbrcore.CBRQuery;
import jcolibri.method.retrieve.RetrievalResult;
import jcolibri.method.retrieve.NNretrieval.similarity.GlobalSimilarityFunction;
import jcolibri.util.ProgressController;

/**
 * Performs a Nearest Neighbor numeric scoring comparing attributes. 
 * It uses global similarity functions to compare compound attributes 
 * (CaseComponents) and 
 * local similarity functions to compare simple attributes.
 * The configuration of this method is stored in the NNConfig object.
 * @author Juan A. Recio-García
 * @version 2.0
 * @see jcolibri.method.retrieve.NNretrieval.NNConfig
 */
public class NNScoringMethod {

	
      /**
       * Performs the NN scoring over a collection of cases comparing them with a query. 
       * This method is configured through the NNConfig object.
       */
	public static Collection evaluateSimilarity(Collection cases, CBRQuery query, NNConfig simConfig)
	{
		List res = new ArrayList();
		ProgressController.init(NNScoringMethod.class,"Numeric Similarity Computation", cases.size());
		GlobalSimilarityFunction gsf = simConfig.getDescriptionSimFunction();
		for(Object on: cases)
		{
			CBRCase _case = (CBRCase)on;
			res.add(new RetrievalResult(_case, gsf.compute(_case.getDescription(), query.getDescription(), _case, query, simConfig)));
			ProgressController.step(NNScoringMethod.class);
		}
		java.util.Collections.sort(res);
		ProgressController.finish(NNScoringMethod.class);
		
		return res;
	}
}
