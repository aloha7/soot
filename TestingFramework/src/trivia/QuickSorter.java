/*
 * QuickSorter.java
 *
 * Created on July 5, 2001, 8:43 AM
 *
 * See LICENSE file for license conditions.
 */

package trivia;

/**
 *
 * @author  chowells
 * @version 
 */
public class QuickSorter implements Sorter
{
	public void sort(int [] A)
	{
		recQuickSort(A, 0, A.length);
	} // sort
	
	// first is inclusive, last is exclusive
	private void recQuickSort(int [] A, int first, int last)
	{
		if (first >= last - 1) return;
		
		int middle = partition(A, first, last);
		
		recQuickSort(A, first, middle);
		recQuickSort(A, middle + 1, last);
	} // recQuickSort
	
	// first is inclusive, last is exclusive
	private int partition(int [] A, int first, int last)
	{
		int pivot = first;
		int temp = A[pivot];
		
		for (int i = first; i < last; i++)
		{
			if (A[i] < temp)
			{
				A[pivot] = A[i];
				pivot++;
				A[i] = A[pivot];
			}
		} // for
		
		A[pivot] = temp;
		
		return pivot;
	} // partition
	
	public String toString()
	{
		return "quick sort";
	} // toString
	
} // QuickSorter