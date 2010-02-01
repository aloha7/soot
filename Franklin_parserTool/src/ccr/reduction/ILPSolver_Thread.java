package ccr.reduction;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import ccr.help.ILPOutput;
import ccr.help.ProgramRunnable;
import ccr.test.Logger;
import ccr.test.TestCase;

public class ILPSolver_Thread extends ProgramRunnable {
	public String modelFile;
	ArrayList<TestCase> tcArray = new ArrayList<TestCase>();
	public String infoFile;
	public ILPOutput output= new ILPOutput();
	
	public ILPSolver_Thread(String modelFile, ArrayList<TestCase> tcArray, String infoFile){
		this.modelFile = modelFile;
		this.tcArray = tcArray;
		this.infoFile = infoFile;
	}
	
	
	public void run() {
		
		output = ILPSolver.solveILPModel(modelFile, tcArray, infoFile);
		
		
//		ArrayList<String> selectedTestCases = new ArrayList<String>();
//		
//		StringBuilder infoRecorder = new StringBuilder();
//		File tmp = new File(modelFile);
//		if(tmp.exists()){			
//			try {		
//				
//				String[] strs = tmp.getName().substring(0, tmp.getName().lastIndexOf(".")).split("_");
//				output.criterion = strs[1];
//				if(strs[2].equals("SingleObj")){//single-objective ILP model
//					output.alpha = Double.MIN_VALUE;
//					output.test = 0;
//					output.testSetId = Integer.parseInt(strs[3]);
//				}else{
//					output.alpha = Double.parseDouble(strs[2]);
//					output.testSetSize = Integer.parseInt(strs[3]);
//					output.testSetId = Integer.parseInt(strs[4]);
//				}
//				
//				
//				
//				LpSolve solver = LpSolve.readLp(modelFile, LpSolve.CRITICAL, null);
//
//				long startTime = System.currentTimeMillis();
//				int ret = solver.solve();
//				long exeTime = System.currentTimeMillis() - startTime;
//				infoRecorder.append("Execute Time:").append(exeTime/(double)1000).append(" s\n");
//				
//				output.time = (double)exeTime/(double)1000;
//				
//				if(ret == LpSolve.OPTIMAL)
//					ret = 0;
//				else
//					ret = 5;
//				
//				if(ret == 0){ //solve the problem successfully				
//					double[] var = solver.getPtrVariables();
//					for(int i = 0; i < var.length; i ++){
//						if(var[i]==1){
//							String index = tcArray.get(i).index;
//							selectedTestCases.add(index);
//							
//						}
//					}
//					
//					output.reducedTestSet.testcases = selectedTestCases;
//					
//					infoRecorder.append("Objective value:").append(new DecimalFormat("0.0000").format(solver.getObjective())).append("\n");
//					
//					output.objectiveValue = solver.getObjective();
//					
//					infoRecorder.append("Reduced Test Suite Size:").append(selectedTestCases.size()).append("\n");
//					infoRecorder.append("Selected Test Cases:").append("\n");
//					for(int i = 0; i < selectedTestCases.size(); i ++){
//						infoRecorder.append(selectedTestCases.get(i) + ",");
//					}
//					
//				}else{
//					infoRecorder.append("cannot get the solution");
//				}
//				
//				Logger.getInstance().setPath(infoFile, true);//append execution info into the file
//				Logger.getInstance().write(infoRecorder.toString());
//				Logger.getInstance().close();
//
//				String outputFile = tmp.getParent() + "/" + tmp.getName().
//					substring(0, tmp.getName().lastIndexOf("."))+"_output.txt";
//				
//				Logger.getInstance().setPath(outputFile, false);
//				Logger.getInstance().write(output.toString());
//				Logger.getInstance().close();
//			} catch (LpSolveException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}		
//		}
	}

}
