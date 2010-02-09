package profile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PathReporter {
	public static void __link() {}
	
	public PathReporter() {}
	
	/** Inspects main class to retrieve values of static field arrays with BL path cov info */
	public void reportPaths(Class mainCls) {
		System.out.println("PATH COVERAGE:");
		
		int totalPaths = 0;
		int totalCovered = 0;
		List pathCov = new ArrayList();
		
		// traverse all fields in main class
		Field[] mainFields = mainCls.getFields();
		for (int fldIdx = 0; fldIdx < mainFields.length; ++fldIdx) {
			// inspect field
			Field f = mainFields[fldIdx];
			String name = f.getName();
			if (name.startsWith("<p_")) {
				// found path array; retrieve array id (id represents method at which path starts)
				final int idEndPos = name.lastIndexOf('>');
				final String pathArrIdStr = name.substring("<p_".length(), idEndPos);
				final int pathArrId = Integer.valueOf(pathArrIdStr).intValue();
				
				// get access to cov array, of type int[]
				try {
					final int[] pathCovArr = (int[]) f.get(null); // base is null, since it has to be a static field
					
					// report immediately
					System.out.print(" " + pathArrId + ": ");
//					String strCov = "";
					int covered = 0;
					for (int i = 0; i < pathCovArr.length; ++i) {
//						strCov += pathCovArr[i] + " ";
						if (pathCovArr[i] > 0) {
							++covered;
							pathCov.add(new Integer(1));
						}
						else
							pathCov.add(new Integer(0));
					}
					System.out.println(covered + "/" + pathCovArr.length); // + "  " + strCov);
					
					totalPaths += pathCovArr.length;
					totalCovered += covered;
				}
				catch (IllegalArgumentException e) { System.out.println("SERIOUS PROBLEM: " + e); }
		        catch (IllegalAccessException e) { System.out.println("SERIOUS PROBLEM: " + e); }
			}
		}
		
		System.out.println(" PATH SUMMARY: " + totalCovered + "/" + totalPaths);
		
		// output coverage row to file
		int[] pathCovArr = new int[pathCov.size()];
		int pId = 0;
		for (Iterator it = pathCov.iterator(); it.hasNext(); )
			pathCovArr[pId++] = ((Integer)it.next()).intValue();
		CommonReporter.reportToCovMatrixFile(pathCovArr, "path");
	}
	
}
