package context.test.util;

import java.util.Vector;
import java.util.Random;
import java.util.StringTokenizer;

public class TestCase extends Vector{	
	//Test case is a Vector to keep ContextEvent	
	public int length;
	public double CI; 
	
	public TestCase(){	
		length = 0;
	}
	
	
	
	public TestCase(String testCase){
		StringTokenizer token = new StringTokenizer(testCase, Constant.SEPERATOR);
		while(token.hasMoreTokens()){
			this.addElement(new ContextEvent(token.nextToken()));
		}			
		this.length = this.size();
	}
	
	
	/**Test case is a event sequence in fact, which consists of 
	 * Events which indicate event sequences and a length of this sequence	
	 * @param widgetNum
	 * @return
	 */
	public  TestCase initializeTestCase(int widgetNum){
		Random random = Randomer.getInstance();
		//2008/7/10: length couldn't be 0, maybe it can be 0.
		while(length == 0 )
			length = random.nextInt(10);
		
		for(int i = 0; i < length; i ++){
			addElement(new ContextEvent(random.nextInt(widgetNum), 
					random.nextInt(3)));						
		}		
		return this;
	}
	
	public boolean isEqual(TestCase sample){
		boolean result = false;
		if( length == sample.length ){
			int i = 0;
			for(; i < length; i ++){
				ContextEvent event = (ContextEvent)this.get(i);
				if(!event.isEqual((ContextEvent)sample.get(i)))
					break;
			}
			if(i == length)
				result = true;
		}
		return result;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < length; i ++){
			sb.append((ContextEvent)this.get(i) + Constant.SEPERATOR);
		}
		return sb.toString();
	}

}
