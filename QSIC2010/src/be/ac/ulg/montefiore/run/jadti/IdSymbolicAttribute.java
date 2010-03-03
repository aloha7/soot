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
 * A symbolic attribute where each attribute value is associated with an object,
 * usually its name coded as a String.
 **/
public class IdSymbolicAttribute 
    extends SymbolicAttribute {
    
    final private Object[] ids;

    
    /**
     * Builds a new symbolic attribute.  Each value of this attribute is
     * associated with an object.
     * 
     * @param ids A vector of objects.  Each object is associated with the
     *            corresponding symbolic value.
     **/
    public IdSymbolicAttribute(Vector ids) {
	super(ids.size());
	
	this.ids = new Object[ids.size()];
	
	for (int i = 0; i < ids.size(); i++)
	    this.ids[i] = ids.elementAt(i);
    }


    /**
     * Builds a new named symbolic attribute.
     *
     * @param name The attribute name.
     * @param ids A vector of objects.  Each object is associated with the
     *            corresponding symbolic value.
     **/
    public IdSymbolicAttribute(String name, Vector ids) {
	super(name, ids.size());
	
	this.ids = new Object[ids.size()];
	
	for (int i = 0; i < ids.size(); i++)
	    this.ids[i] = ids.elementAt(i);
    }
    
    
    /**
     * Converts a symbolic value to string.  This string is the obtained by
     * applying the method <code>toString</code> to the matching id object.
     *
     * @param value The value to convert.
     * @return The value converted to a String.
     */
    public String valueToString(SymbolicValue value) {
	if (value instanceof UnknownSymbolicValue)
	    return value.toString();

	int index = ((KnownSymbolicValue) value).intValue;
	if (index < 0 || index >= nbValues)
	    throw new IllegalArgumentException("Invalid index");

	return ids[index].toString();
    }

    
   
    public Attribute copy(String name) {
	return new IdSymbolicAttribute(name, new Vector(Arrays.asList(ids)));
    }
}
