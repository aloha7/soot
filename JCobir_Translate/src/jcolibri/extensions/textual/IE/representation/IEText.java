/**
 * IEText.java
 * jCOLIBRI2 framework. 
 * @author Juan A. Recio-García.
 * GAIA - Group for Artificial Intelligence Applications
 * http://gaia.fdi.ucm.es
 * 15/06/2007
 */
package jcolibri.extensions.textual.IE.representation;

import java.util.ArrayList;
import java.util.List;

import jcolibri.datatypes.Text;
import jcolibri.extensions.textual.IE.representation.info.FeatureInfo;
import jcolibri.extensions.textual.IE.representation.info.PhraseInfo;

/**
 * Represents a Textual attribute that will be processed to extract information.
 * A text is composed by paragraphs, paragraphs by sentences and sentences by tokens:
 * <p><center><img src="IETextRepresentation.jpg"/></center></p>
 * This organization is created by a specific method.
 * <br>
 * This object stores a list of paragraphs in the order they appear in the text.
 * <br>
 * This class also stores the extracted information:
 * <ul>
 * <li>Phrases identified in the text.
 * <li>Features: identifier-value pairs extracted from the text.
 * <li>Topics: combining phrases and features a topic can be associated to a text. A topic is a classification of the text.
 * </ul>
 * 
 * @author Juan A. Recio Garcia
 * @version 1.0
 * @see jcolibri.extensions.textual.IE.representation.Paragraph
 * @see jcolibri.extensions.textual.IE.representation.Sentence
 * @see jcolibri.extensions.textual.IE.representation.Token
 */
public class IEText extends Text
{

    protected List paragraphs;

    protected List phrases;

    protected List features;

    protected List topics;

    /**
     * Creates an empty IEText
     */
    public IEText()
    {
	paragraphs = new ArrayList();
	phrases = new ArrayList();
	features = new ArrayList();
	topics = new ArrayList();
    }

    /**
     * Creates an IEText from a String
     * @param content
     */
    public IEText(String content)
    {
	super(content);
	paragraphs = new ArrayList();
	phrases = new ArrayList();
	features = new ArrayList();
	topics = new ArrayList();
    }

    /**
     * Returns the original text of this IEText object
     */
    public String getRAWContent()
    {
	return rawContent;
    }

    /**
     * Returns the annotations extracted in this text
     */
    public String printAnnotations()
    {
	StringBuffer sb = new StringBuffer();
	for (Object on : paragraphs){
		Paragraph par = (Paragraph)on;
		sb.append(par.toString());
	}
	    
	return sb.toString() + "\nPHRASES: " + phrases.toString() + "\nFEATURES: " + features.toString();
    }

    /**
     * Returns the features
     */
    public List getFeatures()
    {
	return features;
    }

    /**
     * Adds features
     */
    public void addFeatures(List features)
    {
	features.addAll(features);
    }

    /**
     * Adds a feature
     */
    public void addFeature(FeatureInfo feature)
    {
	features.add(feature);
    }

    /**
     * Returns the paragraphs
     */
    public List getParagraphs()
    {
	return paragraphs;
    }

    /**
     * Adds paragraphs
     */
    public void addParagraphs(List paragraphs)
    {
	this.paragraphs.addAll(paragraphs);
    }
    
    /**
     * Adds a paragraph
     */
    public void addParagraph(Paragraph paragraph)
    {
	this.paragraphs.add(paragraph);
    }

    /**
     * Returns the phrases
     */
    public List getPhrases()
    {
	return phrases;
    }

    /**
     * Adds phrases
     */
    public void addPhrases(List phrases)
    {
	this.phrases.addAll(phrases);
    }

    /**
     * Adds a phrase
     */
    public void addPhrase(PhraseInfo phrase)
    {
	this.phrases.add(phrase);
    }

    /**
     * Returns the topcis
     */
    public List getTopics()
    {
	return topics;
    }

    /***
     * Adds topics
     */
    public void addTopics(List topics)
    {
	this.topics.addAll(topics);
    }

    /**
     * Adds a topic
     */
    public void addTopic(String topics)
    {
	this.topics.add(topics);
    }

    /**
     * Returns all the sentences of this texts iterating over all paragraphs
     */
    public List getAllSentences()
    {
	List sentences = new ArrayList();
	for (Object on : paragraphs){
		Paragraph p = (Paragraph)on;
		sentences.addAll(p.getSentences());
	}
	    
	return sentences;
    }

    /**
     * Returs all the tokens of this texts iterating over all paragraphs and their contained sentences.
     */
    public List getAllTokens()
    {
	List tokens = new ArrayList();
	for (Object on : paragraphs){
		Paragraph p = (Paragraph)on;
		for (Object om : p.getSentences()){
			Sentence s = (Sentence)om;
			tokens.addAll((List)s.getTokens());
		}
			
	}
	    
	return tokens;
    }

}
