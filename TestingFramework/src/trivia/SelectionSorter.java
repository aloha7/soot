/*
 * SelectionSorter.java
 *
 * Created on July 5, 2001, 8:41 AM
 *
 * See LICENSE file for license conditions.
 */

package trivia;

/**
 *
 * @author  chowells
 * @version 
 */
public class SelectionSorter implements Sorter
{
	public void sort(int [] A)
	{
		for (int i = A.length - 1; i > 0; i--)
		{
			int maxIndex = i;
			for (int j = i - 1; j >= 0; j--)
				if (A[maxIndex] < A[j])
					maxIndex = j;
			
			int temp = A[i];
			A[i] = A[maxIndex];
			A[maxIndex] = temp;
		} // for
	} // sort
	
	public String toString()
	{
		return "selection sort";
	} // toString
	
} // SelectionSorter