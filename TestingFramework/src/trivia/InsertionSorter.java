/*
 * InsertionSorter.java
 *
 * Created on July 5, 2001, 8:42 AM
 *
 * See LICENSE file for license conditions.
 */

package trivia;

/**
 *
 * @author  chowells
 * @version 
 */
public class InsertionSorter implements Sorter
{
	public void sort(int [] A)
	{
		// using the default java compiler options, this is faster
		int size = A.length;
		
		for (int i = 1; i < size; i++)
		{
			int val = A[i];
			int j = i - 1;
			while (j >= 0 && val < A[j])
			{
				A[j + 1] = A[j];
				j--;
			}
			A[j + 1] = val;
		} // for
	} // sort
	
	public String toString()
	{
		return "insertion sort";
	} // toString
	
} // InsertionSorter