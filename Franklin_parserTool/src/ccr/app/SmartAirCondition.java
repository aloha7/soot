package ccr.app;

public class SmartAirCondition extends Application{
	public int heatUp(double temp){
		//ENTRY
		// Program ID [c]
		
		// Ordinary Variable [time]
		
		// Context Variable [temp]
		
		// Assignment [=]
				
		
		/*just a test
		 * 
		 */
		int time = 0;
		while(temp!=28){		
			temp++;
			//just a test
			time ++;
		}		
		return time;	
		//EXIT
	}
	
	public int heatUp(int temp){
		int time = 0;
		while(temp!=28){		
			temp++;
			time ++;
		}		
		return time;			
	}
	
	public int coolDown(int temp){
		int time = 0;
		do{
			temp --;
			time ++;
		}while(temp!=28);
		return time;
	}
	
	public int tune(int temp){
		int time = 0;
		do{
			if(temp>28){
				temp --;
			}else{
				temp ++;
			}
			time ++;
		}while(temp!=28);
		return time;
	}
	
	public Object application(String testcase){
		return "";
	}
	
	protected void resolve(){
		
	}
	
	public static void main(String[] args){
		for(int i = 0; i < 5; i ++){
			System.out.println(i);
		}
	}

}
