/*
 * HeapSorter.java
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
public class HeapSorter implements Sorter
{
	public void sort(int [] A)
	{
		int size = A.length;
		
		// build heap in array:
		for (int i = parent(size - 1); i >= 0; i--)
			heapify(A, size, i);
		
		// heap sort array
		while (size > 0)
		{
			size--;
			int temp = A[0];
			A[0] = A[size];
			A[size] = temp;
			
			heapify(A, size, 0);
		}
	} // sort
	
	private void heapify(int [] A, int size, int index)
	{
		int biggest = index;
		int l = left(index);
		int r = right(index);
		
		if (l < size && A[biggest] < A[l]) biggest = l;
		if (r < size && A[biggest] < A[r]) biggest = r;
		
		if (biggest != index)
		{
			int temp = A[biggest];
			A[biggest] = A[index];
			A[index] = temp;
			heapify(A, size, biggest);
		}
		
	} // heapify
	
	private int left(int index)
	{
		return (index * 2) + 1;
	} // left
	
	private int right(int index)
	{
		return (index * 2) + 2;
	} // right
	
	private int parent(int index)
	{
		return (index - 1) / 2;
	} // parent
	
	public String toString()
	{
		return "heap sort";
	} // toString
	
} // HeapSorter