package hku.cs.cfg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
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

public class Class {
	private class Node{
		public Set<Integer> succs = new HashSet<Integer>();
		public List<String> callee = new ArrayList<String>();
	}
	
	public void doClassAnalysis(File dir, String tstClassName) throws IOException, JDOMException{
		
		Options.v().set_keep_line_number(true);
		Scene.v().loadBasicClasses();
		SootClass sClass = Scene.v().loadClassAndSupport(tstClassName);
		sClass.setApplicationClass();
		Iterator<SootMethod> methodIt = sClass.getMethods().iterator();
		File fy = new File("C:\\xujian\\dirty.txt");
		FileWriter fw;
		try {
			fw =new FileWriter(fy,true);
			fw.append("class123");
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Element xmlRoot = new Element("class");
		xmlRoot.setAttribute("name", sClass.getName());
		Document xmlDoc = new Document(xmlRoot);
		

		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();

			Element xmlMethod = new Element("method");
			String methodName = m.toString();
			xmlMethod.setAttribute("name", methodName.substring(methodName
					.indexOf(":") + 2, methodName.length() - 1));

			if (m.isAbstract()) {
				xmlMethod.setAttribute("abstract", "true");
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

				// while(size < cfg.size()){
				// tmpLine++;
				// Iterator k;
				// if(cfg.containsKey(tmpLine)){
				// Set<Integer> tmpSet = cfg.get(tmpLine).succs;
				// System.out.println("{" + tmpLine + ":");
				// k = tmpSet.iterator();
				// while (k.hasNext()) {
				// System.out.print(k.next().toString() + " ");
				// }
				// System.out.print("\n");
				// List<String> tmpList = cfg.get(tmpLine).callee;
				// k = tmpList.iterator();
				// while(k.hasNext()){
				// System.out.println(k.next());
				// }
				// System.out.println("}");
				// size++;
				// }
				// }

				while (size < cfg.size()) {
					tmpLine++;
					Iterator<String> k;
					Iterator<Integer> l;
					if (cfg.containsKey(tmpLine)) {
						Element xmlStmt = new Element("statement");
						xmlStmt.setAttribute("line", Integer.toString(tmpLine));

						List<String> tmpList = cfg.get(tmpLine).callee;
						k = tmpList.iterator();
						while (k.hasNext()) {
							Element xmlCallee = new Element("callee");
							xmlCallee.setText(k.next());
							xmlStmt.addContent(xmlCallee);
						}

						Set<Integer> tmpSet = cfg.get(tmpLine).succs;
						l = tmpSet.iterator();
						while (l.hasNext()) {
							Element xmlSuccs = new Element("successor");
							xmlSuccs.setText(l.next().toString());
							xmlStmt.addContent(xmlSuccs);
						}
						size++;
						xmlMethod.addContent(xmlStmt);
					}
				}
			}
			xmlRoot.addContent(xmlMethod);			
		}
		
		
		new XMLOutputter().output(xmlDoc, new FileOutputStream(new File(dir, tstClassName+".xml")));
	}
	
	public static void main(String[] args) {
//		args = new String[] { "C:\\xujian","tester.TestClass" };

//		System.out.println(System.getProperty("java.class.path"));
		
		if (args.length == 0) {
			System.out
					.println("Usage: java  class_to_analyse");
			System.exit(0);
		}
		
		File f = new File(args[0]);
		if (!f.exists())
			f.mkdirs();
		else if(!f.isDirectory()){
			f.delete();
			f.mkdirs();
		}
		
		try {
			new Class().doClassAnalysis(new File(args[0]), args[1]);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

	}
}


