package hku.cs.cfg;


import java.awt.event.InputEvent;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import org.jdom.JDOMException;

import att.grappa.Edge;
import att.grappa.Element;
import att.grappa.GrappaAdapter;
import att.grappa.GrappaPanel;
import att.grappa.GrappaPoint;
import att.grappa.Node;
import att.grappa.Subgraph;

public class MyAdapter extends GrappaAdapter {
	public void grappaClicked(Subgraph subg, Element elem, GrappaPoint pt,
			int modifiers, int clickCount, GrappaPanel panel) {

		if (clickCount == 1) {
			// looks like Java has a single click occur on the way to a
			// multiple click, so this code always executes (which is
			// not necessarily a bad thing)
			if (subg.getGraph().isSelectable()) {
				if (modifiers == InputEvent.BUTTON1_MASK) {
					// select element
					if (elem == null) {
						if (subg.currentSelection != null) {
							if (subg.currentSelection instanceof Element) {
								((Element) (subg.currentSelection)).highlight &= ~HIGHLIGHT_MASK;
							} else {
								Vector vec = ((Vector) (subg.currentSelection));
								for (int i = 0; i < vec.size(); i++) {
									((Element) (vec.elementAt(i))).highlight &= ~HIGHLIGHT_MASK;
								}
							}
							subg.currentSelection = null;
							subg.getGraph().repaint();
						}
					} else {
						if (subg.currentSelection != null) {
							if (subg.currentSelection == elem)
								return;
							if (subg.currentSelection instanceof Element) {
								((Element) (subg.currentSelection)).highlight &= ~HIGHLIGHT_MASK;
							} else {
								Vector vec = ((Vector) (subg.currentSelection));
								for (int i = 0; i < vec.size(); i++) {
									((Element) (vec.elementAt(i))).highlight &= ~HIGHLIGHT_MASK;
								}
							}
							subg.currentSelection = null;
						}
						elem.highlight |= SELECTION_MASK;
						subg.currentSelection = elem;
						if (subg.currentSelection instanceof Node) {
							Node src = (Node) (subg.currentSelection);
							Subgraph srcSubgraph = src.getSubgraph();
							String tips = src.getAttribute("tip")
									.getStringValue();
							System.out.println(tips);
							String srcName = src.getName();

							if (tips.indexOf("#") >= 0) {
								if (!src.hasTag("open")) {
									String[] tip = tips.split("#");
									for (int i = 0; i < tip.length; i++) {
										String tmpName = srcName + "callee"
												+ Integer.toString(i);
										Node n = new Node(srcSubgraph, tmpName);
										n.setAttribute("tip", tip[i]);
										n.setAttribute("color", "green");
										n.setAttribute("shape", "square");
										new Edge(srcSubgraph, src, n, srcName
												+ "->callee"
												+ Integer.toString(i));
									}
									src.addTag("open");									
									DemoFrame.gpLayout();
								}
								else{									
									Enumeration a =  src.outEdgeElements();
									while(a.hasMoreElements()){
										Edge e = (Edge)a.nextElement();
										if(e.getName().indexOf("callee") >= 0){
											e.getHead().delete();
											e.delete();
										}			
									}
									src.removeTag("open");
								}
							}
							
							String isCallee = src.getAttribute("shape").getStringValue();
							if(isCallee.equals("box")){
								String strTip = src.getAttribute("tip").getStringValue();
								String className = strTip.substring(0, strTip.indexOf(":"));
								GrappaPanel ag = DemoFrame.graphSet.get(className);
								if(ag == null){
									try {
										DemoFrame.load(className);
										DemoFrame.backList.add(DemoFrame.className);
										DemoFrame.className = className;
									} catch (JDOMException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}else{		
									DemoFrame.backList.add(DemoFrame.className);
									DemoFrame.className = className;
									DemoFrame.gpLayout();								
								}
								
								
								
								
									
							}
							subg.getGraph().printGraph(System.out);
						}
						subg.getGraph().repaint();
					}
				} else if (modifiers == (InputEvent.BUTTON1_MASK | InputEvent.CTRL_MASK)) {
					// adjust selection
					if (elem != null) {
						if ((elem.highlight & SELECTION_MASK) == SELECTION_MASK) {
							// unselect element
							elem.highlight &= ~SELECTION_MASK;
							if (subg.currentSelection == null) {
								// something got messed up somewhere
								throw new InternalError(
										"currentSelection improperly maintained");
							} else if (subg.currentSelection instanceof Element) {
								if (((Element) (subg.currentSelection)) != elem) {
									// something got messed up somewhere
									throw new InternalError(
											"currentSelection improperly maintained");
								}
								subg.currentSelection = null;
							} else {
								Vector vec = ((Vector) (subg.currentSelection));
								boolean problem = true;
								for (int i = 0; i < vec.size(); i++) {
									if (((Element) (vec.elementAt(i))) == elem) {
										vec.removeElementAt(i);
										problem = false;
										break;
									}
								}
								if (problem) {
									// something got messed up somewhere
									throw new InternalError(
											"currentSelection improperly maintained");
								}
							}
						} else {
							// select element
							elem.highlight |= SELECTION_MASK;
							if (subg.currentSelection == null) {
								subg.currentSelection = elem;
							} else if (subg.currentSelection instanceof Element) {
								Object obj = subg.currentSelection;
								subg.currentSelection = new Vector();
								((Vector) (subg.currentSelection)).add(obj);
								((Vector) (subg.currentSelection)).add(elem);
							} else {
								((Vector) (subg.currentSelection)).add(elem);
							}
						}
						subg.getGraph().repaint();
					}
				}
			}
		}

	}
}
