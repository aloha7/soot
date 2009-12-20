package ccr.reduction;

import java.io.BufferedReader;
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
			
			
			Vector<TestCase> tcArray = new Vector<TestCase>(); 
			while((str=br.readLine())!= null){
				tcArray.add(new TestCase(str, true)); 
			}
			br.close();
			
			//2.create LP file
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
			int infeasible_counter = 0;
			for(int i = 0; i < rowNo; i ++){ //for each constraint of a program element				
				Integer[] constraint = new Integer[tcArray.size()];
				for(int j = 0; j < constraint.length; j ++){
					Vector<Integer> hitSet= tcArray.get(j).hitSet;
					constraint[j] = hitSet.get(i);
				}
				
				//eliminate all infeasible program elements				
				int sumHit = 0;
				for(int j = 0; j < constraint.length; j++){
					sumHit += constraint[j];
				}
				if(sumHit != 0){ //a program element is infeasible if no test case can cover it
					constraints.add(constraint);
				}else{
					infeasible_counter ++;
				}
			}
			System.out.println("infeasible element no.:" + infeasible_counter);
			
			//save all into (coverage matrix of all test cases with respect to program elements)
			for(int i = 0; i < constraints.size(); i ++){
				Integer[] constraint = constraints.get(i);
				for(int j = 0; j < constraint.length; j ++){
					sb.append(" + " + constraint[j] + " x" + (j+1));
				}
				sb.append(">= 1;\n");
			
			}
			sb.append("\n");
			
			
			for(int i = 0; i < rowNo; i ++){ //each row is a constraint
				for(int j = 0; j < tcArray.size(); j ++){
					Vector<Integer> hitSet = tcArray.get(j).hitSet;
					sb.append(" + " + hitSet.get(i) + " x" + (j + 1));
				}
				sb.append(">= 1;\n");				
			}
			sb.append("\n");
			
			sb.append("int "); //specify the binary variables			
			for(int i = 0; i < tcArray.size()-1; i ++){
				sb.append("x"+ (i + 1) + ", ");
			}
			
			
			sb.append("x" + (tcArray.size()-1) +";");
			Logger.getInstance().setPath(saveFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
			
			//3. solve the ILP problem
			try {
				LpSolve solver = LpSolve.readLp(saveFile, LpSolve.CRITICAL, null);
				
				int ret = solver.solve();
				if(ret == LpSolve.OPTIMAL)
					ret = 0;
				else
					ret = 5;
				if(ret == 0){
					solver.writeLp(saveFile);	
				}else{
					System.out.println("cannot get the solution");
				}
				
//				solver.printLp();
//				solver.printObjective();
//				solver.printSolution(1);
//				solver.printConstraints(1);

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
	
	
	
	public static void main(String[] args) {
		if(args.length ==0){
			System.out.println("No date is specified ");
		}else{
			String date = args[0];
			String criterion = args[1];
			String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/" + criterion + "/" + "TestCaseStatistics_" + criterion + "_" 
				 + "1000_2000.txt";
			
//			String filename = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
//				+ date + "/TestCaseStatistics_" + criterion + ".txt";
			String saveFile = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
				+ date + "/model.lp";
			double alpha = 1.0;
			boolean containHeader = false;
			new ILPSolver().createILPModel_biCriteria(filename, containHeader, alpha, saveFile);
		}
	}

}
