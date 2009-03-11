package ccr.app;

import ccr.test.*;

public abstract class Application {
	
	public static final String ENTRY_TAG = "ENTRY";
	public static final String EXIT_TAG = "EXIT";
	public static final String PROGRAM_ID_TAG = "Program ID";
	public static final String VARIABLE_TAG = "Ordinary Variable";
	public static final String CONTEXT_TAG = "Context Variable";
	public static final String ASSIGNMENT_TAG = "Assignment";
	public static final String SET_PREFIX = "[";
	public static final String SET_POSTFIX = "]";
	public static final String SET_DELIMITER = ", ";
	
	public static final String POLICY_TAG = "Policy";
	public static final String POLICY_CONTEXT_TAG = "Context";
	public static final String CONSTRAINT_TAG = "Constraint";
	public static final String SOLUTION_TAG = "Solution";
	public static final String DISCARD_SOLUTION = "discard";
	public static final String POLICY_NODE_DELIMITER = ":";
	public static final String POLICY_INDEX_PREFIX = "P";
	
	public static final String RECORD_METHOD = "record";
	public static final String CONTEXT_DEFINE_INDEX = "updateIndex";
	public static final String CONTEXT_DEFINE_TAG = "Context definition";
	
	public static final String UNARY_DELIMITER[] = {" ", "\t", "\"", "'", 
		"{", "}", "(", ")", ";", ",", ".", ":", "?", 
		"+", "-", "*", "/", ">", "<", "&", "|", "!", "="};
	public static final String BINARY_DELIMITER[] = {"++", "--", 
		"+=", "-=", "*=", "/=", ">=", "<=", "&&", "||", "!=", "=="};
	
	protected String updateIndex;
	
	abstract public Object application(String testcase);
	
	abstract protected void resolve();
	
	protected void record(String index) {
		
		Trace.getInstance().add(index);
	}

}
