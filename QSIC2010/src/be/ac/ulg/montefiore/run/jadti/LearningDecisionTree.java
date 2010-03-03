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
 * A learning decision tree.  This kind of tree associates to each node
 * a learning set of elements matching this node.<p>
 * All the nodes of the tree implement the {@link LearningNode LearningNode}
 * interface.
 **/
public class LearningDecisionTree 
    extends DecisionTree {
    
    /**
     * Creates an empty learning decision tree.
     *
     * @param attributeSet A set of attribute.  The set of attributes of the
     *                     items given to this tree.  Can be set to 'null'.
     * @param goalAttribute The goalAttribute.  Can be set to 'null'.
     **/
    public LearningDecisionTree(AttributeSet attributeSet, 
				SymbolicAttribute goalAttribute,
				ItemSet learningSet) {
	super(attributeSet, goalAttribute);

	root().replace(new LearningOpenNode(0, learningSet));
    }

    
    /**
     * Returns a {@link DecisionTree decision tree} equivalent to this
     * learning tree (i.e. without learning sets).
     *
     * @return A {@link DecisionTree decision} tree.
     **/
    public DecisionTree decisionTree() {
	DecisionTree tree = 
	    new DecisionTree(getAttributeSet(), getGoalAttribute());
	
	Iterator BFIterator = breadthFirstIterator();
	LinkedList list = new LinkedList();
	list.add(tree.root());
	
	while (BFIterator.hasNext()) {
	    OpenNode openNode = (OpenNode) list.removeLast();
	    Node newNode = convertNode((Node) BFIterator.next());
	    
	    for (int i = 0; i < newNode.nbSons(); i++)
		list.addFirst(newNode.son(i));
	    
	    openNode.replace(newNode);
	}
	
	return tree;
    }
    
    
    private Node convertNode(Node node) {
	if (node instanceof LearningTestNode)
	    return convertTestNode((LearningTestNode) node);
	else if (node instanceof LearningLeafNode)
	    return convertLeafNode((LearningLeafNode) node);
	else
	    return convertOpenNode((LearningOpenNode) node);
    }
    
    
    private TestNode convertTestNode(LearningTestNode node) {
	return new ScoreTestNode(node.weight, node.test(), node.getScore());
    }
    
    
    private LeafNode convertLeafNode(LearningLeafNode node) {
	LeafNode leafNode = new LeafNode(node.learningSet().size());
	
	leafNode.setEntropy(node.learningSet().entropy(getGoalAttribute()));
	leafNode.setGoalValueDistribution(node.goalValuesDistribution());
		
	return leafNode;
    }
    
    
    private OpenNode convertOpenNode(LearningOpenNode node) {
	return new OpenNode(node.weight);
    }
}
