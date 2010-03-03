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

    
public class TestScore 
    implements Comparable {
    
    public final Test test;
    public final double score;
    
    TestScore(Test test, double score) {
	this.test = test;
	this.score = score;
    }
    
    /**
     * Compares TestScore objects according to their score values.
     * This ordering is <i>not consistent with equal</i>.
     *
     * @param o The object to compare.
     *
     * @return -1 if this element has a score smaller than the argument, 
     *         0 if they are equal, 1 otherwise.
     **/
    public int compareTo(Object o) {
	TestScore ts = (TestScore) o;
	
	if (score < ts.score)
	    return -1;
	
	if (score > ts.score)
	    return 1;
       
	return 0;
    }

    public String toString() {
	return "Test: " + test + " Score : " + score;
    }
}
