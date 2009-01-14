package context.test.contextIntensity;


public class A2_thread extends Thread{
	
	int count = 10;
	public void run(){
		for(int i = 0; i<count; i++ ){
		System.out.println(this.getName());
			int pos = Manipulator.getInstance().enterScheduler(this.getName(), "10");
			System.out.println(this.getName() + " is running(" +pos+")");	
			Manipulator.getInstance().exitScheduler(this.getName(), "10");
//			System.out.println(this.getName() + " hello(" +pos+")");
		}		
	}
		
	public static void main(String[] args){
		new A2_thread().start();
		new A2_thread().start();
	}
}
