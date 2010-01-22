package ccr.help;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import ccr.test.Logger;



public class MutantStatistics {
	String[] missingConstructs = {"AOD", "COD", "LOD", "IHD",
			"IOD", "ISD", "IPC", "PCD", "OMD", "JTD", "JSD", "JID"};
	
	String[] wrongConstructs = {"AOR", "ROR", "COR", "SOR", 
			"LOR", "ASR", "IOP", "IOR", "PMD", "PPD", "PCC",
			"PRV", "OMR", "OAC", "EOA", "EOC", "EAM", "EMM"};
	
	
	
	
	
	public 
	String[] extraneousConstructs = {"AOI", "COI", "LOI", "IHI", 
			"ISI", "PNC", "PCI", "JTI", "JSI", "JDC"};
	
	/**2010-01-22: load mutants from the file via the offline way.
	 * 
	 * @param date
	 * @param containHeader
	 * @return
	 */
	public static ArrayList<String> loadMutants_offline(String date, boolean containHeader){
		ArrayList<String> mutantList = new ArrayList<String>();
		String mutantFile = "/src/ccr" +
			"/experiment/Context-Intensity_backup/TestHarness/"+ date
			+ "/Mutant/FaultList.txt";
		
		File tmp = new File(mutantFile);
		if(!tmp.exists()){
			try {
				BufferedReader br = new BufferedReader(new FileReader(mutantFile));
				if(containHeader)
					br.readLine();
				
				String str = null;
				if((str = br.readLine())!= null){
					mutantList.add(str);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}else{
			System.out.println("Mutant file " + mutantFile + " does not exist");
		}
		
		return mutantList;
	}
	
	public HashMap<String, HashMap<String, Integer>> getStatistics_ClassLevel(String mutantDir){
		HashMap<String, HashMap<String, Integer>> class_Mutants = 
			new HashMap<String, HashMap<String, Integer>>();
		//class-level mutants: class-> (modification ->) mutant operators-> mutant numbers
		
		File resultsDir = new File(mutantDir);
		System.out.println(resultsDir.getName());
		
		if(resultsDir.exists()){
			File[] classesDir = resultsDir.listFiles();
			for(File classDir: classesDir){	
				if(classDir.isDirectory()){
					System.out.println(classDir.getName());
					
					//get statistics of class-level mutants
					File classMutantsDir = new File(classDir.getAbsolutePath() +
							File.separator + "class_mutants");
					
					File[] mutantOperators = classMutantsDir.listFiles();
					for(File mutantOperator: mutantOperators){
						System.out.println(mutantOperator.getName());
						if(mutantOperator.isDirectory()){
							String[] tmp = mutantOperator.getName().split("_");
							String mutantoperator = tmp[0];
							String clazz = classDir.getName();
							
							if(class_Mutants.containsKey(clazz)){ //className exists
								HashMap<String, Integer> tmpHash = class_Mutants.get(clazz);
								if(tmpHash.containsKey(mutantoperator)){ //mutant operator exists
									Integer mutantNum = tmpHash.get(mutantoperator);
									mutantNum ++;
									tmpHash.put(mutantoperator, mutantNum);
									class_Mutants.put(clazz, tmpHash);
								}else{ //mutant operator does not exist 
									tmpHash.put(mutantoperator, 1);
									class_Mutants.put(clazz, tmpHash);
									
								}
							}else{ //className does not exist
								HashMap<String, Integer> tmpHash = new HashMap<String, Integer>();
								tmpHash.put(mutantoperator, 1);
								class_Mutants.put(clazz, tmpHash);
							}
						}
					}
				}
			}
		}else{
			System.out.println("The mutant directory does not exist at all!");
		}
		
		return class_Mutants;
	}
	
	public void saveToFile_ClassLevel(HashMap<String, HashMap<String, Integer>> class_Mutants, String saveFile){
		StringBuilder sb = new StringBuilder();
		sb.append("Class").append("\t").append("Function").append("\t").
		append("MutantOperators").append("\t").append("MutantNumber").append("\n");
		
		//record all class-level mutants
		for(String clazz: class_Mutants.keySet()){
			HashMap<String, Integer> operators_Mutants = class_Mutants.get(clazz);
			for(String operator: operators_Mutants.keySet()){
				sb.append(clazz).append("\t").append("global").append("\t").
				append(operator).append("\t").append(operators_Mutants.get(operator)).append("\n");
			}
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}

	public void saveToDB_ClassLevel(HashMap<String, HashMap<String, Integer>> class_Mutants){
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO mutant (class, function, operator, mutantNumber) VALUES ");
		
		for(String clazz: class_Mutants.keySet()){
			HashMap<String, Integer> operators_Mutants = class_Mutants.get(clazz);
			for(String operator: operators_Mutants.keySet()){				
				sb.append("(\'").append(clazz).append("\'").append(",");
				sb.append("\'").append("global").append("\'").append(",");
				sb.append("\'").append(operator).append("\'").append(",");
				sb.append("\'").append(operators_Mutants.get(operator)).append("\'").append("),");				
			}
		}
		String sql = sb.substring(0, sb.lastIndexOf(","));
		DatabaseManager.getInstance().update(sql);
		System.out.println("save class-level mutants into Database");
	}
	
	public HashMap<String, HashMap<String, HashMap<String, Integer>>> getStatistic_MethodLevel(String mutantDir){
		HashMap<String, HashMap<String, HashMap<String, Integer>>> method_Mutants = 
			new HashMap<String, HashMap<String, HashMap<String, Integer>>>();
		
		File resultsDir = new File(mutantDir);
		System.out.println(resultsDir.getName());
		
		if(resultsDir.exists()){
			File[] classesDir = resultsDir.listFiles();
			for(File classDir: classesDir){	
				if(classDir.isDirectory()){
					System.out.println(classDir.getName());
					
					//get statistics of method-level mutants;
					File classMutantsDir_T = new File(classDir.getAbsolutePath() +
							File.separator + "traditional_mutants");
					File[] methodMutants_T = classMutantsDir_T.listFiles(); 
					for(File methodMutant: methodMutants_T){
						System.out.println(methodMutant.getName());
						if(methodMutant.isDirectory()){ //This directory represents one method instead of a mutantion log file
							File[] mutantOperatorDir = methodMutant.listFiles();						
							for(File mutantOperator: mutantOperatorDir){
								System.out.println(mutantOperator.getName());
								String[] temp = mutantOperator.getName().split("_");
								String mutantoperator = temp[0];
								String method = methodMutant.getName();
								String clazz = classDir.getName();

								if(method_Mutants.containsKey(clazz)){
									HashMap<String, HashMap<String, Integer>> tmp2 = method_Mutants.get(clazz);
									if(tmp2.containsKey(method)){
										HashMap<String, Integer> operator_Mutants = tmp2.get(method);
										if(operator_Mutants.containsKey(mutantoperator)){
											Integer mutantNum = operator_Mutants.get(mutantoperator);
											mutantNum ++;
											operator_Mutants.put(mutantoperator, mutantNum);
											tmp2.put(method, operator_Mutants);
											method_Mutants.put(clazz, tmp2);
										}else{ //mutant operator does not exist
											operator_Mutants.put(mutantoperator, 1);
											tmp2.put(method, operator_Mutants);
											method_Mutants.put(clazz, tmp2);
										}
									}else{ //method does not exist
										HashMap<String, Integer> tmp1 = new HashMap<String, Integer>();										
										tmp1.put(mutantoperator, 1);
										tmp2.put(method, tmp1);
										method_Mutants.put(clazz, tmp2);
									}
								}else{//class does not exist
									HashMap<String, Integer> tmp1= new HashMap<String, Integer>();
									tmp1.put(mutantoperator, 1);
									HashMap<String, HashMap<String, Integer>> tmp2 = new HashMap<String, HashMap<String,Integer>>();
									tmp2.put(method, tmp1);
									method_Mutants.put(clazz, tmp2);
								}
							}
						}
					}
				}
			}
		}else{
			System.out.println("The mutant directory does not exist at all!");
		}
		
		return method_Mutants;
	}
	
	public void saveToFile_MethodLevel(HashMap<String, HashMap<String, HashMap<String, Integer>>> method_Mutants, String saveFile){
		StringBuilder sb = new StringBuilder();
		sb.append("Class").append("\t").append("Function").append("\t").
		append("MutantOperators").append("\t").append("MutantNumber").append("\n");
		
		//record all method-level mutants
		for(String clazz: method_Mutants.keySet()){
			HashMap<String, HashMap<String, Integer>> function_Mutants = method_Mutants.get(clazz);
			for(String function: function_Mutants.keySet()){
				HashMap<String, Integer> operator_Mutants = function_Mutants.get(function);
				for(String operator: operator_Mutants.keySet()){
					sb.append(clazz).append("\t").append(function).append("\t").
					append(operator).append("\t").append(operator_Mutants.get(operator)).append("\n");
				}
			}
		}
		
		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}

	public void saveToDB_MethodLevel(HashMap<String, HashMap<String, HashMap<String, Integer>>> method_Mutants){
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO mutant (class, function, operator, mutantNumber) VALUES ");
		
		for(String clazz: method_Mutants.keySet()){
			HashMap<String, HashMap<String, Integer>> function_Mutants = method_Mutants.get(clazz);
			for(String function: function_Mutants.keySet()){
				HashMap<String, Integer> operator_Mutants = function_Mutants.get(function);
				for(String operator: operator_Mutants.keySet()){
					sb.append("(\'").append(clazz).append("\'").append(",");
					sb.append("\'").append(function).append("\'").append(",");
					sb.append("\'").append(operator).append("\'").append(",");
					sb.append("\'").append(operator_Mutants.get(operator)).append("\'").append("),");										
				}
			}
		}
		
		String sql = sb.substring(0, sb.lastIndexOf(","));
		DatabaseManager.getInstance().update(sql);
		System.out.println("save method-level mutants into the Database");
	}

	/**2009-12-30: assemble distributed mutant log files (operator:lineNumber:function:description)
	 * into a complete one
	 * 
	 * @param mutantDir
	 * @param saveFile
	 */
	public void saveToFile_mutantLog(String mutantDir, String saveFile){
		File resultDir = new File(mutantDir);
		System.out.println(resultDir.getName());
		
		StringBuilder sb = new StringBuilder();
		sb.append("class").append("\t").append("operator").append("\t").
			append("lineNumber").append("\t").append("function").append("\t").append("description").append("\n");
		
		if(resultDir.exists()){
			File[] classesDir = resultDir.listFiles();
			for(File classDir: classesDir){
				if(classDir.isDirectory()){
					System.out.println(classDir.getName());
					String clazz = classDir.getName();
					
					File methodMutantLog = new File(classDir.getAbsolutePath() 
							+ File.separator + "traditional_mutants" + File.separator + 
							"mutation_log");
					if(methodMutantLog.isFile()){
						try {
							BufferedReader br = new BufferedReader(new FileReader(methodMutantLog.getAbsolutePath()));
							String str = null;
							while((str = br.readLine()) != null){
								String[] temp = str.split(":");
								sb.append(clazz).append("\t");
								for(String tmp: temp){
									sb.append(tmp).append("\t");	
								}
								sb.append("\n");
							}
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					File classMutantLog = new File(classDir.getAbsolutePath() 
							+ File.separator + "class_mutants" + File.separator + 
							"mutation_log");
					if(classMutantLog.isFile()){
						try {
							BufferedReader br = new BufferedReader(new FileReader(classMutantLog.getAbsolutePath()));
							String str = null;
							while((str = br.readLine()) != null){
								String[] temp = str.split(":");
								
								sb.append(clazz).append("\t");
								sb.append(temp[0]).append("\t");//operator
								sb.append(temp[1]).append("\t");//lineNumber
								sb.append("global").append("\t");//function								
								sb.append(temp[2]).append("\t");//description								
								sb.append("\n");
							}
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
			
			Logger.getInstance().setPath(saveFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
		}else{
			System.out.println("The mutant directory does not exist at all!");
		}
	}
	
	/**2009-12-30: save the mutant logs (operator:lineNumber:function:description)
	 * into the database
	 * @param mutantDir
	 */
	public void saveToDB_mutantLog(String mutantDir){
		File resultDir = new File(mutantDir);
		System.out.println(resultDir.getName());
		
		StringBuilder sql = new StringBuilder();
//		sb.append("class").append("\t").append("operator").append("\t").
//		append("lineNumber").append("\t").append("function").append("\t").append("description").append("\n");
		sql.append("INSERT INTO mutantdetail (class, operator, lineNumber, function, description) VALUES ");
		
		if(resultDir.exists()){
			File[] classesDir = resultDir.listFiles();
			for(File classDir: classesDir){
				if(classDir.isDirectory()){
					System.out.println(classDir.getName());
					String clazz = classDir.getName();
					
					File methodMutantLog = new File(classDir.getAbsolutePath() 
							+ File.separator + "traditional_mutants" + File.separator + 
							"mutation_log");
					if(methodMutantLog.isFile()){
						try {
							BufferedReader br = new BufferedReader(new FileReader(methodMutantLog.getAbsolutePath()));
							String str = null;
							while((str = br.readLine()) != null){
								String[] temp = str.split(":");
//								LOI_632:614:void_resolve():i => ~i
								sql.append("(\'").append(clazz).append("\'").append(",");
								sql.append("\'").append(temp[0]).append("\'").append(",");
								sql.append("\'").append(temp[1]).append("\'").append(",");
								sql.append("\'").append(temp[2]).append("\'").append(",");		
								sql.append("\'").append(temp[3]).append("\'").append("),");								
							}
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					File classMutantLog = new File(classDir.getAbsolutePath() 
							+ File.separator + "class_mutants" + File.separator + 
							"mutation_log");
					if(classMutantLog.isFile()){
						try {
							BufferedReader br = new BufferedReader(new FileReader(classMutantLog.getAbsolutePath()));
							String str = null;
							while((str = br.readLine()) != null){
								String[] temp = str.split(":");
								
								sql.append("(\'").append(clazz).append("\'").append(",");
								sql.append("\'").append(temp[0]).append("\'").append(",");
								sql.append("\'").append(temp[1]).append("\'").append(",");
								sql.append("\'").append("global").append("\'").append(",");										
								sql.append("\'").append(temp[2]).append("\'").append("),");															
							}
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
			
			String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
			DatabaseManager.getInstance().update(sqlStmt);
			System.out.println("save mutant logs into the Database");	
		}else{
			System.out.println("The mutant directory does not exist at all!");
		}
		
	}
	
	public void saveToDB_mutantMapping(String mutantMappingFile, boolean containHeader){
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO mutantmapping (genMutant, mappedMutant) VALUES ");
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(mutantMappingFile));
			String str = null;
			if(containHeader){
				br.readLine();
			}
				
			while((str = br.readLine()) != null ){
				String[] temp = str.split("\t");
				
				sql.append("(\'").append(temp[0]).append("\'").append(",");
				sql.append("\'").append(temp[1]).append("\'").append("),");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
		DatabaseManager.getInstance().update(sqlStmt);
		System.out.println("Save the mutant mapping history into the Database");
	}
	
	private String generateSQLStatement(String colNum, String[] values, boolean include){
		StringBuilder sql = new StringBuilder();
		if(include){
			for(int i = 0; i < values.length -1; i ++){
				sql.append(colNum).append(" LIKE \'%").append(values[i]).append("%\' or ");
			}
			sql.append(colNum).append(" LIKE \'%").append(values[values.length - 1]).append("%\'");	
		}else{
			for(int i = 0; i < values.length -1; i ++){
				sql.append(colNum).append(" NOT LIKE \'%").append(values[i]).append("%\' or ");
			}
			sql.append(colNum).append(" NOT LIKE \'%").append(values[values.length - 1]).append("%\'");
		}
		
		return sql.toString();
	}
	
	public void saveToDB_nonEquivalentFaults(String faultList, boolean containHeader){
		File tmp =  new File(faultList);
		StringBuilder sql = new StringBuilder();
		sql.append("INSERT INTO nonequivalentmutant ( mappedMutant ) VALUES ");
		
		try {
			if(tmp.isFile()){
				BufferedReader br =  new BufferedReader(new FileReader(faultList));
				if(containHeader){
					br.readLine();
				}
				
				String str = null;
				
				while((str = br.readLine())!= null){
					String[] temp = str.split("\t");
					
					sql.append("(\'").append("TestCFG2_" + temp[0] +".java").append("\'").append("),");					
				}
			}else{
				System.out.println("The file:" + faultList+ " does not exist!");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
		DatabaseManager.getInstance().update(sqlStmt);
		System.out.println("Save the non-equivalent mutants into the Database");

	}
	
	public void saveToDB_failureRate(String failureRateFile, boolean containHeader){
		File tmp =  new File(failureRateFile);
	
		
		try {
			if(tmp.isFile()){
				BufferedReader br =  new BufferedReader(new FileReader(failureRateFile));
				if(containHeader){
					br.readLine();
				}
				
				String str = null;
				
				StringBuilder sql = new StringBuilder();
				sql.append("INSERT INTO mutantfailurerate ( mappedMutant, failureRate) VALUES ");
				
				while((str = br.readLine())!= null){
					String[] temp = str.split("\t");
					
					sql.append("(\'").append("TestCFG2_" + temp[0] +".java").append("\'").append(",");
					sql.append("\'").append(temp[1]).append("\'").append("),");
				}
				
				String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
				DatabaseManager.getInstance().update(sqlStmt);
				System.out.println("Save the mutant failure rates into the Database");
				
			}else{
				System.out.println("The file:" + failureRateFile+ " does not exist!");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	public void saveToDB_interfaceMutants(String faultList, boolean containHeader){
		File tmp =  new File(faultList);
		
		try {
			if(tmp.isFile()){
				BufferedReader br =  new BufferedReader(new FileReader(faultList));
				if(containHeader){
					br.readLine();
				}
				
				String str = null;
			
				StringBuilder sql = new StringBuilder();
				sql.append("INSERT INTO  interfacemutant ( genMutant ) VALUES ");
				
				while((str = br.readLine())!= null){
					sql.append("(\'").append(str).append("\')").append(",");					
				}
				
				String sqlStmt = sql.substring(0, sql.lastIndexOf(","));
				DatabaseManager.getInstance().update(sqlStmt);
				System.out.println("Save the interface-level mutants into the Database");
				
			}else{
				System.out.println("The file:" + faultList+ " does not exist!");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	/**2009-12-29:summarize mutants for a class&a method based on three categories:
	 * missing construct, wrong constructs and extraneous constructs 
	 * 
	 * @param classes:class list
	 * @param methods: method list
	 * @param includeMethod: include/exclude methods
	 */
	public void mutateSum(String[] classes, String[] methods, boolean includeMethod){
		
		StringBuilder report = new StringBuilder();
		report.append("MissingConstructs").append("\t").append("WrongConstructs").
			append("\t").append("ExtraneousConstructs").append("\t").append("Total").append("\n");
		
		StringBuilder sql = new  StringBuilder();
		
		try {
			//1.get missing constructs
			sql.append("SELECT SUM( mutantNumber ) FROM mutant WHERE ").append("(");
			sql.append(this.generateSQLStatement("class", classes, true));
			sql.append(")").append(" And ").append("(");
			if(includeMethod){
				sql.append(this.generateSQLStatement("function", methods, true));	
			}else{
				sql.append(this.generateSQLStatement("function", methods, false));
			}
			sql.append(")").append(" And ").append("(");
			sql.append(this.generateSQLStatement("operator", missingConstructs, true));
			sql.append(")");			
			ResultSet rs = DatabaseManager.getInstance().query(sql.toString());
			while(rs.next()){
				report.append(rs.getInt(1)).append("\t");
			}		
			sql.setLength(0);
			
			//2.get wrong constructs
			sql.append("SELECT SUM( mutantNumber ) FROM mutant WHERE ").append("(");
			sql.append(this.generateSQLStatement("class", classes, true));
			sql.append(")").append(" And ").append("(");
			if(includeMethod){
				sql.append(this.generateSQLStatement("function", methods, true));	
			}else{
				sql.append(this.generateSQLStatement("function", methods, false));
			}
			sql.append(")").append(" And ").append("(");
			System.out.println(sql.toString());
			
			sql.append(this.generateSQLStatement("operator", wrongConstructs, true));
			sql.append(")");			
			rs = DatabaseManager.getInstance().query(sql.toString());
			while(rs.next()){
				report.append(rs.getInt(1)).append("\t");
			}		
			sql.setLength(0);
			
			//3.get extraneous constructs
			sql.append("SELECT SUM( mutantNumber ) FROM mutant WHERE ").append("(");
			sql.append(this.generateSQLStatement("class", classes, true));
			sql.append(")").append(" And ").append("(");
			if(includeMethod){
				sql.append(this.generateSQLStatement("function", methods, true));	
			}else{
				sql.append(this.generateSQLStatement("function", methods, false));
			}
			sql.append(")").append(" And ").append("(");
			sql.append(this.generateSQLStatement("operator", extraneousConstructs, true));
			sql.append(")");			
			rs = DatabaseManager.getInstance().query(sql.toString());
			while(rs.next()){
				report.append(rs.getInt(1)).append("\t");
			}		
			sql.setLength(0);
			
			//4.get mutant sums
			sql.append("SELECT SUM( mutantNumber ) FROM mutant WHERE ").append("(");
			sql.append(this.generateSQLStatement("class", classes, true));
			sql.append(")").append(" And ").append("(");
			if(includeMethod){
				sql.append(this.generateSQLStatement("function", methods, true));	
			}else{
				sql.append(this.generateSQLStatement("function", methods, false));
			}		
			sql.append(")");			
			rs = DatabaseManager.getInstance().query(sql.toString());
			while(rs.next()){
				report.append(rs.getInt(1)).append("\t");
			}		
			sql.setLength(0);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(report.toString());
	}
	
	
	
	public static void main(String[] args) {
//		String mutantDir = "F:\\MyProgram\\eclipse3.3.1.1\\workspace\\TestingFramework\\result\\";
		String mutantDir = "F:\\MyProgram\\eclipse3.3.1.1\\workspace\\ContextDiversity\\result\\";		
		String instruction = args[0];
		
		MutantStatistics ins = new MutantStatistics();
		if(instruction.equals("saveMutants_ClassLevel")){
			//1. save class-level mutants into the file/database		
			String saveFile = mutantDir + "statistics_ClassLevel.txt";			
			HashMap<String, HashMap<String, Integer>> classMutants = ins.getStatistics_ClassLevel(mutantDir);
			ins.saveToFile_ClassLevel(classMutants, saveFile);
//			ins.saveToDB_ClassLevel(classMutants);			
		}else if(instruction.equals("saveMutants_MethodLevel")){
			//2. save method-level mutants into the file/database
			String saveFile = mutantDir + "statistics_MethodLevel.txt";
			HashMap<String, HashMap<String, HashMap<String, Integer>>> methodMutants =ins.getStatistic_MethodLevel(mutantDir); 
			ins.saveToFile_MethodLevel(methodMutants, saveFile);
			ins.saveToDB_MethodLevel(methodMutants);			
		}else if(instruction.equals("classifyMutants")){
			//3. get the summary of method-level/class-level mutants into application, middleware
			String[] classes = {"TestCFG2"};
			String[] methods = {"application"};
			boolean includeMethod = true; //count mutants of the application
			ins.mutateSum(classes, methods, includeMethod);
			includeMethod = false; //count mutants of the middleware
			ins.mutateSum(classes, methods, includeMethod);
		}else if(instruction.equals("saveMutantLog")){
			//4. assemble distributed mutant log into a complete file/database
			String saveFile = mutantDir + "mutantLog.txt";
			ins.saveToFile_mutantLog(mutantDir, saveFile);
			ins.saveToDB_mutantLog(mutantDir);
		}else if(instruction.equals("saveMutantMapping")){
			//5. save the mutant mapping history into database
			String mutantMappingFile = new File(mutantDir).getParent() + "\\MuJava\\MappingList.txt";
			boolean containHeader = false;
			ins.saveToDB_mutantMapping(mutantMappingFile, containHeader);
		}else if(instruction.equals("saveFailureRates")){
			//6. save the failure rate into database
			String failureRateFile = new File(mutantDir).getParent() + "\\src\\ccr" +
					"\\experiment\\Context-Intensity_backup\\TestHarness\\20091230\\FailureRateDetails_5024.txt";
			boolean containHeader = true;
			ins.saveToDB_failureRate(failureRateFile, containHeader);			
		}else if(instruction.equals("saveNonEquivalentMutant")){
			String date = args[1];
			String faultList = new File(mutantDir).getParent() + "\\src\\ccr" +
			"\\experiment\\Context-Intensity_backup\\TestHarness\\"+date+"\\NonEquivalentFaults.txt";
			boolean containHeader = false;
			ins.saveToDB_nonEquivalentFaults(faultList, containHeader);
		}else if(instruction.equals("saveInterfaceMutant")){
			String date = args[1];
			String faultList = new File(mutantDir).getParent() + "\\src\\ccr" +
			"\\experiment\\Context-Intensity_backup\\TestHarness\\"+date+"\\interfaceLevelFaults.txt";
			boolean containHeader = false;
			ins.saveToDB_interfaceMutants(faultList, containHeader);			
		}
	}

}
