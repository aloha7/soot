package hku.cs.rank;

import hku.cs.constant.DirAndFileConstant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;



public class ComputeRank {
	private Statics statics = new Statics();

	
	private String rankMethName;

	public ComputeRank() {
		rankMethName = RankAlgorithm.TARANTULA;
	}

	public ComputeRank(String rankMethName) {
		this.rankMethName = rankMethName;
	}

	public void readData(String mainClass, File inDir, boolean testB) throws JDOMException,
			IOException {
		
		SAXBuilder sb = new SAXBuilder();
		File tf = new File(inDir, mainClass+".xml");
		if (tf.isFile() && tf.getName().indexOf("xml") > 0) {
			Document xmlDoc = sb.build(tf);
			Element xmlRoot = (Element) xmlDoc.getRootElement();
			String className = xmlRoot.getAttributeValue("name");
			
			Iterator xmlMethodIt = xmlRoot.getChildren("method")
					.iterator();
			while (xmlMethodIt.hasNext()) {
				Element xmlMethod = (Element) xmlMethodIt.next();
				Iterator xmlStmtIt = xmlMethod.getChildren("statement")
						.iterator();
				while (xmlStmtIt.hasNext()) {
					Element xmlStmt = (Element) xmlStmtIt.next();

					int times = Integer.parseInt(xmlStmt
							.getAttributeValue("maxtimes"));
					int line = Integer.parseInt(xmlStmt
							.getAttributeValue("line"));
					if (times > 0) {
						statics.add(className, line, testB);
					}
				}
			}
		}
	}


	public void toXML(List<Result> result, File outDir, List<String> args, String msd) throws FileNotFoundException, IOException {
		Element xmlRoot = new Element(this.rankMethName);
		Document xmlDoc = new Document(xmlRoot);

		for (int i = 0; i < result.size(); i++) {
			Result r = result.get(i);
			Element xmlStmt = new Element("statement");
			xmlStmt.setAttribute("index", Integer.toString(i+1));
			xmlStmt.setAttribute("class", r.className);
			xmlStmt.setAttribute("line", Integer.toString(r.line));

			Element xmlRank = new Element("rank");
			xmlRank.setText(Integer.toString(r.rank));
			xmlStmt.addContent(xmlRank);
			Element xmlSus = new Element("suspiciousness");
			xmlSus.setText(Float.toString(r.suspiciousness));
			xmlStmt.addContent(xmlSus);
			
			if (this.rankMethName.equals(RankAlgorithm.TARANTULA)) {
				Element xmlConf = new Element("confidence");
				xmlConf.setText(Float.toString(r.confidence));
				xmlStmt.addContent(xmlConf);

			}

			xmlRoot.addContent(xmlStmt);
		}

		StringBuffer sb = new StringBuffer();
		sb.append(DirAndFileConstant.RANK);
		sb.append(DirAndFileConstant.LB);
		sb.append(this.rankMethName);
		sb.append(DirAndFileConstant.RB);
		
			sb.append(DirAndFileConstant.SP);
			sb.append(msd);
		
		sb.append(DirAndFileConstant.FILEEXTENTION);
		new XMLOutputter().output(xmlDoc, new FileOutputStream(
				new File(outDir,
						sb.toString())));
	}

	public List<Result> analyse() {
		
		RankAlgorithm rankAlg;
		if (RankAlgorithm.TARANTULA.equals(rankMethName))
			rankAlg = new Tarantula();
		else if (RankAlgorithm.SBI.equals(rankMethName))
			rankAlg = new Sbi();
		else if (RankAlgorithm.JACCARD.equals(rankMethName))
			rankAlg = new Jaccard();
		else if (RankAlgorithm.OCHIAI.equals(rankMethName))
			rankAlg = new Ochiai();
		else {
			System.out.print("no this rank method");
			return null;
		}
		List<Result> result = rankAlg.compute(statics);
		doRank(result);
		return result;
		
	}

	
	
	
	public void doDir(File inDir, List<String> args, String msd) {
		
		try {
			SAXBuilder sb = new SAXBuilder();

			
			HashMap<String, Boolean> testResult = new HashMap<String, Boolean>();

			{
				Document xmlDoc = sb.build(new File(inDir, DirAndFileConstant.RESULT+DirAndFileConstant.FILEEXTENTION));

				Element xmlRoot = xmlDoc.getRootElement();

				Iterator xmlTestMethodIt = xmlRoot.getChildren("testmethod").iterator();

				// ////*****************while
				while (xmlTestMethodIt.hasNext()) {
					Element xmlTestMethod = (Element) xmlTestMethodIt.next();
//					int size = Integer.parseInt(xmlRoot.getAttributeValue("number"));				
					
					String name = xmlTestMethod.getAttributeValue("name");
					if (args == null || args.contains(name)) {
							String tmp = xmlTestMethod.getAttributeValue("status");
							boolean b = Boolean.parseBoolean(tmp);
							if (b)
								statics.addTestTrue();
							else
								statics.addTestFalse();
							testResult.put(name, b);
					}
					
					
				}
			}
			
			

			for(String key : testResult.keySet()){
				File tf = new File(inDir, key);
				if(tf.exists()&&tf.isDirectory()){
					File f = new File(tf, DirAndFileConstant.COVERAGE);
					if(f.exists()&&f.isDirectory()){
						String[] ffNames = f.list();
						for(int j=0; j<ffNames.length; j++){
							File ff = new File(f, ffNames[j]);
							if (ff.isFile() && ff.getName().indexOf("xml") >= 0) {
								String className = ff.getName().substring(0,
									ff.getName().lastIndexOf("."));
								readData(className, f, testResult.get(key));
							}
						}
					}					
				}else{
					System.out.println("error");
				}
			}
			List<Result> result = analyse();
			
			toXML(result, inDir, args, msd);
			
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void doRank(List<Result> result) {
		Collections.sort(result);
		Collections.reverse(result);
		int size = result.size();

		if (size > 0)
			result.get(size-1).rank = size;

		for (int i = size-2; i >=0; i--) {
			Result r = result.get(i);
			if (r.compareTo(result.get(i + 1)) == 0)
				r.rank = result.get(i + 1).rank;
			else
				r.rank = i + 1;
		}
		
		for(int i=0; i<size; i++)
			System.out.println(result.get(i).rank + " " + result.get(i).line);
	}

//	public static void main(String[] args) {
//		ComputeRank t = new ComputeRank();
//		t.doDir(new File(args[0]));
//	}
}
