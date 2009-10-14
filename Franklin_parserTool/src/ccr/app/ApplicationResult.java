package ccr.app;

public class ApplicationResult {
	
	private final int moved;
	private final int reliable;
	
	public ApplicationResult(int m, int r) {
		
		moved = m;
		reliable = r;
	}
	
	public boolean equals(Object object) {
		
		if (!(object instanceof ApplicationResult)) {
			return false;
		}
		ApplicationResult ar = (ApplicationResult) object;
		return this.moved == ar.moved || this.reliable == ar.reliable;
	}
	
	public String toString() {
		
		return "moved:" + moved + " reliable:" + reliable; 
	}

}
