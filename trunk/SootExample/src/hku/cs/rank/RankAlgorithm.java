package hku.cs.rank;

import java.util.List;

public interface RankAlgorithm {
	
	public static String TARANTULA = "tarantula";
	public static String SBI = "sbi";
	public static String JACCARD = "jaccard";
	public static String OCHIAI = "ochiai";
	
	public List<Result> compute(Statics statics );
}
