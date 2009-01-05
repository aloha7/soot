package ccr.stat;

import ccr.app.Application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class CFG implements Cloneable {
	
	private NodeSet N;
	private EdgeSet E;
	private VariableMap V;
	private SourceHandler source;
	private final String programID;
	private int index;
	
	public CFG(String filename) {
		
		N = new NodeSet();
		E = new EdgeSet();
		V = new VariableMap();
		source = new SourceHandler(filename);
		programID = source.getContent(Application.PROGRAM_ID_TAG);
		index = 0;
		construct(); //build a CFG
	}
	
	public Object clone() {
		
		try {
			CFG cfg = (CFG) super.clone();
			cfg.N = (NodeSet) this.N.clone();
			cfg.E = (EdgeSet) this.E.clone();
			return cfg;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	private void construct() {
		
		//2008/7/25: we should do this automatically instead of enforcing programmers to define it 
		for (Iterator i = source.getSet(Application.VARIABLE_TAG).iterator(); i.hasNext();) {
			V.add((String) i.next());
		}
		for (Iterator i = source.getSet(Application.CONTEXT_TAG).iterator(); i.hasNext();) {
			V.addContext((String) i.next());
		}
		Node lastNode = null; //the current working node
		NodeSet predNodes = new NodeSet(); // predecessors of a node
		Vector endNodes = new Vector();    // successors of a node
		boolean join = false; // indicate that there are many predecessors
		for (int i = 0; i < source.sizeStatements(); i++) {
			Statement statement = source.getStatement(i);
			if (statement.isNode()) {
				Node node = getNode(statement);
				N.add(node);
				if (!join) {
					if (lastNode != null) {
						E.add(lastNode, node);
					}
				} else {
					while (predNodes.size() > 0) {
						E.add(predNodes.get(0), node);
						predNodes.remove(predNodes.get(0));
					}
				}
				lastNode = node;
			}
			if (statement.prefix().equals("if") || statement.prefix().equals("while")) {
				i = match(i, statement.prefix(), lastNode, predNodes, endNodes); // i is the next node to be processed
				join = true;
				lastNode = (Node) endNodes.get(endNodes.size() - 1);
			} else {
				join = false;
			}
		}
	}
	
	private int match(
			int start, String token, Node startNode, NodeSet predNodes, Vector endNodes) {
		
		int i = start + 1;
		if (!source.getStatement(i).prefix().equals("{")) {
			System.out.println("Unmatchable {}");
			System.exit(1);
		}
		Node lastNode = startNode;
		boolean join = false;
		for (i = i + 1; i < source.sizeStatements(); i++) {
			Statement statement = source.getStatement(i);
			if (statement.isNode()) {
				Node node = getNode(statement);
				N.add(node);
				if (!join) {
					if (lastNode != null) {
						E.add(lastNode, node);
					}
				} else {
					while (predNodes.size() > 0) {
						E.add(predNodes.get(0), node);
						predNodes.remove(predNodes.get(0));
					}
				}
				lastNode = node;
			}
			if (statement.prefix().equals("if") || statement.prefix().equals("while")) {
				i = match(i, statement.prefix(), lastNode, predNodes, endNodes);
				join = true;
				lastNode = (Node) endNodes.get(endNodes.size() - 1);
			} else if (statement.prefix().equals("}")) {
				if (token.equals("if")) {
					if (i + 1 < source.sizeStatements() && 
							source.getStatement(i + 1).prefix().equals("else")) {
						i = match(i + 1, "else", startNode, predNodes, endNodes);
						predNodes.add(lastNode);
					} else {
						predNodes.add(lastNode);
						predNodes.add(startNode);
						endNodes.add(lastNode);
					}
				} else if (token.equals("else")) {
					predNodes.add(lastNode);
					endNodes.add(lastNode);
				} else if (token.equals("while")) {
					E.add(lastNode, startNode);
				//	predNodes.add(lastNode);
					predNodes.add(startNode);
				//	endNodes.add(lastNode);
					endNodes.add(startNode);
				}
				return i;
			} else {
				join = false;
			}
		}
		return i;
	}
	
	private Node getNode(Statement statement) {
		
		VariableSet Def = new VariableSet();
		VariableSet Use = new VariableSet();
		for (Iterator i = statement.getDef().iterator(); i.hasNext();) {
			Def.add(V.get((String) i.next()));
		}
		for (Iterator i = statement.getUse().iterator(); i.hasNext();) {
			Use.add(V.get((String) i.next()));
		}
		return getNode(programID + (index++), statement.toString(), Def, Use);
	}
	
	private Node getNode(String index, String statement, VariableSet Def, VariableSet Use) {
		
		Node node = new Node(index, statement, Def, Use);
		return NodeIndex.getInstance().process(node);
	}
	
	private PolicyNode getPolicyNode(Node node, Policy policy) {
		
		PolicyNode policyNode;
		if (policy.isDiscard()) {
			policyNode = new PolicyNode(
					node.index + Application.POLICY_NODE_DELIMITER + policy.index, 
					policy.constraint, new VariableSet(), new VariableSet(), policy);
		} else {
			policyNode = new PolicyNode(
					node.index + Application.POLICY_NODE_DELIMITER + policy.index, 
					policy.constraint, new VariableSet(node.getDef(0)), new VariableSet(), 
					policy);
		}
		return (PolicyNode) NodeIndex.getInstance().process(policyNode);
	}
	
	private NodeSet getPolicyNodeSet(Node node, Resolution resolution) {

		NodeSet policyNodes = new NodeSet();
		for (int i = 0; i < resolution.size(); i++) {
			policyNodes.add(getPolicyNode(node, resolution.get(i)));
		}
		return policyNodes;
	}
	
	
	
	public Criterion getDUAssociations() {
		
		NodeMap inMap = new NodeMap();
		NodeMap outMap = new NodeMap();
		for (int i = 0; i < N.size(); i++) {
			Node node = N.get(i);
			inMap.put(node, new NodeSet());
			if (node.sizeDef() > 0) {
				outMap.add(node, node);
			} else {
				outMap.put(node, new NodeSet());
			}
		}
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int i = 0; i < N.size(); i++) {
				Node node = N.get(i);
				NodeSet in = inMap.get(node);
				for (int j = 0; /*E.getPred(node) != null && */j < E.getPred(node).size(); j++) {
					in.add(outMap.get(E.getPred(node).get(j)));
				}
				NodeSet out = new NodeSet();
				for (int j = 0; j < in.size(); j++) {
					if (!node.hasSameDef(in.get(j))) {
						out.add(in.get(j));
					}
				}
				if (node.sizeDef() > 0) {
					out.add(node);
				}
				if (!outMap.get(node).equals(out)) {
					changed = true;
					outMap.put(node, out);
				}
			}
		}
		
		Criterion criterion = new Criterion();
		for (int i = 0; i < N.size(); i++) {
			Node node = N.get(i);
			if (node instanceof PolicyNode) {
				criterion.add(node);
			}
			for (int j = 0; j < inMap.get(node).size(); j++) {
				Node reachingNode = inMap.get(node).get(j);
				if (reachingNode.sizeDef() > 0 && node.containsUse(reachingNode.getDef(0))) {
					criterion.add(reachingNode, node);
				/*	if (E.getSucc(node).size() > 1) {
						criterion.add(reachingNode, E.getSucc(node));
					} else {
						criterion.add(reachingNode, node);
					}*/
				}
			}
		}
		return criterion;
	}
	
	public Criterion getAllPUse(Criterion allDU){
		Criterion allPUse = new Criterion();
		for(int i = 0; i < allDU.getDuMaps().keySize(); i ++){
			Node def = allDU.getDuMaps().getKey(i);
			NodeSet uses = allDU.getDuMaps().getValue(i);
			
			NodeSet PUses = new NodeSet();
			for(int j = 0; j < uses.size(); j ++){
				Node use = uses.get(j);
				//a node is a p-use if the node contain more than 1 successors
				if(E.getSucc(use).size() > 1){
					//PUses.add(use);
					PUses.add(E.getSucc(use));
				}
			}
			allPUse.add(def, PUses);
		}		
		return allPUse;
	}
	
	public Criterion getAllCUse(Criterion allDU){
		Criterion allCUse = new Criterion();
		for(int i = 0; i < allDU.getDuMaps().keySize(); i ++){
			Node def = allDU.getDuMaps().getKey(i);
			NodeSet uses = allDU.getDuMaps().getValue(i);
			
			NodeSet CUses = new NodeSet();
			for(int j = 0; j < uses.size(); j ++){
				Node use = uses.get(j);
				//a node is a p-use if the node contain more than 1 successors
				if(E.getSucc(use).size() <= 1){
					CUses.add(use);
				}
			}
			allCUse.add(def, CUses);
		}		
		return allCUse;
	}
	
	public Criterion resolveFullInline() {
		
		NodeSet inlineSet = new NodeSet();
		for (int i = 0; i < N.size(); i++) {
			Node node = N.get(i);
			if (node.hasContextDef()) {
				inlineSet.add(node);
			}
		}
		return resolveNodeInline(inlineSet);
	}
	
	public Criterion resolveKInline(int level) {

		if (level < 0) {
			System.out.println("Incorrect resolve level = " + level);
			System.exit(1);
		}
		if (level == 0) {
			return getDUAssociations();
		}
		
		Criterion criterion = new Criterion();
		NodeMap RDMap = new NodeMap();
		HashMap RWTable = new HashMap();
		HashMap inlineMap = new HashMap();
		for (int i = 0; i < N.size(); i++) {
			Node node = N.get(i);
			if (node.hasContextDef()) {
				criterion.add(resolveNodeInline(node));
				NodeSet RD = getRD(node, node.getDef(0));
				RDMap.put(node, RD);
				Vector RWRow = new Vector();
				RWRow.add(new NodeSet());
				RWRow.add(new NodeSet(node));
				RWTable.put(node, RWRow);
				HashSet inlineSets = new HashSet();
				inlineSets.add(new NodeSet(node));
				inlineMap.put(node, inlineSets);
			}
		}
		
		for (int i = 2; i <= level; i++) {
			for (Iterator j = RWTable.keySet().iterator(); j.hasNext();) {
				Node node = (Node) j.next();
				Vector RWRow = (Vector) RWTable.get(node);
				NodeSet lastRW = (NodeSet) RWRow.get(i - 1);
				NodeSet currentRW = new NodeSet();
				for (int k = 0; k < lastRW.size(); k++) {
					currentRW.add(RDMap.get(lastRW.get(k)));
				}
				RWRow.add(currentRW);
				if (currentRW.size() > 0) {
					HashSet lastInlineSets = (HashSet) inlineMap.get(node);
					HashSet currentInlineSets = new HashSet();
					for (Iterator k = lastInlineSets.iterator(); k.hasNext();) {
						NodeSet lastInlineSet = (NodeSet) k.next();
						for (int r = 0; r < currentRW.size(); r++) {
							NodeSet currentInlineSet = (NodeSet) lastInlineSet.clone();
							currentInlineSet.add(currentRW.get(r));
							currentInlineSets.add(currentInlineSet);
							criterion.add(resolveNodeInline(currentInlineSet));
						}
					}
					inlineMap.put(node, currentInlineSets);
				}
			}
		}
		
		return criterion;
	}
	
	public Criterion resolveNodeInline(Node node) {
		
		CFG cfg = (CFG) this.clone();
		return cfg.inlineResolutionGraph(node).getDUAssociations();
	}
	
	public Criterion resolveNodeInline(NodeSet set) {
		
		CFG cfg = (CFG) this.clone();
		return cfg.inlineResolutionGraph(set).getDUAssociations();
	}
	
	private CFG inlineResolutionGraph(Node node) {
		
		if (!node.hasContextDef()) {
			return this;
		}
		ContextVariable context = (ContextVariable) node.getDef(0);
		Resolution resolution = source.getResolution(context.name);
		Node entryNode = getNode(
				node.index + Application.POLICY_NODE_DELIMITER + "e", node.index + " Entry", 
				new VariableSet(), new VariableSet());
		N.add(entryNode);
		for (int i = 0; i < E.getPred(node).size(); i++) {
			E.add(E.getPred(node).get(i), entryNode);
			E.remove(E.getPred(node).get(i), node);
		}
		Node exitNode = getNode(
				node.index + Application.POLICY_NODE_DELIMITER + "x", node.index + " Exit", 
				new VariableSet(), new VariableSet());
		N.add(exitNode);
		for (int i = 0; i < E.getSucc(node).size(); i++) {
			E.add(exitNode, E.getSucc(node).get(i));
			E.remove(node, E.getSucc(node).get(i));
		}
		E.add(entryNode, node);
		E.add(node, exitNode);
		for (int i = 0; i < resolution.size(); i++) {
			PolicyNode policyNode = getPolicyNode(node, resolution.get(i));
			N.add(policyNode);
			E.add(entryNode, policyNode);
			E.add(policyNode, exitNode);
		}
		return this;
	}
	
	private CFG inlineResolutionGraph(NodeSet set) {
		
		for (int i = 0; i < set.size(); i++) {
			inlineResolutionGraph(set.get(i));
		}
		return this;
	}
	
	public Criterion resolveFullDemand() {
		
		Criterion criterion = new Criterion();
		criterion.add(getDUAssociations());
		for (int i = 0; i < N.size(); i++) {
			Node node = N.get(i);
			if (node.hasContextDef()) {
				ContextVariable context = (ContextVariable) node.getDef(0);
				Resolution resolution = source.getResolution(context.name);
				NodeSet policyNodes = getPolicyNodeSet(node, resolution);
				NodeSet RU = getRU(node, context);

				criterion.add(policyNodes);
				if (resolution.hasDiscard()) {
					NodeSet RRU = new NodeSet();
					NodeSet visited = new NodeSet(node);
					Vector worklist = new Vector();
					worklist.add(node);
					while (!worklist.isEmpty()) {
						Node m = (Node) worklist.remove(0);
						for (int j = 0; j < E.getSucc(m).size(); j++) {
							Node m1 = E.getSucc(m).get(j);
							if (!visited.contains(m1)) {
								visited.add(m1);
								if (!m1.containsUse(context)) {
									worklist.add(m1);
								} else {
									RRU.add(m1);
									worklist.add(m1);
								}
							}
						}
					}
					RRU.remove(RU);
					for (int j = 0; j < policyNodes.size(); j++) {
						if (policyNodes.get(j).hasContextDef()) {
							criterion.add(policyNodes.get(j), RU);
							criterion.add(policyNodes.get(j), RRU);
						}
					}
					criterion.add(node, RRU);
				} else {
					for (int j = 0; j < policyNodes.size(); j++) {
						if (policyNodes.get(j).sizeDef() > 0) {
							criterion.add(policyNodes.get(j), RU);
						}
					}
				}
			}
		}
		
		return criterion;
	}
	
	public Criterion resolveKDemand(int level) {
		
		if (level < 0) {
			System.out.println("Incorrect resolve level = " + level);
			System.exit(1);
		}
		if (level == 0) {
			return getDUAssociations();
		}
		
		Criterion criterion = new Criterion();
		criterion.add(getDUAssociations());
		NodeMap PNMap = new NodeMap();
		NodeMap RDMap = new NodeMap();
		NodeMap RUMap = new NodeMap();
		HashMap RWTable = new HashMap();
		for (int i = 0; i < N.size(); i++) {
			Node node = N.get(i);
			if (node.hasContextDef()) {
				ContextVariable context = (ContextVariable) node.getDef(0);
				Resolution resolution = source.getResolution(context.name);
				NodeSet policyNodes = getPolicyNodeSet(node, resolution);;
				NodeSet RD = getRD(node, context);
				NodeSet RU = getRU(node, context);
				criterion.add(resolveNodeDemand(node, policyNodes, RD, RU));
				if (resolution.hasDiscard()) {
					PNMap.put(node, policyNodes);
					RDMap.put(node, RD);
					RUMap.put(node, RU);
					Vector RWRow = new Vector();
					RWRow.add(new NodeSet());
					RWRow.add(new NodeSet(node));
					RWTable.put(node, RWRow);
				}
			}
		}
		
		for (int i = 2; i <= level; i++) {
			for (Iterator j = RWTable.keySet().iterator(); j.hasNext();) {
				Node node = (Node) j.next();
				Vector RWRow = (Vector) RWTable.get(node);
				NodeSet lastRW = (NodeSet) RWRow.get(i - 1);
				NodeSet currentRW = new NodeSet();
				for (int k = 0; k < lastRW.size(); k++) {
					currentRW.add(RDMap.get(lastRW.get(k)));
				}
				RWRow.add(currentRW);
				for (int k = 0; k < currentRW.size(); k++) {
					Node node1 = currentRW.get(k);
					for (int r = 0; r < PNMap.get(node1).size(); r++) {
						if (PNMap.get(node1).get(r).hasContextDef()) {
							criterion.add(PNMap.get(node1).get(r), RUMap.get(node));
						}
					}
					criterion.add(RDMap.get(node1), RUMap.get(node));
				}
			}
		}
		
		return criterion;
	}
	
	public Criterion resolveNodeDemand(
			Node node, NodeSet policyNodes, NodeSet RD, NodeSet RU) {
		
		Criterion criterion = new Criterion();
		if (!node.hasContextDef()) {
			return criterion;
		}
		ContextVariable context = (ContextVariable) node.getDef(0);
		Resolution resolution = source.getResolution(context.name);
	//	NodeSet policyNodes = getPolicyNodeSet(node, resolution);
	//	NodeSet RD = getRD(node, context);
	//	NodeSet RU = getRU(node, context);
		
		criterion.add(policyNodes);
		for (int i = 0; i < policyNodes.size(); i++) {
			if (policyNodes.get(i).sizeDef() > 0) {
				criterion.add(policyNodes.get(i), RU);
			}
		}
		if (resolution.hasDiscard()) {
			for (int i = 0; i < RD.size(); i++) {
				criterion.add(RD.get(i), RU);
			}
		}
		
		return criterion;
	}
	
	private NodeSet getRD(Node node, Variable variable) {
		
		NodeSet RD = new NodeSet();
		NodeSet visited = new NodeSet(node);
		Vector worklist = new Vector();
		worklist.add(node);
		while (!worklist.isEmpty()) {
			Node m = (Node) worklist.remove(0);
			for (int i = 0; i < E.getPred(m).size(); i++) {
				Node m1 = E.getPred(m).get(i);
				if (!visited.contains(m1)) {
					visited.add(m1);
					if (m1.containsDef(variable)) {
						RD.add(m1);
					} else {
						worklist.add(m1);
					}
				}
			}
		}
		return RD;
	}
	
	private NodeSet getRU(Node node, Variable variable) {
		
		NodeSet RU = new NodeSet();
		NodeSet visited = new NodeSet(node);
		Vector worklist = new Vector();
		worklist.add(node);
		while (!worklist.isEmpty()) {
			Node m = (Node) worklist.remove(0);
			for (int i = 0; i < E.getSucc(m).size(); i++) {
				Node m1 = E.getSucc(m).get(i);
				if (!visited.contains(m1)) {
					visited.add(m1);
					if (!m1.containsDef(variable) && !m1.containsUse(variable)) {
						worklist.add(m1);
					} else if (!m1.containsDef(variable) && m1.containsUse(variable)) {
						RU.add(m1);
						worklist.add(m1);
					} else if (m1.containsDef(variable) && m1.containsUse(variable)) {
						RU.add(m1);
					}
				}
			}
		}
		return RU;
	}
	
	public EdgeSet getEdgeSet() {
		
		return E;
	}
	
	public Criterion getAllUses() {
		
		return getDUAssociations().normalize(E);	
	}
	
	
	//1/16/2008:Martin
	private Criterion deleteOrdinary(Criterion c){
		
		Criterion e = (Criterion)c.cloneWithoutNodeMap();			
		NodeMap dus_e = e.getDuMaps();
		NodeMap dus_c = c.getDuMaps();
		int total = dus_c.keySize();
		for (int i = 0; i < total; i++) {
			Node defNode = dus_c.getKey(i);
			if(defNode.hasContextDef())
				dus_e.add(defNode, dus_c.getValue(i));						
		}					
		return e;
	}
	
	
//	private Criterion deleteOrdinary(Criterion c){
//		
//		Criterion e = (Criterion)c.cloneWithoutNodeMap();			
//		NodeMap dus_e = e.getDuMaps();
//		NodeMap dus_c = c.getDuMaps();
//		int total = dus_c.keySize();
//		for (int i = 0; i < total; i++) {
//			Node defNode = dus_c.getKey(i);
//			NodeSet useNodes = dus_c.getValue(i);
//			
//			VariableSet ctxDefs = defNode.getContextDef();
//			for(int k = 0; k < ctxDefs.size(); k ++){
//				Variable ctxDef = ctxDefs.get(k);
//				
//				for(int j = 0; j < useNodes.size(); j ++){
//					Node use = useNodes.get(j);
//					if(use.containsUse(ctxDef)){
//						dus_e.add(defNode, use);						
//					}
//				}				
//			}								
//		}					
//		return e;
//	}
	
	public Criterion getDUAssociations_noOrdinary(){
		
		return this.deleteOrdinary(this.getDUAssociations());
	}
	
	public Criterion getAllUses_noOrdinary(){
		
		return this.deleteOrdinary(this.getAllUses());
	}
	
	public Criterion getAllCUses_noOrdinary(Criterion allCUse){
		return this.deleteOrdinary(allCUse);
	}
	
	public Criterion getAllPUses_noOrdinary(Criterion allPUse){
		return this.deleteOrdinary(allPUse);
	}
	
	public Criterion getAllUse_noOrdinary(Criterion allDu){
		return this.deleteOrdinary(allDu);
	}
	
	public Criterion getAllPolicies_noOrdinary(){
		
		return this.deleteOrdinary(this.getAllPolicies());
	}
	
	public Criterion getAllKResolvedDU_noOrdinary(int k){
	
		return this.deleteOrdinary(this.getAllKResolvedDU(k));
	}
	
	public Criterion getAllFullResolvedDU_noOrdinary() {
		return this.deleteOrdinary(this.getAllFullResolvedDU());
	}
	
	public Criterion getAllPolicies() {
		
		Criterion criterion = getAllUses();
		for (Iterator i = V.keySet().iterator(); i.hasNext();) {
			String name = (String) i.next();
			if (source.getResolution(name) != null) {
				criterion.add(source.getResolution(name));
			}
		}
		return criterion;
	}
	
	public Criterion getAllKResolvedDU(int k) {
		
		return resolveKDemand(k).normalize(E);
	}
	
	public Criterion getAllFullResolvedDU() {
		
		return resolveFullDemand().normalize(E);
	}
	
	
	
	public String display() {
		
		return N.display() + "\n" + E.display() + "\n";
	}
	
	public String toString() {
		
		return N.toString() + " " + E.toString();
	}
	
	public static void main(String argv[]) {
		
		CFG g = new CFG("src/ccr/app/SmartAirCondition.java");
	//	CFG g = new CFG("src//ccr//app//TestCFG2.java");
		System.out.println(g.display());
	//	System.out.println("DU associations: " + g.getDUAssociations());
		
		
		//What's the difference between FullDemand and FullInline?        
	/*	System.out.println("FullDemand: " + g.resolveFullDemand());
		System.out.println("FullInline: " + g.resolveFullInline());
		System.out.println("FullDemand: " + g.resolveFullDemand().size());
		System.out.println("FullInline: " + g.resolveFullInline().size());
		System.out.println("1 demand: " + g.resolveKDemand(1));
		System.out.println("1 inline: " + g.resolveKInline(1));
		System.out.println("1 demand: " + g.resolveKDemand(1).size());
		System.out.println("1 inline: " + g.resolveKInline(1).size());
		System.out.println("2 demand: " + g.resolveKDemand(2));
		System.out.println("2 inline: " + g.resolveKInline(2));
		System.out.println("2 demand: " + g.resolveKDemand(2).size());
		System.out.println("2 inline: " + g.resolveKInline(2).size());
	//	System.out.println("3 demand: " + g.resolveKDemand(3).normalize(g.getEdgeSet()));
	//	System.out.println("3 inline: " + g.resolveKInline(3).normalize(g.getEdgeSet()));
	//	System.out.println("3 demand: " + g.resolveKDemand(3).normalize(g.getEdgeSet()).size());
	//	System.out.println("3 inline: " + g.resolveKInline(3).normalize(g.getEdgeSet()).size());
		
		System.out.println("All-Uses: (size " + g.getAllUses().size() + ") " + g.getAllUses());
		
		/*
		System.out.println("All-Policies: (size " + g.getAllPolicies().size() + ") " + g.getAllPolicies());
		System.out.println(
				"All-1-Resolved-DU: (size " + g.getAllKResolvedDU(1).size() + ") " + g.getAllKResolvedDU(1));
		System.out.println(
				"All-2-Resolved-DU: (size " + g.getAllKResolvedDU(2).size() + ") " + g.getAllKResolvedDU(2));
		System.out.println(
				"All-Full-Resolved-DU: (size " + g.getAllFullResolvedDU().size() + ") " + g.getAllFullResolvedDU());
		
		System.out.println("Node Index: " + NodeIndex.getInstance().toString());
		*/
		
		
		
		try{
			/*
			String fileName = "src/ccr/experiment/DuPairs.txt";
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
			String line = "All-Uses: (size " + g.getAllUses().size() + ") " + g.getAllUses()
					+ "\n" + "All-Policies: (size " + g.getAllPolicies().size() + ") " + g.getAllPolicies()
					+ "\n" +"All-1-Resolved-DU: (size " + g.getAllKResolvedDU(1).size() + ") " + g.getAllKResolvedDU(1)
					+ "\n" + "All-2-Resolved-DU: (size " + g.getAllKResolvedDU(2).size() + ") " + g.getAllKResolvedDU(2)
					+ "\n" +"All-Full-Resolved-DU: (size " + g.getAllFullResolvedDU().size() + ") " + g.getAllFullResolvedDU();
			bw.write(line);
			bw.flush();
			
			String fileName_noOrdinary = "src/ccr/experiment/DuPairs_noOrdinary.txt";
			BufferedWriter bw_noOrdinary = new BufferedWriter(new FileWriter(fileName_noOrdinary));
			String line_noOrdinary = "All-Uses: (size " + g.getAllUses_noOrdinary().size() + ") " + g.getAllUses_noOrdinary()
					+ "\n" + "All-Policies: (size " + g.getAllPolicies_noOrdinary().size() + ") " + g.getAllPolicies_noOrdinary()
					+ "\n" +"All-1-Resolved-DU: (size " + g.getAllKResolvedDU_noOrdinary(1).size() + ") " + g.getAllKResolvedDU_noOrdinary(1)
					+ "\n" + "All-2-Resolved-DU: (size " + g.getAllKResolvedDU_noOrdinary(2).size() + ") " + g.getAllKResolvedDU_noOrdinary(2)
					+ "\n" +"All-Full-Resolved-DU: (size " + g.getAllFullResolvedDU_noOrdinary().size() + ") " + g.getAllFullResolvedDU_noOrdinary();
			bw_noOrdinary.write(line_noOrdinary);
			bw_noOrdinary.flush();
			
			bw_noOrdinary.close();					
	*/
			String fileName = "src/ccr/experiment/DuPairs.txt";
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
			String line = "All-Uses: (size " + g.getAllUses().size() + ") " + g.getAllUses();					
			bw.write(line);
			bw.flush();
			
			String fileName_noOrdinary = "src/ccr/experiment/DuPairs_noOrdinary.txt";
			BufferedWriter bw_noOrdinary = new BufferedWriter(new FileWriter(fileName_noOrdinary));
			String line_noOrdinary = "All-Uses: (size " + g.getAllUses_noOrdinary().size() + ") " + g.getAllUses_noOrdinary();					
			bw_noOrdinary.write(line_noOrdinary);
			bw_noOrdinary.flush();
			
			bw_noOrdinary.close();
		}catch(Exception e){
			System.out.println(e);
		}								
	}	
}
