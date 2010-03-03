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
 * A symbolic attribute.  Symbolic attributes have a finite set of
 * possible values represented by a positive integer.
 **/
public class SymbolicAttribute 
    extends Attribute {
    
    public final int nbValues;
    
    
    /**
     * Builds a new unnamed symbolic attribute.
     * 
     * @param nbValues The number of different values allowed for this 
     *                 attribute.  The allowed attribute values are
     *                 0...<code>nbValues - 1</code>.
     **/
    public SymbolicAttribute(int nbValues) {
	this(null, nbValues);
    }
    
    /**
     * Builds a new named symbolic attribute.
     *
     * @param name The attribute name.
     * @param nbValues The number of different values allowed for this 
     *                 attribute.
     **/
    public SymbolicAttribute(String name, int nbValues) {
	super(name);
	
	if (nbValues <= 0)
	    throw new IllegalArgumentException("The number of allowed " +
					       "attribute values must be " +
					       "strictly positive");
	    this.nbValues = nbValues;
    }
    
    public Attribute copy(String name) {
	return new SymbolicAttribute(name, nbValues);
    }

    /**
     * Converts a symbolic value to string.
     *
     * @param value The value to convert.
     * @return The value converted to a String.
     */
    public String valueToString(SymbolicValue value) {
	return "" + value;
    }
}
