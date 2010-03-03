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
 * A decision tree node.
 **/
public abstract class Node {
    
    private Node father;
    public final double weight;

    
    /**
     * Creates a new node independant of any tree.
     *
     * @param weight A number representing the weight of this node.  This
     *               should be the number of items in the learning set matching
     *               this node.
     **/
    public Node(double weight) {
	if (weight < 0.)
	    throw new IllegalArgumentException("Weight must be positive");
	
	this.weight = weight;
    }
    
    /**
     * Returns the node's father.
     *
     * @return The node's father.
     **/
    public Node getFather() {
	return father;
    }
    
    /**
     * Sets a new father for this node.
     *
     * @param father The new node's father.
     **/
    protected void setFather(Node father) {
	this.father = father;
    }
    
    /**
     * Checks if this node is the tree root.
     *
     * @return <code>true</code> iff this node is the tree root.
     **/
    public boolean isRoot() {
	return (father != null && father instanceof AnchorNode);
    }
    
    /**
     * Replace this node with another one.  This node father becomes 
     * <code>null</code>.
     * 
     * @param node The new node replacing this one.
     */
    public void replace(Node node) {
	node.setFather(father);
	
	if (father != null)
	    father.replaceSon(this, node);
	
	father = null;
    }

    /**
     * Returns the leftmost open node of the subtree defined by this object.
     * Leftmost means that the son number chosen at each test node while
     * descending the tree is the smallest.
     *
     * @return The leftmost open node in the descendants of this node, or
     *         <code>null</code> if no descendant is an open node.
     **/
    public OpenNode openNode() {
	if (!hasOpenNode())
	    return null;
	
	if (this instanceof OpenNode)
	    return (OpenNode) this;
	else
	    for (int i = 0; i < nbSons(); i++)
		if (son(i).hasOpenNode())
		    return son(i).openNode();

	throw new RuntimeException("Internal error");
    }
    
    /**
     * Shows if one of the descendants of this node is open.  More formally,
     * this function returns true iff this node is open or there exist one son
     * of this node 's' such that <code>s.hasOpenNode() == true</code>.
     *
     * @return <code>true</code> iff one of the descendants of this node is
     *         open. 
     **/
    abstract public boolean hasOpenNode();

    /**
     * Replace a son with a new node.
     * The new and old nodes do not need to be modified, i.e. their father
     * reference have already been updated.
     *
     * @param oldSon The son to be replaced.
     * @param newSon The replacing node.  This argument cannot be null.
     **/
    abstract protected void replaceSon(Node oldSon, Node newSon);

    /**
     * Checks if the return value of <code>hasOpenNode()</code> could have
     * change. If yes, calls <code>father.updateHasOpenNode()</code>.
     **/
    abstract protected void updateHasOpenNode();
    
    /**
     * Returns the number of sons of this node.
     *
     * @return This node's number of sons.
     **/
    abstract public int nbSons();

    /**
     * Returns a son of this node.  Sons are described by a number such that
     * <code>0 <= sonNb < nbSons()</code>.
     *
     * @return This node's number of sons.
     **/
    abstract public Node son(int sonNb);

    /**
     * Checks if this node is a leaf node.  A node is a leaf node iff it has
     * no sons.
     *
     * @return <code>true</code> iff this node is a leaf node.
     **/
    public boolean isLeaf() {
	return (nbSons() == 0);
    }
    
    /**
     * Returns the tree associated to this node.  Notice that this requires
     * to find the root of the tree, an operation with complexity
     * <code>log(nbNodes)</code> on average.
     *
     * @return The node's tree, or null if the node is not associated to a tree.
     **/
    public DecisionTree tree() {
	if (father == null)
	    return null;
	else
	    return father.tree();
    }
}
