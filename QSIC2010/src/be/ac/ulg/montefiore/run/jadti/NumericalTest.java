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
 * A test on a single numerical attribute.<p>
 * The test checks if an attribute's value is smaller than a fixed threshold.
 **/
public class NumericalTest
    extends Test {

    private double threshold;
    
    
    /**
     * Creates a new test.
     *
     * @param attribute The attribute on which the test is performed.
     * @param threshold The test threshold.
     **/
    public NumericalTest(NumericalAttribute attribute, double threshold) {
	super(attribute);
	
	this.threshold = threshold;
    }
    
    /**
     * Applies the test. The test checks if the tested value is smaller
     * than a threshold. 
     *
     * @param value The value to test.
     * @return 1 if the value is smaller than the threshold, 0 otherwise.
     **/
    public int perform(AttributeValue value) {
	if (! (value instanceof KnownNumericalValue))
	    throw new IllegalArgumentException("Wrong value type");
	
	return perform((KnownNumericalValue) value);
    }
    
    /**
     * Applies the test. The test checks if the tested value is smaller
     * than a threshold. 
     *
     * @param value The value to test.
     * @return 1 if the value is smaller than the threshold, 0 otherwise.
     **/
    public int perform(KnownNumericalValue value) {
	return (value.doubleValue < threshold) ? 1 : 0;
    }
    
    public int nbIssues() {
	return 2;
    }
    
    public String toString() {
	return attribute.toString() + " < " + threshold;
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
