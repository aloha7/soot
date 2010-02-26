package helper;

import profile.BranchReporter;
import profile.DUAReporter;

public class TestClass { 
	public static int a = -1; //static field
	public int b = -1; //field
	public int[] aLists = new int[]{1}; //array reference
	
	public TestClass(){
		
	}
	
	public static void print1(int a){
		TestClass ins = new TestClass(); //local variables
		if(a > 0){   	                     	       	   	                
			System.out.println("a");  
			ins.print(a);
		}else if(a == 0){    
			System.out.println("b");
			ins.print(a);
		}else if( a < 0){
			System.out.println("c");
			ins.print(a);
		}
	}
	
	public void print(int a){
		int b= 2, c=0, d=-1;	//local variables	
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
	
	
	static void __link() { BranchReporter.__link(); DUAReporter.__link(); } // to allow Soot to instrument
	
	public static void main(String[] args) {	
		
		System.out.println("TestClass is executing");
		for(int i = 0; i < 3; i ++){
			print1(i);	
		}
		
	}

}
