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

import java.util.*;


/**
 * This class implements a decision tree.<p>
 * A decision tree is a tree where a test has been assigned to non-leaf
 * nodes.  Its aim is to guess the value of an {@link Item item}'s attribute
 * (called the 'goal' attribute) thanks to tests over other attributes.<p>
 * If the topology of the tree is changed, take must be taken to create a
 * valid tree, i.e. an acyclic graph where all the sons of one node are
 * different.<p>
 * The tree is composed of 3 types of nodes:
 * <ul>
 * <li> TestNodes: They are associated to a test over items' attributes.  A
 *      test node has as many sons as the test's number of different outcomes.
 * <li> LeafNodes: They have no sons.  A leaf node is associated a goal
 *      attribute value.
 * <li> OpenNode: A node whose purpose has not been found yet.  It can be
 *      replaced by a test/leaf node later on.  The tree's open nodes can be
 *      efficiently (in log(nbNodes) on average) retreived.
 * </ul>
 **/
public class DecisionTree {
    
    private final AnchorNode anchor;
    private AttributeSet attributeSet;
    private SymbolicAttribute goalAttribute;
    
    
    /**
     * Creates a empty decision tree.
     *
     * @param attributeSet A set of attribute.  The set of attributes of the
     *                     items given to this tree.  Can be set to 'null'.
     * @param goalAttribute The goalAttribute.  Can be set to 'null'.
     **/
    public DecisionTree(AttributeSet attributeSet, 
			SymbolicAttribute goalAttribute) {
	anchor = new AnchorNode(this);
	this.attributeSet = attributeSet;
	this.goalAttribute = goalAttribute;
    }
    
    
    /**
     * Guess goal attribute value of an item.
     *
     * @param item The item compatible with the tree attribute set.
     * @return The goal attribute value, or -1 if the matching leaf node does
     *         not define a goal attribute.
     **/
    public KnownSymbolicValue guessGoalAttribute(Item item) {
	double[] distribution = goalValueDistribution(item);
	
	int index = -1;
	double max = -1.;
	
	for (int i = 0; i < distribution.length; i++)
	    if (distribution[i] > max) {
		index = i;
		max = distribution[i];
	    }
	
	return new KnownSymbolicValue(index);
    }
    
    
    /**
     * Finds the leaf/open node matching an item.  All the (tested) attributes
     * of the item must be known.
     *
     * @param item An item compatible with the tree attribute set.
     * @return The leaf node matching <code>item</code>.
     **/
    public Node leafNode(Item item) {
	if (getAttributeSet() == null || getGoalAttribute() == null)
	    throw new CannotCallMethodException("No attribute set or goal " +
						"attribute defined");
	
	AttributeSet attributeSet = getAttributeSet();
	Node node = root();
	
	while (!(node.isLeaf())) {
	    TestNode testNode = (TestNode) node;
	    
	    int testAttributeIndex =
		attributeSet.indexOf(testNode.test().attribute);
	    
	    node = testNode.
		matchingSon(item.valueOf(testAttributeIndex));
	}
	
	return node;
    }
    
    
    /**
     * Finds the goal value distribution matching an item.  This distribution
     * describes the probability of each potential goal value for this item.
     *
     * @param item An item compatible with the tree attribute set.
     * @return The goal attribute value distribution for the item
     *         <code>item</code>.
     **/
    public double[] goalValueDistribution(Item item) {
	return goalValueDistribution(item, root());
    }
    
    
    protected double[] goalValueDistribution(Item item, Node node) {
	if (node.isLeaf())
	    return ((LeafNode) node).getGoalValueDistribution();
	else
	    if (node instanceof TestNode) {
		TestNode testNode = (TestNode) node;
		
		int testAttributeIndex = 
		    attributeSet.indexOf(testNode.test().attribute);
		
		if (item.valueOf(testAttributeIndex).isUnknown()) {
		    double[] distribution = 
			new double[getGoalAttribute().nbValues];
		    
		    Arrays.fill(distribution, 0.);
		    
		    for (int i = 0; i < testNode.nbSons(); i++)
			add(distribution,
			    times(goalValueDistribution(item, testNode.son(i)),
				  testNode.son(i).weight));
		    
		    times(distribution, 1. / testNode.weight);
		    
		    return distribution;
		} else {
		    Node nextNode = 
			testNode.matchingSon(item.valueOf(testAttributeIndex));
		    
		    return goalValueDistribution(item, nextNode);
		}
	    } else
		throw new CannotCallMethodException("Open node found while " +
						    "exploring tree");
    }
    
    
    /**
     * Returns the root node.
     *
     * @return This tree's root node.
     */
    public Node root() {
	return anchor.son();
    }
    
    
    /**
     * Check if a given node is the root node.
     * 
     * @param node the node to check.
     * @return <code>true</code> iff the argument is the root node of this tree.
     **/
    public boolean isRoot(Node node) {
	if (node == null)
	    throw new IllegalArgumentException("Invalid 'null' argument");

	return (node.equals(root()));
    }
    
    
    /**
     * Change this tree's attribute set. The set of attributes of the items
     * given to this tree. 
     *
     * @param attributeSet The new attribute set. Can be set to 'null'.
     **/
    public void setAttributeSet(AttributeSet attributeSet) {
	this.attributeSet = attributeSet;
    }
    
    
    /**
     * Returns this tree's attribute set.
     *
     * @return This tree's attribute set.  A 'null' value means thaht the set is
     *         undefined.
     **/
    public AttributeSet getAttributeSet() {
	return attributeSet;
    }
    
    
    /**
     * Change this tree's goal attribute. The goal attribute is the attribute
     * guessed by the tree.  
     *
     * @param goalAttribute The new tree's goal attribute.  Can be set to
     *                      'null' if unknown.
     **/
    public void setGoalAttribute(SymbolicAttribute goalAttribute) {
	this.goalAttribute = goalAttribute;
    }
    
    
    /**
     * Get this tree's goal attribute. The goal attribute is the attribute
     * guessed by the tree.
     *
     * @return The tree's goal attribute.  Returns 'null' if unknown.
     **/
    public SymbolicAttribute getGoalAttribute() {
	return goalAttribute;
    }
    
    
    /**
     * Returns the leftmost open node of the tree.
     * 'Leftmost' means that the son chosen at each test node while
     * descending the tree is the smallest number.
     *
     * @return The leftmost open node of the tree, or <code>null</code> if the
     *         tree has no open node.
     **/
    public OpenNode openNode() {
	return anchor.openNode();
    }
    
    
    /**
     * Checks if the tree has open nodes.
     *
     * @return <code>true</code> iff the tree has open nodes.
     **/
    public boolean hasOpenNode() {
	return anchor.hasOpenNode();
    }

    
    /**
     * Returns the nodes of the tree.  The iterator returns the node according
     * to a breadth first search.  The sons of a node are returned left to
     * right (i.e. with increasing son number).
     *
     * @return An iterator over the tree's nodes.
     */
    public Iterator breadthFirstIterator() {
	return new DecisionTreeBFIterator(root());
    }

    
    private double[] times(double[] distribution, double weight) {
	for (int i = 0; i < distribution.length; i++)
	    distribution[i] *= weight;

	return distribution;
    }

    
    private double[] add(double[] d1, double[] d2) {
	if (d1.length != d2.length)
	    throw new IllegalArgumentException("distributions must have " +
					       "the same number of elements");
	
	for (int i = 0; i < d1.length; i++)
	    d1[i] += d2[i];

	return d1;
    }
}
