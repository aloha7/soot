package context.test.incubator;

import java.util.Vector;

import context.arch.widget.WTourDemo;
import context.arch.widget.WTourEnd;
import context.arch.widget.WTourRegistration;

public class VectorTest {
	
	public static void main(String[] args){
		Vector events = new Vector();
		events.add(WTourRegistration.UPDATE);
		events.add(WTourDemo.INTEREST);
		events.add(WTourDemo.VISIT);
		events.add(WTourEnd.END);
		
		Vector test = new Vector();
		test.add("a");
		test.add("b");
		test.add("c");
		test.add("d");
//		System.out.println(test.contains("c"));
		
		Vector test1 = new Vector();
		test1.add("c");
		test1.add("d");
		test1.add("e");
		test1.add("f");
		
		Vector all = new Vector();
		all.add(test);
		all.add(test1);
		System.out.println(all.contains(test1));
	}
}
