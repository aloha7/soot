package ccr.report;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import ccr.stat.CFG;
import ccr.stat.Criterion;
import ccr.stat.Node;
import ccr.stat.NodeMap;
import ccr.stat.NodeSet;
import ccr.stat.VariableSet;
import ccr.test.Adequacy;
import ccr.test.TestDriver;
import ccr.test.TestSet;

public class ICSE11_DUChain {

	public static void getDUChain(String criterion){
		CFG g = new CFG(System.getProperty("user.dir")
				+ "/src/ccr/app/TestCFG2.java");
		Criterion c = null;
		
		if (criterion.equals("AllPolicies"))
			c = g.getAllPolicies();
		else if (criterion.equals("All1ResolvedDU"))
			c = g.getAllKResolvedDU(1);
		else if (criterion.equals("All2ResolvedDU"))
			c = g.getAllKResolvedDU(2);
		
		Criterion duas = (Criterion) c.clone();
		NodeMap duMaps = duas.DUMap; //def(Node)->(use(Node))*
		System.out.println(duMaps.toString());
		
		getDUChain(duMaps);
		
		
	}
	
	public static HashMap<Node, Vector<NodeSet>> getDUChain(NodeMap duMaps){
		
		//1.get all different nodes
		NodeSet nodes = new NodeSet();
		for(int i = 0; i < duMaps.keySize(); i ++){
			Node def = duMaps.getKey(i);
			
			if(!nodes.contains(def)){
				nodes.add(def);
			}
			
			NodeSet useSet = duMaps.getValue(i);
			for(int j = 0; j < useSet.size(); j++){
				Node use = useSet.get(j);
				if(!nodes.contains(use)){
					nodes.add(use);
				}
			}
		}
		
		//2.iterate until all nodes have been visited
		HashMap<Node, Vector<NodeSet>> def_duchains = new HashMap<Node, Vector<NodeSet>>();
		NodeSet visited = new NodeSet();
		int i = 0;
		while(visited.size() < nodes.size() && i < duMaps.keySize()){
			Node def = duMaps.getKey(i);
			
			//1. get the first du-chain
			NodeSet duchain = new NodeSet(); //use to backtrack
			duchain.add(def);			
			Node def_temp = def;
			while(duMaps.containsKey(def_temp)){
				NodeSet uses = duMaps.get(def_temp);
				
				int index = -1; 
				Node use;
				do{
					index ++;
					use = uses.get(index);
				}while(visited.contains(use) && index < uses.size() -1);
					
				if(!visited.contains(use)){
					visited.add(use);
					duchain.add(use);
					def_temp = use;	
				}else{
					//if all use nodes are visited
					break;
				}
			}
			
			if(duchain.size() > 1){
				Vector<NodeSet> duchains = null;
				if(!def_duchains.containsKey(def)){
					duchains = new Vector<NodeSet>();
				}else{
					duchains = def_duchains.get(def);
				}
				duchains.add(duchain);
				def_duchains.put(def, duchains);
			}
			
			
			
			//2. backtrack the du-chain
			NodeSet temp_duchain = (NodeSet)duchain.clone();
			while(temp_duchain.size() > 0){
				
				//remove the last element since it is not in the index of duMaps
				temp_duchain = temp_duchain.remove(temp_duchain.size()-1);
				boolean newDUChain = false;
				
				def_temp = temp_duchain.get(temp_duchain.size() - 1);
				
				while(duMaps.containsKey(def_temp)){					
					NodeSet uses = duMaps.get(def_temp);
					
					int index = -1; 
					Node use;
					do{
						index ++;
						use = uses.get(index);
					}while(visited.contains(use) && index < uses.size() -1);
					
					
					if(!visited.contains(use)){
						visited.add(use);						
					}
					
					temp_duchain.add(use);
					def_temp = use;	
					newDUChain = true;
				}
				
				if(newDUChain){ //it only makes sense for a DUChain to add when it is new  
					Vector<NodeSet> temp_duchains = null;
					if(!def_duchains.containsKey(def)){
						temp_duchains = new Vector<NodeSet>();
					}else{
						temp_duchains = def_duchains.get(def);
					}
					
					temp_duchains.add((NodeSet)temp_duchain.clone()); //avoid the usage of global variable temp_duchain
					def_duchains.put(def, temp_duchains);	
				}
			}
			
			i ++;
		}
		
		return def_duchains;
	}
	
	/**2010-08-03: check whether a given DUChain is duplicate or prefix of previous chains
	 * 
	 * @param DUChain
	 * @param def_duchains
	 * @return
	 */
	public static boolean checkDuplicate(NodeSet DUChain, HashMap<Node, Vector<NodeSet>> def_duchains){
		boolean duplicate = false;
		
		
		Iterator<Vector<NodeSet>> ite_values = def_duchains.values().iterator();
		while(ite_values.hasNext()){
			Vector<NodeSet> duchains = ite_values.next();

			for(int i = 0; i < duchains.size(); i ++){
				NodeSet duchain = duchains.get(i);
				

				if(DUChain.size() <= duchain.size()){
					
					int j = 0;
					for(; j < DUChain.size(); j ++){//check whether DUChain is duplicate or a prefix of duchain
						if(!DUChain.get(j).equals(duchain.get(j))){
							break;
						}
					}
					
					if(j == DUChain.size()){
						duplicate = true;
					}					
				}
				
				if(duplicate){
					break;
				}
			}
			
			if(duplicate){
				break;
			}
		}		
		
		return duplicate;
	}
	
	
	
	
	public static void executeTestSets(String date, String criterion, 
			  String oldOrNew){
		TestSet testSets[][] = new TestSet[1][];
		String testSetFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/" 
			+ criterion + "TestSets_" + oldOrNew  + ".txt";
		
		testSets[0] = Adequacy.getTestSets(testSetFile);
		
		String versionPackageName = "testversion";
		
		String saveFile = testSetFile.substring(0, testSetFile
				.indexOf("."))
				+ "_limited_load.txt";


		
		String faultListFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/FaultList.txt";

		// 1. load the fault list
		ArrayList faultList = new ArrayList();
		try {
			BufferedReader br = new BufferedReader(new FileReader(
					faultListFile));
			String line = null;
			while ((line = br.readLine()) != null) {
				faultList.add(line.trim());
		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// 2.run the test
		TestDriver.test(versionPackageName, "TestCFG2", testSets,
			saveFile, faultList);
	}
		
	public static void testGetDUChain(){
		NodeMap duMaps = new NodeMap();
	
		Node node1=new Node("1", "1", new VariableSet(), new VariableSet());
		Node node2=new Node("2", "2", new VariableSet(), new VariableSet());
		Node node3=new Node("3", "3", new VariableSet(), new VariableSet());
		Node node4=new Node("4", "4", new VariableSet(), new VariableSet());
		Node node5=new Node("5", "5", new VariableSet(), new VariableSet());
		Node node6=new Node("6", "6", new VariableSet(), new VariableSet());
		
		NodeSet set1 = new NodeSet();
		set1.add(node3);
		set1.add(node4);
		
		NodeSet set2 = new NodeSet();
		set2.add(node5);
		set2.add(node6);
		
		duMaps.put(node1, set1);
		duMaps.put(node2, set1);
		
		duMaps.put(node3, set2);
		duMaps.put(node4, set2);
		
		getDUChain(duMaps);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String instruction = args[0];
		if(instruction.equals("executeTestSuites")){
			String date = args[1];		
			String criterion = args[2];
			String oldOrNew = args[3];
			executeTestSets(date, criterion, oldOrNew);
		}else if(instruction.equals("getDUChains")){
			String criterion = args[1];
			getDUChain(criterion);
		}else if(instruction.equals("getDUChain")){
			testGetDUChain();
		}
	}

}
