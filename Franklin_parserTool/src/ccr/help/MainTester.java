package ccr.help;

import ccr.test.ResultAnalyzer;

public class MainTester {
	
	public static void main(String[] args){
//		public static String getActivation(String srcDir, boolean containHeader, String criterion, String rename_criterion){
		String date = "20091019";
		
		String srcDir = "src/ccr/experiment/Context-Intensity_backup/TestHarness/"
			+ date + "/";
		boolean containHeader = true;
		String criterion = "All2ResolvedDU_RA-H_20_Activation";
		String rename_criterion = "A2SU_RA-H";
		
//		System.out.println(ResultAnalyzer.getActivation(srcDir, containHeader, criterion, rename_criterion));
	}
}
