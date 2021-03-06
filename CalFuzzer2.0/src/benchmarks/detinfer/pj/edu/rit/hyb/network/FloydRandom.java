//******************************************************************************
//
// File:    FloydRandom.java
// Package: benchmarks.detinfer.pj.edu.rit.hyb.network
// Unit:    Class benchmarks.detinfer.pj.edu.rit.hyb.network.FloydRandom
//
// This Java source file is copyright (C) 2008 by Alan Kaminsky. All rights
// reserved. For further information, contact the author, Alan Kaminsky, at
// ark@cs.rit.edu.
//
// This Java source file is part of the Parallel Java Library ("PJ"). PJ is free
// software; you can redistribute it and/or modify it under the terms of the GNU
// General Public License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// PJ is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// A copy of the GNU General Public License is provided in the file gpl.txt. You
// may also obtain a copy of the GNU General Public License on the World Wide
// Web at http://www.gnu.org/licenses/gpl.html.
//
//******************************************************************************

package benchmarks.detinfer.pj.edu.rit.hyb.network;

import benchmarks.detinfer.pj.edu.rit.util.Random;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

/**
 * Class FloydRandom is a main program that creates a distance matrix input file
 * for the {@linkplain FloydSeq} or {@linkplain FloydHyb} programs.
 * <P>
 * Usage: java benchmarks.detinfer.pj.edu.rit.hyb.network.FloydRandom <I>seed</I> <I>radius</I>
 * <I>N</I> <I>matrixfile</I>
 * <BR><I>seed</I> = Random seed
 * <BR><I>radius</I> = Node adjacency radius
 * <BR><I>N</I> = Number of nodes
 * <BR><I>matrixfile</I> = Distance matrix file
 * <P>
 * The program:
 * <OL TYPE=1>
 * <LI>
 * Initializes a pseudorandom number generator with <I>seed</I>.
 * <LI>
 * Generates <I>N</I> nodes located at random positions in the unit square.
 * <LI>
 * Sets up the distance matrix <I>D</I>. If two nodes are within a Euclidean
 * distance <I>radius</I> of each other, the nodes are adjacent, otherwise the
 * nodes are not adjacent. <I>radius</I> = 0.25 works well.
 * <LI>
 * Stores the distance matrix in the <I>matrixfile</I>.
 * </OL>
 * <P>
 * The distance matrix file is a binary file written in the format required by
 * class {@linkplain benchmarks.detinfer.pj.edu.rit.io.DoubleMatrixFile}.
 *
 * @author  Alan Kaminsky
 * @version 30-May-2008
 */
public class FloydRandom
	{

// Prevent construction.

	private FloydRandom()
		{
		}

// Main program.

	/**
	 * Main program.
	 */
	public static void main
		(String[] args)
		throws Exception
		{
		// Parse command line arguments.
		if (args.length != 4) usage();
		long seed = Long.parseLong (args[0]);
		double radius = Double.parseDouble (args[1]);
		int n = Integer.parseInt (args[2]);
		String matrixfile = args[3];

		// Set up pseudorandom number generator.
		Random prng = Random.getInstance (seed);

		// Generate random node locations in the unit square.
		double[] x = new double [n];
		double[] y = new double [n];
		for (int i = 0; i < n; ++ i)
			{
			x[i] = prng.nextDouble();
			y[i] = prng.nextDouble();
			}

		// Open output file.
		DataOutputStream out =
			new DataOutputStream
				(new BufferedOutputStream
					(new FileOutputStream (matrixfile)));

		// Write DoubleMatrixFile header information.
		out.writeInt (n); // Number of matrix rows
		out.writeInt (n); // Number of matrix columns
		out.writeInt (0); // Segment starting row index
		out.writeInt (0); // Segment starting column index
		out.writeInt (n); // Number of segment rows
		out.writeInt (n); // Number of segment columns

		// Write distance matrix elements.
		for (int r = 0; r < n; ++ r)
			{
			for (int c = 0; c < n; ++ c)
				{
				double dx = x[r] - x[c];
				double dy = y[r] - y[c];
				double distance = Math.sqrt (dx*dx + dy*dy);
				out.writeDouble
					(distance <= radius ?
						distance :
						Double.POSITIVE_INFINITY);
				}
			}

		// Close output file.
		out.close();
		}

	// Hidden operations.

	/**
	 * Print a usage message and exit.
	 */
	private static void usage()
		{
		System.err.println ("Usage: java benchmarks.detinfer.pj.edu.rit.hyb.network.FloydRandom <seed> <radius> <N> <matrixfile>");
		System.err.println ("<seed> = Random seed");
		System.err.println ("<radius> = Node adjacency radius");
		System.err.println ("<N> = Number of nodes");
		System.err.println ("<matrixfile> = Distance matrix file");
		System.exit (1);
		}

	}
