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
 * A test on a single attribute.
 **/
abstract public class Test {
    
    /**
     * The attribute on which the test is applied.
     **/
    public final Attribute attribute;
        
    /**
     * Creates a new test on a specific attribute.
     *
     * @param attribute The attribute on which the test is performed.
     **/
    public Test(Attribute attribute) {
	if (attribute == null)
	    throw new IllegalArgumentException("Invalid 'null' attribute");
	
	this.attribute = attribute;
    }
    
    /**
     * Applies the test on a given value.
     *
     * @param value The value to test.
     * @return The issue of the test.
     **/
    abstract public int perform(AttributeValue value);
     
    /**
     * Queries the number of possible test issues.
     *
     * @return This test's number of issues.
     **/
    abstract public int nbIssues();

    /**
     * Describes a test issue.
     *
     * @param issueNb The number of the test issue to describe.  Must be such
     *                that <code>0 <= issueNb < nbIssues</code>.
     * @return A <code>String</code> describing the test issue.
     **/
    abstract public String issueToString(int issueNb);
}

