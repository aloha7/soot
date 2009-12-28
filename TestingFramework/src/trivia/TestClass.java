package trivia;

public class TestClass { 
                                                
	public static void main(String[] args) {	
		System.out.println("TestClass is executing");
		int a = Integer.parseInt(args[0]), b= 2, c=0, d=-1;		
		if(a > 0){   	                     	       	   	                
			c = a + b;		                               
			d = a - b;      
		}else{    
			c = a - b;
			d = a + b;
		} 
		
		for(int i = 0; i < 4; i++){
			d = c * d;
			c = c * d;	
		}		
	}

}
