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
 * An instance of this class holds the values of the attributes of an
 * element of the learning/testing set.
 **/
public class Item {

    private AttributeValue[] values;
    
    
    /**
     * Builds a new item.
     *
     * @param values An (non empty) array of attribute values. This array is
     *               copied.
     **/
    public Item(AttributeValue[] values) {
	if (values == null)
	    throw new IllegalArgumentException("Invalid argument");
	
	this.values = new AttributeValue[values.length];
	
	for (int i = 0; i < values.length; i++)
	    this.values[i] = values[i];
    }
    

    /**
     * Returns the value of an attribute.
     *
     * @param attributes The attribute set matching this item.
     * @param attribute The attribute whose value is to be retreived.
     * @return The attribute value.
     **/
    public AttributeValue valueOf(AttributeSet attributes,
				  Attribute attribute) {
	return valueOf(attributes.indexOf(attribute));
    }

    
    /**
     * Returns the value of an attribute.
     *
     * @param index The attribute index 
     *              (0 <= <code>index</code> < <code>nbAttributes()</code>).
     * @return The attribute value.
     **/
    public AttributeValue valueOf(int index) {
	if (index < 0 && index >= values.length)
	    throw new IllegalArgumentException("Invalid argument");
	
	return values[index];
    }

    
    /**
     * Returns the values of this item as an array.
     *
     * @return An array holding this item's attribute values as an array.
     **/
    public AttributeValue[] toArray() {
	AttributeValue[] array = new AttributeValue[values.length];

	for (int i = 0; i < values.length; i++)
	    array[i] = values[i];

	return array;
    }

    /**
     * Returns this item's number of attributes.
     *
     * @return A strictly positive number of attributes.
     **/
    public int nbAttributes() {
	return values.length;
    }
}
