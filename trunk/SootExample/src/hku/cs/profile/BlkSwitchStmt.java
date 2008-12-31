package hku.cs.profile;

import java.util.ArrayList;
import java.util.List;

public class BlkSwitchStmt implements BlkLstStmt{
	List<Condition> caseList = new ArrayList<Condition>();
	Condition defalt = null;

	public void addCase(Condition cn){
		caseList.add(cn);
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer(50);
		sb.append(caseList);
		if(defalt != null)
			sb.append(defalt);
		return sb.toString();
	}
	
}
