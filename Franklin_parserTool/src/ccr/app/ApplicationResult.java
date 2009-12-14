package ccr.app;

public class ApplicationResult {
	
	public final int moved;
	public final int reliable;
	
	public ApplicationResult(int m, int r) {
		
		moved = m;
		reliable = r;
		counter = 0;
	}
	
	public boolean equals(Object object) {
		
		if (!(object instanceof ApplicationResult)) {
			return false;
		}
		ApplicationResult ar = (ApplicationResult) object;
		return this.moved == ar.moved || this.reliable == ar.reliable;
	}
	
	public String toString() {
		
		return "moved: " + moved + " reliable: " + reliable; 
	}

	//2009-1-5:for context-intensity experiments
	public final int counter;
	public ApplicationResult(int m, int r, int c){
		moved = m;
		reliable = r;
		counter = c;
	}
		
}
