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

package be.ac.ulg.montefiore.run.jadti.io;

import java.util.*;
import java.text.*;
import be.ac.ulg.montefiore.run.jadti.*;


/**
 * Writes a .dot graph file representing a given decision tree.
 * A tool for converting .dot files to other file formats is available at<br>
 * <url>http://www.research.att.com/sw/tools/graphviz/</url>.
 **/
public class DecisionTreeToDot {

    static final String header = "digraph decision_tree {\n";
    static final String tailer = "}\n";
    private DecisionTree tree;
    private NumberFormat formatter;
    
    
    /**
     * Build a new decision tree drawer.
     *
     * @param tree The tree to draw.
     **/
    public DecisionTreeToDot(DecisionTree tree) {
	if (tree == null)
	    throw new IllegalArgumentException("Invalid 'null' argument");
	this.tree = tree;
	formatter = NumberFormat.getInstance();
	formatter.setMaximumFractionDigits(3);
    }

    /**
     * Produce the .dot file content encoded in a String.
     *
     * @return The .dot file content.
     **/
    public String produce() {
	String s = header;
	
	Iterator nodesIterator = tree.breadthFirstIterator();
	while (nodesIterator.hasNext()) {
	    Node node = (Node) nodesIterator.next();

	    s += produceLabel(node);
	    
	    if (node instanceof TestNode)
		s += produceTransitions((TestNode) node);
	}
	
	return s + tailer;
    }
    
    private String produceTransitions(TestNode node) {
	String s = "";
	
	for (int i = 0; i < node.nbSons(); i++)
	    s += id(node) + " -> " + id(node.son(i)) +
		" [label = \"" + node.test().issueToString(i) + "\"];\n";
	
	return s;
    }
    
    private String produceLabel(Node node) {
	String label = "";

	if (node instanceof TestNode) {
	    label += ((TestNode) node).test().toString();
	    
	    if (node instanceof ScoreTestNode)
		label += " (score= " + 
		    formatter.format(((ScoreTestNode) node).getScore()) +")";
	}
	else /* Leaf */ {
	    LeafNode leafNode = (LeafNode) node;
	    String goalValueString = "";
	    String entropyString = "";
	    
	    label += "Leaf";
	    
	    if (leafNode.goalValue() != null)
		if (tree.getGoalAttribute() != null)
		    goalValueString = " - " + tree.getGoalAttribute().
			valueToString(leafNode.goalValue());
		else
		    goalValueString = " - " + leafNode.goalValue();
	    
	    if (leafNode.getGoalValueDistribution() != null) {
		double[] distribution = leafNode.getGoalValueDistribution();
	
		label += "[";
		for (int i = 0; i < distribution.length; i++)
		    label += " " + formatter.format(distribution[i]);
		label += " ]";
	    } else 
		if (leafNode.getEntropy() >= 0)
		    entropyString = " Entropy: " +
			formatter.format(leafNode.getEntropy());
	    
	    label += goalValueString + entropyString;
	}
	
	label += " Weight = " + formatter.format(node.weight);
	
	return id(node) + " [label=\"" + label + "\"];\n";
    }

    private String id(Node node) {
	return "\"" + node + "\"";
    }
}
