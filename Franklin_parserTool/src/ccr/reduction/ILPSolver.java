package ccr.reduction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import ccr.test.Logger;
import ccr.test.TestCase;

/**
 * then construct the reduced test suites from the ILP solutions
 * 
 * @author hwang
 *
 */
public class ILPSolver {
	
	/**2009-12-18:create a LP file from existing info files,
	 * then save this LP file. For a bi-criteria ILP model, 
	 * the objective function considers both coverage and Context
	 * Diversity of each test case 
	 * 
	 * @param filename
	 * @param alpha
	 * @param saveFile
	 */
	public void createILPModel_biCriteria(String filename, boolean containHeader, double alpha, String saveFile){
		try {
			//1. read info of all test cases
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String str = null;
			int rowNo = 0;
			if(containHeader){
				rowNo = br.readLine().split("\t").length - 6;
			}
			
			StringBuilder infoRecorder = new StringBuilder();
			Vector<TestCase> tcArray = new Vector<TestCase>(); 
			while((str=br.readLine())!= null){
				tcArray.add(new TestCase(str, true));				
			}
			br.close();
			if(!containHeader){
				rowNo = tcArray.get(0).hitSet.size();
			}
			
			infoRecorder.append("Before dimension reduction:").append("\n");
			infoRecorder.append("ProgramElement:").append(rowNo).append("\n");
			infoRecorder.append("TestCase:").append(tcArray.size()).append("\n");
			infoRecorder.append("--------------------------------------------------------------------------").append("\n");
			
			//2.create LP file: convert program element(row) * testcases(column) into the matrix of testcases(row)* program elements(column)
			StringBuilder sb = new StringBuilder();
			
			sb.append("min:");			//objective function
			for(int i = 0; i < tcArray.size(); i ++){
				//2009-12-20: use CR rather than CD for scaling purpose
				TestCase tc = tcArray.get(i);		
				double CR = tc.CI/Double.parseDouble(tc.length);
				double weight = alpha - (1-alpha)*CR;
				sb.append(" + " + weight + " x"+ (i + 1));
			}
			sb.append(";\n");
			

			Vector<Integer[]> constraints = new Vector<Integer[]>();		
			int infeasible_constraint_counter = 0;
			int equivalent_constraint_counter = 0;
			for(int i = 0; i < rowNo; i ++){ //for each constraint of a program element				
				Integer[] constraint = new Integer[tcArray.size()];
				for(int j = 0; j < constraint.length; j ++){
					Vector<Integer> hitSet= tcArray.get(j).hitSet; 
					constraint[j] = hitSet.get(i); // we list all coverage of test cases with respect to each program element(constraint)  
				}
				
				//eliminate all infeasible program elements				
				int sumHit = 0;
				for(int j = 0; j < constraint.length; j++){
					sumHit += constraint[j];
				}
				if(sumHit != 0){ //a program element is infeasible if no test case can cover it
					//2009-12-20: note that we should remove any equivalent constraints here
					if(!this.isMemberOf(constraints, constraint)){
						constraints.add(constraint);	
					}else{
						equivalent_constraint_counter ++;
					}
				}else{
					infeasible_constraint_counter ++;
				}
			}
			
			infoRecorder.append("After dimension reduction:").append("\n");			
			infoRecorder.append("ProgramElement:").append(constraints.size()).
				append("(").append(rowNo).append(" exclude infeasible:").append(infeasible_constraint_counter).append(",").
				append("equivalent:").append(equivalent_constraint_counter).append(")").append("\n");
			infoRecorder.append("TestCase:").append(tcArray.size()).append("\n");
			infoRecorder.append("--------------------------------------------------------------------------").append("\n");
			
			
			
			//save all into (coverage matrix of all test cases with respect to program elements)
			for(int i = 0; i < constraints.size(); i ++){
				Integer[] constraint = constraints.get(i);
				for(int j = 0; j < constraint.length; j ++){
					sb.append(" + " + constraint[j] + " x" + (j+1));
				}
				sb.append(" >= 1;\n");
			
			}
			sb.append("\n");
			
			
			sb.append("bin "); //specify the binary variables			
			for(int i = 0; i < tcArray.size()-1; i ++){
				sb.append("x"+ (i + 1) + ", ");
			}
			
			
			sb.append("x" + (tcArray.size()) +";");
			Logger.getInstance().setPath(saveFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();	
			
			File file = new File(filename);
			String criterion = file.getName().split("_")[1];
			String resultPath = file.getParent() + "\\Result_" +
				  criterion +"_" + alpha + ".txt";
			Logger.getInstance().setPath(resultPath, true);
			Logger.getInstance().write(infoRecorder.toString());
			Logger.getInstance().close();
			
			System.out.println("Finish to construct the model");
			System.out.println("Start to solve the model");
			//3. solve the ILP problem
			try {
				LpSolve solver = LpSolve.readLp(saveFile, LpSolve.CRITICAL, null);
				
				long startTime = System.currentTimeMillis();
				int ret = solver.solve();
				long exeTime = System.currentTimeMillis() - startTime;
				infoRecorder.append("Execute Time:").append(exeTime/(double)1000).append(" s\n");
				
				if(ret == LpSolve.OPTIMAL)
					ret = 0;
				else
					ret = 5;
				
				if(ret == 0){ //solve the problem successfully
//					solver.printObjective();
					
					infoRecorder.append("Reduced Test Suite Size:").append(solver.getObjective()).append("\n").
							append("Selected Test Cases:").append("\n");
					double[] var = solver.getPtrVariables();
					for(int i = 0; i < var.length; i ++){
						if(var[i]==1){
//							System.out.print(i +  ",");
							String index = tcArray.get(i).index;
							infoRecorder.append(index + ",");
						}
					}
					
//					solver.printLp();
//					solver.printSolution(1);
//					solver.printConstraints(1);
//					solver.writeLp(saveFile);	
					
				}else{
//					System.out.println("cannot get the solution");
					infoRecorder.append("cannot get the solution");
				}
				System.out.println("Finish solving the model");
				
				Logger.getInstance().setPath(resultPath, true);
				Logger.getInstance().write(infoRecorder.toString());
				Logger.getInstance().close();

			} catch (LpSolveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	private boolean isMemberOf(Vector<Integer[]> constraints, Integer[] constraint){
		boolean isMember = false;
		for(int i = 0; i < constraints.size() && !isMember; i ++){
			Integer[] constraint_temp = constraints.get(i);
			int j = 0;
			for(; j < constraint_temp.length; j ++){
				if(constraint_temp[j]!= constraint[j]){
					break; //break the loop if any element is different
				}
			}
			if(j == constraint_temp.length){//find an equivalent constraint
				isMember = true;
			}
		}
		
		return isMember;
	}
	
	public static void main(String[] args) {
		if(args.length ==0){
			System.out.println("No date is specified ");
		}else{
			String date = args[0];
			String criterion = args[1];
			String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/TestCaseStatistics_" + criterion + ".txt"; 
			
//			String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
//				+ date + "/TestCaseStatistics_" + criterion + ".txt";
			String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/model.lp";
			double alpha = 1.0;
			boolean containHeader = true;
			new ILPSolver().createILPModel_biCriteria(filename, containHeader, alpha, saveFile);
		}
	}

}
