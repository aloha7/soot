package hku.cs.profile;

import hku.cs.constant.DirAndFileConstant;
import hku.cs.tools.DirTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NullConstant;
import soot.options.Options;
import soot.tagkit.LineNumberTag;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.ExceptionalBlockGraph;

public class Check {

	private class HelpBlkNode {
		int presNull;
		int succsNull;
		int presNNT = 0;
		int succsNNT = 0;

		public HelpBlkNode(int p, int s) {
			presNull = p;
			succsNull = s;
		}
	}

	HashMap<String, HashMap<Integer, Integer>> blkCnt;
	HashMap<String, HashMap<Integer, LineNode>> result = new HashMap<String, HashMap<Integer, LineNode>>();

	public Check() {
		Scene.v().loadBasicClasses();
		Options.v().set_keep_line_number(true);
		Options.v().setPhaseOption("jb", "use-original-names:true");
	}

	public void analyze(String className) {
		SootClass sClass = Scene.v().loadClassAndSupport(className);
		Iterator<SootMethod> methodIt = sClass.getMethods().iterator();
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod) methodIt.next();
			if (m.isAbstract())
				continue;
			Body b = m.retrieveActiveBody();// it will error when the class is
			// abstrract
			ExceptionalBlockGraph bGraph = new ExceptionalBlockGraph(b);

			String methodName = m.toString();
			methodName = methodName.substring(methodName.indexOf(":") + 2,
					methodName.length() - 1);
			HashMap<Integer, HelpBlkNode> inHelpBlk = new HashMap<Integer, HelpBlkNode>();
			HashMap<Integer, Integer> inBlkCnt = blkCnt.get(methodName);
			HashMap<Integer, BlkNode> inResultBlk = new HashMap<Integer, BlkNode>();
			HashMap<Integer, LineNode> inResultLine = new HashMap<Integer, LineNode>();
			// initialize
			for (Iterator<Block> blockIt = bGraph.getBlocks().iterator(); blockIt
					.hasNext();) {
				Block block = blockIt.next();
				int blockIdx = block.getIndexInMethod();
				Integer blockTimes;
				if (inBlkCnt == null)
					blockTimes = 0;
				else {
					blockTimes = inBlkCnt.get(blockIdx);
					if (blockTimes == null)
						blockTimes = 0;
				}
				// ///////////////////////
				HashMap<Integer, Integer> inPres = new HashMap<Integer, Integer>();
				int presNull = 0;

				for (Iterator<Block> blockPreIt = block.getPreds().iterator(); blockPreIt
						.hasNext();) {
					Block blockPre = blockPreIt.next();
					int idx = blockPre.getIndexInMethod();
					inPres.put(idx, -1);
					presNull++;
				}

				HashMap<Integer, Integer> inSuccs = new HashMap<Integer, Integer>();
				int succsNull = 0;

				for (Iterator<Block> blockSuccsIt = block.getSuccs().iterator(); blockSuccsIt
						.hasNext();) {
					Block blockSucc = blockSuccsIt.next();
					int idx = blockSucc.getIndexInMethod();
					inSuccs.put(idx, -1);
					succsNull++;
				}

				inResultBlk.put(blockIdx, new BlkNode(inPres, inSuccs,
						blockTimes));
				inHelpBlk.put(blockIdx, new HelpBlkNode(presNull, succsNull));
			}

			// ////////////////////////////////////////////////////////////////////

			computBlkInOut(inHelpBlk, inResultBlk);

			computLineAndPredicate(bGraph, inResultBlk, inResultLine);

			result.put(m.toString(), inResultLine);
		}
		// System.out.println(result.toString());
	}

	public void computLineAndPredicate(ExceptionalBlockGraph bGraph,
			HashMap<Integer, BlkNode> inResultBlk,
			HashMap<Integer, LineNode> inResultLine) {
		for (Iterator<Block> blockIt = bGraph.getBlocks().iterator(); blockIt
				.hasNext();) {
			Block block = blockIt.next();
			int blockIdx = block.getIndexInMethod();

			int blockTimes = inResultBlk.get(blockIdx).times;

			Iterator<Unit> uIt = bGraph.getBody().getUnits().iterator(
					block.getHead(), block.getTail());
			while (uIt.hasNext()) {
				Unit u = uIt.next();
				int lnt = ((LineNumberTag) u.getTag("LineNumberTag"))
						.getLineNumber();
				LineNode n = inResultLine.get(lnt);
				if (n == null)
					n = new LineNode();
				n.setMaxTimes(blockTimes);
				n.setMinTimes(blockTimes);

				BlkLstStmt bls = null;

				if (u.branches()) {
					if (u instanceof GotoStmt) {
						bls = new BlkGotoStmt(blockTimes,
								((LineNumberTag) ((GotoStmt) u).getTarget()
										.getTag("LineNumberTag"))
										.getLineNumber());
					} else if (u instanceof IfStmt) {
						IfStmt is = (IfStmt) u;
						BlkIfStmt bis = new BlkIfStmt();

						List<Block> tmpB = block.getSuccs();
						Block blockSucc = tmpB.get(0);
						int idx = blockSucc.getIndexInMethod();
						if (blockSucc.getHead().equals(is.getTarget())) {
							bis.setTru(new Condition(is.getCondition(),
									inResultBlk.get(blockIdx).succs.get(idx),
									((LineNumberTag) (blockSucc.getHead()
											.getTag("LineNumberTag")))
											.getLineNumber()));
							bis.setFal(new Condition(is.getCondition(),
									inResultBlk.get(blockIdx).succs.get(tmpB
											.get(1).getIndexInMethod()),
									((LineNumberTag) (tmpB.get(1).getHead()
											.getTag("LineNumberTag")))
											.getLineNumber()));
						} else {
							bis.setFal(new Condition(is.getCondition(),
									inResultBlk.get(blockIdx).succs.get(idx),
									((LineNumberTag) (blockSucc.getHead()
											.getTag("LineNumberTag")))
											.getLineNumber()));
							bis.setTru(new Condition(is.getCondition(),
									inResultBlk.get(blockIdx).succs.get(tmpB
											.get(1).getIndexInMethod()),
									((LineNumberTag) (tmpB.get(1).getHead()
											.getTag("LineNumberTag")))
											.getLineNumber()));
						}
						bls = bis;

					} else if (u instanceof LookupSwitchStmt) {
						LookupSwitchStmt ss = (LookupSwitchStmt) u;
						BlkSwitchStmt bss = new BlkSwitchStmt();
						int caseIdx = 0;

						List<Block> lSuc = block.getSuccs();
						for (Iterator caseIt = ss.getLookupValues().iterator(); caseIt
								.hasNext();) {
							IntConstant l = (IntConstant) caseIt.next();
							for (Iterator<Block> blockSuccsIt = lSuc.iterator(); blockSuccsIt
									.hasNext();) {

								Block blockSucc = blockSuccsIt.next();
								Unit target = ss.getTarget(caseIdx);
								if (blockSucc.getHead().equals(target)) {
									bss.caseList
											.add(new Condition(
													l,
													inResultBlk.get(blockIdx).succs
															.get(blockSucc
																	.getIndexInMethod()),
													((LineNumberTag) (blockSucc
															.getHead()
															.getTag("LineNumberTag")))
															.getLineNumber()));
									break;
								}
							}
							caseIdx++;
						}

						Unit target = ss.getDefaultTarget();
						if (target != null) {
							for (Iterator<Block> blockSuccsIt = lSuc.iterator(); blockSuccsIt
									.hasNext();) {
								Block blockSucc = blockSuccsIt.next();

								if (blockSucc.getHead().equals(target)) {
									bss.defalt = new Condition(
											NullConstant.v(),
											inResultBlk.get(blockIdx).succs
													.get(blockSucc
															.getIndexInMethod()),
											((LineNumberTag) (blockSucc
													.getHead()
													.getTag("LineNumberTag")))
													.getLineNumber());
								}
							}
						} else
							throw new RuntimeException("This access failed");

						bls = bss;
					}
				}
				if (bls != null)
					n.predicates.add(bls);
				inResultLine.put(lnt, n);
			}
		}
	}

	public void computBlkInOut(HashMap<Integer, HelpBlkNode> inHelpBlk,
			HashMap<Integer, BlkNode> inResultBlk) {
		int hasNotDealt = inResultBlk.size();
		if (hasNotDealt <= 1) {
			return;
		}

		boolean goon = false;
		while (hasNotDealt > 0) {

			if (goon) {
				goon = false;
			} else
				System.out.println("some wait");

			for (Iterator<Integer> k = inHelpBlk.keySet().iterator(); k
					.hasNext();) {
				int blockIdx = k.next();
				HelpBlkNode h = inHelpBlk.get(blockIdx);

				if (h.presNull != 0 || h.succsNull != 0) {
					BlkNode n = inResultBlk.get(blockIdx);

					if (h.presNull == 1) {
						int sum = 0;
						int findTarget = -1;
						for (Iterator<Integer> i = n.pres.keySet().iterator(); i
								.hasNext();) {
							Integer key = i.next();
							if (n.pres.get(key) == -1) {
								findTarget = key;
							} else
								sum += n.pres.get(key);
						}
						n.pres.put(findTarget, n.times - sum);
						h.presNull = 0;

						goon = true;

					} else if (h.presNull > 1) {
						if (h.presNNT == n.times) {
							for (Iterator<Integer> i = n.pres.keySet()
									.iterator(); i.hasNext();) {
								Integer key = i.next();
								if (n.pres.get(key) == -1) {
									n.pres.put(key, 0);
								}
							}
							h.presNull = 0;

							goon = true;

						} else {
							for (Iterator<Integer> i = n.pres.keySet()
									.iterator(); i.hasNext();) {
								Integer key = i.next();
								if (n.pres.get(key) == -1) {
									int tmp = inResultBlk.get(key).succs
											.get(blockIdx);
									if (tmp != -1) {
										n.pres.put(key, tmp);
										h.presNull--;
										h.presNNT += tmp;

										if (h.presNNT > n.times)
											System.out.print("error");

										goon = true;

									}
								}
							}
						}
					}

					if (h.succsNull == 1) {
						int sum = 0;
						int findTarget = -1;
						for (Iterator<Integer> i = n.succs.keySet().iterator(); i
								.hasNext();) {
							Integer key = i.next();
							if (n.succs.get(key) == -1) {
								findTarget = key;
							} else
								sum += n.succs.get(key);
						}
						n.succs.put(findTarget, n.times - sum);
						h.succsNull = 0;

						goon = true;

					} else if (h.succsNull > 1) {
						if (h.succsNNT == n.times) {
							for (Iterator<Integer> i = n.succs.keySet()
									.iterator(); i.hasNext();) {
								Integer key = i.next();
								if (n.succs.get(key) == -1) {
									n.succs.put(key, 0);
								}
							}
							h.succsNull = 0;

							goon = true;

						} else {
							for (Iterator<Integer> i = n.succs.keySet()
									.iterator(); i.hasNext();) {
								Integer key = i.next();
								if (n.succs.get(key) == -1) {
									int tmp = inResultBlk.get(key).pres
											.get(blockIdx);
									if (tmp != -1) {
										n.succs.put(key, tmp);
										h.succsNull--;
										h.succsNNT += tmp;

										if (h.succsNNT > n.times)
											System.out.print("error");

										goon = true;

									}
								}
							}
						}
					}

					if (h.presNull == 0 && h.succsNull == 0) {
						hasNotDealt--;
						goon = true;
					}

					inResultBlk.put(blockIdx, n);
					inHelpBlk.put(blockIdx, h);
				}
			}
		}
	}

	public void toSimpleXML(String className, File outputDir)
			throws FileNotFoundException, IOException {
		Element xmlRoot = new Element("class");
		xmlRoot.setAttribute("name", className);
		Document xmlDoc = new Document(xmlRoot);

		for (String mk : result.keySet()) {
			HashMap<Integer, LineNode> inResultLine = result.get(mk);

			Element xmlMethod = new Element("method");
			xmlMethod.setAttribute("name", mk.substring(mk.indexOf(":") + 2, mk
					.length() - 1));

			for (Integer i = 0, num = 0; num < inResultLine.size(); i++) {
				if (inResultLine.get(i) != null) {
					num++;
					Element xmlLine = new Element("statement");
					xmlLine.setAttribute("line", i.toString());

					LineNode ln = inResultLine.get(i);

					xmlLine.setAttribute("maxtimes", Integer
							.toString(ln.maxTimes));

					List<BlkLstStmt> predicats = ln.predicates;
					if (predicats != null) {
						for (int pi = 0; pi < predicats.size(); pi++) {
							BlkLstStmt bls = predicats.get(pi);
							if (bls instanceof BlkIfStmt) {
								Element xmlIf = new Element("if");
								Condition tru = ((BlkIfStmt) bls).getTru();
								Condition fal = ((BlkIfStmt) bls).getFal();
								xmlIf.setAttribute("condition", tru.cond
										.toString());

								Element xmlTrue = new Element("true");
								xmlTrue.setAttribute("times", Integer
										.toString(tru.times));
								xmlTrue.setAttribute("target", Integer
										.toString(tru.target));
								xmlIf.addContent(xmlTrue);

								Element xmlFalse = new Element("false");
								xmlFalse.setAttribute("times", Integer
										.toString(fal.times));
								xmlFalse.setAttribute("target", Integer
										.toString(fal.target));
								xmlIf.addContent(xmlFalse);

								xmlLine.addContent(xmlIf);
							} else if (bls instanceof BlkSwitchStmt) {
								Element xmlSwitch = new Element("switch");
								List<Condition> caseList = ((BlkSwitchStmt) bls).caseList;

								for (int ci = 0; ci < caseList.size(); ci++) {
									Condition cse = caseList.get(ci);
									Element xmlCase = new Element("case");
									xmlCase.setAttribute("value", cse.cond
											.toString());
									xmlCase.setAttribute("times", Integer
											.toString(cse.times));
									xmlCase.setAttribute("target", Integer
											.toString(cse.target));
									xmlSwitch.addContent(xmlCase);
								}
								Condition def = ((BlkSwitchStmt) bls).defalt;
								if (def != null) {
									Element xmlDefault = new Element("default");
									xmlDefault.setAttribute("times", Integer
											.toString(def.times));
									xmlDefault.setAttribute("target", Integer
											.toString(def.target));
									xmlSwitch.addContent(xmlDefault);
								}
							}
						}
					}

					xmlMethod.addContent(xmlLine);
				}
			}
			xmlRoot.addContent(xmlMethod);
		}

		new XMLOutputter().output(xmlDoc, new FileOutputStream(outputDir
				.getCanonicalPath()
				+ File.separator + className + ".xml"));

		result.clear();
	}

	public void toXML(String className, File outputDir)
			throws FileNotFoundException, IOException {
		Element xmlRoot = new Element("class");
		xmlRoot.setAttribute("name", className);
		Document xmlDoc = new Document(xmlRoot);

		for (String mk : result.keySet()) {
			HashMap<Integer, LineNode> inResultLine = result.get(mk);
			Element xmlMethod = new Element("method");
			xmlMethod.setAttribute("name", mk.substring(mk.indexOf(":") + 2, mk
					.length() - 1));

			for (Integer i : inResultLine.keySet()) {
				Element xmlLine = new Element("statement");
				xmlLine.setAttribute("line", i.toString());

				LineNode ln = inResultLine.get(i);
				xmlLine.setAttribute("maxtimes", Integer.toString(ln.maxTimes));
				xmlLine.setAttribute("mintimes", Integer.toString(ln.minTimes));

				List<BlkLstStmt> predicats = ln.predicates;
				if (predicats != null) {
					for (int pi = 0; pi < predicats.size(); pi++) {
						BlkLstStmt bls = predicats.get(pi);
						if (bls instanceof BlkGotoStmt) {
							Element xmlGoto = new Element("goto");
							xmlGoto.setAttribute("times", Integer
									.toString(((BlkGotoStmt) bls).times));
							xmlGoto.setAttribute("target", Integer
									.toString(((BlkGotoStmt) bls).target));
							xmlLine.addContent(xmlGoto);
						} else if (bls instanceof BlkIfStmt) {
							Element xmlIf = new Element("if");
							Condition tru = ((BlkIfStmt) bls).getTru();
							Condition fal = ((BlkIfStmt) bls).getFal();
							xmlIf
									.setAttribute("condition", tru.cond
											.toString());

							Element xmlTrue = new Element("true");
							xmlTrue.setAttribute("times", Integer
									.toString(tru.times));
							xmlTrue.setAttribute("target", Integer
									.toString(tru.target));
							xmlIf.addContent(xmlTrue);

							Element xmlFalse = new Element("false");
							xmlFalse.setAttribute("times", Integer
									.toString(fal.times));
							xmlFalse.setAttribute("target", Integer
									.toString(fal.target));
							xmlIf.addContent(xmlFalse);

							xmlLine.addContent(xmlIf);
						} else if (bls instanceof BlkSwitchStmt) {
							Element xmlSwitch = new Element("switch");
							List<Condition> caseList = ((BlkSwitchStmt) bls).caseList;

							for (int ci = 0; ci < caseList.size(); ci++) {
								Condition cse = caseList.get(ci);
								Element xmlCase = new Element("case");
								xmlCase.setAttribute("value", cse.cond
										.toString());
								xmlCase.setAttribute("times", Integer
										.toString(cse.times));
								xmlCase.setAttribute("target", Integer
										.toString(cse.target));
								xmlSwitch.addContent(xmlCase);
							}
							Condition def = ((BlkSwitchStmt) bls).defalt;
							if (def != null) {
								Element xmlDefault = new Element("default");
								xmlDefault.setAttribute("times", Integer
										.toString(def.times));
								xmlDefault.setAttribute("target", Integer
										.toString(def.target));
								xmlSwitch.addContent(xmlDefault);
							}
						}
					}
				}
				xmlMethod.addContent(xmlLine);
			}
			xmlRoot.addContent(xmlMethod);
		}

		new XMLOutputter().output(xmlDoc, new FileOutputStream(new File(
				outputDir, className + ".xml")));

		result.clear();
	}

	public void readData(String className, File inputDir) throws JDOMException,
			IOException {
		HashMap<String, HashMap<Integer, Integer>> data = new HashMap<String, HashMap<Integer, Integer>>();

		SAXBuilder sb = new SAXBuilder();

		Document xmlDoc = sb.build(new File(inputDir, className + ".xml"));
		Element xmlRoot = xmlDoc.getRootElement();

		Iterator xmlMethodIt = xmlRoot.getChildren("method").iterator();

		while (xmlMethodIt.hasNext()) {
			Element xmlMethod = (Element) xmlMethodIt.next();
			String methodName = xmlMethod.getAttributeValue("name");
			Iterator xmlBlockIt = xmlMethod.getChildren("block").iterator();
			HashMap<Integer, Integer> bl = new HashMap<Integer, Integer>();
			while (xmlBlockIt.hasNext()) {
				Element xmlBlock = (Element) xmlBlockIt.next();
				int index = Integer.parseInt(xmlBlock
						.getAttributeValue("index"));
				int times = Integer.parseInt(xmlBlock
						.getAttributeValue("times"));
				bl.put(index, times);
			}
			data.put(methodName, bl);
		}
		blkCnt = data;
	}

	public static void main(String args[]) {

		Check c = new Check();

		String outputDir;
		String inputDir;

		if (args == null || args.length == 0) {
			inputDir = DirAndFileConstant.BLOCK;
			outputDir = DirAndFileConstant.COVERAGE;
		} else if (args.length != 1) {
			System.out
					.println("use java Check Process-dir Output-dir[default 'coverage']");
			return;
		} else {
			inputDir = args[0] + DirAndFileConstant.BLOCK+File.separator;
			outputDir = args[0] + DirAndFileConstant.COVERAGE+File.separator;
		}

		c.doDir(new File(inputDir), DirTools.prepare(outputDir));
	}

	public void doDir(File inDir, File outDir) {

		try {
			File pathFileName = inDir;
			String[] fileNames = pathFileName.list();

			if (fileNames == null)
				return;

			for (int i = 0; i < fileNames.length; i++) {
				File tf = new File(pathFileName, fileNames[i]);
				if (tf.isFile() && tf.getName().indexOf("xml") >= 0) {
					String className = tf.getName().substring(0,
							tf.getName().lastIndexOf("."));
					readData(className, inDir);
					analyze(className);
					toXML(className, outDir);
				} else if (tf.isDirectory()) {
					doDir(tf, new File(outDir, tf.getName()));
				}
			}

		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}