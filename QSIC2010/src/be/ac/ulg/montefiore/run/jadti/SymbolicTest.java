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
 * A test on a single symbolic attribute.<p>
 * The test checks if an attribute's value belongs to a given set or not.
 **/
public class SymbolicTest
    extends Test {

    private KnownSymbolicValue[] values;
    
    
    /**
     * Creates a new test on a specific symbolic attribute.
     *
     * @param attribute The attribute on which the test is performed.
     * @param values The values to test.  This array is copied.
     **/
    public SymbolicTest(SymbolicAttribute attribute,
			KnownSymbolicValue[] values) {
	super(attribute);
	
	if (values == null)
	    throw new IllegalArgumentException("Invalid values argument");
	
	this.values = new KnownSymbolicValue[values.length];
	for (int i = 0; i < values.length; i++)
	    this.values[i] = values[i];
    }
    
    
    /**
     * Applies the test.  The test checks if an attribute value belongs to a
     * given set of values.
     *
     * @param value The value to test.  This must be a
     *              {@link SymbolicValue SymbolicAttributeValue}.
     * @return 1 if the value belongs to the set of admitted values, 0
     *         otherwise.
     **/
    public int perform(AttributeValue value) {
	if (value.isUnknown())
	    throw new IllegalArgumentException("Cannot perform test on an " +
					       "unknown value");

	if (!(value instanceof KnownSymbolicValue))
	    throw new IllegalArgumentException("Wrong value type");
	
	return perform((KnownSymbolicValue) value);
    }
    
    
    /**
     * Applies the test.  The test checks if an attribute value belongs to a
     * given set of values.
     *
     * @param value The value to test.  This must be a
     *              {@link SymbolicValue SymbolicAttributeValue}.
     * @return 1 if the value belongs to the set of admitted values, 0
     *         otherwise.
     **/
    public int perform(KnownSymbolicValue value) {
	for (int i = 0; i < values.length; i++)
	    if (value.equals(values[i]))
		return 1;
	
	return 0;
    }
    

    public int nbIssues() {
	return 2;
    }


    public String toString() {
	String s = attribute.toString() + " in [";
	
	for (int i = 0; i < values.length; i++)
	    s += " " + ((SymbolicAttribute) attribute).valueToString(values[i]);
	
	return s + " ]";
    }
    

    public String issueToString(int issueNb) {
	switch (issueNb) {
	case 0:
	    return "No";
	case 1:
	    return "Yes";
	default:
	    throw new IllegalArgumentException("Invalid issue number");
	}
    }
}
