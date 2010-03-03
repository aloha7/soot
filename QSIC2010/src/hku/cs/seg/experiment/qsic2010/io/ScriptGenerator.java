package hku.cs.seg.experiment.qsic2010.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class ScriptGenerator {
	
	public static final String Dir_Base = "../../";
	public static final String Dir_Mutants = Dir_Base + "mutants";
	public static final String Dir_GoldenVersion = Dir_Base + "goldenversion";
	public static final String Dir_TestPool = Dir_Base + "testpool";
	public static final String Dir_TestResult = Dir_Base + "testresult";
	public static final String Dir_TestOutput = Dir_Base + "testoutput";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
//			String listFilename = "C:\\Jack\\workspace\\qsic2010_pri\\mutants\\mutants.list";
//			String listFilename = "C:\\Jack\\workspace\\qsic2010_pri\\mutants\\mutants_fr.list";
			String listFilename = "C:\\Jack\\workspace\\qsic2010_pri\\mutants\\mutants_abt1000_c40.list";
			String scriptPath = "C:\\Jack\\workspace\\qsic2010_pri\\mutants\\scripts\\";
			
			int con = Integer.parseInt(args[0]);
			String testpoolIdentity = args[1];
			String testpoolName = Dir_TestPool + "/" + testpoolIdentity + ".pool";
			int fromMutant = Integer.parseInt(args[2]);
			int toMutant = Integer.parseInt(args[3]);
			int timeoutTolerance = Integer.parseInt(args[4]);
			
			ArrayList<String> mutantIds = new ArrayList<String>();
			ArrayList<String> mutantPackages = new ArrayList<String>();
			int nMutant = 0;
				BufferedReader br = new BufferedReader(new FileReader(listFilename));
				String str = null;
				while((str = br.readLine())!= null){
					if (str.trim().equals("")) break;
					String[] strs = str.split("\t");
					mutantIds.add(strs[0]);
					mutantPackages.add(strs[3].substring(0, strs[3].lastIndexOf(".")).replace('.', '/'));
					nMutant++;
				}
			if (toMutant > nMutant) toMutant = nMutant;
			if (fromMutant > toMutant) fromMutant = toMutant;
				
			BufferedWriter bwAll = new BufferedWriter(new FileWriter(scriptPath + "runcon.sh"));
			bwAll.write("echo Sync...\n\n");
			for (int i = 0; i < con; i++) {
				String conDir = "con" + (i + 1);
				bwAll.write("# Sync " + conDir + "\n");
				bwAll.write("\\rm -rf " + conDir + "\n");
				bwAll.write("\\mkdir " + conDir + "\n");
				bwAll.write("\\cp -rf " + Dir_GoldenVersion + "/* " + conDir + "\n\n");
			}
			bwAll.write("# Sync completed. \n\n");
			
			int nPerCon = (toMutant - fromMutant + con) / con;
			int mutantIndex = fromMutant;
			for (int i = 0; i < con && mutantIndex <= toMutant; i++) {
				if (i == (toMutant - fromMutant) % con + 1) {
					nPerCon --;
				}
				String conDir = "con" + (i + 1); 
				
				String filename = "run" + (mutantIndex) + "-" + (mutantIndex + nPerCon > nMutant ? nMutant : mutantIndex + nPerCon - 1) + ".sh";
				bwAll.write("./" + filename + " &\n");
				
				BufferedWriter bwPerCon = new BufferedWriter(new FileWriter(scriptPath + filename));
				for (int j = 0; j < nPerCon && mutantIndex <= toMutant; j++) {
					String mutantId = mutantIds.get(mutantIndex - 1);
					String mutantPackage = mutantPackages.get(mutantIndex - 1); 
//					bwPerCon.write("echo Testing mutant " + mutantIndex + "-" + conDir + "\n");
					bwPerCon.write("\\cp -f " + Dir_Mutants + "/" + mutantId + "/*.class " + conDir + "/" + mutantPackage + "\n");
//					bwPerCon.write("java -Xmx2560m -classpath $CLASSPATH:" + conDir + " hku.cs.seg.experiment.qsic2010.TestLauncher " + mutantId + " " + timeoutTolerance + " " + testpoolName + " " + Dir_TestResult + "/" + testpoolIdentity + "/" + mutantId + ".log\n");
					bwPerCon.write("java -classpath $CLASSPATH:" + conDir + " hku.cs.seg.experiment.qsic2010.OutputGetter " + mutantId + " " + testpoolName + " " + Dir_TestOutput + "/" + testpoolIdentity + "/" + mutantId + ".out\n");
					bwPerCon.write("\\cp -f " + Dir_GoldenVersion + "/" + mutantPackage + "/*.class " + conDir + "/" + mutantPackage + "\n");
					bwPerCon.write("echo Mutant " + mutantIndex  + "-" + conDir + " tested. \n\n");
					mutantIndex++;
				}
				bwPerCon.close();
			}
			
//			bwAll.write("./kill_overTimeProc.sh\n");
			bwAll.close();			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
