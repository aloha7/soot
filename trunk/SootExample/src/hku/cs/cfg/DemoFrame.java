package hku.cs.cfg;


import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import att.grappa.Edge;
import att.grappa.Graph;
import att.grappa.GrappaPanel;
import att.grappa.GrappaSupport;
import att.grappa.Node;
import att.grappa.Subgraph;

class DemoFrame extends JFrame implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5276304238065153942L;

	public static JScrollPane jsp = new JScrollPane();;	
	public static HashMap<String, GrappaPanel> graphSet = new HashMap<String, GrappaPanel>();
	public static String className;
	
	public static List<String> backList = new ArrayList<String>();
	
	JPanel panel = null;
	JButton layout = null;
	JButton reset = null;
	JButton back = null;
	
	public static void main(String[] args) {
		DemoFrame df = new DemoFrame();
		try {
			df.load("tester.TestClass");
			DemoFrame.className= "tester.TestClass";
			DemoFrame.gpLayout();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		df.setVisible(true);
	}

	public DemoFrame() {
		super("DemoFrame");
		setSize(600, 400);
		setLocation(100, 100);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent wev) {
				Window w = wev.getWindow();
				w.setVisible(false);
				w.dispose();
				System.exit(0);
			}
		});

		jsp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
		jsp.getViewport().setBackground(Color.WHITE);
		GridBagConstraints gbc = new GridBagConstraints();

		GridBagLayout gbl = new GridBagLayout();

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;

		panel = new JPanel();
		panel.setLayout(gbl);

		layout = new JButton("Layout");
		gbl.setConstraints(layout, gbc);
		panel.add(layout);
		layout.addActionListener(this);
		
		reset = new JButton("Reset");
		gbl.setConstraints(reset, gbc);
		panel.add(reset);
		reset.addActionListener(this);
		
		back = new JButton("Back");
		gbl.setConstraints(back, gbc);
		panel.add(back);
		back.addActionListener(this);
		
		getContentPane().add("Center", jsp);
		getContentPane().add("West", panel);
		setVisible(true);
	}
	
	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() instanceof JButton) {
			JButton tgt = (JButton) evt.getSource();
			if (tgt == layout) {
				DemoFrame.gpLayout();
			}else if(tgt ==  reset){
				DemoFrame.reset();
			}else if(tgt == back){
				if(DemoFrame.backList.size() == 0)
					return;
				DemoFrame.className = DemoFrame.backList.get(DemoFrame.backList.size()-1);
				DemoFrame.backList.remove(DemoFrame.backList.size()-1);
				DemoFrame.gpLayout();
			}
		}
		
	}
		


	public static void load(String classname) throws JDOMException, IOException {
		Node[] nd = { null, null };

		SAXBuilder sb = new SAXBuilder();
		Document xmlDoc = sb.build("output"+File.separator+"cfg"+File.separator+"data"+File.separator+classname+".xml");

		Element xmlRoot = xmlDoc.getRootElement();

		
		Graph graph = new Graph(xmlRoot.getAttributeValue("name")/* , true, false */);
		graph.setMenuable(true);
		
		graph.setAttribute("label", xmlRoot.getAttributeValue("name"));

		graph.setAttribute("rankdir", "LB");
		graph.setAttribute("fontstyle", "bold");
		graph.setAttribute("fontsize", "24");
		graph.setAttribute("font", "Helvetica");

		graph.setNodeAttribute("shape", "ellipse");
		graph.setNodeAttribute("style", "filled");
		graph.setNodeAttribute("color", "beige");
		graph.setNodeAttribute("tip", "A Node");

		graph.setEdgeAttribute("color", "darkgreen");
		graph.setEdgeAttribute("tip", "An Edge");

		Iterator xmlMethodIt = xmlRoot.getChildren("method").iterator();
		int i=0;
		while (xmlMethodIt.hasNext()) {
			Element xmlMethod = (Element) xmlMethodIt.next();
			Subgraph subGraph = new Subgraph(graph, "cluster"+Integer.toString(i));
			i++;
			
			subGraph.setAttribute("label", xmlMethod.getAttributeValue("name"));
			subGraph.setAttribute("fontsize", "10");
			subGraph.setAttribute("margin", "2 2");
			
			Iterator xmlStmtIt = xmlMethod.getChildren("statement").iterator();
			while (xmlStmtIt.hasNext()) {
				Element xmlStmt = (Element) xmlStmtIt.next();
				String sourceNodeName = "Line"+xmlStmt.getAttributeValue("line");
				
				if ((nd[0] = subGraph.findNodeByName(sourceNodeName)) == null) {
					nd[0] = new Node(subGraph, sourceNodeName);
				}
				
	
				List<Element> xmlCalleeList = xmlStmt.getChildren("callee");
				if (null != xmlCalleeList) {
					if (xmlCalleeList.size() > 0)
						nd[0].setAttribute("color", "blue");
					Iterator xmlCalleeIt = xmlCalleeList.iterator();
					String tip = "";
					while (xmlCalleeIt.hasNext()) {
						tip += ((Element) xmlCalleeIt.next()).getText()+"# ";
					}
					nd[0].setAttribute("tip", tip);
				}
				
				List<Element> xmlSuccsList = xmlStmt.getChildren("successor");			
				if(xmlSuccsList.size()>1)
					nd[0].setAttribute("shape", "diamond");
				Iterator xmlSuccsIt = xmlSuccsList.iterator();
				while (xmlSuccsIt.hasNext()) {
					String destNodeName = "Line"+((Element) xmlSuccsIt.next())
							.getText();
					if ((nd[1] = subGraph.findNodeByName(destNodeName)) == null) {
						nd[1] = new Node(subGraph, destNodeName);
					}
					new Edge(subGraph, nd[0], nd[1], sourceNodeName + "->"
							+ destNodeName);
				}
			}
		}
		GrappaPanel gp = new GrappaPanel(graph);
		gp.addGrappaListener(new MyAdapter());
		gp.setScaleToFit(false);
		DemoFrame.graphSet.put(classname, gp);
//		DemoFrame.backList.add(classname);
		graph.printGraph(System.out);
	}
	
	public static void reset(){
		GrappaPanel gp = DemoFrame.graphSet.get(DemoFrame.className);
		Graph graph = (Graph)gp.getSubgraph();
		Enumeration gem = graph.elements();
		while(gem.hasMoreElements()){
			att.grappa.Element el = (att.grappa.Element)gem.nextElement();
			if(el.getName().indexOf("callee")>=0){
				el.delete();
			}
		}
		DemoFrame.gpLayout();		
	}
	
	public static void gpLayout(){
		GrappaPanel gp = DemoFrame.graphSet.get(DemoFrame.className);
		Graph graph = (Graph)gp.getSubgraph();
		Object connector = null;
		try {
			connector = Runtime.getRuntime()
					.exec("/home/xj/workspace/demostrator/formatDot");
		} catch (Exception ex) {
			System.err
					.println("Exception while setting up Process: "
							+ ex.getMessage()
							+ "\nTrying URLConnection...");
			connector = null;
		}
		if (connector == null) {
			try {
				connector = (new URL(
						"http://www.research.att.com/~john/cgi-bin/format-graph"))
						.openConnection();
				URLConnection urlConn = (URLConnection) connector;
				urlConn.setDoInput(true);
				urlConn.setDoOutput(true);
				urlConn.setUseCaches(false);
				urlConn.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
			} catch (Exception ex) {
				System.err
						.println("Exception while setting up URLConnection: "
								+ ex.getMessage()
								+ "\nLayout not performed.");
				connector = null;
			}
		}
		if (connector != null) {
			if (!GrappaSupport.filterGraph(graph, connector)) {
				System.err
						.println("ERROR: somewhere in filterGraph");
			}
			if (connector instanceof Process) {
				try {
					int code = ((Process) connector).waitFor();
					if (code != 0) {
						System.err
								.println("WARNING: proc exit code is: "
										+ code);
					}
				} catch (InterruptedException ex) {
					System.err
							.println("Exception while closing down proc: "
									+ ex.getMessage());
					ex.printStackTrace(System.err);
				}
			}
			connector = null;
		}
//		graph.repaint();
		jsp.setViewportView(gp);
	}
	
}