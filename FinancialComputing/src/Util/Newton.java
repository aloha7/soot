package Util;

public class Newton {
	double S, K, T, r, C;
	
	public Newton(double S, double K, double r, double T){
		this.S = S;
		this.K = K;
		this.r = r;
		this.T = T;
	}
	
	public Newton(double S, double K, double r, double T, double C){
		this.S = S;
		this.K = K;
		this.r = r;
		this.T = T;
		this.C = C;
	}
	
	public double getRoot(double sigma_init, double EPSILON){
		double sigma = sigma_init;
		int loops = 0;
		while(Math.abs(y(sigma)) > EPSILON){
			sigma = sigma - y(sigma)/y1(sigma);
			loops ++;
		}
		System.out.println(loops+" iterations[Newton.root]" + "\t EPSILON:"+ Math.abs(y(sigma)));
		return sigma;
	}
	
	private double C(double sigma){
		return S*N(d1(sigma)) - K * Math.pow(Math.E, -r*T)*N(d2(sigma));
	}
	
	//primitive function
	public double y(double sigma){
		double result = S*N(d1(sigma)) - K * Math.pow(Math.E, -r*T)*N(d2(sigma)) - C;
		return result;
	}
	
	//derived function
	public double y1(double sigma){
		double result = S*N1(d1(sigma))*d11(sigma) - K* Math.pow(Math.E, -r*T)*N1(d2(sigma))*d21(sigma);
		return result;
	}
	
	public double N(double x){
		double result = 0.0;
		
		double beta = 0.2316419;
		double a1 = 0.319381530;
		double a2 = -0.356563782;
		double a3 = 1.781477937;
		double a4 = -1.821255978;
		double a5 = 1.330274429;		
		double k = 1.0/(1+ beta*x);

		
		if(x<0){
			result = 1- N(-x);
		}else{
			result = 1- N1(x)*(a1*k + a2* Math.pow(k, 2) + a3*Math.pow(k, 3) + a4 * Math.pow(k, 4) + a5 * Math.pow(k, 5));
		}
		return result;
	}
	
	public double N1(double x){
		double result = Math.pow(Math.E, -x*x/2)/Math.sqrt(2*Math.PI);
		return result;
	}
	
	public double d1(double sigma){
		double result = (Math.log(S/K) + (r + sigma * sigma/2)*T)/ (sigma * Math.sqrt(T));
		return result;
	}
	
	public double d11(double sigma){
		double result = ((T*sigma)*(sigma*Math.sqrt(T)) - (Math.log(S/K) + (r + sigma * sigma /2)*T) * Math.sqrt(T))/sigma*sigma*T;
		return result;
	}
	
	public double d2(double sigma){
		double result = d1(sigma)- sigma*Math.sqrt(T);
		return result;
	}
	
	public double d21(double sigma){
		double result = d11(sigma)-Math.sqrt(T);
		return result;
	}
	
	
	public static void main(String[] args){
		double S = 100;
		double K = 90;
		double r = 0.04;
		double T = 1.0/12.0;
		Newton newton = new Newton(S, K, r, T);

		double sigma = 0.26;
		double C = newton.C(sigma);
		System.out.println("C:" + C);
		
		Newton newton1 = new Newton(S, K, r, T, C);
		
		double EPSILON = Math.pow(10, -9);		
		double sigma_init = 2.1;
		System.out.println(newton1.getRoot(sigma_init, EPSILON));
	}
	
	
}
