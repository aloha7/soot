/*
 * SortDemo.java
 *
 * Created on July 5, 2001, 8:11 AM
 *
 * See LICENSE file for license conditions.
 */

package trivia;

/**
 *
 * @author  chowells
 * @version 
 */
public class SortDemo
{
	public static void main(String [] args)
	{
		int sortnum = -1;
		int size = -1;
		
		try
		{
			sortnum = Integer.parseInt(args[0]);
			size = Integer.parseInt(args[1]);
		}
		catch (Exception e)
		{
			inputError();
		}
		
		boolean verify = false;
		try
		{
			verify = "verify".equalsIgnoreCase(args[2]);
		}
		catch (IndexOutOfBoundsException e) {}
		
		
		Sorter s = null;
		
		switch (sortnum)
		{
			case 0:
				s = new BubbleSorter();
				break;
			case 1:
				s = new SelectionSorter();
				break;
			case 2:
				s = new InsertionSorter();
				break;
			case 3:
				s = new HeapSorter();
				break;
			case 4:
				s = new MergeSorter();
				break;
			case 5:
				s = new QuickSorter();
				break;
			default:
				inputError();
				break;
		}
		
		
		int [] A = null;
		try
		{
			A = createRandomArray(size);
		}
		catch (Exception e)
		{
			inputError();
		}
		
		long time = System.currentTimeMillis();
		s.sort(A);
		time = System.currentTimeMillis() - time;
		
		System.out.println("Using " + s + ", sorting " + size + " elements took " + time + " milliseconds.");
		
		if (verify)
		{
			if (sorted(A))
				System.out.println("The resulting order from the sort was correct");
			else
				System.out.println("The resulting order from the sort was incorrect");
		}
		
	} // main
	
	public static boolean sorted(int [] A)
	{
		for (int i = A.length - 1; i > 0; i--)
			if (A[i] < A[i - 1])
				return false;
		
		return true;
	} // sorted
	
	public static int [] createRandomArray(int size)
	{
		if (size < 0) throw new IllegalArgumentException();
		java.util.Random r = new java.util.Random(size);
		int [] result = new int[size];
		
		for (int i = size - 1; i >= 0; i--)
			result[i] = r.nextInt();
		
		return result;
	} // createRandomArray
	
	public static void inputError()
	{
		System.out.println("Error reading input.  Please invoke with:");
		System.out.println("  java example.SortDemo sortnum sortsize [verify]");
		System.out.println();
		System.out.println("where sortnum is one of:");
		System.out.println("  0 - Bubble Sort");
		System.out.println("  1 - Selection Sort");
		System.out.println("  2 - Insertion Sort");
		System.out.println("  3 - Heap Sort");
		System.out.println("  4 - Merge Sort");
		System.out.println("  5 - Quick Sort");
		System.out.println();
		System.out.println("Sortsize is the positive number of integers to sort");
		System.out.println();
		System.out.println("The array tested with will always be the same for a");
		System.out.println("given size, as the size is used as the seed for the");
		System.out.println("random number generator");
		System.out.println();
		System.out.println("verify is optional.  If the word verify is there,");
		System.out.println("the sort results will be verified");
		System.exit(1);
	} // inputError
	
} // SortDemo