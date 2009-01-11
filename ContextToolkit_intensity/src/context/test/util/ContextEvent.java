package context.test.util;

import java.util.StringTokenizer;

public class ContextEvent {
	public int index;          //the index of context in the widget 
	public int duration;      //the lasting time of this context event
	public String context;    //the content of this event, it can be WTourRegistration.UPDATE, WTourDemo.VISIT, WTourEnd.End
	
	public ContextEvent(int index, int duration, String context){
		this.index = index;
		this.duration = duration;
		this.context = context;
	}
	
	public ContextEvent(int index, int duration){
		this.index = index;
		this.duration = duration;
		this.context = "";
	}
	
	public ContextEvent(String event){
		StringTokenizer token = new StringTokenizer(event, Constant.SUB_SEPERATOR);
		this.index = Integer.parseInt(token.nextToken());
		this.duration = Integer.parseInt(token.nextToken());
		this.context = token.nextToken();		
	}
	
	public boolean isEqual(ContextEvent sample){
		boolean result = false;
		if((this.index == sample.index) && (this.duration == sample.duration)
				/*&& this.context.equals(sample.context)*/){
			result = true;
		}
		return result;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(this.index + Constant.SUB_SEPERATOR + this.duration + Constant.SUB_SEPERATOR
				+ this.context);
		return sb.toString();
	}
}
