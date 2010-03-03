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
 * This class implements a breadth-first search over nodes of a tree.
 * It does not allow to remove nodes.
 **/
public class DecisionTreeBFIterator
    implements Iterator {
    
    private LinkedList queue;
    
    
    /**
     * Returns an iterator over nodes of a tree.  The iteration is done
     * breadth-first.
     *
     * @param root The node where the search begins.
     **/
    public DecisionTreeBFIterator(Node root) {
	if (root == null)
	    throw new IllegalArgumentException("Invalid 'null' root");
	
	queue = new LinkedList();
	queue.add(root);
    }

    public boolean hasNext() {
	return queue.size() != 0;
    }

    public Object next() {
	Node node = (Node) queue.removeLast();
	
	if (node instanceof TestNode) {
	    TestNode testNode = (TestNode) node;
	    for (int i = 0; i < testNode.nbSons(); i++)
		queue.addFirst(testNode.son(i));
	}
	
	return node;
    }

    public void remove() {
	throw new UnsupportedOperationException("Cannot remove nodes");
    }
}
