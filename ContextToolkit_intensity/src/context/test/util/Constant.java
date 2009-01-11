package context.test.util;
import java.io.*;

public class Constant {
	
	public static String SUB_SEPERATOR = ",";
	
	public static String SEPERATOR ="	";
	
	public static String LINESHIFTER =	"\n";
	
	public static String FILESEPERATOR = File.separator;
	
	public static String baseFolder = System.getProperty("user.dir") + FILESEPERATOR;
	
	public static String mutantFile = baseFolder + "test" + FILESEPERATOR + "FaultyVersion.txt";

	
}
