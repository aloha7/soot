package hku.cs.cfg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitBox;
import soot.Value;
import soot.ValueBox;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class ClassNoXML {
	private class Node{
		public Set<Integer> succs = new HashSet<Integer>();
		public List<String> callee = new ArrayList<String>();
	}
	
	public void doClassAnalysis(String tstClassName) throws IOException, JDOMException{
		Options.v().set_keep_line_number(true);
		Scene.v().loadBasicClasses();
		SootClass sClass = Scene.v().loadClassAndSupport(tstClassName);
		sClass.setApplicationClass();
		Iterator<SootMethod> methodIt = sClass.getMethods().iterator();
	

		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();


			if (m.isAbstract()) {

			} else {
				Body b = m.retrieveActiveBody();

				UnitGraph graph = new ExceptionalUnitGraph(b);
				HashMap<Integer, Node> cfg = new HashMap<Integer, Node>();
				Iterator<Unit> j;
				int lineNumber = 0;

				for (Iterator<Unit> i = graph.iterator(); i.hasNext();) {
					Unit unit = (Unit) i.next();
					lineNumber = ((LineNumberTag) unit.getTag("LineNumberTag"))
							.getLineNumber();

					Node node;
					node = (Node) cfg.get(lineNumber);
					if (null == node)
						node = this.new Node();
					// add callee
					List<ValueBox> useAndDef = unit.getUseAndDefBoxes();
					Iterator<ValueBox> udIt = useAndDef.iterator();
					while (udIt.hasNext()) {
						Value v = udIt.next().getValue();
						if (v instanceof InvokeExpr) {
							// System.out.println(v.toString());
							// InvokeExpr ie = (InvokeExpr)v;
							String strIe = v.toString();
							String strName = strIe.substring(
									strIe.indexOf("<") + 1, strIe
											.lastIndexOf(">"));
							if ("hku".equals(strName.substring(0, strName
									.indexOf("."))))
								node.callee.add(strName);
						}
					}
					// add successor
					j = graph.getSuccsOf(unit).iterator();
					while (j.hasNext()) {
						Unit xx = (Unit) j.next();
						int tmpLineNumber = ((LineNumberTag) xx
								.getTag("LineNumberTag")).getLineNumber();
						if (tmpLineNumber != lineNumber)
							node.succs.add(tmpLineNumber);
						// not carefully judge whether can this line goto this
						// line.
						else if (xx instanceof IfStmt) {
							Iterator<UnitBox> ix = xx.getUnitBoxes().iterator();
							Unit u;
							while (ix.hasNext()) {
								u = ix.next().getUnit();
								LineNumberTag lnt = (LineNumberTag) u
										.getTag("LineNumberTag");
								if (lineNumber == lnt.getLineNumber())
									node.succs.add(lineNumber);
							}
						}

					}
					cfg.put(lineNumber, node);
				}

				// to do sort
				int size = 0, tmpLine = 0;

				while (size < cfg.size()) {
					tmpLine++;
					Iterator<String> k;
					Iterator<Integer> l;
					if (cfg.containsKey(tmpLine)) {
						System.out.print("{line: " + tmpLine + ":");
						
						Set<Integer> tmpSet = cfg.get(tmpLine).succs;
						l = tmpSet.iterator();
						if(!tmpSet.isEmpty()){
							System.out.println("\n-----------");
							System.out.print("succs:");
						}			
						while (l.hasNext()) {
							System.out.print("  "+ l.next().toString());
						}
						
						List<String> tmpList = cfg.get(tmpLine).callee;
						if(!tmpList.isEmpty()){
							System.out.println("\n-----------");
							System.out.println("callees:");
						}							
						k = tmpList.iterator();
						while (k.hasNext()) {
							System.out.println(k.next().toString());
						}

						
						size++;
						System.out.println("}\n");
					}
				}
			}
		}
		
		
	}
	
	public static void main(String[] args) {
		args = new String[] { "hku.tester.TestClass" };

		if (args.length == 0) {
			System.out
					.println("Usage: java  class_to_analyse");
			System.exit(0);
		}
		
		try {
			new ClassNoXML().doClassAnalysis(args[0]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}


