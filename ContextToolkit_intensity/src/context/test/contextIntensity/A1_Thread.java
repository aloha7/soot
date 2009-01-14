package context.test.contextIntensity;

public class A1_Thread implements Runnable{

	public void run(){
		while(true){
			System.out.println(this);
			System.out.println("A1_Thread is running");	
		}		
	}
	
	public static void main(String[] args){
		(new Thread(new A1_Thread())).start();
	}
}
