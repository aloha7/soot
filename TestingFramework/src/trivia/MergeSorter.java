/*
 * MergeSorter.java
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
public class MergeSorter implements Sorter
{
	public void sort(int [] A)
	{
		recMergeSort(A, 0, A.length, new int[A.length]);
	} // sort
	
	// first is inclusive, last is exclusive
	private void recMergeSort(int [] A, int first, int last, int [] temp)
	{
		if (first >= last - 1) return;
		
		int middle = (first + last) / 2;
		
		recMergeSort(A, first, middle, temp);
		recMergeSort(A, middle, last, temp);
		
		merge(A, first, middle, last, temp);
		
	} // recMergeSort
	
	private void merge(int [] A, int first, int middle, int last, int [] temp)
	{
		int i = first;
		int j = middle;
		
		int k = first;
		
		while (true)
		{
			if (A[i] <= A[j])
			{
				temp[k] = A[i];
				i++;
				k++;
				if (i >= middle)
				{
					System.arraycopy(temp, first, A, first, k - first);
					break;
				}
			}
			else
			{
				temp[k] = A[j];
				j++;
				k++;
				if (j >= last)
				{
					System.arraycopy(A, i, A, last - middle + i, last - k);
					System.arraycopy(temp, first, A, first, k - first);
					break;
				}
			}
		}
	} // merge
	
	public String toString()
	{
		return "merge sort";
	} // toString
	
} // MergeSorter