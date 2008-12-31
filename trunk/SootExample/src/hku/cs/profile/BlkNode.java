package hku.cs.profile;


import java.util.HashMap;
import java.util.Iterator;

public class BlkNode {
	HashMap<Integer, Integer> pres;
	HashMap<Integer, Integer> succs;

	int times;
	
	BlkLstStmt bls;

	public BlkNode(HashMap<Integer, Integer> a, HashMap<Integer, Integer> b, int t) {
		pres = a;
		succs = b;		
		times = t;
	}

	public String toString() {
		StringBuffer ret = new StringBuffer(100);
		ret.append("[Pres ");
		for (Iterator<Integer> i = pres.keySet().iterator(); i.hasNext();) {
			Integer tmp = i.next();
			ret.append(tmp);
			ret.append(":");
			ret.append(pres.get(tmp));
			ret.append(",");
		}

		ret.append("\nSuccs ");
		for (Iterator<Integer> i = succs.keySet().iterator(); i.hasNext();) {
			Integer tmp = i.next();
			ret.append(tmp);
			ret.append(":");
			ret.append(succs.get(tmp));
			ret.append(",");
		}
		ret.append("\n");

		ret.append("times=");
		ret.append(times);
		ret.append("]\n");
		return ret.toString();
	}
}
