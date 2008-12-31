package hku.cs.tscore;

import java.io.File;

import org.jdom.input.SAXBuilder;




public class Graph {
	
	public Graph(File f){
		SAXBuilder sb = new SAXBuilder();
		try {
			sb.build(f);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
}
