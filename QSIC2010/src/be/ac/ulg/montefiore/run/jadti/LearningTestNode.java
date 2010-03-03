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
 * A test node implementing the {@link LearningNode LearningNode}
 * interface.
 **/
public class LearningTestNode 
    extends ScoreTestNode implements LearningNode {
    
    private final ItemSet learningSet;

    
    /**
     * Creates a new learning open node.
     *
     * @param weight The weight of this node.
     * @param test The test associated to the node.
     * @param score The score associated to the test.
     **/
    public LearningTestNode(double weight, Test test, double score,
			    ItemSet learningSet) {
	super(weight, test, score);
	
	for (int i = 0; i < nbSons(); i++)
	    son(i).replace(new LearningOpenNode(son(i).weight, null));
	
	this.learningSet = learningSet;
    }
    
    
    public ItemSet learningSet() {
	return learningSet;
    }

    
    public void replace(Node node) {
	if (!(node instanceof LearningNode))
	    throw new IllegalArgumentException("A learning node can only " +
					       "be replaced by another "+
					       "learning node");
	super.replace(node);
    }
 }
