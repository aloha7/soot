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
 * A leaf node.  A leaf node is a node with no sons.
 **/
public class LeafNode 
    extends Node {
    
    private KnownSymbolicValue goalValue;
    private double entropy;
    private double[] goalValueDistribution;

    
    /**
     * Creates a new leaf node.  The node weight is set to 1.
     *
     * @param weight The node weight.
     **/
    public LeafNode(double weight) {
	super(weight);

	goalValue = null;
	entropy = -1.;
    }
    
    
    /**
     * Returns the most likely goal value of this node given its goal values
     * distribution (i.e. the value corresponding to the maximum value of
     * the goal value distribution).
     *
     * @return The most likely goal value.
     **/
    public KnownSymbolicValue goalValue() {
	if (goalValueDistribution == null)
	    throw new CannotCallMethodException("Goal value distribution " +
						"unknown");
	
	int mostFrequent = -1;
	double mostFrequentFrequency = -1;
	
	for (int gav = 0; gav < goalValueDistribution.length; gav++)
	    if (goalValueDistribution[gav] > mostFrequentFrequency) {
		mostFrequent = gav;
		mostFrequentFrequency = goalValueDistribution[gav];
	    }
	
	return new KnownSymbolicValue(mostFrequent);
    }
    
    
    /**
     * Returns the distribution of goal values.  The <code>i</code>-th element
     * of the returned array gives the number of learning items matching this
     * node having the <code>i</code>-th goal value.
     *
     * @return The goal attribute value distribution. Can be <code>null</code> 
     *         if no distribution defined.
     **/
    public double[] getGoalValueDistribution() {
	return (double[]) goalValueDistribution.clone();
    }
    
    
    /**
     * Sets the distribution of goal values.  This array is represented by an
     * array of double. The <code>i</code>-th element of the returned array
     * gives the proportion of items matching this node having the
     * <code>i</code>-th goal value.
     *
     * @param distribution The goal attribute value distribution.  Can be
     *                     <code>null</code> to unset the current distribution.
     **/
    public void setGoalValueDistribution(double[] distribution) {
	this.goalValueDistribution = (double[]) distribution.clone();
    }
    
    
    /**
     * Returns the entropy of this node.  This value reflects the certainty
     * of the associated symbolic value.
     *
     * @return The entropy of this node, or a negative value if this value has
     *         not been fixed.
     **/
    public double getEntropy() {
	return entropy;
    }
    
    
    /**
     * Set the entropy of this node.  This value reflects the certainty
     * of the associated symbolic value.
     *
     * @param entropy The entropy associated to this node, or a negative value
     *                to unset the current value.
     **/
    public void setEntropy(double entropy) {
	this.entropy = entropy;
    }

    
    public boolean hasOpenNode() {
	return false;
    }


    protected void updateHasOpenNode() {
    }
    

    protected void replaceSon(Node oldSon, Node newSon) {
	throw new CannotCallMethodException("This node has no son");
    }

    
    public Node son(int sonNb) {
	throw new CannotCallMethodException("Node has no son");
    }

    
    public int nbSons() {
	return 0;
    }
}
