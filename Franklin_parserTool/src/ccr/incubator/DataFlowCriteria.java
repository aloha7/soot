package ccr.incubator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.options.Options;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SimpleLocalUses;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.toolkits.scalar.UnitValueBoxPair;


public class DataFlowCriteria {
	
	/**Return a set of def-use pairs. Defs and Uses are represented by line numbers.
	 * 
	 * @param m
	 * @return
	 */
	private HashMap<Integer, HashSet<Integer>> getDUChains(SootMethod m){
		HashMap<Integer, HashSet<Integer>> duChains = new 
							HashMap<Integer, HashSet<Integer>>();
		
		Body b = m.retrieveActiveBody();
		UnitGraph graph = new CompleteUnitGraph(b);
		SimpleLiveLocals s = new SimpleLiveLocals(graph);
		
		Iterator<Unit> gIt = graph.iterator();
		
		//generate du-pairs
		while(gIt.hasNext()){
			Unit defUnit = gIt.next();
			
			SmartLocalDefs des = new SmartLocalDefs(graph, s); //defs of local variables
			SimpleLocalUses uses = new SimpleLocalUses(graph, des);
			
			List<UnitValueBoxPair> ul = uses.getUsesOf(defUnit);
			if(ul != null && ul.size() != 0){
				//defs in this unit have some uses
				int defLine = Integer.parseInt(defUnit.getTag("LineNumberTag").toString());
				HashSet<Integer> useLines = duChains.get(defLine); //save my use lines
				if(useLines == null){
					useLines = new HashSet<Integer>();
				}
				
				//all uses lines here
				for(UnitValueBoxPair vbp: ul){
					String use = vbp.getUnit().getTag("LineNumberTag").toString(); //
					useLines.add(Integer.parseInt(use));					
				}
				
				duChains.put(defLine, useLines);
			}		
		}		
		return duChains;
	}
	
	
	public void runMain(String clazz){		
		Options.v().set_keep_line_number(true); //keep line numbers
		Options.v().setPhaseOption("jb", "use-original-names:true");// use original variable names
		Scene.v().loadClassAndSupport(clazz);
		SootClass sClass = Scene.v().getSootClass(clazz);
		sClass.setApplicationClass();
		List<SootMethod> methods = sClass.getMethods();
		
		HashMap<Integer, HashSet<Integer>> temp = null;
		for(SootMethod m: methods){
			temp = this.getDUChains(m);			
			System.out.println(m.getName() + "\n" +print(temp) + "\n\n");
		}
	}
	
	/**The du-pairs are organized by variables: for each def-variable, 
	 * its def-use chains are represented by def-uses list. 
	 * 
	 * @param clazz
	 */
	public void getDUPairs_test(String clazz){
		
		Options.v().set_keep_line_number(true);
		Options.v().setPhaseOption("jb", "use-original-names:true");
		Scene.v().loadClassAndSupport(clazz);
		SootClass sClass = Scene.v().getSootClass(clazz);
		sClass.setApplicationClass();
		
		
		Iterator<SootMethod> mIt = sClass.getMethods().iterator();
		StringBuilder sb = new StringBuilder();
		
		while(mIt.hasNext()){			
			SootMethod m = mIt.next();
			
			if(m.getName().contains("application")){
				Body b = m.retrieveActiveBody();	
				
				UnitGraph graph = new CompleteUnitGraph(b);
				SimpleLiveLocals s = new SimpleLiveLocals(graph);
				
				Iterator<Unit> gIt = graph.iterator();
				
				HashMap<Value, HashSet<DUPair>> dupairs = new 
						HashMap<Value, HashSet<DUPair>>();
				//generate du-pairs
				while(gIt.hasNext()){
					Unit defUnit = gIt.next();
					
					SmartLocalDefs des = new SmartLocalDefs(graph, s);
					
					SimpleLocalUses uses = new SimpleLocalUses(graph, des);
					
					List<UnitValueBoxPair> ul = uses.getUsesOf(defUnit);
					if(ul != null && ul.size() != 0){
						for(UnitValueBoxPair vbp: ul){
							Value defVariable = vbp.getValueBox().getValue();
							HashSet<DUPair> dupairList = dupairs.get(defVariable);
							if(dupairList == null){
								dupairList = new HashSet<DUPair>(); 
							}								
							Unit useUnit =  vbp.getUnit();
							dupairList.add(new DUPair(defUnit, useUnit));
							dupairs.put(defVariable, dupairList);
						}
					}
				}

				//reorganize du-pairs and print them
				if(dupairs.size() > 0 ){ // Only methods which have def-use pairs are printed 
					sb.append("\n================================\n");
					sb.append("MethodName:" + m.toString() + "\n");
					
					
					//print DU-Chains here
					HashMap<String, HashMap<Unit, HashSet<Unit>>> temp = new HashMap<String, 
										HashMap<Unit, HashSet<Unit>>>();
					
					for(Value defVariable: dupairs.keySet()){
						String varStr = defVariable.toString();
						HashMap<Unit, HashSet<Unit>> dus = temp.get(varStr);
						if(dus == null){
							dus = new HashMap<Unit, HashSet<Unit>>();
						}
						
						
						HashSet<DUPair> duList = dupairs.get(defVariable);
						for(DUPair dupair: duList){
							Unit defUnit = dupair.defUnit;
							HashSet<Unit> useUnits = dus.get(defUnit);
							if(useUnits == null){
								useUnits = new HashSet<Unit>();
							}
							useUnits.add(dupair.useUnit);
							dus.put(defUnit, useUnits);
						}
						
						temp.put(varStr, dus);
					}
					
					int count = 0;
					for(String var: temp.keySet()){						
						if(!var.contains("$")){
							count ++;
							sb.append(var + "\n");	
						}					
					}
					sb.append(count + "\n\n");
					
					for(String var: temp.keySet()){
						if(!var.contains("$")){ //for any non-intermediate variables
							
							HashMap<Unit, HashSet<Unit>> dus = temp.get(var);
							sb.append("Variable:" + var + "(" +dus.size() + ")\n");
							for(Unit def: dus.keySet()){
								sb.append(def +"("+ def.getTag("LineNumberTag") + ")->" 
										+ dus.get(def).size() + "\n");							
							}
							sb.append("\n\n");	
						}
					}
				}
			}
			
		}

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter("c:\\dump.txt"));
			bw.write(sb.toString());
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(sb.toString());
	}
	
	public String print(HashMap<Integer, HashSet<Integer>> duPairs){
		StringBuilder sb = new StringBuilder();
		int total = 0;
		for(Integer def: duPairs.keySet()){
			sb.append(def + "->");
			for(Integer use: duPairs.get(def)){
				sb.append(use + ",");
				total ++;
			}
			sb.append("\n");
		}
		
		sb.append("Total du-pairs numbers:" + total );		
		return sb.toString();
	}
	
	
	
	public static void main(String[] args){
		if(args.length == 0){
			System.out.println("Usage: java Soot.DUChain class_to_analysis");
			System.exit(0);
		}
		
		new DataFlowCriteria().getDUPairs_test(args[0]);
	}
}
