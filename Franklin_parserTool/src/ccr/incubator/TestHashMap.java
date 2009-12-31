package ccr.incubator;

import java.util.HashMap;

public class TestHashMap {

	public static void add(HashMap temp){
		temp.remove("a");
		
		temp.put("a", "c");
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HashMap a = new HashMap();
		a.put("a", "b");
		add(a);
		System.out.println(a.size() + " "+a.get("a"));
	}

}
