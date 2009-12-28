/*
 * BubbleSorter.java
 *
 * Created on July 5, 2001, 8:36 AM
 *
 * See LICENSE file for license conditions.
 */

package trivia;

/**
 *
 * @author  chowells
 * @version 
 */
public class BubbleSorter implements Sorter
{
	public void sort(int [] A)
	{
		boolean changed = true;
		while (changed)
		{
			changed = false;
			
			for (int i = A.length - 1; i > 0; i--)
			{
				if (A[i] < A[i - 1])
				{
					int temp = A[i];
					A[i] = A[i - 1];
					A[i - 1] = temp;
					changed = true;
				}
			} // for
		} // while
	} // sort
	
	public String toString()
	{
		return "bubble sort";
	} // toString
	
	public static void main(String[] args){
		System.out.println("in Bubble sort;");
	}
}