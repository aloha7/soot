package RandomWalk;

public class RandomStep {
	public int timeStep;
	public double randomNum;
	public double deltaS;
	public double drift;
	public double S;
	public double noise;

	public RandomStep(int timeStep, double drift, double S, double noise) {
		this.timeStep = timeStep;
		this.randomNum = getRandomNum();
		this.drift = drift;
		this.S = S;
		this.noise = noise;
	}

	private double getRandomNum() {
		double sum = 0.0;
		for (int i = 0; i < 16; i++) {
			sum += Math.random();
		}
		return sum - 8;
	}

	
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TimeStep" + "\t" + "Random #" + "\t" + "Delta S" + "\t" 
				+ "\t" + "Drift" + "\t" + "S" + "\t" + "Noise" + "\n");
		
		sb.append(this.timeStep + "\t" + this.randomNum + "\t" + this.deltaS
				+ "\t" + this.drift + "\t" + this.S + "\t" + this.noise + "\n");
		
		return sb.toString();

	}
}
