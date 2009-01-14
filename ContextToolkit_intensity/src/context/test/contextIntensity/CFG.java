package context.test.contextIntensity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CFG implements Cloneable {

	public NodeSet N;
	public EdgeSet E;
	public final String ID;

	public CFG(String id) {
		ID = id;
		N = new NodeSet();
		E = new EdgeSet();
	}

	/**Traverse CFG from the Start to get the first unvisited Context-Aware Program Point 
	 * 
	 * @return
	 */
	public Node BFSToGetCapp(String id_startNode) {
		Node capps = N.get("end");
		NodeSet nodes = E.getSucc(id_startNode);

		for (int i = 0; i < nodes.size(); i++) { //how to avoid loop?
			capps = nodes.get(i);
			if (!capps.traversed) {
				capps.traversed = true;
				return capps;
			}

		}

		for (int i = 0; i < nodes.size(); i++) {
			capps = BFSToGetCapp(((Node) nodes.get(i)).index);
			if (!capps.traversed) {
				capps.traversed = true;
				return capps;
			}
		}

		capps.traversed = true;
		return capps;
	}
	
	/**This function is used to judge whether srcCappID can reach to the exit through destCappID
	 * 
	 * @param srcCappID
	 * @param destCappID
	 * @return
	 */
	public boolean canReachToExitThroughCapps(String srcCappID, String destCappID){
		boolean reachable  = false;
		NodeSet nodes = E.getSucc(srcCappID);
		for(int i = 0; i < nodes.size(); i ++){ //how to avoid loop?
			Node node = nodes.get(i);
			if(node.index.equals(destCappID)){
				reachable = true;
				break;
			}
		}
		for(int i = 0; i < nodes.size(); i ++){
			reachable = canReachToExitThroughCapps((nodes.get(i).index), destCappID);
			if(reachable){
				break;
			}
		}
		return reachable;
		
	}
	
	/**set all nodes not traversed 
	 * 
	 */
	public void refresh(){
		for(int i = 0; i < N.size(); i ++){
			((Node)N.get(i)).traversed = false;
		}
	}

}
