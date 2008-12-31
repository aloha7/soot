package hku.cs.rank;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Sbi implements RankAlgorithm {

	@Override
	public List<Result> compute(Statics statics) {
		List<Result> result = new LinkedList<Result>();
		
		for (String key : statics.getClassNames()) {
			
			for (Integer i : statics.getClassLines(key)) {
				int falseCases = statics.getClassLinesFalse(key, i);
				int trueCases = statics.getClassLinesTrue(key, i);
				float susc;
				if (falseCases == 0 && trueCases == 0)
					susc = 0;
				else
					susc = (float) falseCases
							/ (falseCases + trueCases);
				result.add(new Result(susc, key, i));
			}
		}
		
		return result;
	}

}
