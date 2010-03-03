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
 * A leaf node implementing the {@link LearningNode LearningNode}
 * interface.
 **/
public class LearningLeafNode 
    extends LeafNode implements LearningNode {
    
    private final ItemSet learningSet;
    

    /**
     * Creates a new learning leaf node.
     *
     * @param weight The node weight.
     **/
    public LearningLeafNode(double weight, ItemSet learningSet) {
	super(weight);

	this.learningSet = learningSet;
    }
    
    
    /**
     * Returns the current learning set associated to this node.
     *
     * @return The learning set associated to this node.
     **/
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
    
    
    /**
     * Returns the symbolic value associated to this node.  This value is
     * the goal (guessed) symbolic attribute value associated to this leaf.
     * This value is computed thanks to the learning set associated to this
     * node.
     *
     * @return The goal attribute value associated to this node, or -1 if
     *         this value has not been fixed.
     **/
    public KnownSymbolicValue goalValue() {
	double[] goalValueDistribution = goalValuesDistribution();
	
	int mostFrequent = -1;
	double mostFrequentFrequency = -1;
	
	for (int gav = 0; gav < goalValueDistribution.length; gav++)
	    if (goalValueDistribution[gav] > mostFrequentFrequency) {
		mostFrequent = gav;
		mostFrequentFrequency = goalValueDistribution[gav];
	    }
	
	return new KnownSymbolicValue(mostFrequent);
    }
    
    
    public double[] getGoalValueDistribution() {
	return goalValuesDistribution();
    }
    
    
    public void setGoalValueDistribution(double[] distribution) {
	throw new CannotCallMethodException("Goal value is computed");
    }
    

    /**
     * Returns the distribution of goal values.  This distribution is 
     * represented by an array such that its <code>i</code>-th element is
     * proportional to the weight of the <code>i</code>-th goal value.
     * The sum of the elements of this array is equal to 1.
     *
     * @return An array describing the goal value distribution associated to
     *         this node, or <code>null</code> if it cannot be determined
     *         (e.g. because the node is not linked to a tree, or has
     *         no associated learning set). 
     **/
    public double[] goalValuesDistribution() {
	WeightedItemSet itemSet;
	DecisionTree tree = tree();
	
	if (tree == null || learningSet() == null)
	    return null;

	if (!(learningSet() instanceof WeightedItemSet))
	    itemSet = new WeightedItemSet(learningSet());
	else
	    itemSet = (WeightedItemSet) learningSet();
	
	SymbolicAttribute goalAttribute = tree.getGoalAttribute();
	
	if (goalAttribute == null)
	    return null;
	
	/* Find the most frequent goal value in the items of the learning set */
	double[] frequencies = new double[goalAttribute.nbValues];

	for (int i = 0; i < itemSet.nbItems(); i++)
	    frequencies[((KnownSymbolicValue)
			 itemSet.item(i).
			 valueOf(itemSet.attributeSet().
				 indexOf(goalAttribute))).intValue] += 
		itemSet.weight(i);
	
	for (int i = 0; i < frequencies.length; i++)
	    frequencies[i] /= itemSet.size();
	
	return frequencies;
    }
}
