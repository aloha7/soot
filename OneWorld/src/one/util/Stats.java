/*
 * Copyright (c) 2001, University of Washington, Department of
 * Computer Science and Engineering.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither name of the University of Washington, Department of
 * Computer Science and Engineering nor the names of its contributors
 * may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package one.util;

import java.util.ArrayList;

/** 
 * Implementation of a simple statistics collector. Provides a way to keep 
 * track of a group of numbers and perform
 * statistical calculations on them, as well as providing static
 * methods for performing statistical calculations on arrays of
 * numbers.
 * <p>
 * Note: this class uses an unsynchronized internal data representation,
 * so all operations on instances of this object should be 
 * <code>synchronized</code> in
 * multithreaded environments. 
 *
 * @version  $Revision: 1.4 $
 * @author   Adam MacBeth
 */
public class Stats implements java.io.Serializable {

  /** An empty array of Doubles. */
  private static final Double[] DOUBLE_ARRAY = new Double[0];

  /** The list of numbers. */
  private ArrayList list;

  /** Constructs an empty stats object. */
  public Stats() {
    list = new ArrayList();
  }

  /** Clear the numbers stored in this object. */
  public void reset() {
    list.clear();
  }

  /** Returns the number of numbers stored in this object. 
   *
   * @return  The number of numbers in stored in this object.
   */
  public int size() {
    return list.size(); 
  }

  /** Add a double to the collection of numbers. 
   *
   * @param  num  The double to add to the internal list.
   */
  public void add(double num) {
    list.add(new Double(num));
  }

  /** Add an int to the collection of numbers.
   *
   * @param  num  The number to add to the internal list.
   */
  public void add(int num) {
    list.add(new Double(num));
  }

  /** Compute the average of the numbers stored in this object. 
   *
   * @return  The average of the numbers stored in this object, or zero
   * if there are no stored numbers.
   */
  public double average() {
    return Stats.average((Double[])(list.toArray(DOUBLE_ARRAY)));
  }

  /** Compute the standard deviation of the numbers stored in this object. 
   *
   * @return  The average of the numbers stored in this object, or zero
   * if there are no stored numbers.
   */
  public double stdev() {
    return Stats.stdev((Double[])(list.toArray(DOUBLE_ARRAY)));
  }

  /** Compute the average of an array of doubles.
   *
   * @param  numbers  The array of numbers of which to calculate the average.
   *
   * @return  The average of the numbers stored in the specified array, or zero
   * if the array is empty
   */
  public static double average(double[] numbers) {
    
    double sum = 0;

    if (0 == numbers.length) return 0;
    for(int i = 0; i < numbers.length; i++) {
      sum += numbers[i];
    }
    return sum/(double)numbers.length;
  }

  /** Compute the average of an array of doubles. 
   *
   * @param  numbers  The array of numbers of which to calculate the average.
   * 
   * @return  The average of the numbers stored in the specified array,
   * or zero if the array is empty. 
   */
  public static double average(Double[] numbers) {
    
    double sum = 0;

    if (0 == numbers.length) return 0;
    for(int i = 0; i < numbers.length; i++) {
      sum += numbers[i].doubleValue();
    }
    return sum/(double)numbers.length;
  }

  /** Compute the average of an array of integers. 
   *
   * @param  numbers  The array of numbers of which to calculate the average.
   *
   * @return  The average of the numbers stored in the specified array,
   * or zero if the array is empty.
   */
  public static double average(int[] numbers) {
      
    double sum = 0;

    if (0 == numbers.length) return 0;
    for(int i = 0; i < numbers.length; i++) {
      sum += numbers[i];
    }
    return sum/(double)numbers.length;
  }

 /** Compute the average of an array of integers. 
  *
  * @param  numbers  The array of numbers of which to calculate the average.
  *
  * @return  The average of the numbers in the specified array, or zero
  * if the array is empty.
  */
  public static double average(Integer[] numbers) {
      
    double sum = 0;

    if (0 == numbers.length) return 0;
    for(int i = 0; i < numbers.length; i++) {
      sum += numbers[i].doubleValue();
    }
    return sum/(double)numbers.length;
  }

  /** Compute the standard deviation of an array of doubles. 
   *
   * @param  numbers  The array of numbers of which to calculate the average.
   *
   * @return  The standard deviation of the numbers in the specified array,
   * or zero is the array is empty.
   */
  public static double stdev(double[] numbers) {
    double average = average(numbers);
    double sum = 0;

    if (0 == numbers.length) return 0;
    for(int i = 0; i < numbers.length; i++) {
      sum += Math.pow( (average - numbers[i]), 2);
    }
    return Math.sqrt(sum/(double)numbers.length);

  }

  /** Compute the standard deviation of an array of doubles. 
   *
   * @param  numbers  The array of numbers of which to calculate the standard
   * deviation.
   *
   * @return  The standard deviation of the numbers in the specified array,
   * or zero if the array is empty.
   */
  public static double stdev(Double[] numbers) {
    double average = average(numbers);
    double sum = 0;
    if (0 == numbers.length) return 0;
    for(int i = 0; i < numbers.length; i++) {
      sum += Math.pow( (average - numbers[i].doubleValue()), 2);
    }
    return Math.sqrt(sum/(double)numbers.length);

  }

  /** Compute the standard deviation of an array of integers. 
   *  
   * @param  numbers  The array of numbers of which to calculate the standard
   * deviation.
   *
   * @return  The standard deviation of the numbers in the specified array, or 
   * zero if the array is empty.
   */
  public static double stdev(int[] numbers) {
    double average = average(numbers);
    double sum = 0;

    if (0 == numbers.length) return 0;
    for(int i = 0; i < numbers.length; i++) {
      sum += Math.pow( (average - numbers[i]), 2);
    }
    return Math.sqrt(sum/(double)numbers.length);

  }

  /** Computes the standard deviation of the specified array of Integers. 
   * 
   * @param  numbers  The array of numbers of which to calculate the standard
   * deviation.
   *
   * @return  The standard deviation of the numbers in the specified array,
   * or zero if the array is empty.
   */
  public static double stdev(Integer[] numbers) {
    double average = average(numbers);
    double sum = 0;

    if (0 == numbers.length) return 0;
    for(int i = 0; i < numbers.length; i++) {
      sum += Math.pow( (average - numbers[i].doubleValue()), 2);
    }
    return Math.sqrt(sum/(double)numbers.length);

  }

  /** 
   * Runs simple tests of both the static and instance methods of the class.
   */
  public static void main(String[] args) {
    //test
    int[] nums = { 1, 2, 3, 4, 5, 6, 7, 8 };
    System.out.println("Average = " + average(nums));
    System.out.println("Stdev = " + stdev(nums));

    Stats stat = new Stats();
    stat.add(1);
    stat.add(2);
    stat.add(3);
    stat.add(4);
    stat.add(5);
    stat.add(6);
    stat.add(7);
    stat.add(8);
    System.out.println("Average = " + stat.average());
    System.out.println("Stdev = " + stat.stdev());

    //empty list tests
    stat.reset();
    System.out.println("Average = " + stat.average());
    System.out.println("Stdev = " + stat.stdev());

    nums = new int[0];
    System.out.println("Average = " + average(nums));
    System.out.println("Stdev = " + stdev(nums));
  }
}
