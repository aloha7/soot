package ccr.stat;

import ccr.app.*;

import java.io.*;
import java.util.*;

public class SourceHandler {
	
	private String sourceFile;
	private final Vector statements;
	private final HashMap resolutions;
	
	public SourceHandler(String filename) {
		
		sourceFile = filename;
		statements = getStatements(sourceFile);
		resolutions = getResolutions(sourceFile);
	}
	
	public String getContent(String tag) {
		
		return getContent(sourceFile, tag);
	}
	
	public HashSet getSet(String tag) {
		
		return getSet(sourceFile, tag);
	}
	
	public Statement getStatement(int i) {
		
		return (Statement) statements.get(i);
	}
	
	public int sizeStatements() {
		
		return statements.size();
	}
	
	public Resolution getResolution(String name) {
		
		return (Resolution) resolutions.get(name);
	}
	
	public int sizeResolutions() {
		
		return resolutions.size();
	}
	
	public static String getContent(String filename, String tag) {
		
		String line = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			line = br.readLine();
			while (line != null) {
				if (line.indexOf(tag) != -1) {
					break;
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		if (line == null || line.indexOf(tag) == -1) {
			return "";
		}
		int i = line.indexOf(Application.SET_PREFIX, line.indexOf(tag));
		int j = line.indexOf(Application.SET_POSTFIX, i);
		return line.substring(i + Application.SET_PREFIX.length(), j);
	}
	
	public static HashSet getSet(String filename, String tag) {
		
		HashSet set = new HashSet();
		StringTokenizer st = new StringTokenizer(getContent(filename, tag), Application.SET_DELIMITER);
		while (st.hasMoreTokens()) {
			set.add(st.nextToken());
		}
		return set;
	}
	
	public static Vector getStatements(String filename) { 
		
		String source = extractSource(filename); //delete all blanks and comments start with "//" 
		
		Vector tokens = extractTokens(new StringBuffer(source));//treat source code as a string sequence consists of defined keywords and variables 
																//2008/7/25: add a keyword set, we can get all user-defined variable, we hope to identify context variables automatically
		Vector statements = new Vector();
		
		Statement entry = new Statement();
		entry.add("// " + Application.ENTRY_TAG);
		statements.add(entry);
		
		for (int i = 0; i < tokens.size(); i++) {
			Statement statement = new Statement(); //"if((a+b)!=1)", "while((a+b)!=2)", "i++;" are statements
			String token = (String) tokens.get(i);
			if (token.equals("{") || token.equals("}") || token.equals("else")) {
				statement.add(token); //"{","}", "else" are statements 
			} else if (token.equals("if") || token.equals("while")){ // a "if" and "while" statement
				statement.add(token);
				i++;
				token = (String) tokens.get(i);
				if (!token.equals("(")) { // the next token must be "("
					System.out.println("Unmatchable " + token);
					System.exit(1);
				}
				statement.add(token);
				int depth = 1;
				for (i = i + 1; i < tokens.size(); i++) {
					token = (String) tokens.get(i);
					statement.add(token);
					if (token.equals("(")) {
						depth++;
					} else if (token.equals(")")) {
						depth--;
					}
					if (depth == 0) {
						break;
					}
				}
			} else {
				for (; i < tokens.size(); i++) { // a normal statement;
					token = (String) tokens.get(i);
					statement.add(token);
					if (token.equals(";")) {
						break;
					}
				}
			}
			statements.add(statement);
		}
		
		Statement exit = new Statement();
		exit.add("// " + Application.EXIT_TAG);
		statements.add(exit);
		
		normalizeStatements(statements);
		
		HashSet variables = getSet(filename, Application.VARIABLE_TAG);
		HashSet contexts = getSet(filename, Application.CONTEXT_TAG);
		HashSet assignments = getSet(filename, Application.ASSIGNMENT_TAG);
		for (int i = 0; i < statements.size(); i++) {
			Statement statement = (Statement) statements.get(i);
			statement.analyzeDefUse(variables, contexts, assignments);
		}
		
		return statements;
	}
	
	/**2008/7/25: delete all comments
	 * 
	 * @param filename
	 * @return
	 */
	private static String extractSource(String filename) { 
		
		StringBuffer source = new StringBuffer();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			boolean inApp = false;
			while (line != null) {
				if (line.indexOf(Application.ENTRY_TAG) != -1) {
					inApp = true;
				}
				if (inApp) {
					int i = line.indexOf("//"); 
					if (i != -1) {
						source.append(removeBlank(line.substring(0, i)));
					} else {
						source.append(removeBlank(line));
					}
				}
				if (line.indexOf(Application.EXIT_TAG) != -1) {
					inApp = false;
					break;
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		
		for (int i = source.indexOf("/*"); i != -1; i = source.indexOf("/*")) {
			int j = source.indexOf("*/", i + 2);
			if (i < j) {
				source.delete(i, j + 2);
			}
		}
		return source.toString();
	}
	
	private static Vector extractTokens(StringBuffer source) {
		
		Vector tokens = new Vector();
		HashSet unaryDelim = new HashSet();
		HashSet binaryDelim = new HashSet();
		for (int i = 0; i < Application.UNARY_DELIMITER.length; i++) {
			unaryDelim.add(Application.UNARY_DELIMITER[i]);
		}
		for (int i = 0; i < Application.BINARY_DELIMITER.length; i++) {
			binaryDelim.add(Application.BINARY_DELIMITER[i]);
		}
		
		while (source.length() > 0) {
			String prefix = source.substring(0, 1);
			if (prefix.equals(" ") || prefix.equals("\t")) {
				source.delete(0, 1);
				continue;
			}
			if (source.length() > 1) {
				prefix = source.substring(0, 2);
				if (binaryDelim.contains(prefix)) {
					tokens.add(prefix); //adds elements in binaryDelim 
					source.delete(0, 2);
					continue;
				}
			}
			prefix = source.substring(0, 1); //match too much, so match it a little
			if (prefix.equals("\"") || prefix.equals("'")) { //String or char
				int i = source.indexOf(prefix, 1);
				if (i == -1) {
					System.out.println("Unmatchable " + prefix);
					System.exit(1);
				} else {
					tokens.add(source.substring(0, i + 1)); 
					source.delete(0, i + 1);
				}
			} else if (unaryDelim.contains(prefix)) {
				tokens.add(prefix);  //add elements in unaryDelim
				source.delete(0, 1);
			} else {
				int i = 1;
				while (i < source.length()) {
					if (unaryDelim.contains(source.substring(i, i + 1))) { //variable must be identified before unaryDelim
						break;
					}
					i++;
				}
				tokens.add(source.substring(0, i)); // add a variable
				source.delete(0, i);
			}
		}
		return tokens;
	}
	
	private static void normalizeStatements(Vector statements) {
		
		for (int i = 0; i < statements.size(); i++) {
			Statement statement = (Statement) statements.get(i);
			if (statement.prefix().equals("if") || statement.prefix().equals("while")) {
				i = match(statements, i, statement.prefix());
			}
		}
	}
	
	private static int match(Vector statements, int start, String token) {
		
		int i = start + 1;
		Statement statement = (Statement) statements.get(i);
		if (statement.prefix().equals("{")) {
			for (i = i + 1; i < statements.size(); i++) {
				Statement statement1 = (Statement) statements.get(i);
				if (statement1.prefix().equals("if") || statement1.prefix().equals("while")) {
					i = match(statements, i, statement1.prefix());
				} else if (statement1.prefix().equals("}")) {
					if (i + 1 < statements.size() && token.equals("if") && 
							((Statement) statements.get(i + 1)).prefix().equals("else")) {
						return match(statements, i + 1, "else");
					} else {
						return i;
					}
				}
			}
		} else if (statement.prefix().equals("if") || statement.prefix().equals("while")) {
			int j = match(statements, i, statement.prefix());
			statements.add(i, new Statement("{"));
			j = j + 2;
			statements.add(j, new Statement("}"));
			if (j + 1 < statements.size() && token.equals("if") && 
					((Statement) statements.get(j + 1)).prefix().equals("else")) {
				return match(statements, j + 1, "else");
			} else {
				return j;
			}
		} else {
			statements.add(i, new Statement("{"));
			i = i + 2;
			statements.add(i, new Statement("}"));
			if (i + 1 < statements.size() && token.equals("if") && 
					((Statement) statements.get(i + 1)).prefix().equals("else")) {
				return match(statements, i + 1, "else");
			} else {
				return i;
			}
		}
		return i;
	}
	
	private static String removeBlank(String line) {
		
		if (line.length() > 0 && (line.charAt(0) == '\t' || line.charAt(0) == ' ')) {
			return removeBlank(line.substring(1));
		}
		if (line.length() > 0 && 
				(line.charAt(line.length() - 1) == '\t' || 
						line.charAt(line.length() - 1) == ' ')) {
			return removeBlank(line.substring(0, line.length() - 1));
		}
		return line;
	}
	
	public static HashMap getResolutions(String filename) {
		
		HashMap resolutions = new HashMap();
		int index = 0;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();
			while (line != null) {
				if (line.indexOf(Application.POLICY_TAG) != -1) {
					int i = line.indexOf(
							Application.SET_PREFIX, 
							line.indexOf(Application.POLICY_CONTEXT_TAG));
					int j = line.indexOf(Application.SET_POSTFIX, i);
					String context = line.substring(i + Application.SET_PREFIX.length(), j);
					i = line.indexOf(
							Application.SET_PREFIX, line.indexOf(Application.CONSTRAINT_TAG));
					j = line.indexOf(Application.SET_POSTFIX, i);
					String constraint = line.substring(
							i + Application.SET_PREFIX.length(), j);
					i = line.indexOf(
							Application.SET_PREFIX, line.indexOf(Application.SOLUTION_TAG));
					j = line.indexOf(Application.SET_POSTFIX, i);
					String solution = line.substring(i + Application.SET_PREFIX.length(), j);
					Policy policy = new Policy(
							context, Application.POLICY_INDEX_PREFIX + (index++), 
							constraint, solution);
					if (resolutions.containsKey(context)) {
						Resolution resolution = (Resolution) resolutions.get(context);
						resolution.add(policy);
					} else {
						Resolution resolution = new Resolution(context);
						resolution.add(policy);
						resolutions.put(context, resolution);
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		
		return resolutions;
	}
	
/*	private static String[] toStringArray(Vector elements) {
		
		String result[] = new String[elements.size()];
		for (int i = 0; i < elements.size(); i++) {
			result[i] = (String) elements.get(i);
		}
		return result;
	}*/
	
	public static void main(String argv[]){
		
		Vector statements = getStatements("src/ccr/app/TestCFG1.java");
		for (int i = 0; i < statements.size(); i++) {
			System.out.println(statements.get(i));
		}
	}

}
