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


/**
 * This class implements the value of a symbolic attribute.
 **/
public final class KnownSymbolicValue
    extends SymbolicValue {
    
    /**
     * This attribute value represented as an integer.
     **/
    public final int intValue;

    
    /**
     * Creates a new symbolic value.
     *
     * @param value This attribute value represented as a (positive) integer.
     **/
    public KnownSymbolicValue(int value) {
	if (value < 0)
	    throw new IllegalArgumentException("Value must be positive");
	
	intValue = value;
    }

    public boolean isUnknown() {
	return false;
    }
    
    public boolean equals(Object o) {
	if (o == null || !(o instanceof KnownSymbolicValue))
	    return false;
	else
	    return ((KnownSymbolicValue) o).intValue == intValue;
    }
    
    public int hashCode() {
	return intValue;
    }

    public String toString() {
	return "" + intValue;
    }
}
