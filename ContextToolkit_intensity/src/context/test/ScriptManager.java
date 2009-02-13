package context.test;

import context.test.util.Constant;
import context.test.util.Logger;

public class ScriptManager {

	public void generateExecutionScript(int min_Version, int max_Version,
			int iteration, int min_TestCase, int max_TestCase, int interval) {
		StringBuilder sb = new StringBuilder();
		for (int versionNumber = min_Version; versionNumber <= max_Version; versionNumber++) {
			int begin = min_TestCase;
			do{
				sb.append("java context.test.TestManagers " + versionNumber
						+ " " + versionNumber + " " + iteration + " "
						+ begin + " " + (begin + interval -1) + " &"+"\n");
				begin = begin + interval;
			}while(begin < max_TestCase);
			
		}
		if(min_Version == max_Version){
			Logger.getInstance().setPath(Constant.baseFolder + "runScript_"+max_Version+".sh", false);
		}else{
			Logger.getInstance().setPath(Constant.baseFolder + "runScript_"+min_Version + "_" + max_Version+".sh", false);			
		}
		
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}

	public static void main(String[] args) {
		ScriptManager manager_SC = new ScriptManager();
		
		int min_Version = 1;
		int max_Version = 1;
		int iteration = 100;
		int min_TestCase = 0;
		int max_TestCase = 999;
		int interval = 0;
		if(args.length == 6){
			min_Version = Integer.parseInt(args[0]);
			max_Version = Integer.parseInt(args[1]);
			iteration = Integer.parseInt(args[2]);
			min_TestCase = Integer.parseInt(args[3]);
			max_TestCase = Integer.parseInt(args[4]);
			interval = Integer.parseInt(args[5]);
//			for(int versionNumber = min_Version; versionNumber < max_Version; versionNumber ++){
//				manager_SC.generateExecutionScript(versionNumber, versionNumber, iteration, min_TestCase, max_TestCase, interval);
//			}
		}
			manager_SC.generateExecutionScript(min_Version, max_Version, iteration, min_TestCase, max_TestCase, interval);			
		
		
		
	}

}
