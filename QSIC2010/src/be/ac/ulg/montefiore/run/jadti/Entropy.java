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

import java.lang.*;


/**
 * A class holding entropy-related functions.
 **/
public final class Entropy {
    /**
     * Computes the entropy of a random variable. 
     *
     * @param probabilities The distribution of a random variable,
     *                      given as a set of probabilities. This set do not
     *                      need not be normalized: every element is
     *                      finaly divided by the sum of all the elements of
     *                      the array.  All the elements must be positive.
     * @return The entropy of the variable, <i>i.e.</i> minus the sum of
     *         <code>p<sub>i</sub> log(p<sub>i</sub></code>) (for all 
     *         <code>i</code>), where p<sub>i</sub> values are the ponderated
     *         elements of the <code>probabilities</code> array.
     **/
    static public double entropy(double[] probabilities) {
	if (probabilities == null)
	    throw new IllegalArgumentException("Invalid 'null' array");
	
	double sum = 0.;
	double result = 0.;

	for (int i = 0; i < probabilities.length; i++) {
	    if (probabilities[i] < 0.)
		throw new IllegalArgumentException("Invalid negative " +
						   "probability");
	    
	    if (probabilities[i] > 0.) {
		result -= probabilities[i] * Math.log(probabilities[i]);
		sum += probabilities[i];
	    }
	}
	
	if (sum <= 0.)
	    return 0.;

	result += sum * Math.log(sum);

	return result / (Math.log(2.) * sum);
    }
}
