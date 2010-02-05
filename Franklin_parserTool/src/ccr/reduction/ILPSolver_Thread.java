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
	}

}
