package hku.cs.instrument;

import hku.cs.constant.DirAndFileConstant;
import hku.cs.tools.DirTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

public class Counter {
	public static HashMap<String, HashMap<String, HashMap<Integer, Integer>>> counter = new HashMap<String, HashMap<String, HashMap<Integer, Integer>>>();

	public static synchronized void increase(String className,
			String methodName, int blkIdx) {
		HashMap<String, HashMap<Integer, Integer>> metCnt = counter
				.get(className);
		if (metCnt == null)
			metCnt = new HashMap<String, HashMap<Integer, Integer>>();

		HashMap<Integer, Integer> al = metCnt.get(methodName);

		if (al == null)
			al = new HashMap<Integer, Integer>();

		if (null == al.get(blkIdx)) {
			al.put(blkIdx, 1);
		} else {
			int tmp = al.get(blkIdx);
			tmp++;
			al.put(blkIdx, tmp);
		}

		metCnt.put(methodName, al);
		counter.put(className, metCnt);
	}

	
	
	public static void report(String path, String klassName, String testName) {

		for (Iterator<String> classIt = counter.keySet().iterator(); classIt
				.hasNext();) {
			String className = classIt.next();
			HashMap<String, HashMap<Integer, Integer>> metCnt = counter
					.get(className);

			Element xmlRoot = new Element("class");
			xmlRoot.setAttribute("name", className);
			Document xmlDoc = new Document(xmlRoot);

			for (Iterator<String> methodIt = metCnt.keySet().iterator(); methodIt
					.hasNext();) {
				String methodName = methodIt.next();
				Element xmlMethod = new Element("method");
				xmlMethod.setAttribute("name", methodName);

				HashMap<Integer, Integer> cnt = metCnt.get(methodName);
				for (Iterator<Integer> i = cnt.keySet().iterator(); i.hasNext();) {
					Integer idx = i.next();
					Element xmlBlock = new Element("block");
					xmlBlock.setAttribute("index", idx.toString());
					xmlBlock
							.setAttribute("times", cnt.get(idx).toString());

					xmlMethod.addContent(xmlBlock);
				}
				xmlRoot.addContent(xmlMethod);
			}

			String sep = File.separator;
			StringBuffer sb = new StringBuffer();
			sb.append(path);
			sb.append(sep);
			String k[] = klassName.split(" ");
			if(k.length==2)
				klassName = k[1];
			sb.append(klassName);
			sb.append(sep);
			sb.append(testName);
			sb.append(sep);
			sb.append(DirAndFileConstant.BLOCK);
			File f = DirTools.prepare(sb.toString());
			
			System.out.println("write block profile information to "+f.getPath());
					
			try {
				new XMLOutputter().output(xmlDoc, new FileOutputStream(new File(f, className
						+ ".xml")));
				counter.clear();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
