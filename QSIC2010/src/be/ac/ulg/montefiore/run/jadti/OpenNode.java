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
 * This class implements a 'open' node.<p>
 * A node is open when its purpose has not been fixed.  It has no son.<p>
 * When a tree is created, its root is an open node.  The tree is completed
 * when all the open nodes have been replaced by test nodes or leaves.
 **/
public class OpenNode 
    extends Node {
    
    /**
     * Creates a new open node independant of any tree.
     *
     * @param weight The node weight.
     **/
    public OpenNode(double weight) {
	super(weight);
    }

    public boolean hasOpenNode() {
	return true;
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
