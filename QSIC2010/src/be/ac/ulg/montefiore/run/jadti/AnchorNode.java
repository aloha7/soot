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
 * An anchor node.  Each tree as one (and only one) anchor root node which is
 * root's father; this node has thus only one son.
 **/
class AnchorNode 
    extends Node {
    
    /**
     * The tree to which this this node is linked.
     **/
    protected final DecisionTree tree;
    private Node root;
    
    
    /**
     * Creates a new anchor node.  An new open node is created an set as this
     * node's son.
     *
     * @param tree The decision tree to which this node is linked.
     **/
    protected AnchorNode(DecisionTree tree) {
	super(0.);
	
	this.tree = tree;
	this.root = new OpenNode(0.);
	this.root.setFather(this);
    }
    
    protected void replaceSon(Node oldRoot, Node newRoot) {
	if (oldRoot != root)
	    throw new IllegalArgumentException("First argument is invalid.");
	
	this.root = newRoot;
    }
    
    /**
     * Returns this node's son. This son is also the tree root.
     *
     * @return The node's son.
     **/
    public Node son() {
	return root;
    }

    public Node son(int sonNb) {
	if (sonNb != 0)
	    throw new IllegalArgumentException("Argument must be 0");
	
	return root;
    }
    
    public boolean hasOpenNode() {
	return root.hasOpenNode();
    }
    
    protected void updateHasOpenNode() {
    }

    public int nbSons() {
	return 1;
    }

    public boolean isLeaf() {
	return false;
    }

    public DecisionTree tree() {
	return tree;
    }
}
