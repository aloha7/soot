package context.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.Vector;
import java.util.ArrayList;
import java.util.StringTokenizer;

import context.test.util.Constant;
import context.test.util.Logger;


public class MutantMaker {

	public void seedMutant(int testSuiteSize, int startVersionNum, int endVersionNum, boolean visual){
		try{
			
		    BufferedReader br = new BufferedReader(new FileReader(Constant.mutantFile));
		    String line = null;
		    int faultyVersion = 1;
		    ArrayList<String> m_compileList = new ArrayList<String>();
		    m_compileList.add("0");

		  //Every line represents one faulty version
		    while((line = br.readLine())!= null ){		    	
		    	//Seed a specified range of faults
		    	if((faultyVersion >= startVersionNum) &&
		    			(faultyVersion <= endVersionNum)){
		    		
		    		//1.Parse the class path
					StringBuilder sb = new StringBuilder();
					sb.append(Constant.baseFolder + "src");		    	
			    	StringTokenizer st = new StringTokenizer(line, "$");
			        String m_class = st.nextToken();		    			     		     
			        String[] m_splits = m_class.split("\\.");		        
			    	for(int i = 0; i < m_splits.length-1; i++)
			    		sb.append(Constant.FILESEPERATOR + m_splits[i]);		    			    	
			    	String m_className = sb.append(".java").toString(); 		       		       
			        
			    	//2.Parse the line number of mutants
			    	String t_line = st.nextToken();
			    	int m_line = Integer.valueOf(t_line.substring(t_line.indexOf("Line")+4)).intValue();
			    				
					//3.Parse the mutant operator itself
			    	st = new StringTokenizer(st.nextToken(),":");		    	
			    	String m_before = st.nextToken().trim();		    
			    	String m_after = st.nextToken().trim();
			    	
			    	//4.Read the source file and modify it
			    	BufferedReader mut_br = new BufferedReader(new FileReader(m_className));
			    	sb.delete(0, sb.length());
			    	
System.out.println("Seed faulty version " + faultyVersion + " :" + line );			    	
			    	
					String m_src = null; //backs up the source code who is mutated.
			    	int count = 0;
			    	while((line = mut_br.readLine())!= null){
			    	//Since the line may begin with " ", so contains is a good choice
			    		//if(!line.contains("Logger.getInstance()")&&!line.contains("import files.dcm.logger.*;"))			    	
			    		count ++;
			    		if(count != m_line)
			    			sb.append(line + "\n");
			    		else //insert the mutation in this line		    		
			    		{
			    			m_src = line; //backup the source code
					    	sb.append(MutantMaker.replaceFirst(line, m_before, m_after)+"\n");
			    		}
			    	}
			    	mut_br.close();
			    	
			    	//5.keep the mutant version
			    	Logger log = Logger.getInstance();
			    	log.setPath(m_className, false);			    	
			    	log.write(sb.toString());
			    	log.close();
			    	
			    	
			    	
					//6.produce outputs for this mutant version	
			    	int minVersion = faultyVersion;
			    	int maxVersion = faultyVersion;
			    	OutputProducer maker = new OutputProducer();			    				    
			    	maker.produceOutput(testSuiteSize, minVersion, maxVersion, visual);
			    	
					//7.Recovery the source codes: it doesn't matter whether the faults 
					//can be compiled or not
					BufferedReader rec_br = new BufferedReader(new FileReader(m_className));
			    	sb.delete(0, sb.length());		    			    
			    	count = 0;
			    	while((line = rec_br.readLine())!= null){	
			    	//	if(!line.startsWith("Logger.getInstance().writePath")&&!line.startsWith("import files.dcm.logger.*;"))
			    			count ++;
			    		if(count != m_line)
			    			sb.append(line + "\n");
			    		else		    					    		
					    	sb.append(m_src+"\n");			    		
			    	}
			    	rec_br.close();
System.out.println("Recovery success");			    	
			    	log.setPath(m_className, false);
			    	log.write(sb.toString());
			    	log.close();			    				    	    				  
		    	}		    			    	
				faultyVersion ++;							
		    }
		    br.close();		    		    		  		 
		}catch(Exception e){
			System.out.println(e);
		}		
	}
	
	
	
	
	public boolean contains(String fileName, String label){
		try{
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = null;
			while((line = br.readLine()) != null){
				if(line.indexOf(label) >= 0)
					return true;
			}			
			
		}catch(Exception e){
			System.out.println(e);
		}
		return false;
	}

	private long getSize(File file){
		long size = 0;
		if(file.isFile())
			size = file.length();
		else{
			File[] files = file.listFiles();
			for(File m_file: files)
				size += this.getSize(m_file);
		}	
		return size;
	}
		
	public static String replaceFirst(String src, String pattern, String replacement){
		
		String result = src;
		System.out.println("before(GenMutant):"+result);
		int index = src.indexOf(pattern);
		if(index >= 0){
			StringBuilder sb = new StringBuilder();
			sb.append(src.substring(0, index));
			sb.append(replacement);
			sb.append(src.substring(index + pattern.length(), src.length()));
			result = sb.toString();
		}
		System.out.println("after(GenMutant):"+result);		
		return result;
	}  
	
}
