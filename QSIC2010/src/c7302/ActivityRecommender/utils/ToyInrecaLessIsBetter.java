package c7302.ActivityRecommender.utils;

import hku.cs.seg.experiment.core.ExecutionTimer;
import jcolibri.exception.NoApplicableSimilarityFunctionException;
import jcolibri.method.retrieve.NNretrieval.similarity.local.recommenders.InrecaLessIsBetter;

public class ToyInrecaLessIsBetter extends InrecaLessIsBetter {

	public ToyInrecaLessIsBetter(double maxAttributeValue, double jumpSimilarity) {
		super(maxAttributeValue, jumpSimilarity);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public double compute(Object paramObject1, Object paramObject2)
		throws NoApplicableSimilarityFunctionException{
		try {
			ExecutionTimer.me().startCounter("siml_comp");
			double rval = super.compute(paramObject1, paramObject2);
			
			return rval;
		} catch (NoApplicableSimilarityFunctionException e) {
			// TODO Auto-generated catch block
			throw e;
		} finally {
			ExecutionTimer.me().endCounter("siml_comp");
		}
	}

}
