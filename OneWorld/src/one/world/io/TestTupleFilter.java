/*
 * Copyright (c) 1999, 2000, University of Washington, Department of
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

package one.world.io;

import one.world.core.*;
import one.util.Guid;
import one.util.Bug;
import java.util.Random;
import java.lang.reflect.*;
import java.io.*;
import one.fonda.TestCollection;
import one.fonda.Harness;
// import one.world.io.SerializedTupleFilter;

import java.util.Iterator;


/** 
 * <p>Test set for TupleFilter and SerializedTupleFilter.</p>
 * 
 * <p>The test set does not do the "normal thing" - having a giant switch 
 * on the test number.  Instead, the test number is converted to a 
 * set of characteristics.  This characteristics are then setup, and 
 * the test commences.</p>
 */
public class TestTupleFilter implements TestCollection {

  testCharacteristics test;
  Harness h;
  Tuple targetTuple;

/**
 * The base tuple used for the most tests.  This tuple has elements of many 
 * types to allow many query variants to be tested.
 */
  private static class TestTuple extends Tuple {
    /** A char field */
    public char aChar; 
    /** An int field */
    public int anInt;
    /** A boxed int field */
//    public Integer aBoxedInt;
    /** A float field */
    public float aFloat;
    /** A boolean field(set to false) */
    public boolean aBooleanFalse;
    /** A boolean field(set to true) */
    public boolean aBooleanTrue;
    /** A Guid field */
    public Guid aGuid;
    /** A byte array field */
    public byte[] aByteArray;
    /** A String field */
    public String  aString;
    /** A field holding a pointer to this tuple */
    public TestTuple aTuple;
    /** an object type(pointing to null */
    public Object aNull;
    
    /** Create a TestTuple with no fields initialized*/ 
    public TestTuple() {
    }
    
    /** 
     * Create an initialized TestTuple.  All fields are given some value.
     *
     * @param i The value the anInt field should be initialized to.
     */ 
    public TestTuple(int i) {
      aChar = 'm';
      anInt = i;  //23 + 4
//      aBoxedInt = new Integer(i);
      aFloat=(float)5.5;
      aBooleanFalse = false;
      aBooleanTrue = true;
      aGuid = new Guid ("88888888-8888-8888-8888-888888888888");
      aByteArray = new byte[] {'a', 'b', 'c', 'd'};
      aString = "lmnop";
      aTuple = this;
      aNull = null;
    }
  }

  /**
   * A random small tuple.
   */
  private static class SmallTypeTuple {
    /** A boxed Integer field */
    public Integer jj;

    /** Make an unitialized SmallTypeTuple */
    public SmallTypeTuple() {}
 
    /** 
     * Make a tuple initialized to jj
     *
     * @param jj The value to intialize the tuple field to
     */
    public SmallTypeTuple(Integer jj) {
      this.jj=jj;
    }
    public String toString() {
      return "SmallTypeTyple: jj="+jj.toString();
    }
  }

  /**
   * The main tuple for testing HAS_TYPE and HAS_SUBTYPE queries.
   */
  private static class TypeTestTuple extends Tuple
  {
    /** A String field */
    public String name;
    /** A boxed Integer field */
    public Integer hours;
    /** An unboxed integer field */
    public int testInt;
    /** Create an unitialized tuple */
    public Tuple internalTuple;

    public TypeTestTuple() { }

    /** 
     * Create an initialized tuple 
     *
     * @param name The value to initialize the name field to
     * @param hours The value to initialize the hours field to  
     */
    public TypeTestTuple(String name,Integer hours,Tuple internalTuple) {
      this.name=name;
      this.hours=hours;
      this.testInt=hours.intValue();
      this.internalTuple=internalTuple;
    }

    public String toString() {
      return "TypeTestTuple: name="+name+","+
             "               hours="+hours;
    }
  }

  /**
   * A tuple derived from TypeTestTuple to help in testing
   * HAS_TYPE and HAS_SUBTYPE queries.
   */
  private static class TypeTestTuple2 extends TypeTestTuple 
  {
    /** A String field */
    public String address;

    /** 
     * An object field.  Intended to help test the difference between
     * queries on declared and actual type.
     */
    public Object stt;

    /** Create an uninitialized tuple */
    public TypeTestTuple2() {};
    
    /** 
     * Create an initialized tuple 
     *
     * @param name The value to initialize the name field to
     * @param hours The value to initialize the hours field to  
     * @param address The value to initialize the address field to  
     * @param stt The value to initialize the stt field to  
     */
    public TypeTestTuple2(String name,
                          Integer hours,
                          Tuple internalTuple,
                          String address,
                          Object stt) {
      this.address=address;
      this.name=name;
      this.internalTuple=internalTuple;
      this.hours=hours;
      this.stt=stt;
    };

    public String toString() {
      return "TypeTestTuple2: name="    + name+",\n"+
             "                hours="   + hours+",\n"+
             "                address=" + address+"\n"+
             "                stt="     + stt+"\n";
    }
   
  }
 
  /**
   * <p> A tuple to help in testing HAS_TYPE and HAS_SUBTYPE queries.</p>
   *
   * <p> This tuple is designed to have fields with the same names as
   *     TestTuple, with one having the same type and one having a 
   *     different type than the fields in TypeTestTuple</p>
   */
  private static class TypeTestTuple3 extends Tuple
  {
    /** A String field(Same type as in TypeTestTuple) */
    public String name;
    /** A String field(Different type than in TypeTestTuple) */
    public String hours;

    /** Create an uninitialized tuple */
    public TypeTestTuple3() {};

    /** 
     * Create an initialized tuple 
     *
     * @param name The value to initialize the name field to
     * @param hours The value to initialize the hours field to  
     */
    public TypeTestTuple3(String name,String hours) {
      this.name=name;
      this.hours=hours;
    };
    public String toString() {
      return "TypeTestTuple3: name="+name+",\n"+
             "                hours="+hours+",\n";
    }
  }


 
  /** Get the name of this test collection. */
  public String getName() {
    return "one.world.io.TestTupleFilter";
  }

  /** Get a description for this test collection. */
  public String getDescription() {
    return "Checks serialized and instantiated tuple filters";
  }

  /** Get the number of tests. */
  public int getTestNumber() {
    return testCharacteristics.getTestNumber();
  }

  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return false;
  }

  /** Initialize this test collection. */
  public boolean initialize(Environment env) throws Throwable {
    return false;
  }

  /**
   * A class for breaking a test number into a set of various 
   * characteristics
   */
  static class testCharacteristics {

    /** 
     * If 1, only checks TupleFilter.
     * If 2, also checks SerializedTupleFilter.
     */
    static final int NUM_OF_FILTERS = 1;

    /** number of tests to perform only once per filter type. */
    static final int NUM_INDIVIDUAL = 107;
   
    /** number of base tests with objects derived from
     *  MatchableObject.
    */
    static final int NUM_DIFFERENCE_TESTS = 17;

    /** 
     *  number of base tests with objects derived from 
     *  MatchablePatternObject.
     */
    static final int NUM_PATTERN_TESTS = 13;
    
    /**
     * number of operations possible on a test derived 
     * from MatchableObject.
     */
    static final int NUM_DIFFERENCE_CASES = 6;
  
    /** 
     * number of operations possible on a test derived
     * from MatchablePatternObject.
     */
    static final int NUM_PATTERN_CASES = 9;
    
    /**
     * Possible situations for a comparison:
     *   1 - Name and Type make match possible.
     *   2 - Correct name not found.
     *   3 - Name matches target, but type isn't right.
     *   4 - Target matches as close a possible while being null.
     */
    static final int KINDS_OF_MATCHES = 4;

    /** 
     *  is test case not interesting under difference 
     *  characteristics.
     */ 
    public boolean isIndividual;

    /** Use TupleFilter(0) or SerializedTupleFilter(1) */ 
    public int filterType;

    /** Do we do contains, begins with, and ends with tests?*/
    public boolean isPattern;

    /** Which kind of match (see KINDS_OF_MATCHES) */
    public int matchKind;
  
    /** Which testcase */
    public int testcase;

    /** Which operation */
    public int operation;

    /** Which base test case to use*/
    public int number;

    /** returns number of test */
    static int getTestNumber() {
      return  (NUM_DIFFERENCE_TESTS * NUM_DIFFERENCE_CASES +
	       NUM_PATTERN_TESTS * NUM_PATTERN_CASES) *
	KINDS_OF_MATCHES * NUM_OF_FILTERS;
    }


    /** constructs a testCharacterics object for a 
     *  given test number.
     *
     * @param   number  number of the test case
     */
    testCharacteristics(int number) {
      this.number = number;
      number--;
      
      filterType = number % NUM_OF_FILTERS;
      number /= NUM_OF_FILTERS;

      if (number < NUM_INDIVIDUAL) {
	testcase = number;
	isIndividual = true;
	return;
      }

      number -= NUM_INDIVIDUAL;
      matchKind = number % KINDS_OF_MATCHES;
      number /= KINDS_OF_MATCHES;

      if (number < NUM_DIFFERENCE_TESTS * NUM_DIFFERENCE_CASES) {
	isPattern = false;
	operation = number/ NUM_DIFFERENCE_TESTS;
	testcase = number % NUM_DIFFERENCE_TESTS;
      } else {
	number -= NUM_DIFFERENCE_TESTS * NUM_DIFFERENCE_CASES;
	isPattern = true;
	operation = number / NUM_PATTERN_TESTS;
	testcase = number % NUM_PATTERN_CASES + NUM_DIFFERENCE_TESTS;
      }
    }
  }

  /** Run the specified test. */
  public Object runTest(int number, Harness h, boolean verbose)
    throws Throwable {

    DynamicTuple dTuple;
    test = new testCharacteristics(number);
    this.h = h;
    targetTuple = new TestTuple(7);
    

    if (test.isIndividual) {
      Query testQuery = null;
      String ft = "with the " + ((test.filterType==0)?
				 "n instantiated":
				 " serialized") + " filter";
      switch (test.testcase) {
      case 0:
	h.enterTest(test.number, "(true AND true)+ ft", Boolean.TRUE);
	testQuery = new Query(new Query(), Query.BINARY_AND, new Query());
	break;
      case 1:
	h.enterTest(test.number, "(true AND ~true)+ ft", Boolean.FALSE);
	testQuery = new Query(new Query(), Query.BINARY_AND, 
			      new Query(Query.UNARY_NOT, new Query()));
	break;
      case 2:
	h.enterTest(test.number, "(true OR ~true)+ ft", Boolean.TRUE);
	testQuery = new Query(new Query(), Query.BINARY_OR, 
			      new Query(Query.UNARY_NOT, new Query()));
	break;
      case 3:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"\" == Tuple",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_TYPE,
                              Tuple.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 4:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"\" == TypeTestTuple",
                    Boolean.TRUE);
	testQuery = new Query("", Query.COMPARE_HAS_TYPE,
                              TypeTestTuple.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 5:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"\" == TypeTestTuple2",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_TYPE,
                              TypeTestTuple2.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 6:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"\" == TypeTestTuple3",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_TYPE,
                              TypeTestTuple3.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 7:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"\" == Tuple",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_TYPE,
                              Tuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,
                                         "4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 8:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"\" == TypeTestTuple",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_TYPE,
                              TypeTestTuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,
                                         "4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 9:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"\" == TypeTestTuple2",
                    Boolean.TRUE);
	testQuery = new Query("", Query.COMPARE_HAS_TYPE,
                              TypeTestTuple2.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 10:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"\" == TypeTestTuple3",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_TYPE,
                              TypeTestTuple3.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 11:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"\" == Tuple",
                    Boolean.TRUE);
	testQuery = new Query("", Query.COMPARE_HAS_SUBTYPE,
                              Tuple.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 12:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"\" == TypeTestTuple",
                    Boolean.TRUE);
	testQuery = new Query("", Query.COMPARE_HAS_SUBTYPE,
                              TypeTestTuple.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 13:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"\" == TypeTestTuple2",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_SUBTYPE,
                              TypeTestTuple2.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 14:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"\" == TypeTestTuple3",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_SUBTYPE,
                              TypeTestTuple3.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 15:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple2 field \"\" == Tuple",
                    Boolean.TRUE);
	testQuery = new Query("", Query.COMPARE_HAS_SUBTYPE,
                              Tuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 16:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple2 field \"\" == TypeTestTuple",
                    Boolean.TRUE);
	testQuery = new Query("", Query.COMPARE_HAS_SUBTYPE,
                              TypeTestTuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 17:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple2 field \"\" == TypeTestTuple2",
                    Boolean.TRUE);
	testQuery = new Query("", Query.COMPARE_HAS_SUBTYPE,
                              TypeTestTuple2.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 18:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple2 field \"\" == TypeTestTuple3",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_SUBTYPE,
                              TypeTestTuple3.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 19:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"\" == Tuple",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_DECLARED_TYPE,
                              Tuple.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 20:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"\" == TypeTestTuple",
                    Boolean.TRUE);
	testQuery = new Query("", Query.COMPARE_HAS_DECLARED_TYPE,
                              TypeTestTuple.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 21:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"\" == TypeTestTuple2",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_DECLARED_TYPE,
                              TypeTestTuple2.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 22:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"\" == TypeTestTuple3",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_DECLARED_TYPE,
                              TypeTestTuple3.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 23:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple2 field \"\" == Tuple",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_DECLARED_TYPE,
                              Tuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 24:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple2 field \"\" == TypeTestTuple",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_DECLARED_TYPE,
                              TypeTestTuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 25:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple2 field \"\" == TypeTestTuple2",
                    Boolean.TRUE);
	testQuery = new Query("", Query.COMPARE_HAS_DECLARED_TYPE,
                              TypeTestTuple2.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 26:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"\" == TypeTestTuple3",
                    Boolean.FALSE);
	testQuery = new Query("", Query.COMPARE_HAS_TYPE,
                              TypeTestTuple3.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;

      case 27:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"hours\" == Integer",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_TYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 28:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"hours\" == Integer",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_TYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 29:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple3 field \"hours\" == Integer",
                    Boolean.FALSE);
	testQuery = new Query("hours", Query.COMPARE_HAS_TYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple3("bob", "fifteen");
        break;
      case 30:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"address\" == String",
                    Boolean.FALSE);
	testQuery = new Query("address", Query.COMPARE_HAS_TYPE,
                              String.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 31:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"address\" == String",
                    Boolean.TRUE);
	testQuery = new Query("address", Query.COMPARE_HAS_TYPE,
                              String.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 32:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple3 field \"address\" == String",
                    Boolean.FALSE);
	testQuery = new Query("address", Query.COMPARE_HAS_TYPE,
                              String.class);
        targetTuple = new TypeTestTuple3("bob", "fifteen");
        break;
      case 33:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"stt(really Object)\" == Object",
                    Boolean.TRUE);
	testQuery = new Query("stt", Query.COMPARE_HAS_TYPE,
                              Object.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new Object());
        break;

      case 34:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"stt(really STT)\" == Object",
                    Boolean.FALSE);
	testQuery = new Query("stt", Query.COMPARE_HAS_TYPE,
                              Object.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;

      case 35:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"stt(really Object)\" == STT",
                    Boolean.FALSE);
	testQuery = new Query("stt", Query.COMPARE_HAS_TYPE,
                              SmallTypeTuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new Object());
        break;

      case 36:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple2 field \"stt(really STT)\" == STT",
                    Boolean.TRUE);
	testQuery = new Query("stt", Query.COMPARE_HAS_TYPE,
                              SmallTypeTuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;

      case 37:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"hours\" == Integer",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_SUBTYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 38:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple2 field \"hours\" == Integer",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_SUBTYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 39:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple3 field \"hours\" == Integer",
                    Boolean.FALSE);
	testQuery = new Query("hours", Query.COMPARE_HAS_SUBTYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple3("bob", "fifteen");
        break;
      case 40:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"address\" == String",
                    Boolean.FALSE);
	testQuery = new Query("address", Query.COMPARE_HAS_SUBTYPE,
                              String.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 41:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple2 field \"address\" == String",
                    Boolean.TRUE);
	testQuery = new Query("address", Query.COMPARE_HAS_SUBTYPE,
                              String.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 42:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple3 field \"address\" == String",
                    Boolean.FALSE);
	testQuery = new Query("address", Query.COMPARE_HAS_SUBTYPE,
                              String.class);
        targetTuple = new TypeTestTuple3("bob", "fifteen");
        break;
      case 43:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple2 field \"stt(really Object)\" == Object",
                    Boolean.TRUE);
	testQuery = new Query("stt", Query.COMPARE_HAS_SUBTYPE,
                              Object.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new Object());
        break;

      case 44:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple2 field \"stt(really STT)\" == Object",
                    Boolean.TRUE);
	testQuery = new Query("stt", Query.COMPARE_HAS_SUBTYPE,
                              Object.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;

      case 45:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple2 field \"stt(really Object)\" == STT",
                    Boolean.FALSE);
	testQuery = new Query("stt", Query.COMPARE_HAS_SUBTYPE,
                              SmallTypeTuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new Object());
        break;

      case 46:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple2 field \"stt(really STT)\" == STT",
                    Boolean.TRUE);
	testQuery = new Query("stt", Query.COMPARE_HAS_SUBTYPE,
                              SmallTypeTuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;

      case 47:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"hours\" == Integer",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_DECLARED_TYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 48:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple2 field \"hours\" == Integer",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_DECLARED_TYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 49:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple3 field \"hours\" == Integer",
                    Boolean.FALSE);
	testQuery = new Query("hours", Query.COMPARE_HAS_DECLARED_TYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple3("bob", "fifteen");
        break;
      case 50:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"address\" == String",
                    Boolean.FALSE);
	testQuery = new Query("address", Query.COMPARE_HAS_DECLARED_TYPE,
                              String.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 51:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple2 field \"address\" == String",
                    Boolean.TRUE);
	testQuery = new Query("address", Query.COMPARE_HAS_DECLARED_TYPE,
                              String.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;
      case 52:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple3 field \"address\" == String",
                    Boolean.FALSE);
	testQuery = new Query("address", Query.COMPARE_HAS_DECLARED_TYPE,
                              String.class);
        targetTuple = new TypeTestTuple3("bob", "fifteen");
        break;
      case 53:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple2 field \"stt(really Object)\" == Object",
                    Boolean.TRUE);
	testQuery = new Query("stt", Query.COMPARE_HAS_DECLARED_TYPE,
                              Object.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new Object());
        break;

      case 54:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple2 field \"stt(really STT)\" == Object",
                    Boolean.TRUE);
	testQuery = new Query("stt", Query.COMPARE_HAS_DECLARED_TYPE,
                              Object.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;

      case 55:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple2 field \"stt(really Object)\" == STT",
                    Boolean.FALSE);
	testQuery = new Query("stt", Query.COMPARE_HAS_DECLARED_TYPE,
                              SmallTypeTuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new Object());
        break;

      case 56:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple2 field \"stt(really STT)\" == STT",
                    Boolean.FALSE);
	testQuery = new Query("stt", Query.COMPARE_HAS_DECLARED_TYPE,
                              SmallTypeTuple.class);
        targetTuple = new TypeTestTuple2("bob",new Integer(8),null,"4131 11th",
                                         new SmallTypeTuple(new Integer(34)));
        break;

      case 57:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"hours\" == Integer",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_TYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
       break;
      case 58:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"hours\" == int",
                    Boolean.FALSE);
	testQuery = new Query("hours", Query.COMPARE_HAS_TYPE,
                              Integer.TYPE);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 59:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"hours\" == Object",
                    Boolean.FALSE);
	testQuery = new Query("hours", Query.COMPARE_HAS_TYPE,
                              Object.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;

      case 60:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"testInt\" == Integer",
                    Boolean.FALSE);
	testQuery = new Query("testInt", Query.COMPARE_HAS_TYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
       break;
      case 61:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"testInt\" == int",
                    Boolean.TRUE);
	testQuery = new Query("testInt", Query.COMPARE_HAS_TYPE,
                              Integer.TYPE);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 62:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"testInt\" == Object",
                    Boolean.FALSE);
	testQuery = new Query("testInt", Query.COMPARE_HAS_TYPE,
                              Object.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;


      case 63:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"hours\" == Integer",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_SUBTYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
       break;
      case 64:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"hours\" == int",
                    Boolean.FALSE);
	testQuery = new Query("hours", Query.COMPARE_HAS_SUBTYPE,
                              Integer.TYPE);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 65:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"hours\" == Object",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_SUBTYPE,
                              Object.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;

      case 66:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"testInt\" == Integer",
                    Boolean.FALSE);
	testQuery = new Query("testInt", Query.COMPARE_HAS_SUBTYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
       break;
      case 67:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"testInt\" == int",
                    Boolean.TRUE);
	testQuery = new Query("testInt", Query.COMPARE_HAS_SUBTYPE,
                              Integer.TYPE);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 68:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"testInt\" == Object",
                    Boolean.FALSE);
	testQuery = new Query("testInt", Query.COMPARE_HAS_SUBTYPE,
                              Object.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 69:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"hours\" == Integer",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_DECLARED_TYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
       break;
      case 70:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"hours\" == int",
                    Boolean.FALSE);
	testQuery = new Query("hours", Query.COMPARE_HAS_DECLARED_TYPE,
                              Integer.TYPE);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 71:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"hours\" == Object",
                    Boolean.FALSE);
	testQuery = new Query("hours", Query.COMPARE_HAS_DECLARED_TYPE,
                              Object.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;

      case 72:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"testInt\" == Integer",
                    Boolean.FALSE);
	testQuery = new Query("testInt", Query.COMPARE_HAS_DECLARED_TYPE,
                              Integer.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
       break;
      case 73:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"testInt\" == int",
                    Boolean.TRUE);
	testQuery = new Query("testInt", Query.COMPARE_HAS_DECLARED_TYPE,
                              Integer.TYPE);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 74:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"testInt\" == Object",
                    Boolean.FALSE);
	testQuery = new Query("testInt", Query.COMPARE_HAS_DECLARED_TYPE,
                              Object.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 75:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"internalTuple\"(really null) == Object",
                    Boolean.FALSE);
	testQuery = new Query("internalTuple", Query.COMPARE_HAS_DECLARED_TYPE,
                              Object.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 76:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"internalTuple\"(really null) == Object",
                    Boolean.FALSE);
	testQuery = new Query("internalTuple", Query.COMPARE_HAS_TYPE,
                              Object.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 77:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"internalTuple\"(really null) == Object",
                    Boolean.FALSE);
	testQuery = new Query("internalTuple", Query.COMPARE_HAS_SUBTYPE,
                              Object.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 78:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE TypeTestTuple field \"internalTuple\"(really null) == Tuple",
                    Boolean.TRUE);
	testQuery = new Query("internalTuple", Query.COMPARE_HAS_DECLARED_TYPE,
                              Tuple.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 79:
        h.enterTest(test.number,
                    "HAS_TYPE TypeTestTuple field \"internalTuple\"(really null) == Tuple",
                    Boolean.FALSE);
	testQuery = new Query("internalTuple", Query.COMPARE_HAS_TYPE,
                              Tuple.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 80:
        h.enterTest(test.number,
                    "HAS_SUBTYPE TypeTestTuple field \"internalTuple\"(really null) == Tuple",
                    Boolean.FALSE);
	testQuery = new Query("internalTuple", Query.COMPARE_HAS_SUBTYPE,
                              Tuple.class);
        targetTuple = new TypeTestTuple("bob",new Integer(8),null);
        break;
      case 81:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE DynamicTuple field \"howdyString\"(really String) == String",
                    Boolean.FALSE);
	testQuery = new Query("howdyString", Query.COMPARE_HAS_DECLARED_TYPE,
                              String.class);
        targetTuple = new DynamicTuple();
        targetTuple.set("howdyString",new String("Hi there!!"));
        targetTuple.set("goodNumber",new Integer(42));
        break;
      case 82:
        h.enterTest(test.number,
                    "HAS_TYPE DynamicTuple field \"howdyString\"(really String) == String",
                    Boolean.TRUE);
	testQuery = new Query("howdyString", Query.COMPARE_HAS_TYPE,
                              String.class);
        targetTuple = new DynamicTuple();
        targetTuple.set("howdyString",new String("Hi there!!"));
        targetTuple.set("goodNumber",new Integer(42));
        break;
      case 83:
        h.enterTest(test.number,
                    "HAS_SUBTYPE DynamicTuple field \"howdyString\"(really String) == String",
                    Boolean.TRUE);
	testQuery = new Query("howdyString", Query.COMPARE_HAS_SUBTYPE,
                              String.class);
        targetTuple = new DynamicTuple();
        targetTuple.set("howdyString",new String("Hi there!!"));
        targetTuple.set("goodNumber",new Integer(42));
        break;
      case 84:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE DynamicTuple field \"howdyString\"(really String) == Object",
                    Boolean.TRUE);
	testQuery = new Query("howdyString", Query.COMPARE_HAS_DECLARED_TYPE,
                              Object.class);
        targetTuple = new DynamicTuple();
        targetTuple.set("howdyString",new String("Hi there!!"));
        targetTuple.set("goodNumber",new Integer(42));
        break;
      case 85:
        h.enterTest(test.number,
                    "HAS_TYPE DynamicTuple field \"howdyString\"(really String) == Object",
                    Boolean.FALSE);
	testQuery = new Query("howdyString", Query.COMPARE_HAS_TYPE,
                              Object.class);
        targetTuple = new DynamicTuple();
        targetTuple.set("howdyString",new String("Hi there!!"));
        targetTuple.set("goodNumber",new Integer(42));
        break;
      case 86:
        h.enterTest(test.number,
                    "HAS_SUBTYPE DynamicTuple field \"howdyString\"(really String) == Object",
                    Boolean.TRUE);
	testQuery = new Query("howdyString", Query.COMPARE_HAS_SUBTYPE,
                              Object.class);
        targetTuple = new DynamicTuple();
        targetTuple.set("howdyString",new String("Hi there!!"));
        targetTuple.set("goodNumber",new Integer(42));
        break;
      case 87:
        h.enterTest(test.number,
                    "HAS_DECLARED_TYPE DynamicTuple field \"invalidField\"(no such field) == Object",
                    Boolean.FALSE);
	testQuery = new Query("invalidField", Query.COMPARE_HAS_DECLARED_TYPE,
                              Object.class);
        targetTuple = new DynamicTuple();
        targetTuple.set("howdyString",new String("Hi there!!"));
        targetTuple.set("goodNumber",new Integer(42));
        break;
      case 88:
        h.enterTest(test.number,
                    "HAS_TYPE DynamicTuple field \"invalidField\"(no such field) == Object",
                    Boolean.FALSE);
	testQuery = new Query("invalidField", Query.COMPARE_HAS_TYPE,
                              Object.class);
        targetTuple = new DynamicTuple();
        targetTuple.set("howdyString",new String("Hi there!!"));
        targetTuple.set("goodNumber",new Integer(42));
        break;
      case 89:
        h.enterTest(test.number,
                    "HAS_SUBTYPE DynamicTuple field \"invalidField\"(no such field) == Object",
                    Boolean.FALSE);
	testQuery = new Query("invalidField", Query.COMPARE_HAS_SUBTYPE,
                              Object.class);
        targetTuple = new DynamicTuple();
        targetTuple.set("howdyString",new String("Hi there!!"));
        targetTuple.set("goodNumber",new Integer(42));
        break;

      case 90:
        h.enterTest(test.number,
                   "HAS_FIELD DynamicTuple field \"test\"(exists, String)",
                    Boolean.TRUE);
	testQuery = new Query("test", Query.COMPARE_HAS_FIELD,
                              null);
        targetTuple = new DynamicTuple();
        targetTuple.set("test",new String("Hi there!!"));
        break;

      case 91:
        h.enterTest(test.number,
                   "HAS_FIELD DynamicTuple field \"test\"(exists, Object)",
                    Boolean.TRUE);
	testQuery = new Query("test", Query.COMPARE_HAS_FIELD,
                              null);
        targetTuple = new DynamicTuple();
        targetTuple.set("test",new Object());
        break;
      case 92:
        h.enterTest(test.number,
                   "HAS_FIELD DynamicTuple field \"test\"(exists, null)",
                    Boolean.TRUE);
	testQuery = new Query("test", Query.COMPARE_HAS_FIELD,
                              null);
        targetTuple = new DynamicTuple();
        targetTuple.set("test",null);
        break;

      case 93:
        h.enterTest(test.number,
                   "HAS_FIELD DynamicTuple field \"invalidTuple\"(doesn't exist)",
                    Boolean.FALSE);
	testQuery = new Query("invalidTuple", Query.COMPARE_HAS_FIELD,
                              null);
        targetTuple = new DynamicTuple();
        targetTuple.set("test",new Object());

        break;
      case 94:
        h.enterTest(test.number,
                   "HAS_FIELD TypeTestTuple field \"hours\"(exists, Integer)",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_FIELD,
                              null);
        targetTuple = new TypeTestTuple("hi there",new Integer(34),null);
        break;
      case 95:
        h.enterTest(test.number,
                   "HAS_FIELD TypeTestTuple field \"hours\"(exists, null)",
                    Boolean.TRUE);
	testQuery = new Query("hours", Query.COMPARE_HAS_FIELD,
                              null);
        targetTuple = new TypeTestTuple();
        break;
      case 96:
        h.enterTest(test.number,
                   "HAS_FIELD TypeTestTuple field \"testInt\"(exists, int)",
                    Boolean.TRUE);
	testQuery = new Query("testInt", Query.COMPARE_HAS_FIELD,
                              null);
        targetTuple = new TypeTestTuple("hi there",new Integer(34),null);
        break;
      case 97:
        h.enterTest(test.number,
                   "HAS_FIELD TypeTestTuple field \"invalidField\"(doesn't exist)",
                    Boolean.FALSE);
	testQuery = new Query("invalidField", Query.COMPARE_HAS_FIELD,
                              null);
        targetTuple = new TypeTestTuple("hi there",new Integer(34),null);
        break;
//NEW
      case 98: {
          DynamicTuple dt1 = new DynamicTuple();
          DynamicTuple dt2 = new DynamicTuple();
          DynamicTuple dt3 = new DynamicTuple();
          dt1.set("test",dt2);
          dt1.set("bad",new Object());
          dt2.set("of",dt3);
          dt2.set("final","Hello");
          dt3.set("nesting",new Integer(5));
          h.enterTest(test.number,"Single nested HasField",Boolean.TRUE);
          targetTuple = dt1;
          testQuery = new Query("test.final", Query.COMPARE_HAS_FIELD,
                              null);
        }
        break;
      case 99: {
          DynamicTuple dt1 = new DynamicTuple();
          DynamicTuple dt2 = new DynamicTuple();
          DynamicTuple dt3 = new DynamicTuple();
          dt1.set("test",dt2);
          dt1.set("bad",new Object());
          dt2.set("of",dt3);
          dt2.set("final","Hello");
          dt3.set("nesting",new Integer(5));
          h.enterTest(test.number,"Double nested HasField",Boolean.TRUE);
          targetTuple = dt1;
          testQuery = new Query("test.of.nesting", Query.COMPARE_HAS_FIELD,
                              null);
        }
        break;
      case 100: {
          DynamicTuple dt1 = new DynamicTuple();
          DynamicTuple dt2 = new DynamicTuple();
          DynamicTuple dt3 = new DynamicTuple();
          dt1.set("test",dt2);
          dt1.set("bad",new Object());
          dt2.set("of",dt3);
          dt2.set("final","Hello");
          dt3.set("nesting",new Integer(5));
          h.enterTest(test.number,"Nonexistent Double nested HasField",Boolean.FALSE);
          targetTuple = dt1;
          testQuery = new Query("test.ntt.nesting", Query.COMPARE_HAS_FIELD,
                              null);
        }
        break;
      case 101: {
          DynamicTuple dt1 = new DynamicTuple();
          DynamicTuple dt2 = new DynamicTuple();
          DynamicTuple dt3 = new DynamicTuple();
          dt1.set("test",dt2);
          dt1.set("bad",new Object());
          dt2.set("of",dt3);
          dt2.set("final","Hello");
          dt3.set("nesting",new Integer(5));
          h.enterTest(test.number,"Nonexistent Double nested HasField",Boolean.FALSE);
          targetTuple = dt1;
          testQuery = new Query("test.of.ggh", Query.COMPARE_HAS_FIELD,
                              null);
        }
        break;
      case 102: {
          DynamicTuple dt1 = new DynamicTuple();
          DynamicTuple dt2 = new DynamicTuple();
          DynamicTuple dt3 = new DynamicTuple();
          dt1.set("test",dt2);
          dt1.set("bad",new Object());
          dt2.set("of",dt3);
          dt2.set("final","Hello");
          dt3.set("nesting",new Integer(5));
          h.enterTest(test.number,"Nonexistent Double nested HasField",Boolean.FALSE);
          targetTuple = dt1;
          testQuery = new Query("test.final.nesting", Query.COMPARE_HAS_FIELD,
                              null);
        }
        break;
      case 103: {
          DynamicTuple dt1 = new DynamicTuple();
          DynamicTuple dt2 = new DynamicTuple();
          DynamicTuple dt3 = new DynamicTuple();
          dt1.set("test",dt2);
          dt1.set("bad",new Object());
          dt2.set("of",dt3);
          dt2.set("final","Hello");
          dt3.set("nesting",new Integer(5));
          h.enterTest(test.number,"Double nested HAS_TYPE",Boolean.TRUE);
          targetTuple = dt1;
          testQuery = new Query("test.of.nesting", Query.COMPARE_HAS_TYPE,
                              Integer.class);
        }
        break;
      case 104: {
          DynamicTuple dt1 = new DynamicTuple();
          DynamicTuple dt2 = new DynamicTuple();
          DynamicTuple dt3 = new DynamicTuple();
          dt1.set("test",dt2);
          dt1.set("bad",new Object());
          dt2.set("of",dt3);
          dt2.set("final","Hello");
          dt3.set("nesting",new Integer(5));
          h.enterTest(test.number,"Double nested HAS_TYPE",Boolean.FALSE);
          targetTuple = dt1;
          testQuery = new Query("test.of.nesting", Query.COMPARE_HAS_TYPE,
                              Object.class);


        }
        break;
     case 105: {
          DynamicTuple dt1 = new DynamicTuple();
          DynamicTuple dt2 = new DynamicTuple();
          DynamicTuple dt3 = new DynamicTuple();
          dt1.set("test",dt2);
          dt1.set("bad",new Object());
          dt2.set("of",dt3);
          dt2.set("final","Hello");
          dt3.set("nesting",new Integer(5));
          h.enterTest(test.number,"Double nested GREATER",Boolean.TRUE);
          targetTuple = dt1;
          testQuery = new Query("test.of.nesting", Query.COMPARE_GREATER
                              ,new Integer(3));


        }
        break;

     case 106: {
          DynamicTuple dt1 = new DynamicTuple();
          DynamicTuple dt2 = new DynamicTuple();
          DynamicTuple dt3 = new DynamicTuple();
          dt1.set("test",dt2);
          dt1.set("bad",new Object());
          dt2.set("of",dt3);
          dt2.set("final","Hello");
          dt3.set("nesting",new Integer(5));
          h.enterTest(test.number,"Double nested GREATER",Boolean.FALSE);
          targetTuple = dt1;
          testQuery = new Query("test.of.nesting", Query.COMPARE_GREATER
                              ,new Integer(7));


        }
        break;

      default:
        h.enterTest(test.number,
                    "Error: Test length mismatch",
                    Boolean.TRUE);
        return Boolean.FALSE;
        
      }
 

      return doFiltering(testQuery);
    }
    
    switch (test.testcase) {
    case 0:
      return go("7", "7", "anInt", new Integer(7), 41);
    case 1:
      return go("7", "-565","anInt", new Integer(-565), 14);
    case 2: 
      return go("7", "3","anInt", new Integer(3), 14);
    case 3:  
      return go("7", "9","anInt", new Integer(9), 50);
    
    case 4:
      return go("m", "m","aChar", new Character('m'), 41); 
    case 5:
      return go("m", "b","aChar", new Character('b'), 14);
    case 6:
      return go("m", "y","aChar", new Character('y'), 50);

    case 7:
      return go("5.5", "5.5","aFloat", new Float(5.5), 41); 
    case 8:
      return go("5.5", "-45","aFloat", new Float(-45), 14);
    case 9:
      return go("5.5", "5.55","aFloat", new Double(5.55), 50);

    case 10:
      return go("true", "true","aBooleanTrue", new Boolean(true), 1);//was 41 
    case 11:    
      return go("true", "false","aBooleanTrue", new Boolean(false), 2);//was 14

    case 12:
      return go("false", "true","aBooleanFalse", new Boolean(true), 2);//was 50
    case 13: 
      return go("false", "false","aBooleanFalse", new Boolean(false), 1);//was 41
      
    case 14:
      return go("88...", "88...","aGuid",
                new Guid ("88888888-8888-8888-8888-888888888888"), 1); //was 41 
    case 15:
      return go("88...", "66...","aGuid",
                new Guid ("66666666-6666-6666-6666-666666666666"), 2); //was 14
    case 16:
      return go("88...", "BB...","aGuid",
                new Guid ("BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB"), 2); // was 50
      // includes pattern tests for here on
    case 17:
      return go("abcd", "abcd","aByteArray", new byte[] {'a', 'b', 'c', 'd'},
                489); 
    case 18:
      return go("abcd", "abcde","aByteArray", new byte[] {'a', 'b', 'c', 'd',
                                                          'e'},  14);
    case 19:
      return go("abcd", "abc","aByteArray", new byte[] {'a', 'b', 'c'}, 242);
    case 20:
      return go("abcd", "zbcd","aByteArray", new byte[] {'z', 'b', 'c', 'd'},
                14);
    case 21:
      return go("abcd", "aacd","aByteArray", new byte[] {'a', 'a', 'c', 'd'},
                50); 
    case 22:
      return go("abcd", "bc","aByteArray", new byte[] {'b', 'c',}, 142);
    case 23:
      return go("abcd", "cd","aByteArray", new byte[] {'c', 'd'}, 398); 
      
    case 24:
      return go("lmnop", "lmnop", "aString", "lmnop", 489); 
    case 25:
      return go("lmnop", "lmno", "aString", "lmno", 206);
    case 26:
      return go("lmnop", "lmnopq", "aString", "lmnopq", 50);
    case 27:
      return go("lmnop", "no", "aString", "no", 178);
    case 28:
      return go("lmnop", "acz", "aString", "acz", 14);
    case 29:
      return go("lmnop", "op", "aString", "op", 434);
    }
    return null;
  }

  /** Clean up this test collection. */
  public void cleanup() {
    // Nothing to do.
  }

  /**
   * Given paramaters and the outter class variable
   * testCharacteristics, setup and perform the test.  
   */

  Boolean go(String lhs, String rhs, String fieldname, 
	     Object value, int answers) throws Throwable {
    boolean answer = false;
    String op = null;
    String name = null;
    
    switch (test.operation) {
    case 0:
      op = "==";
      break;
    case 1:
      op = "!=";
      break;
    case 2:
      op = ">";
      break;
    case 3:
      op = ">=";
      break;
    case 4:
      op = "<";
      break;
    case 5:
      op = "<=";
      break;
    case 6:
      op = "starts with";
      break;
    case 7:
      op = "contains";
      break;
    case 8:
      op = "ends with";
      break;
    }
    switch (test.matchKind) {
    case 0:
      if (test.operation==1) {
        answer = !(  (answers&(1<<0)) != 0);
      } else {
        answer = (  (answers&(1<<test.operation)) != 0);
      }
      name = "\nIs "+fieldname+ "(equal to "+lhs+") on a"+ 
	((test.filterType==0)?"n instantiated":" serialized")
	+" tuple "+op+" "+rhs;
      break;
    case 1:
      if (test.operation==1) {
        answer=true;
      }
      name = "\nIs a non-existant field on a"+ 
	((test.filterType==0)?"n instantiated":" serialized") 
	+" tuple "+op+" "+rhs;
      fieldname = "UnFieldable";
      break;
    case 2:
      if (test.operation==1) {
        answer=true;
      }
      // Changed from using just the test.number as seed because that only 
      // ranges a small amount which results in all random numbers starting
      // with 0.7...  
      Random rnd = new Random((Long.MAX_VALUE/getTestNumber())*test.number);
      Field fields[] = targetTuple.getClass().getDeclaredFields();

      String potentialname;
      do {
	potentialname = fields[(int)(rnd.nextDouble() * 
				     (double)fields.length)].getName();
      } while (fieldname.equals(potentialname) ||
	     (fieldname.equals("aChar") && potentialname.equals("anInt")) || 
	     (fieldname.equals("anInt") && potentialname.equals("aChar")) ||
	     (fieldname.equals("aBooleanFalse") && 
	      potentialname.equals("aBooleanTrue")) || 
	     (fieldname.equals("aBooleanTrue") && 
	      potentialname.equals("aBooleanFalse")));

      fieldname = potentialname;
   
      name =  "\nIs "+fieldname + " on a"+ 
	((test.filterType==0)?"n instantiated":" serialized") 
	+ " tuple " + op + " " + rhs;
      break;
    case 3:
      if (test.operation==1) {
        answer=true;
      }
      if (fieldname != "aByteArray" &&
	  fieldname != "aString") {
	fieldname = "aGuid";
      }
      targetTuple.getClass().getField(fieldname).set(targetTuple, null);
      
      name = "\nIs "+fieldname+ "(equal to null) on a"+ 
	((test.filterType==0)?"n instantiated":" serialized")
	+" tuple "+op+" "+rhs;

      break;
    }
    
    h.enterTest(test.number, name, new Boolean(answer));
 
    Query testQuery = new Query(fieldname, 
				Query.COMPARE_EQUAL+test.operation, 
				value);
 
    /*
    TupleFilter filter = new TupleFilter(testQuery);
    return new Boolean( filter.check(targetTuple));
    */
    return doFiltering(testQuery);
  }

  Boolean doFiltering(Query testQuery) throws IOException, Exception{
    if (test.filterType == 0) {
      TupleFilter filter = new TupleFilter(testQuery);
      return new Boolean( filter.check(targetTuple));
    } else {
      /*
         Insert serialized filter test here.
      */
      return null;
      }
  }
}



