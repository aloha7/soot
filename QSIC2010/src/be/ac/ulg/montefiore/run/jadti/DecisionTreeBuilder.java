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
 * A builder of decision trees from a database.<p>
 * The decision tree aims to guess a (so called) 'goal' attribute thanks to
 * 'test attributes' values.<p>
 * Certain values may be unknown. The algorithms involving unknown values are
 * those of <i>C4.5</i> (Ross Quinlan).
 **/
public class DecisionTreeBuilder
    extends SimpleDecisionTreeBuilder {
    
    public DecisionTreeBuilder(ItemSet learningSet, 
			       AttributeSet testAttributes,
			       SymbolicAttribute goalAttribute) {
	super(new WeightedItemSet(learningSet), testAttributes, goalAttribute);
    }
}
