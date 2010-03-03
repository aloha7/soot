/* jaDTi package - v0.6.1 */

/*
 *  Copyright (c) 2004, Jean-Marc Francois.
 *
 *  This file is part of jaDTi.
 *  jaDTi is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  jaDTi is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jahmm; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 */

package be.ac.ulg.montefiore.run.jadti.io;

import be.ac.ulg.montefiore.run.jadti.*;
import java.io.*;
import java.util.*;


/**
 * This class reads a database file. <p>
 * The database format is very simple. The first line is the name of the 
 * database. The second line contains the attribute names immediately followed
 * by their types: <tt>numerical</tt> or <tt>symbolic</tt> or <tt>name</tt>
 * (see below). A numerical attribute can be real or integer (anyway it will be
 * converted to a double value). A symbolic attribute denotes any discrete
 * attribute. Its value can be symbols, numbers or both.<p>
 * Each line corresponds to one item and contains the values of the
 * attributes separated by white spaces. The special value '?' denotes an
 * unknown value.  Symbolic values can be represented by an integer.<p>
 * If the first attribute name is object and its type is name, then the first 
 * column is the object name (otherwise the name will be compute on the fly
 * based on the line number).<p>
 * Comment lines start with a ';'.<p>
 */
public class ItemSetReader {

    private static final char UNKNOWN_VALUE = '?';
    
    
    /* Holds a temporary symbolic attribute */
    static private class MutableAttribute {
	final String name;
	final Vector values;
	
	MutableAttribute(String name) {
	    this.name = name;
	    this.values = new Vector();
	}
	
	void add(String value) {
	    values.add(value);
	}
	
	/* Returns -1 if an object named 'name' is not found */
	int find(String name) {
	    return (values.indexOf(name));
	}
	
	int nbValues() {
	    return values.size();
	}

	SymbolicAttribute toSymbolicAttribute() {
	    return new IdSymbolicAttribute(name, values);
	}
    }
    
    
    /**
     * Reads a database file.
     * 
     * @param reader Holds a reader of the observation sequence file.
     */
    static public ItemSet read(Reader reader) 
	throws IOException, FileFormatException {

	return read(reader, null);
    }

    
    /**
     * Reads a database file.
     * 
     * @param reader Holds a reader of the observation sequence file.
     * @param attributeSet The attribute set associated to the database.
     *                     Can be <code>null</code> if unknown.  If set, the
     *                     database attribute line is read and matched with
     *                     this argument using attribute names.  If the database
     *                     file has extra attributes, they are discarded.
     */
    static public ItemSet read(Reader reader, AttributeSet attributeSet)
	throws IOException, FileFormatException {
	
	StreamTokenizer st = new StreamTokenizer(reader);
	int lineNumber = 1;
	boolean named = false;           /* Is first column the item's name ? */
	Vector attributes = null;
	Vector items = new Vector();

	initSyntaxTable(st);
	
	
	for (st.nextToken(); st.ttype != StreamTokenizer.TT_EOF; 
	     st.nextToken()) {
	    
	    if (st.ttype == StreamTokenizer.TT_EOL)
		continue;

	    st.pushBack();

	    if (lineNumber == 1)
		readFirstLine(st);          /* The database name is forgotten */
	    else if (lineNumber == 2) {
		attributes = readAttributesLine(st);
	    }
	    else
		items.add(readLine(st, named, attributes, lineNumber));
	    
	    lineNumber++;
	}
	
	ItemSet set;
	if (attributeSet == null)
	    set = buildItemSet(attributes, items);
	else
	    set = buildItemSet(attributes, attributeSet, items);

	return set;
    }
    

    /* Initialize the syntax table of a stream tokenizer */
    static private void initSyntaxTable(StreamTokenizer st) {
	st.resetSyntax();
	st.parseNumbers();
	st.whitespaceChars((int) ' ', (int) ' ');
	st.whitespaceChars((int) '\t', (int) '\t');
	st.wordChars('a', 'z');
	st.wordChars('A', 'Z');
	st.wordChars('_', '_');
	st.ordinaryChar(UNKNOWN_VALUE);
	st.eolIsSignificant(true);
	st.commentChar((int) ';');
    }

    
    static private String readFirstLine(StreamTokenizer st) 
	throws FileFormatException, IOException {
	String name;
		
	if (st.nextToken() != StreamTokenizer.TT_WORD)
	    throw new FileFormatException("Invalid database name" + st.ttype);
	
	name = st.sval;
	
	if (st.nextToken() != StreamTokenizer.TT_EOL)
	    throw new FileFormatException("First line must only hold one word" +
					  " (the database name)");
	
	return name;
    }

    
    static private Vector readAttributesLine(StreamTokenizer st) 
	throws FileFormatException, IOException {

	Vector attributes = new Vector();
	String name;
	
	for (st.nextToken(); st.ttype != StreamTokenizer.TT_EOL; 
	     st.nextToken()) {
	    st.pushBack();
	    
	    if (st.nextToken() != StreamTokenizer.TT_WORD)
		throw new FileFormatException("Invalid attribute name");
	    
	    name = st.sval;
	    
	    if (st.nextToken() != StreamTokenizer.TT_WORD)
		throw new FileFormatException("Attribute name expected");
	    
	    if (st.sval.equals("symbolic"))
		attributes.add(new MutableAttribute(name));
	    else if (st.sval.equals("numerical"))
		attributes.add(new NumericalAttribute(name));
	    else if (name.equals("object") && st.sval.equals("name") && 
		       attributes.size() == 0)
		attributes.add(new MutableAttribute("name"));
	    else
		throw new FileFormatException("Attributes must be followed by" +
					      " their type ('symbolic' or " +
					      "'numerical')");
	}
	
	if (attributes.size() == 0)
	    throw new FileFormatException("No attribute defined");
	
	return attributes;
    }

    
    static private AttributeValue[] readLine(StreamTokenizer st, boolean named,
					     Vector attributes, int lineNumber)
	throws FileFormatException, IOException {
	
	AttributeValue[] values = new AttributeValue[attributes.size()];
	int attributeNb = 0;
	
	
	for (st.nextToken(); st.ttype != StreamTokenizer.TT_EOL && 
		 attributeNb < attributes.size(); 
	     st.nextToken(), attributeNb++) {
	    
	    switch(st.ttype) {
	    case StreamTokenizer.TT_WORD:
		if (!(attributes.elementAt(attributeNb) instanceof
		      MutableAttribute))
		    throw new FileFormatException("Symbolic value " +
						  "expected");
		else {
		    MutableAttribute attribute =
			(MutableAttribute) attributes.elementAt(attributeNb);
		    
		    if (attribute.find(st.sval) == -1) {
			values[attributeNb] = 
			    new KnownSymbolicValue(attribute.nbValues());
			attribute.add(st.sval);
		    } else 
			values[attributeNb] =
			    new KnownSymbolicValue(attribute.find(st.sval));
		}
		break;
		
	    case StreamTokenizer.TT_NUMBER:
		if (!(attributes.elementAt(attributeNb) instanceof
		      NumericalAttribute)) {
		    MutableAttribute attribute =
			(MutableAttribute) attributes.elementAt(attributeNb);

		    if (st.nval != (double) ((int) st.nval))
			throw new FileFormatException("Symbolic values cannot" +
						      " be represented by a" +
						      "non integer number");
		    
		    String stringValue = "" + ((int) st.nval);
		    
		    if (attribute.find(stringValue) == -1) {
			values[attributeNb] = 
			    new KnownSymbolicValue(attribute.nbValues());
			attribute.add(stringValue);
		    } else 
			values[attributeNb] =
			    new KnownSymbolicValue(attribute.find(stringValue));
		} else
		    values[attributeNb] = new KnownNumericalValue(st.nval);
		
		break;
		
	    case UNKNOWN_VALUE:
		AttributeValue value;
		
		if (attributes.elementAt(attributeNb) instanceof
		    MutableAttribute)
		    value = new UnknownSymbolicValue();
		else
		    value = new UnknownNumericalValue();
		
		values[attributeNb] = value;
		break;
		
	    default:
		throw new FileFormatException("Word or number expected");
	    }
	}
	
	if (attributeNb != attributes.size() || 
	    st.ttype != StreamTokenizer.TT_EOL)
	    throw new FileFormatException("Bad number of attributes");
	
	return values;
    }
    
    
    /* Matches the attributes of 'set' with the attributes of 'vector' */
    static private int[] switchArray(AttributeSet set, Vector vector) {
	int[] switchArray = new int[set.size()];
	
	aLoop:
	for (int i = 0; i < switchArray.length; i++) {
	    Attribute attribute = set.attribute(i);
	    
	    for (int i2 = 0; i2 < vector.size(); i2++) {
		if (vector.elementAt(i2) instanceof MutableAttribute) {
		    MutableAttribute mutableAttribute = 
			(MutableAttribute) vector.elementAt(i2);
		    
		    if (attribute.name().equals(mutableAttribute.name)) {
			if (!(attribute instanceof IdSymbolicAttribute))
			    throw new 
				IllegalArgumentException("Symbolic attribute " +
							 "matched with unname" +
							 "d or numerical " +
							 "attribute");

			switchArray[i] = i2;
			continue aLoop;
		    }
		} else { /* Numerical attribute */
		    NumericalAttribute numericalAttribute =
			(NumericalAttribute) vector.elementAt(i2);

		    if (attribute.name().equals(numericalAttribute.name())) {
			switchArray[i] = i2;
			continue aLoop;
		    }
		}
	    }
	}

	return switchArray;
    }
    

    static private ItemSet buildItemSet(Vector attributes, Vector items) {
	/* Build attribute set */
	for (int i = 0; i < attributes.size(); i++) {
	    Object attribute = attributes.elementAt(i);
	    
	    if (attribute instanceof MutableAttribute)
		attributes.set(i, ((MutableAttribute) attribute).
			       toSymbolicAttribute());
	}
		
	/* Build items */
	ItemSet itemSet = new ItemSet(new AttributeSet(attributes));
	for (int i = 0; i < items.size(); i++) 
	    itemSet.add(new Item((AttributeValue[]) items.elementAt(i)));
	
	return itemSet;
    }
    
    
    /* Converts the 'attributes'-compatible 'values' array to a new array of
       values compatible with 'attributeSet' */
    static private AttributeValue[] convert(AttributeValue[] values,
					    Vector attributes,
					    AttributeSet attributeSet) {
	AttributeValue[] newValues = new AttributeValue[attributeSet.size()];
	int[] switchArray = switchArray(attributeSet, attributes);
	
	valuesLoop:
	for (int i = 0; i < newValues.length; i++) {
	    AttributeValue value = values[switchArray[i]];
	    
	    if (value.isUnknown()) {                         /* Unknown value */
		newValues[i] = value;
		continue;
	    }
	    
	    if (value instanceof SymbolicValue) {           /* Symbolic Value */
		IdSymbolicAttribute attribute = 
		    (IdSymbolicAttribute) attributeSet.attribute(i);
		String symbolicValueString = 
		    (String) ((MutableAttribute)
			      attributes.elementAt(switchArray[i])).
		    values.elementAt(((KnownSymbolicValue) value).intValue);
		
		for (int j = 0; j < attribute.nbValues; j++) {
		    SymbolicValue testValue = new KnownSymbolicValue(j);
		    
		    if (attribute.valueToString(testValue).
			equals(symbolicValueString))
			newValues[i] = testValue;
		}
	    } else                                         /* Numerical value */
		newValues[i] = value;
	}

	return newValues;
    }
    

    static private ItemSet buildItemSet(Vector attributes,
					AttributeSet attributeSet,
					Vector items) {
	ItemSet itemSet = new ItemSet(attributeSet);
	for (int i = 0; i < items.size(); i++) {
	    AttributeValue[] values = (AttributeValue[]) items.elementAt(i);
	    
	    itemSet.add(new Item(convert(values, attributes, attributeSet)));
	}
	
	return itemSet;
    }
}
