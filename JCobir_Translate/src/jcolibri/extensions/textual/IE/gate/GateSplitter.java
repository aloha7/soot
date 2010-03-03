/**
 * GateSplitter.java
 * jCOLIBRI2 framework. 
 * @author Juan A. Recio-García.
 * GAIA - Group for Artificial Intelligence Applications
 * http://gaia.fdi.ucm.es
 * 19/06/2007
 */
package jcolibri.extensions.textual.IE.gate;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.GateConstants;
import gate.creole.splitter.SentenceSplitter;
import gate.creole.tokeniser.DefaultTokeniser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jcolibri.cbrcore.Attribute;
import jcolibri.cbrcore.CBRCase;
import jcolibri.cbrcore.CBRQuery;
import jcolibri.extensions.textual.IE.IEutils;
import jcolibri.extensions.textual.IE.representation.IEText;
import jcolibri.extensions.textual.IE.representation.Paragraph;
import jcolibri.extensions.textual.IE.representation.Sentence;
import jcolibri.extensions.textual.IE.representation.Token;
import jcolibri.util.AttributeUtils;
import jcolibri.util.ProgressController;

/**
 * Organizes an IETextGate object in paragraphs, sentences and tokens.
 * This implementation uses the GATE algorithms to obtain sentences and tokens.
 * @author Juan A. Recio-Garcia
 * @version 1.0
 */
public class GateSplitter
{
    /**
     * Performs the algorithm in the given attributes of a collection of cases.
     * These attributes must be IETextGate objects.
     */
    public static void split(Collection cases, Collection attributes)
    {
	org.apache.commons.logging.LogFactory.getLog(GateSplitter.class).info("Splitting Gate text.");
	ProgressController.init(GateSplitter.class, "Splitting Gate text", cases.size());
	for(Object on: cases)
	{
		CBRCase c = (CBRCase)on;
	    for(Object om: attributes)
	    {
	    	Attribute a = (Attribute)om;
		Object o = AttributeUtils.findValue(a, c);
		if(o instanceof IETextGate)
		    split((IETextGate)o);
	    }
	    ProgressController.step(GateSplitter.class);
	}
	ProgressController.finish(GateSplitter.class);
    }

    /**
     * Performs the algorithm in the given attributes of a query.
     * These attributes must be IETextGate objects.
     */
    public static void split(CBRQuery query, Collection attributes)
    {
	    org.apache.commons.logging.LogFactory.getLog(GateSplitter.class).info("Splitting Gate text.");
	    for(Object om: attributes)
	    {
	    	Attribute a = (Attribute)om;
		Object o = AttributeUtils.findValue(a, query);
		if(o instanceof IETextGate)
		    split((IETextGate)o);
	    }
    }
    
    public static void split(Collection cases)
    {
	org.apache.commons.logging.LogFactory.getLog(GateSplitter.class).info("Splitting Gate text.");
	ProgressController.init(GateSplitter.class, "Splitting Gate text", cases.size());
	for(Object om: cases)
	{
		CBRCase c = (CBRCase)om;
	    Collection texts = IEutils.getTexts(c);
	    for(Object on : texts){
	    	IEText t = (IEText)on;
	    	if(t instanceof IETextGate)
			    split((IETextGate)t);
	    }
		
	    ProgressController.step(GateSplitter.class);
	}
	ProgressController.finish(GateSplitter.class);
    }
    
    /**
     * Performs the algorithm in all the IETextGate typed attributes of a collection of cases.
     */ 
    public static void split(CBRQuery query)
    {	    
	org.apache.commons.logging.LogFactory.getLog(GateSplitter.class).info("Splitting Gate text.");
	Collection texts = IEutils.getTexts(query);
        for(Object om : texts){
        	IEText t = (IEText)om;
        	if(t instanceof IETextGate)
            	split((IETextGate)t);
        }
            
    }
    
    /**
     * Performs the algorithm in all the IETextGate typed attributes of a query.
     */
    public static void split(IETextGate text)
    {
	try
	{
	    DefaultTokeniser tokeniser = getTokeniser();
	    tokeniser.setDocument(text.getDocument());
	    tokeniser.execute();
	    
	    SentenceSplitter sentenceSplitter = getSentenceSplitter();
	    sentenceSplitter.setDocument(text.getDocument());
	    sentenceSplitter.execute();
	    
	    organizeText(text);
	    
	} catch (Exception e)
	{
	    org.apache.commons.logging.LogFactory.getLog(GateSplitter.class).error(e);   
	}
    }
    
    
    /**
     * Performs the algorithm in a given IETextGate object
     */
    protected static void organizeText(IETextGate text)
    {
	Document doc = text.getDocument();
	String content = text.getRAWContent();
	
	//Paragraphs
	AnnotationSet parAnnot = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);
	AnnotationSet paragraphs = parAnnot.get("paragraph");
	AnnotationSet annot = doc.getAnnotations();

	List sents = new ArrayList(annot.get("Sentence"));
	java.util.Collections.sort(sents);
	
	List tokens = new ArrayList(annot.get("Token"));
	java.util.Collections.sort(tokens);
	
	ArrayList sentsToRemove = new ArrayList();
	ArrayList tokensToRemove = new ArrayList();
	for(int p=0; p<paragraphs.size(); p++)
	{
	    Annotation par = (Annotation)paragraphs.get(p);
	    int beginP = par.getStartNode().getOffset().intValue();
	    int endP   = par.getEndNode().getOffset().intValue();
	    Paragraph myPar = new Paragraph(content.substring(beginP,endP));
	    text.addParagraph(myPar);
	    text.setParagraphMapping(myPar, par);
	    
	    //Sentences
	    sentsToRemove.clear();
	    for(int s=0; s<sents.size(); s++)
	    {
		Annotation sent = (Annotation)sents.get(s);
		int beginS = sent.getStartNode().getOffset().intValue();
		int endS   = sent.getEndNode().getOffset().intValue();
		if((beginS<beginP)||(endS>endP))
		    continue;
		Sentence mySent = new Sentence(content.substring(beginS, endS));
		myPar.addSentence(mySent);
		text.setSentenceMapping(mySent, sent);
		sentsToRemove.remove(sent);
		
		
		//Tokens
		tokensToRemove.clear();
		for(int t=0; t<tokens.size(); t++)
		{
		    Annotation token = (Annotation)tokens.get(t);
		    int beginT = token.getStartNode().getOffset().intValue();
		    int endT   = token.getEndNode().getOffset().intValue();
		    if((beginT<beginS)||(endT>endS))
			continue;
		    Token myToken = new Token(content.substring(beginT,endT));
		    mySent.addToken(myToken);
		    text.setTokenMapping(myToken, token);
		    tokensToRemove.add(token);
		}
		tokens.removeAll(tokensToRemove);
	    }
	    sents.removeAll(sentsToRemove);
	    
	}
	
	    
    }
    
    private static DefaultTokeniser tokeniser = null;
    private static DefaultTokeniser getTokeniser() throws Exception
    {
	if(tokeniser == null)
	{
	    tokeniser = (DefaultTokeniser) Factory.createResource(
	 			  "gate.creole.tokeniser.DefaultTokeniser");
	    tokeniser.init();
	}
	return tokeniser;
    }
    
    private static  SentenceSplitter sentenceSplitter = null;
    private static SentenceSplitter getSentenceSplitter() throws Exception{
	
	if(sentenceSplitter == null)
	{
	    sentenceSplitter = (SentenceSplitter)Factory.createResource(
		    	"gate.creole.splitter.SentenceSplitter");
	    sentenceSplitter.init();
	}
	return sentenceSplitter;
    }
 
    
}
