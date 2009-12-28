package edu.cs.hku.mutantGeneration;

import java.io.File;
import java.util.HashMap;

import edu.cs.hku.util.Logger;

public class MutantStatistics {
	
	/**2009-12-28:
	 * 
	 * @param mutantDir
	 * @param savePath
	 */
	public void getMutantStatistics(String mutantDir, String savePath){
		HashMap<String, HashMap<String, Integer>> class_Mutants = 
			new HashMap<String, HashMap<String, Integer>>();
		//class-level mutants: class-> mutant operators-> mutant numbers
		
		HashMap<String, HashMap<String, HashMap<String, Integer>>> method_Mutants = 
			new HashMap<String, HashMap<String, HashMap<String, Integer>>>();
		//method-level mutants: class->function->mutant operators-> mutant numbers
		
		File resultsDir = new File(mutantDir);
		System.out.println(resultsDir.getName());
		
		if(resultsDir.exists()){
			File[] classesDir = resultsDir.listFiles();
			for(File classDir: classesDir){	
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
		}else{
			System.out.println("The mutant directory does not exist at all!");
		}

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
		
		Logger.getInstance().setPath(savePath, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	public static void main(String[] args) {
		String mutantDir = "F:\\MyProgram\\eclipse3.3.1.1\\workspace\\TestingFramework\\result\\";
		String savePath = System.getProperty("user.dir") + File.separator +"result" + 
				File.separator + "mutantStatistics.txt";
		new MutantStatistics().getMutantStatistics(mutantDir, savePath);
	}

}
