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
 * This class implements a testing node with an associated score value.
 **/
public class ScoreTestNode 
    extends TestNode {
    
    private double score;
    
    
    /**
     * Creates a new test node independant of any tree.  The sons of this node
     * are newly created open nodes.
     *
     * @param weight The node weight.
     * @param test This node's test.
     * @param score The score associated to the test.
     **/
    public ScoreTestNode(double weight, Test test, double score) {
	super(weight, test);
	
	this.score = score;
    }
    
    
    /**
     * Creates a new test node independant of any tree.  The sons of this node
     * are newly created open nodes.
     *
     * @param test This node's test.
     **/
    public ScoreTestNode(double weight, Test test, Node[] sons, double score) {
	super(weight, test, sons);
	
	this.score = score;
    }
    
    
    /**
     * Returns the score associated to this node.
     *
     * @return This test score.
     **/
    public double getScore() {
	return score;
    }
    
    
    /**
     * Sets the score associated to this node.
     *
     * @param score This node's score.
     **/
    public void setScore(double score) {
	this.score = score;
    }
}
