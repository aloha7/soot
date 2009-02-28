package context.test.util;

import java.util.Vector;

public class ContextStream {
	public int ID = 0;
	public int length = 0;
	public double CI = 0.0;
	public Vector eventSequence = null;
	
	
	public ContextStream(int ID, int length, double CI, Vector events){
		this.ID = ID;
		this.length = length;
		this.CI = CI;
		eventSequence = events;
	}
	
	public ContextStream(int ID, int length, double CI, String[] events){
		this.eventSequence = new Vector();
		for(int i = 0; i < events.length; i ++){
			this.eventSequence.add(events[i]);
		}
		this.ID = ID;
		this.length = length;
		this.CI = CI;
	}
	
	//2009-02-29: ignore ID when comparing
	public boolean equalTo(ContextStream cs){
		boolean equal = false;
		if(cs.length == length && cs.CI == CI ) {
			int i = 0;
			for(; i < cs.eventSequence.size(); i ++){
				if(!cs.eventSequence.get(i).equals(eventSequence.get(i)))
					break;
			}
			if(i == cs.eventSequence.size())
				equal = true;
		}
		return equal;
	}
	
	public boolean equalTo(Vector eventSequence){
		boolean equal = false;
		if(this.eventSequence.size() ==  eventSequence.size()){
			int i = 0;
			for(; i < this.eventSequence.size(); i ++){
				if(!this.eventSequence.get(i).equals(eventSequence.get(i))){
					break;
				}
			}
			if(i ==  this.eventSequence.size())
				equal = true;
		}
		return equal;
	}
}
