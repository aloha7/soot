package context.apps.InOutBoard;

import cmp.QuickSort.Comparator;

public class InOutRecord implements Comparator {

	private String name;
	private String status;
	private String info;
	
	
	public InOutRecord (String n, String s, String i) {
	
		name = n;
		status = s;
		info = i;
	}
	
	
	public String getName () {
		return name;
	}
	
	public String getStatus () {
		return status;
	}
	
	public String getInfo () {
		return info;
	}
	
	public void setName (String n) {
		name = n;
	}
	
	public void setStatus (String s) {
		status = s;
	}
	
	public void setInfo (String i) {
		info = i;
	}
	
	public String toString () {
		// DS HACK WARNING: Format designed for Frontier CGI
		return ("{\"" + name + "\",\"" + status + "\",\"" + info + "\"}");
	}
	
	public final int compare (Object a, Object b) {
	
		// we assume all names are First Last and we sort on Last
		String na = ((InOutRecord) a).getName();
		String nb = ((InOutRecord) b).getName();
		int ia = na.lastIndexOf (' ');
		int ib = nb.lastIndexOf (' ');
		
		String lasta = ((InOutRecord) a).getName().substring (ia + 1);
		String lastb = ((InOutRecord) b).getName().substring (ib + 1);
		
		return lasta.compareTo(lastb);
		
	}
	
   public final boolean equals (Object a, Object b) {
      return ((InOutRecord)a).getName().equals(((InOutRecord)b).getName());
   }
	
}

