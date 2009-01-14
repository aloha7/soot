package context.test.contextIntensity;

import java.util.Vector;

public class Switches {
	private int switches;
	private Vector seqs;

	public Switches(int k) {
		switches = k;
		seqs = new Vector();
	}

	public void addSwitch(String seq) {
		if (seqs != null && seqs.size() < (switches + 1)) {
			seqs.add(seq);
		}
	}
	
	public String getSwitch(int index){
		return (String)seqs.get(index);
	}
	
	public Vector findDuplicate(){
		Vector duplicates = new Vector();
		for(int i = 0; i < seqs.size(); i ++){
			String seq = (String)seqs.get(i);
			for(int j = i; j < seqs.size(); j++){
				if(((String)seqs.get(j)).equals(seq)){
					duplicates.add(seq);
					break;
				}
			}
		}		
		return duplicates;
	}
	
	public void remove(int index){
		if(index >-1 && index < seqs.size()){
			seqs.remove(index);
		}
	}
	
	public boolean isEmpty(){
		boolean isEmpty = false;
		if(seqs.size() == 0)
			isEmpty = true;
		return isEmpty;
	}
	
	public int getLength(){
		return seqs.size();
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < seqs.size(); i ++){
			sb.append(seqs.get(i)+ "\t");
		}
		return sb.toString();
	}
}
