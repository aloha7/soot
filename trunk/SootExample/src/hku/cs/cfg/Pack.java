package hku.cs.cfg;
import hku.cs.constant.DirAndFileConstant;

import java.io.File;
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

public class Pack {
	String packName;
	String pathName;
	
	private class Node{
		public Set<Integer> succs = new HashSet<Integer>();
		public List<String> callee = new ArrayList<String>();
		public Set<Integer> pres = new HashSet<Integer>();
	}
	
	public Pack(String pack){
		Options.v().set_keep_line_number(true);
		Scene.v().loadBasicClasses();
		packName = pack;
	}

	public Pack(String path, String pack) {
		this(pack);
		pathName = path;
	}


	public void doAnalysis() {
		doAnalysisDirectory(pathName, packName);
	}


	public void doAnalysisDirectory(String pathName, String packName) {
		try {
			File pathFileName = new File(pathName);
			String[] fileNames = pathFileName.list();
			
			for (int i = 0; i < fileNames.length; i++) {
				File tf = new File(pathFileName.getPath(), fileNames[i]);			
				if (tf.isDirectory()) {		
					String subDirName = packName + "." + tf.getName();
					doAnalysisDirectory(tf.getCanonicalPath(), subDirName );
				} else if (tf.getName().indexOf("class") >= 0) {
					String className = packName + "." + tf.getName();
					System.out.println("analyze " + className.substring(0, className.lastIndexOf(".")));
					doClassAnalysis(className.substring(0, className.lastIndexOf(".")));
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void doClassAnalysis(String tstClassName) throws IOException, JDOMException{
		SootClass sClass = Scene.v().loadClassAndSupport(tstClassName);
		sClass.setApplicationClass();
		Iterator<SootMethod> methodIt = sClass.getMethods().iterator();
		
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
							String strIe = v.toString();
							String strName = strIe.substring(
									strIe.indexOf("<") + 1, strIe
											.lastIndexOf(">"));
							if (packName.equals(strName.substring(0, strName
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
					j = graph.getPredsOf(unit).iterator();
					while (j.hasNext()) {
						Unit xx = (Unit) j.next();
						int tmpLineNumber = ((LineNumberTag) xx
								.getTag("LineNumberTag")).getLineNumber();
						if (tmpLineNumber != lineNumber)
							node.pres.add(tmpLineNumber);
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
									node.pres.add(lineNumber);
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
						Element xmlStmt = new Element("statement");
						xmlStmt.setAttribute("line", Integer.toString(tmpLine));

						List<String> tmpList = cfg.get(tmpLine).callee;
						k = tmpList.iterator();
						while (k.hasNext()) {
							Element xmlCallee = new Element("callee");
							xmlCallee.setText(k.next());
							xmlStmt.addContent(xmlCallee);
						}

						Set<Integer> tmpSet = cfg.get(tmpLine).pres;
						l = tmpSet.iterator();
						while(l.hasNext()){
							Element xmlPres = new Element("predecessor");
							xmlPres.setText(l.next().toString());
							xmlStmt.addContent(xmlPres);
						}
											
						tmpSet = cfg.get(tmpLine).succs;
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
		new XMLOutputter().output(xmlDoc, new FileOutputStream(new File(DirAndFileConstant.CFG+File.separator+tstClassName+".xml")));
	}
	
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("-p packname");
			System.out.println("-c classname without .class");
			System.exit(0);
		}
		
		if(args[0].equals("-p")){
			System.out.println("Start analyze package test --------- ");

			String pack;
			int i = args[1].lastIndexOf(File.separator);
			if (i == -1)
				pack = args[1];
			else
				pack = args[1].substring(i + 1, args[1].length());
			Pack doCFG = new Pack(args[1], pack);
			doCFG.doAnalysis();
			System.out.println("Over --------- ");
		}
		else if(args[0].equals("-c")){
			Pack doCFG = new Pack(args[1].substring(0, args[1].indexOf(".")));
			try {
				doCFG.doClassAnalysis(args[1]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JDOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
	}
}
