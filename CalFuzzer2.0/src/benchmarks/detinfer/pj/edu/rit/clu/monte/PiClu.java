//******************************************************************************
//
// File:    PiClu.java
// Package: benchmarks.detinfer.pj.edu.rit.clu.monte
// Unit:    Class benchmarks.detinfer.pj.edu.rit.clu.monte.PiClu
//
// This Java source file is copyright (C) 2007 by Alan Kaminsky. All rights
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

package benchmarks.detinfer.pj.edu.rit.clu.monte;

import benchmarks.detinfer.pj.edu.rit.mp.buf.LongItemBuf;

import benchmarks.detinfer.pj.edu.rit.pj.Comm;

import benchmarks.detinfer.pj.edu.rit.pj.reduction.LongOp;

import benchmarks.detinfer.pj.edu.rit.util.LongRange;
import benchmarks.detinfer.pj.edu.rit.util.Random;

/**
 * Class PiClu is a cluster parallel program that calculates an approximate
 * value for &pi; using a Monte Carlo technique. The program generates a number
 * of random points in the unit square (0,0) to (1,1) and counts how many of
 * them lie within a circle of radius 1 centered at the origin. The fraction of
 * the points within the circle is approximately &pi;/4.
 * <P>
 * Usage: java -Dpj.np=<I>K</I> benchmarks.detinfer.pj.edu.rit.clu.monte.PiClu <I>seed</I> <I>N</I>
 * <BR><I>K</I> = Number of parallel processes
 * <BR><I>seed</I> = Random seed
 * <BR><I>N</I> = Number of random points
 * <P>
 * The computation is performed in parallel in multiple threads. The program
 * uses class benchmarks.detinfer.pj.edu.rit.util.Random for its pseudorandom number generator. To
 * improve performance, each process has its own pseudorandom number generator,
 * and the program uses the reduction pattern to determine the count. The
 * program uses the "sequence splitting" technique with the pseudorandom number
 * generators to yield results identical to the sequential version. The program
 * measures the computation's running time.
 *
 * @author  Alan Kaminsky
 * @version 27-Jun-2007
 */
public class PiClu
	{

// Prevent construction.

	private PiClu()
		{
		}

// Program shared variables.

	// World communicator.
	static Comm world;
	static int size;
	static int rank;

	// Command line arguments.
	static long seed;
	static long N;

	// Pseudorandom number generator.
	static Random prng;

	// Number of points within the unit circle.
	static long count;

// Main program.

	/**
	 * Main program.
	 */
	public static void main
		(String[] args)
		throws Exception
		{
		// Start timing.
		long time = -System.currentTimeMillis();

		// Initialize middleware.
		Comm.init (args);
		world = Comm.world();
		size = world.size();
		rank = world.rank();

		// Validate command line arguments.
		if (args.length != 2) usage();
		seed = Long.parseLong (args[0]);
		N = Long.parseLong (args[1]);

		// Determine range of iterations for this thread.
		LongRange range = new LongRange (0, N-1) .subrange (size, rank);
		long my_N = range.length();

		// Set up PRNG and skip ahead over the random numbers the lower-ranked
		// processes will generate.
		prng = Random.getInstance (seed);
		prng.skip (2 * range.lb());

		// Generate random points in the unit square, count how many are in the
		// unit circle.
		count = 0L;
		for (long i = 0L; i < my_N; ++ i)
			{
			double x = prng.nextDouble();
			double y = prng.nextDouble();
			if (x*x + y*y <= 1.0) ++ count;
			}

		// Reduce all processes' counts together into process 0.
		LongItemBuf buf = new LongItemBuf();
		buf.item = count;
		world.reduce (0, buf, LongOp.SUM);
		count = buf.item;

		// Stop timing.
		time += System.currentTimeMillis();

		// Print results.
		System.out.println (time + " msec total " + rank);
		if (rank == 0)
			{
			System.out.println
				("pi = 4 * " + count + " / " + N + " = " +
				 (4.0 * count / N));
			}
		}

// Hidden operations.

	/**
	 * Print a usage message and exit.
	 */
	private static void usage()
		{
		System.err.println ("Usage: java -Dpj.np=<K> benchmarks.detinfer.pj.edu.rit.clu.monte.PiClu <seed> <N>");
		System.err.println ("<K> = Number of parallel processes");
		System.err.println ("<seed> = Random seed");
		System.err.println ("<N> = Number of random points");
		System.exit (1);
		}

	}
