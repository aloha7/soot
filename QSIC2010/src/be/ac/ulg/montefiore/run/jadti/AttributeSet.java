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

package be.ac.ulg.montefiore.run.jadti;

import java.util.*;


/**
 * This class holds an ordered set of attributes. <p>
 * This object is immutable: attributes cannot be added or removed.  This
 * ensure that an attribute index will not change over time.
 **/
public class AttributeSet {
    
    private Vector attributes; 
    private Hashtable attributesHash; /* Retrieve an attribute index in
					 constant time */
    
    
    /**
     * Creates a new attribute set. 
     * 
     * @param attributes A Vector of {@link Attribute attributes} to include
     *                   in the set.
     **/
    public AttributeSet(Vector attributes) {
	if (attributes == null)
	    throw new IllegalArgumentException("Invalid attribute set");
	
	this.attributes = new Vector();
	attributesHash = new Hashtable();
	for (int i = 0; i < attributes.size(); i++)
	    add((Attribute) attributes.elementAt(i));
    }
    

    /**
     * Retreive the index of an attribute.
     *
     * @param attribute An attribute of the set.
     * @return The attribute index.  This index is such that
     *         0 <= index < size().
     **/
    public int indexOf(Attribute attribute) {
	Integer index = (Integer) attributesHash.get(attribute);
	
	if (index == null)
	    throw new IllegalArgumentException("Unknown attribute");
	
	return index.intValue();
    }
    

    /**
     * Retreive an attribute given its index.
     *
     * @param index An attribute index.  This index is such that
     *              0 <= index < size().
     * @return The indexed attribute.
     **/
    public Attribute attribute(int index) {
	if (index < 0 || index > attributes.size())
	    throw new IllegalArgumentException("Invalid index");
	
	return (Attribute) attributes.elementAt(index);
    }


    /**
     * Tests if an attribute belongs to the set.
     *
     * @param attribute The attribute to test.
     * @return <code>true</code> iff the attribute belongs to the set.
     **/
    public boolean contains(Attribute attribute) {
	if (attribute == null)
	    throw new IllegalArgumentException("Invalid 'null' attribute");
	
	return (attributesHash.get(attribute) != null);
    }


    /**
     * Finds an attribute using its name.
     *
     * @param name The searched attribute name.
     * @return The attribute if found, else <code>null</code>.
     **/
    public Attribute findByName(String name) {
	for (int i = 0; i < attributes.size(); i++) {
	    Attribute attribute = (Attribute) attributes.elementAt(i);
	 
	    if (attribute.name().equals(name))
		return attribute;
	}
	
	return null;
    }

    
    /**
     * Returns the attributes of this set in the proper order.
     *
     * @return The attributes of this set.
     **/
    public Vector attributes() {
	Vector attributes = new Vector();

	for (int i = 0; i < this.attributes.size(); i++)
	    attributes.add(this.attributes.elementAt(i));

	return attributes;
    }

    
    /**
     * Returns the number of attributes in this set.
     *
     * @return The number of attributes.
     **/
    public int size() {
	return attributes.size();
    }

    
    /* Adds an attribute to the set */
    private void add(Attribute attribute) {
	if (attribute == null)
	    throw new IllegalArgumentException("Invalid 'null' attribute");

	Object oldValue = attributesHash.put(attribute, 
					     new Integer(attributes.size()));
	if (oldValue != null) {
	    attributesHash.put(attribute, oldValue); 
	    throw new IllegalArgumentException("Attribute already present");
	}
	
	attributes.add(attribute);
   }


    /**
     * Checks an object for equality.  An object is equal to this set if it
     * is an attribute set with the same attributes in the same order.
     *
     * @param attributeSet The set the compare for equality.
     * @return True iif the sets are equal.
     **/
    public boolean equals(Object attributeSet) {
	if (attributeSet == null || !(attributeSet instanceof AttributeSet))
	    return false;
	
	return attributes.equals(((AttributeSet) attributeSet).attributes);
    }

    
    public int hashCode() {
	return attributes.hashCode();
    }


    public String toString() {
	String s = "";

	for (int i = 0; i < attributes.size(); ) {
	    s += attributes.elementAt(i);
	
	    if (++i < attributes.size())
		s += " ";
	}

	return s;
    }
}
