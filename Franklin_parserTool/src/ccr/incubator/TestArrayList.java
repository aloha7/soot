package ccr.incubator;

import java.util.ArrayList;

public class TestArrayList {

	public static void main(String[] args){
		ArrayList a = new ArrayList();
		ArrayList b = new ArrayList();
		ArrayList c = new ArrayList();
//		String[] d = new String[]{"1", "2"};
		
		a.add("a");
		a.add("C");
		b.add("b");
		b.add("D");
//		Object[] a1 = a.toArray();
//		Object[] b1 = b.toArray();
		c.addAll(a);
		c.addAll(b);
//		c.addAll(d);
		for(int i = 0; i < c.size(); i ++){
			System.out.println(c.get(i));	
		}
		
	}
}
