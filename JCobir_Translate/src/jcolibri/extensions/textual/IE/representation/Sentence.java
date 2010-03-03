/**
 * Sentence.java
 * jCOLIBRI2 framework. 
 * @author Juan A. Recio-García.
 * GAIA - Group for Artificial Intelligence Applications
 * http://gaia.fdi.ucm.es
 * 15/06/2007
 */
package jcolibri.extensions.textual.IE.representation;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sentence of the text. A sentence is composed by Tokens.
 * @author Juan A. Recio-Garcia
 * @version 1.0
 * @see jcolibri.extensions.textual.IE.representation.Sentence
 */
public class Sentence {

	protected  List tokens;
	
	protected String text;
	
	/**
	 * Creates a sentence object that representes the text of the parameter.
	 */
	public Sentence(String text)
	{
		this.text = text;
		tokens = new ArrayList();
	}
	
	/**
	 * Returns the original text of the sentence
	 */
	public String getRawContent()
	{
		return text;
	}


	/**
	 * Returns the tokens
	 */
	public List getTokens() {
		return tokens;
	}
	
	/**
	 * Adds tokens
	 */
	public void addTokens(List tokens){
		this.tokens.addAll(tokens);
	}
	
	/**
	 * Adds a token
	 */
	public void addToken(Token token){
		this.tokens.add(token);
	}
	
	/**
	 * Prints the content and annotations.
	 */
	public String toString()
	{
	    StringBuffer sb = new StringBuffer();
	    sb.append("  SENTENCE begin\n");
	    for(Object on: tokens){
	    	Token tok = (Token)on;
	    	sb.append(tok.toString());
	    }
		
	    sb.append("  SENTENCE end\n");
	    return sb.toString();
	}
	
	
}
