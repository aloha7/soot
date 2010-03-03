package hku.cs.seg.experiment.qsic2010.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class CrossLevelEquivalentSetCounter {
	
	private static HashMap<String, Integer> mutSet;
	
	private static void ReadAndCategorize(String levelFile, int type) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(levelFile));
		String str = null;
		while ((str = br.readLine()) != null) {
			if (str.equals("")) break;				
			mutSet.put(str, type);
		}
		br.close();
	}

	public static void main(String[] args) {
		String path = "C:\\Jack\\workspace\\QSIC2010\\Distribution_equivalentMutants";
		String appLvl = path + "\\NonEquivalentFaults_applicationLevel.txt";
		String midLvl = path + "\\NonEquivalentFaults_middlewareLevel.txt";
		String intLvl = path + "\\NonEquivalentFaults_interfaceLevel.txt";
		
		String equSet = path + "\\EquivalentFaults.txt";
		
		mutSet = new HashMap<String, Integer>();
		int[][] count = new int[3][3];
		
		int app_mid = 0, app_int = 0, mid_int = 0;
		int pairCount = 0;
		
		try {
			ReadAndCategorize(appLvl, 0);
			ReadAndCategorize(midLvl, 1);
			ReadAndCategorize(intLvl, 2);
			
			BufferedReader br = new BufferedReader(new FileReader(equSet));
			String str = null;

			while ((str = br.readLine()) != null) {
				if (str.equals("")) break;
				String[] strs = str.split("\t");
				for (int i = 0; i < strs.length - 1; i++) {
					int type1 = mutSet.get(strs[i]);
					for (int j = i + 1; j < strs.length; j++) {
						int type2 = mutSet.get(strs[j]);
						count[type1][type2]++;
						pairCount++;
						if (type1 != type2)
							count[type2][type1]++;
					}
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("\tapp\tmid\tint");
		for (int i = 0; i < 3; i++) {
			if (i == 0) System.out.print("app");
			else if (i== 1) System.out.print("mid");
			else System.out.print("int");
			for (int j = 0; j < 3; j++) {
				System.out.print("\t" + count[i][j]);
			}
			System.out.println();
		}
		
		System.out.println("\nPair count = " + pairCount + "\n");
	}

}
