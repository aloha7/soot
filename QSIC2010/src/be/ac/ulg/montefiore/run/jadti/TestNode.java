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
 * This class implements a testing node.  All the nodes between the root and
 * the leaves are test node.<p>
 * Each test node can perform a test, i.e. can split a test in several classes
 * according to the test issue.  A test node has as many leaves as the number
 * of issues of the test.
 **/
public class TestNode 
    extends Node {
    
    private Test test;
    private Node[] sons;
    private boolean hasOpenNode;
    
    
    /**
     * Creates a new test node independant of any tree.  The sons of this node
     * are newly created open nodes.
     *
     * @param weight The node weight.
     * @param test This node's test.
     **/
    public TestNode(double weight, Test test) {
	super(weight);

	if (test == null)
	    throw new IllegalArgumentException("Invalid 'null' test");

	this.test = test;
	
	sons = new Node[test.nbIssues()];
	for (int i = 0; i < sons.length; i++) {
	    sons[i] = new OpenNode(0.);
	    sons[i].setFather(this);
	}
	
	hasOpenNode = true;
    }
    
    
    /**
     * Creates a new test node independant of any tree.
     *
     * @param weight The node weight.
     * @param test This node's test.
     * @param sons An array of sons.  The length of the array must be equal
     *             to <code>nbSons</code>.
     **/
    public TestNode(double weight, Test test, Node[] sons) {
	super(weight);
	
	if (test == null || sons == null || sons.length != this.sons.length)
	    throw new IllegalArgumentException("Invalid argument");

	this.test = test;
	
	sons = (Node[]) sons.clone();
	hasOpenNode = false;
	for (int i = 0; i < sons.length; i++) {
	    if (sons[i] == null)
		throw new IllegalArgumentException("Cannot set a 'null' son");
	    if (sons[i].hasOpenNode())
		hasOpenNode = true;
	}
    }
    

    /**
     * Return this node's test.
     *
     * @return This node's test.
     **/
    public Test test() {
	return test;
    }
    

    /**
     * Returns the son matching an item given this node's test.  The test is
     * applied to the argument's test attribute value and the matching son is
     * returned.
     *
     * @param item The item to test.
     * @return The node matching the test issue.
     **/
    public Node matchingSon(Item item) {
	if (item == null)
	    throw new IllegalArgumentException("Invalid 'null' argument");

	DecisionTree tree = tree();

	if (tree == null) 
	    throw new CannotCallMethodException("The node is not attached to " +
						"a tree");
	
	return matchingSon(item.valueOf(tree.getAttributeSet(),
					test.attribute));
    }
    

    /**
     * Returns the son matching a value given this node's test.  The argument is
     * tested and the matching son is returned.  If the test is numerical, the
     * argument must be a <code>Double</code> object, else it must be a
     * <code>Integer</code>.
     *
     * @param value The value to test.
     * @return The node matching the test issue.
     **/
    public Node matchingSon(AttributeValue value) {
	return sons[test.perform(value)];
    }
    
    
    protected void replaceSon(Node oldSon, Node newSon) {
	if (newSon == null)
	    throw new IllegalArgumentException("Invalid 'null' argument");
	
	for (int i = 0; i < sons.length; i++)
	    if (sons[i] == oldSon) {
		sons[i] = newSon;
		updateHasOpenNode();
		return;
	    }
	
	throw new IllegalArgumentException("First argument is not a son");
    }
    

    public boolean hasOpenNode() {
	return hasOpenNode;
    }
    

    protected void updateHasOpenNode() {
	boolean hasOpenNode = false;
	
	for (int i = 0; i < nbSons(); i++) 
	    if (sons[i].hasOpenNode()) {
		hasOpenNode = true;
		break;
	    }
	
	if (this.hasOpenNode != hasOpenNode) {
	    this.hasOpenNode = hasOpenNode;
	    
	    if (getFather() != null)
		getFather().updateHasOpenNode();
	}
    }

    
    public Node son(int sonNb) {
	if (sonNb < 0 || sonNb >= test.nbIssues())
	    throw new IllegalArgumentException("Invalid argument");
	
	return sons[sonNb];
    }
    

    /**
     * Return the number of sons of this node. It is equal to the number of
     * issues of this node's test.
     *
     * @return The number of sons of this node.
     **/
    public int nbSons() {
	return sons.length;
    }
}
