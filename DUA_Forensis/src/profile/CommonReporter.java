package profile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

public class CommonReporter {

	/** Appends new coverage (or count) row to entity coverage matrix output file */
	public static void reportToCovMatrixFile(BitSet duaCov, int numDUAs, String filenameSuffix) {
		// translate to int array
		int[] intCovArray = new int[numDUAs];
		for (int i = 0; i < numDUAs; ++i)
			intCovArray[i] = duaCov.get(i)? 1 : 0;
		
		reportToCovMatrixFile(intCovArray, filenameSuffix);
	}
	
	/** Appends new coverage row to entity coverage matrix output file */
	public static void reportToCovMatrixFile(int[] countCovArray, String filenameSuffix) {
		try {
			// Open out file for appending
			File fCovMatrix = new File("exereport.out" + ((filenameSuffix.length() > 0)? "." + filenameSuffix : ""));
			BufferedWriter writer = new BufferedWriter(new FileWriter(fCovMatrix, true));
			
			// Write coverage or count; id is implicit in left-to-right ordering
			for (int i = 0; i < countCovArray.length; ++i) {
				writer.write(
						Integer.toString(countCovArray[i]) +
						((i < countCovArray.length - 1)? " " : "\n")); // add space after entity id, or carriage return if last entity
			}
			
			writer.flush();
			writer.close();
		}
		catch (FileNotFoundException e) { System.err.println("Couldn't write to entity coverage matrix file: " + e); }
		catch (SecurityException e) { System.err.println("Couldn't write to entity coverage matrix file: " + e); }
		catch (IOException e) { System.err.println("Couldn't write to entity coverage matrix file: " + e); }
	}
	
	/** Returns an array of the space-separated integers found in the string */
	public static int[] parseIds(String s) {
		// create array list dynamically with parsed id integer objects
		ArrayList idsList = new ArrayList();
		int start = 0;
		int end;
		while ((end = s.indexOf(' ', start)) != -1) {
			idsList.add(Integer.valueOf(s.substring(start, end)));
			start = end + 1;
		}
		if (start < s.length())
			idsList.add(Integer.valueOf(s.substring(start)));
		
		// transform to native int array
		int[] ids = new int[idsList.size()];
		int idIdx = 0;
		for (Iterator it = idsList.iterator(); it.hasNext(); ++idIdx)
			ids[idIdx] = ((Integer) it.next()).intValue();
		
		return ids;
	}
	
}
