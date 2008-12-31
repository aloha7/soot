package hku.cs.rank;

import java.util.LinkedList;
import java.util.List;

public class Tarantula implements RankAlgorithm {

	@Override
	public List<Result> compute(Statics statics) {
		List<Result> result = new LinkedList<Result>();

		for (String key : statics.getClassNames()) {
			for (Integer i : statics.getClassLines(key)) {
				int falseCases = statics.getClassLinesFalse(key, i);
				int trueCases = statics.getClassLinesTrue(key, i);
				float perFalse;
				if (falseCases == 0)
					perFalse = 0;
				else
					perFalse = falseCases/ (float) statics.getTestFalse();
				float perTrue;
				if (trueCases == 0)
					perTrue = 0;
				else
					perTrue = trueCases / (float) statics.getTestTrue();

				float susc;
				if (perFalse + perTrue == 0)
					susc = 0;
				else
					susc = perFalse / (perFalse + perTrue);
				float conf = perFalse > perTrue ? perFalse : perTrue;
				result.add(new Result(susc, conf, key, i));
			}
		}
		
		return result;
	}

}
