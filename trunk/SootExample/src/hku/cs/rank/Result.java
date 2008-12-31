package hku.cs.rank;


public class Result implements Comparable<Result> {
	float suspiciousness = 0;
	float confidence = 0;
	int rank = 0;

	String className;
	int line;

	public int compareTo(Result r) {
		if (this.suspiciousness > r.suspiciousness)
			return 1;
		else if (this.suspiciousness == r.suspiciousness) {
			if (this.confidence > r.confidence)
				return 1;
			else if (this.confidence == r.confidence)
				return 0;
			else
				return -1;
		} else
			return -1;
	}

	public Result(float s, String n, int l) {
		this.suspiciousness = s;
		this.className = n;
		this.line = l;
	}

	public Result(float s, float c, String n, int l) {
		this.suspiciousness = s;
		this.confidence = c;
		this.className = n;
		this.line = l;
	}
}