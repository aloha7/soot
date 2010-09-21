package RandomWalk;

import java.util.ArrayList;

public class RandomWalk {
	public double growthRate;
	public double volatility;
	public double deltaT;
	public double rate_perStep;
	public double vol_perStep;
	public ArrayList randomProcess;
	
	public RandomWalk(double growthRate, double volatility, double deltaT){
		this.growthRate = growthRate;
		this.volatility = volatility;
		this.deltaT = deltaT;
		this.rate_perStep = growthRate * deltaT;
		this.vol_perStep = volatility * Math.sqrt(deltaT); 
	}
	
	public void genRandomProcess(int length){
		randomProcess = new ArrayList();
		
		int timeStep = 0;
		double drift = 1;
		double S = 1;
		double noise = S - drift;		
		RandomStep step = new RandomStep(timeStep, drift, S, noise);
		step.deltaS = step.S * (this.rate_perStep + step.randomNum * this.vol_perStep);		
		randomProcess.add(step);
		
		for(int i = 1; i < length; i ++){
			timeStep = i;

			RandomStep step_previous = (RandomStep)randomProcess.get(i-1);
			drift = step_previous.drift * (1 + this.rate_perStep);
			S = step_previous.S + step_previous.deltaS;
			noise =  step_previous.S*step_previous.randomNum*this.vol_perStep;
			RandomStep step_current = new RandomStep(timeStep, drift, S, noise);
			step_current.deltaS = step_current.S *( this.rate_perStep + step_current.randomNum * this.vol_perStep);
			randomProcess.add(step_current);
		}
		showRandomProcess();
	}
	
	public void showRandomProcess(){
		System.out.println(this.toString());
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < randomProcess.size(); i ++){
			RandomStep step = (RandomStep)randomProcess.get(i);
			sb.append(step.toString());
		}
		return sb.toString();
	}
}
