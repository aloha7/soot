/*
 * Copyright (c) 1999, 2000, Robert Grimm.
 *    All rights reserved.
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
 * 3. Neither name of Robert Grimm nor the names of his contributors
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

package one.eval;

import java.util.List;

import one.util.Bug;
import one.util.NegativeSizeException;

/**
 * Implementation of the Scheme standard procedures that apply to data
 * structures. This class implements the operators defined in
 * &sect;6.1, &sect;6.2, and &sect;6.3 of R<sup><font
 * size="-1">5</font></sup>RS.
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Data extends AbstractApplicable {

  // ======================================================================
  //                               Constants.
  // ======================================================================

  /** The opcodes. */
  private static final int
    // 6.1 Equivalence predicates
    EQV_Q = 30001, EQ_Q = 30002, EQUAL_Q = 30003,

    // 6.2 Numbers
    NUMBER_Q = 30004, COMPLEX_Q = 30005, REAL_Q = 30006, RATIONAL_Q = 30007,
    INTEGER_Q = 30008, EXACT_Q = 30009, INEXACT_Q = 30010, ZERO_Q = 30011,
    POSITIVE_Q = 30012, NEGATIVE_Q = 30013, ODD_Q = 30014, EVEN_Q = 30015,
    MAX = 30016, MIN = 30017, ADD = 30018, MULTIPLY = 30019, SUBTRACT = 30020,
    DIVIDE = 30021, ABS = 30022, QUOTIENT = 30023, REMAINDER = 30024,
    MODULO = 30025, GCD = 30026, LCM = 30027, NUMERATOR = 30028,
    DENOMINATOR = 30029, FLOOR = 30030, CEILING = 30031, TRUNCATE = 30032,
    ROUND = 30033, RATIONALIZE = 30034, EXP = 30035, LOG = 30036, SIN = 30037,
    COS = 30038, TAN = 30039, ASIN = 30040, ACOS = 30041, ATAN = 30042,
    SQRT = 30043, EXPT = 30044, MAKE_RECTANGULAR = 30045,
    MAKE_POLAR = 30046, REAL_PART = 30047, IMAG_PART = 30048, MAGNITUDE = 30049,
    ANGLE = 30050, EXACT_TO_INEXACT = 30051, INEXACT_TO_EXACT = 30052,
    NUMBER_TO_STRING = 30053, STRING_TO_NUMBER = 30054,

    NUM_EQ = 30055, NUM_LQ = 30056, NUM_GQ = 30057, NUM_LEQ = 30058,
    NUM_GEQ = 30059,

    // 6.3.1 Booleans
    NOT = 30060, BOOLEAN_Q = 30061,

    // 6.3.2 Pairs and lists
    PAIR_Q = 30070, CONS = 30071, CAR = 30072, CDR = 30073, SET_CAR = 30074,
    SET_CDR = 30075, NULL_Q = 30076, LIST_Q = 30077, LIST = 30078,
    LENGTH = 30079, APPEND = 30080, REVERSE = 30081, LIST_TAIL = 30082,
    LIST_REF = 30083, MEMQ = 30084, MEMV = 30085, MEMBER = 30086, ASSQ = 30087,
    ASSV = 30088, ASSOC = 30089,

    CAAR = 30090, CADR = 30091, CDAR = 30092, CDDR = 30093,
    CAAAR = 30094, CAADR = 30095, CADAR = 30096, CADDR = 30097, CDAAR = 30098,
    CDADR = 30099, CDDAR = 30100, CDDDR = 30101,
    CAAAAR = 30102, CAAADR = 30103, CAADAR = 30104, CAADDR = 30105,
    CADAAR = 30106,
    CADADR = 30107, CADDAR = 30108, CADDDR = 30109, CDAAAR = 30110,
    CDAADR = 30111,
    CDADAR = 30112, CDADDR = 30113, CDDAAR = 30114, CDDADR = 30115,
    CDDDAR = 30116,
    CDDDDR = 30117,

    // 6.3.3 Symbols
    SYMBOL_Q = 30130, SYMBOL_TO_STRING = 30131, STRING_TO_SYMBOL = 30132,

    // 6.3.4 Characters
    CHAR_Q = 30140, CHAR_EQ = 30141, CHAR_LQ = 30142, CHAR_GQ = 30143,
    CHAR_LEQ = 30144, CHAR_GEQ = 30145, CHAR_CI_EQ = 30146, CHAR_CI_LQ = 30147,
    CHAR_CI_GQ = 30148, CHAR_CI_LEQ = 30149, CHAR_CI_GEQ = 30150,
    CHAR_ALPHABETIC_Q = 30151, CHAR_NUMERIC_Q = 30152,
    CHAR_WHITESPACE_Q = 30153, CHAR_UPPER_CASE_Q = 30154,
    CHAR_LOWER_CASE_Q = 30155, CHAR_TO_INTEGER = 30156,
    INTEGER_TO_CHAR = 30157, CHAR_UPCASE = 30158, CHAR_DOWNCASE = 30159,

    // 6.3.5 Strings
    STRING_Q = 30170, MAKE_STRING = 30171, STRING = 30172,
    STRING_LENGTH = 30173, STRING_REF = 30174, STRING_SET = 30175,
    STRING_EQ = 30176, STRING_CI_EQ = 30177, STRING_LQ = 30178,
    STRING_GQ = 30179, STRING_LEQ = 30180, STRING_GEQ = 30181,
    STRING_CI_LQ = 30182, STRING_CI_GQ = 30183, STRING_CI_LEQ = 30184,
    STRING_CI_GEQ = 30185, SUBSTRING = 30186, STRING_APPEND = 30187,
    STRING_TO_LIST = 30188, LIST_TO_STRING = 30189, STRING_COPY = 30190,
    STRING_FILL = 30191,

    // 6.3.6 Vectors
    VECTOR_Q = 30200, MAKE_VECTOR = 30201, VECTOR = 30202,
    VECTOR_LENGTH = 30203, VECTOR_REF = 30204, VECTOR_SET = 30205,
    VECTOR_TO_LIST = 30206, LIST_TO_VECTOR = 30207, VECTOR_FILL = 30208;

  // ======================================================================
  //                              Constructor.
  // ======================================================================

  /**
   * Create a new data operator with the specified name, opcode,
   * and number of arguments.
   *
   * @param  name     The name of the data operator.
   * @param  opcode   The opcode of the data operator.
   * @param  minArgs  The non-negative minimum number of arguments
   *                  for this data operator.
   * @param  maxArgs  The non-negative maximum number of arguments
   *                  for this data operator, or -1 if this
   *                  operator takes an unlimited maximum number
   *                  of arguments. In the former case,
   *                  <code>maxArgs >= minArgs</code> must be
   *                  <code>true</code>.
   */
  private Data(String name, int opcode, int minArgs, int maxArgs) {
    this.name    = name;
    this.opcode  = opcode;
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }

  // ======================================================================
  //                              Equality.
  // ======================================================================

  /**
   * Determine whether this data operator equals the specified object.
   *
   * @param   o  The object with which to compare.
   * @return     <code>true</code> if this data operator equals the
   *             specified object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Data)) return false;

    Data other = (Data)o;

    return (   (name.equals(other.name))
            && (opcode  == other.opcode)
            && (minArgs == other.minArgs)
            && (maxArgs == other.maxArgs));
  }

  /**
   * Return a hash code for this data operator.
   *
   * @return  A hash code for this data operator.
   */
  public int hashCode() {
    return name.hashCode();
  }

  // ======================================================================
  //                             Application.
  // ======================================================================

  /**
   * Apply this data operator on the specified arguments.
   *
   * @param   l          The arguments as a proper list.
   * @param   numArgs    The number of arguments in <code>l</code>.
   * @param   evaluator  The calling evaluator.
   * @return             The result of applying this operator
   *                     on the specified arguments.
   * @throws  EvaluatorException
   *                     Signals an exceptional condition when
   *                     applying this operator to the specified
   *                     arguments.
   */
  protected Object apply1(Pair l, int numArgs, Evaluator evaluator)
    throws EvaluatorException {

    // Set up arguments.
    Object o1   = null;
    Object o2   = null;
    Object o3   = null;

    {
      Pair temp = l;
      
      switch (numArgs) {

      case 1:
        o1 = temp.car;
        break;
        
      case 2:
        o1   = temp.car;
        temp = (Pair)temp.cdr;
        o2   = temp.car;
        break;
        
      case 3:
        o1   = temp.car;
        temp = (Pair)temp.cdr;
        o2   = temp.car;
        temp = (Pair)temp.cdr;
        o3   = temp.car;

      default:
        // Nothing to do.
      }
    }

    // Do the deed.
    double d;
    int    i;

    switch (opcode) {
      // ============= 6.1 Equivalence predicates
    case EQV_Q:
      return Cast.box(Equivalence.EQV.compare(o1, o2));
    case EQ_Q:
      return Cast.box(Equivalence.EQ.compare(o1, o2));
    case EQUAL_Q:
      return Cast.box(Equivalence.EQUAL.compare(o1, o2));

      // ============= 6.2 Numbers
    case NUMBER_Q:
      return Cast.box(Math.isNumber(o1));
    case COMPLEX_Q:
      return Cast.box(Math.isComplex(o1));
    case REAL_Q:
      return Cast.box(Math.isReal(o1));
    case RATIONAL_Q:
      return Cast.box(Math.isRational(o1));
    case INTEGER_Q:
      return Cast.box(Math.isInteger(o1));
    case EXACT_Q:
      return Cast.box(Math.isExact(Math.toNumber(o1)));
    case INEXACT_Q:
      return Cast.box(Math.isInexact(Math.toNumber(o1)));
    case NUM_EQ:
      return Cast.box(Math.compare(Math.COMPARE_EQUAL,         l));
    case NUM_LQ:
      return Cast.box(Math.compare(Math.COMPARE_LESS,          l));
    case NUM_GQ:
      return Cast.box(Math.compare(Math.COMPARE_GREATER,       l));
    case NUM_LEQ:
      return Cast.box(Math.compare(Math.COMPARE_LESS_EQUAL,    l));
    case NUM_GEQ:
      return Cast.box(Math.compare(Math.COMPARE_GREATER_EQUAL, l));
    case ZERO_Q:
      return Cast.box(Math.isZero(Math.toNumber(o1)));
    case POSITIVE_Q:
      return Cast.box(Math.isPositive(Math.toNumber(o1)));
    case NEGATIVE_Q:
      return Cast.box(Math.isNegative(Math.toNumber(o1)));
    case ODD_Q:
      return Cast.box(Math.isOdd(Math.toInteger(Math.toNumber(o1))));
    case EVEN_Q:
      return Cast.box(Math.isEven(Math.toInteger(Math.toNumber(o1))));
    case MAX:
      return Math.compute(Math.COMPUTE_MAX,      l);
    case MIN:
      return Math.compute(Math.COMPUTE_MIN,      l);
    case ADD:
      return Math.compute(Math.COMPUTE_ADD,      l);
    case MULTIPLY:
      return Math.compute(Math.COMPUTE_MULTIPLY, l);
    case SUBTRACT:
      return Math.compute(Math.COMPUTE_SUBTRACT, l);
    case DIVIDE:
      try {
        return Math.compute(Math.COMPUTE_DIVIDE,   l);
      } catch (ArithmeticException x) {
        throw new BadArgumentException("Unable to divide by zero");
      }
    case ABS:
      return Math.abs(Math.toNumber(o1));
    case QUOTIENT:
      return Math.quotient(Math.toNumber(o1), Math.toNumber(o2));
    case REMAINDER:
      return Math.remainder(Math.toNumber(o1), Math.toNumber(o2));
    case MODULO:
      return Math.modulo(Math.toNumber(o1), Math.toNumber(o2));
    case GCD:
      return Math.gcd(l);
    case LCM:
      return Math.lcm(l);
    case NUMERATOR:
      return Math.numerator(Math.toNumber(o1));
    case DENOMINATOR:
      return Math.denominator(Math.toNumber(o1));
    case FLOOR:
      return Math.floor(Math.toNumber(o1));
    case CEILING:
      return Math.ceil(Math.toNumber(o1));
    case TRUNCATE:
      return Math.truncate(Math.toNumber(o1));
    case ROUND:
      return Math.rint(Math.toNumber(o1));
    case RATIONALIZE:
      return Math.rationalize(Math.toNumber(o1), Math.toNumber(o2));
    case EXP:
      return Cast.box(java.lang.Math.exp(Math.toNumber(o1).doubleValue()));
    case LOG:
      d = Math.toNumber(o1).doubleValue();
      if (0.0D >= d) {
        throw new BadArgumentException(
          "Unable to take log of non-positive number", o1);
      } else {
        return Cast.box(java.lang.Math.log(d));
      }
    case SIN:
      return Cast.box(java.lang.Math.sin(Math.toNumber(o1).doubleValue()));
    case COS:
      return Cast.box(java.lang.Math.cos(Math.toNumber(o1).doubleValue()));
    case TAN:
      return Cast.box(java.lang.Math.tan(Math.toNumber(o1).doubleValue()));
    case ASIN:
      return Cast.box(java.lang.Math.asin(Math.toNumber(o1).doubleValue()));
    case ACOS:
      return Cast.box(java.lang.Math.acos(Math.toNumber(o1).doubleValue()));
    case ATAN:
      return Cast.box(
        ((numArgs == 1)?
         java.lang.Math.atan(Math.toNumber(o1).doubleValue())
         : java.lang.Math.atan2(Math.toNumber(o1).doubleValue(),
                                Math.toNumber(o2).doubleValue())));
    case SQRT:
      d = Math.toNumber(o1).doubleValue();
      if (0.0D > d) {
        throw new BadArgumentException(
          "Unable to take sqrt of negative number", o1);
      } else {
        return Cast.box(java.lang.Math.sqrt(d));
      }
    case EXPT:
      return Math.expt(Math.toNumber(o1), Math.toNumber(o2));
    case EXACT_TO_INEXACT:
      return Math.toInexact(Math.toNumber(o1));
    case INEXACT_TO_EXACT:
      return Math.toExact(Math.toNumber(o1));
    case NUMBER_TO_STRING:
      try {
        return ((numArgs == 1)?
                Math.toString(Math.toNumber(o1))
                : Math.toString(Math.toNumber(o1), Cast.toInt(o2)));
      } catch (IllegalArgumentException x) {
        throw new BadArgumentException(x.getMessage(), o2);
      }
    case STRING_TO_NUMBER:
      try {
        return ((numArgs == 1)?
                Math.parse(Cast.toString(o1))
                : Math.parse(Cast.toString(o1), Cast.toInt(o2)));
      } catch (BadFormatException x) {
        return Boolean.FALSE;
      }

      // ============= 6.3.1 Booleans
    case NOT:
      if ((o1 instanceof Boolean) && (! ((Boolean)o1).booleanValue())) {
        return Boolean.TRUE;
      } else {
        return Boolean.FALSE;
      }
    case BOOLEAN_Q:
      return Cast.box(o1 instanceof Boolean);
      
      // ============= 6.3.2 Pairs and lists
    case PAIR_Q:
      return Cast.box(o1 instanceof Pair);
    case CONS:
      return new Pair(o1, o2);
    case CAR:
      return Pair.toPair(o1).car;
    case CDR:
      return Pair.toPair(o1).cdr;
    case SET_CAR:
      return Pair.toModifiablePair(o1).car = o2;
    case SET_CDR:
      return Pair.toModifiablePair(o1).cdr = o2;
    case NULL_Q:
      return ((o1 == null)? Boolean.TRUE : Boolean.FALSE);
    case LIST_Q:
      if (Pair.isNull(o1)) {
        return Boolean.TRUE;
      } else if (o1 instanceof Pair) {
        return Cast.box(((Pair)o1).isList());
      } else {
        return Boolean.FALSE;
      }
    case LIST:
      return l; // The arguments are consed up to a list by the evaluator.
    case LENGTH:
      return Cast.box((Pair.isNull(o1)? 0 : Pair.toPair(o1).length()));
    case APPEND:
      return Pair.append(l);
    case REVERSE:
      return (Pair.isNull(o1)? Pair.EMPTY_LIST : Pair.toPair(o1).reverse());
    case LIST_TAIL:
      try {
        return Pair.toPair(o1).getListTail(Cast.toInt(o2));
      } catch (IndexOutOfBoundsException x) {
        throw new BadArgumentException("Invalid list index", o2);
      }
    case LIST_REF:
      try {
        return Pair.toPair(o1).getListRef(Cast.toInt(o2));
      } catch (IndexOutOfBoundsException x) {
        throw new BadArgumentException("Invalid list index", o2);
      }
    case MEMQ:
      return Pair.normalize((Pair.isNull(o2)? Pair.EMPTY_LIST
                             : Pair.toPair(o2).member(o1, Equivalence.EQ)));
    case MEMV:
      return Pair.normalize((Pair.isNull(o2)? Pair.EMPTY_LIST
                             : Pair.toPair(o2).member(o1, Equivalence.EQV)));
    case MEMBER:
      return Pair.normalize((Pair.isNull(o2)? Pair.EMPTY_LIST
                             : Pair.toPair(o2).member(o1, Equivalence.EQUAL)));
    case ASSQ:
      return Pair.normalize((Pair.isNull(o2)? Pair.EMPTY_LIST
                             : Pair.toPair(o2).assoc(o1, Equivalence.EQ)));
    case ASSV:
      return Pair.normalize((Pair.isNull(o2)? Pair.EMPTY_LIST
                             : Pair.toPair(o2).assoc(o1, Equivalence.EQV)));
    case ASSOC:
      return Pair.normalize((Pair.isNull(o2)? Pair.EMPTY_LIST
                             : Pair.toPair(o2).assoc(o1, Equivalence.EQUAL)));

      // ============= 6.3.3 Symbols
    case SYMBOL_Q:
      return Cast.box(o1 instanceof Symbol);
    case SYMBOL_TO_STRING:
      return Symbol.toSymbol(o1).toString();
    case STRING_TO_SYMBOL:
      return Symbol.intern(Cast.toString(o1));

      // ============= 6.3.4 Characters
    case CHAR_Q:
      return Cast.box(o1 instanceof Character);
    case CHAR_EQ:
      return Cast.box(Cast.toChar(o1) == Cast.toChar(o2));
    case CHAR_LQ:
      return Cast.box(Cast.toChar(o1) < Cast.toChar(o2));
    case CHAR_GQ:
      return Cast.box(Cast.toChar(o1) > Cast.toChar(o2));
    case CHAR_LEQ:
      return Cast.box(Cast.toChar(o1) <= Cast.toChar(o2));
    case CHAR_GEQ:
      return Cast.box(Cast.toChar(o1) >= Cast.toChar(o2));
    case CHAR_CI_EQ:
      return Cast.box(compareIgnoreCase(Cast.toChar(o1), Cast.toChar(o2)) == 0);
    case CHAR_CI_LQ:
      return Cast.box(compareIgnoreCase(Cast.toChar(o1), Cast.toChar(o2)) < 0);
    case CHAR_CI_GQ:
      return Cast.box(compareIgnoreCase(Cast.toChar(o1), Cast.toChar(o2)) > 0);
    case CHAR_CI_LEQ:
      return Cast.box(compareIgnoreCase(Cast.toChar(o1), Cast.toChar(o2)) <= 0);
    case CHAR_CI_GEQ:
      return Cast.box(compareIgnoreCase(Cast.toChar(o1), Cast.toChar(o2)) >= 0);
    case CHAR_ALPHABETIC_Q:
      return Cast.box(Character.isLetter(Cast.toChar(o1)));
    case CHAR_NUMERIC_Q:
      return Cast.box(Character.isDigit(Cast.toChar(o1)));
    case CHAR_WHITESPACE_Q:
      return Cast.box(Character.isSpaceChar(Cast.toChar(o1)));
    case CHAR_UPPER_CASE_Q:
      return Cast.box(Character.isUpperCase(Cast.toChar(o1)));
    case CHAR_LOWER_CASE_Q:
      return Cast.box(Character.isLowerCase(Cast.toChar(o1)));
    case CHAR_TO_INTEGER:
      return Cast.box((int)Cast.toChar(o1));
    case INTEGER_TO_CHAR:
      i = Cast.toInt(o1);
      if ((i < Character.MIN_VALUE) || (i > Character.MAX_VALUE)) {
        throw new BadArgumentException("Invalid character code", o1);
      } else {
        return Cast.box((char)i);
      }
    case CHAR_UPCASE:
      return Cast.box(Character.toUpperCase(Cast.toChar(o1)));
    case CHAR_DOWNCASE:
      return Cast.box(Character.toLowerCase(Cast.toChar(o1)));

      // ============= 6.3.5 Strings
    case STRING_Q:
      return Cast.box((o1 instanceof StringBuffer) || (o1 instanceof String));
    case MAKE_STRING:
      {
        int  length = Cast.toInt(o1);
        char init   = ((numArgs == 1)? '*' : Cast.toChar(o2));

        StringBuffer buf = new StringBuffer(length);
        for (i=0; i<length; i++) {
          buf.append(init);
        }

        return buf;
      }
    case STRING:
      return createFromCharacterList(l);
    case STRING_LENGTH:
      return Cast.box((treatAsJavaString(o1)?
                       ((String)o1).length()
                       : ((StringBuffer)o1).length()));
    case STRING_REF:
      i = Cast.toInt(o2);
      try {
        return Cast.box((treatAsJavaString(o1)?
                         ((String)o1).charAt(i) :
                         ((StringBuffer)o1).charAt(i)));
      } catch (IndexOutOfBoundsException x) {
        throw new BadArgumentException("Invalid string index", o2);
      }
    case STRING_SET:
      try {
        toStringBuffer(o1).setCharAt(Cast.toInt(o2), Cast.toChar(o3));
      } catch (IndexOutOfBoundsException x) {
        throw new BadArgumentException("Invalid string index", o2);
      }
      return Boolean.FALSE;
    case STRING_EQ:
      return Cast.box(Cast.toString(o1).compareTo(Cast.toString(o2)) == 0);
    case STRING_LQ:
      return Cast.box(Cast.toString(o1).compareTo(Cast.toString(o2)) < 0);
    case STRING_GQ:
      return Cast.box(Cast.toString(o1).compareTo(Cast.toString(o2)) > 0);
    case STRING_LEQ:
      return Cast.box(Cast.toString(o1).compareTo(Cast.toString(o2)) <= 0);
    case STRING_GEQ:
      return Cast.box(Cast.toString(o1).compareTo(Cast.toString(o2)) >= 0);
    case STRING_CI_EQ:
      return Cast.box(Cast.toString(o1).compareToIgnoreCase(Cast.toString(o2))
                      == 0);
    case STRING_CI_LQ:
      return Cast.box(Cast.toString(o1).compareToIgnoreCase(Cast.toString(o2))
                      < 0);
    case STRING_CI_GQ:
      return Cast.box(Cast.toString(o1).compareToIgnoreCase(Cast.toString(o2))
                      > 0);
    case STRING_CI_LEQ:
      return Cast.box(Cast.toString(o1).compareToIgnoreCase(Cast.toString(o2))
                      <= 0);
    case STRING_CI_GEQ:
      return Cast.box(Cast.toString(o1).compareToIgnoreCase(Cast.toString(o2))
                      >= 0);
    case SUBSTRING:
      {
        int start = Cast.toInt(o2);
        int end   = Cast.toInt(o3);
        try {
          return new StringBuffer(Cast.toString(o1).substring(start, end));
        } catch (IndexOutOfBoundsException x) {
          throw new BadArgumentException("Invalid substring from " + start +
                                         " to " + end, o1);
        }
      }
    case STRING_APPEND:
      return createFromStringList(l);
    case STRING_TO_LIST:
      {
        Pair result = Pair.EMPTY_LIST;

        if (treatAsJavaString(o1)) {
          String s = (String)o1;
          for (i=s.length()-1; i>=0; i--) {
            result = new Pair(new Character(s.charAt(i)), result);
          }

        } else {
          StringBuffer buf = (StringBuffer)o1;
          for (i=buf.length()-1; i>=0; i--) {
            result = new Pair(new Character(buf.charAt(i)), result);
          }
        }
        return result;
      }
    case LIST_TO_STRING:
      return ((Pair.isNull(o1)? new StringBuffer()
               : createFromCharacterList(Pair.toPair(o1))));
    case STRING_COPY:
      return new StringBuffer(Cast.toString(o1));
    case STRING_FILL:
      {
        StringBuffer buf    = toStringBuffer(o1);
        char         fill   = Cast.toChar(o2);
        int          length = buf.length();

        for (i=0; i<length; i++) {
          buf.setCharAt(i, fill);
        }

        return Boolean.FALSE;
      }

      // ============= 6.3.6 Vectors
    case VECTOR_Q:
      return Cast.box((o1 instanceof List) || Cast.isArray(o1));
    case MAKE_VECTOR:
      try {
        return Vector.create(Cast.toInt(o1),
                             ((numArgs == 1)? Pair.EMPTY_LIST : o2));
      } catch (NegativeSizeException x) {
        throw new BadArgumentException("Negative vector size", o1);
      }
    case VECTOR:
      return Vector.create(l);
    case VECTOR_LENGTH:
      if (o1 instanceof List) {
        return Cast.box(((List)o1).size());
      } else if (Cast.isArray(o1)) {
        return Cast.box(java.lang.reflect.Array.getLength(o1));
      } else {
        throw new BadTypeException("Not a vector", o1);
      }
    case VECTOR_REF:
      try {
        if (o1 instanceof List) {
          return ((List)o1).get(Cast.toInt(o2));
        } else if (Cast.isArray(o1)) {
          return java.lang.reflect.Array.get(o1, Cast.toInt(o2));
        } else {
          throw new BadTypeException("Not a vector", o1);
        }
      } catch (IndexOutOfBoundsException x) {
        throw new BadArgumentException("Invalid vector index", o2);
      }
    case VECTOR_SET:
      if (o1 instanceof List) {
        try {
          ((List)o1).set(Cast.toInt(o2), o3);
        } catch (UnsupportedOperationException x) {
          throw new BadArgumentException("Constant vector", o1);
        } catch (IndexOutOfBoundsException x) {
          throw new BadArgumentException("Invalid vector index", o2);
        } catch (ClassCastException x) {
          throw new WrappedException("Invalid vector element type", x, o3);
        } catch (IllegalArgumentException x) {
          throw new WrappedException("Invalid vector element", x, o3);
        }
      } else if (Cast.isArray(o1)) {
        try {
          java.lang.reflect.Array.set(o1, Cast.toInt(o2),
                                      Cast.convert(o3,
                                            o1.getClass().getComponentType()));
        } catch (IndexOutOfBoundsException x) {
          throw new BadArgumentException("Invalid vector index", o2);
        }
      } else {
        throw new BadTypeException("Not a vector", o1);
      }
      return Boolean.FALSE;
    case VECTOR_TO_LIST:
      if (o1 instanceof List) {
        return Pair.createList(((List)o1));
      } else if (Cast.isArray(o1)) {
        return Pair.createList(o1);
      } else {
        throw new BadTypeException("Not a vector", o1);
      }
    case LIST_TO_VECTOR:
      return ((Pair.isNull(o1)? Vector.EMPTY
               : Vector.create(Pair.toPair(o1))));
    case VECTOR_FILL:
      if (o1 instanceof List) {
        try {
          java.util.Collections.fill(((List)o1), o2);
        } catch (UnsupportedOperationException x) {
          throw new BadArgumentException("Constant vector", o1);
        }
      } else if (Cast.isArray(o1)) {
        Object fill = Cast.convert(o2, o1.getClass().getComponentType());
        int    len  = java.lang.reflect.Array.getLength(o1);

        for (i=0; i<len; i++) {
          java.lang.reflect.Array.set(o1, i, fill);
        }
      } else {
        throw new BadTypeException("Not a vector", o1);
      }
      return Boolean.FALSE;

      // ============= The rest, namely cxr and error.
    default:
      if ((opcode >= CAAR) && (opcode <= CDDDDR)) {
        return Pair.toPair(o1).cxr(name.substring(1, name.length() - 1));
      } else {
        throw new Bug("Invalid opcode " + opcode + " for data operator " +
                      toString());
      }
    }
  }

  // ======================================================================
  //                                Helpers.
  // ======================================================================

  /**
   * Compare the specified characters without case considerations.
   *
   * @param   c1  The first character to compare.
   * @param   c2  The second character to compare.
   * @return      A negative number if <code>c1 < c2</code>, 0 if
   *              <code>c1 == c2</code>, and a positive number if
   *              <code>c1 > c2</code>, all ignoring case
   *              considerations.
   */
  private static int compareIgnoreCase(char c1, char c2) {
    if (c1 != c2) {
      c1 = Character.toUpperCase(c1);
      c2 = Character.toUpperCase(c2);
      if (c1 != c2) {
        c1 = Character.toLowerCase(c1);
        c2 = Character.toLowerCase(c2);
        if (c1 != c2) {
          return c1 - c2;
        }
      }
    }

    return 0;
  }

  /**
   * Cast the specified object to a string buffer.
   *
   * @param   o  The object to cast.
   * @return     The specified object as a string buffer.
   * @throws  BadArgumentException
   *             Signals that <code>o</code> is a Java string.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is neither a string
   *             nor a string buffer.
   */
  private static StringBuffer toStringBuffer(Object o)
    throws BadArgumentException, BadTypeException {

    if (o instanceof StringBuffer) {
      return (StringBuffer)o;
    } else if (o instanceof String) {
      throw new BadArgumentException("Constant string", o);
    } else {
      throw new BadTypeException("Not a string", o);
    }
  }

  /**
   * Ensure that the specified object is a string and determine
   * whether it is a Java string. Both string buffers and Java strings
   * are considered strings by this method.
   *
   * @param   o  The object to check.
   * @return     <code>true</code> iff the specified object is
   *             a Java string.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is neither a string buffer
   *             nor a Java string.
   */
  private static boolean treatAsJavaString(Object o) throws BadTypeException {
    if (o instanceof String) {
      return true;
    } else if (o instanceof StringBuffer) {
      return false;
    } else {
      throw new BadTypeException("Not a string", o);
    }
  }

  /**
   * Create a new string buffer that is the result of appending all
   * characters in the specified list together.
   *
   * @param   p  The pair starting the list of characters.
   * @return     A  string buffer that is the result of
   *             appending all characters in the specified list.
   * @throws  BadPairStructureException
   *             Signals that the list starting at the specified
   *             pair is not a proper list.
   * @throws  BadTypeException
   *             Signals that an element in the specified list
   *             is not a character.
   */
  private static StringBuffer createFromCharacterList(Pair p)
    throws BadPairStructureException, BadTypeException {

    if (Pair.EMPTY_LIST == p) return new StringBuffer();

    StringBuffer buf = new StringBuffer(p.length());

    do {
      buf.append(Cast.toChar(p.car));
      p = (Pair)p.cdr;
    } while (Pair.EMPTY_LIST != p);

    return buf;
  }

  /**
   * Create a new string buffer that is the result of appending all
   * strings in the specified list together. Both string buffers and
   * Java strings are considered as strings by this method. The
   * specified list is not verified to be a proper list.
   *
   * @param   p  The pair starting the list of strings.
   * @return     A new string buffer that is the result of
   *             appending all strings in the specified list.
   * @throws  BadTypeException
   *             Signals that an element in the specified list
   *             is not a string.
   */
  private static StringBuffer createFromStringList(Pair p)
    throws BadTypeException {

    if (Pair.EMPTY_LIST == p) {
      return new StringBuffer();
    }

    Pair head   = p;
    int  length = 0;

    // Determine length of result string.
    do {
      Object o = p.car;
      if (o instanceof String) {
        length += ((String)o).length();
      } else if (o instanceof StringBuffer) {
        length += ((StringBuffer)o).length();
      } else {
        throw new BadTypeException("Not a string", o);
      }

      p = (Pair)p.cdr;
    } while (Pair.EMPTY_LIST != p);
    
    if (0 == length) return new StringBuffer();

    // Create string.
    StringBuffer buf = new StringBuffer(length);

    // Copy strings in list into result.
    p = head;

    do {
      Object o = p.car;
      if (o instanceof String) {
        buf.append((String)o);
      } else {
        buf.append(o.toString());
      }

      p = (Pair)p.cdr;
    } while (Pair.EMPTY_LIST != p);

    // Done.
    return buf;
  }

  // ======================================================================
  //                            Installation.
  // ======================================================================

  /**
   * Install the data operators defined in sections &sect;6.1,
   * &sect;6.2, and &sect;6.3 of R<sup><font *
   * size="-1">5</font></sup>RS into the specified environment.
   *
   * @param   env     The environment to install the data
   *                  operators into.
   */
  public static void install(Environment env) {
    // 6.1 Equivalence predicates
    add(env, "eqv?",                 EQV_Q,              2,  2);
    add(env, "eq?",                  EQ_Q,               2,  2);
    add(env, "equal?",               EQUAL_Q,            2,  2);
    
    // 6.2 Numbers
    add(env, "number?",              NUMBER_Q,           1,  1);
    add(env, "complex?",             COMPLEX_Q,          1,  1);
    add(env, "real?",                REAL_Q,             1,  1);
    add(env, "rational?",            RATIONAL_Q,         1,  1);
    add(env, "integer?",             INTEGER_Q,          1,  1);
    add(env, "exact?",               EXACT_Q,            1,  1);
    add(env, "inexact?",             INEXACT_Q,          1,  1);
    add(env, "=",                    NUM_EQ,             2, -1);
    add(env, "<",                    NUM_LQ,             2, -1);
    add(env, ">",                    NUM_GQ,             2, -1);
    add(env, "<=",                   NUM_LEQ,            2, -1);
    add(env, ">=",                   NUM_GEQ,            2, -1);
    add(env, "zero?",                ZERO_Q,             1,  1);
    add(env, "positive?",            POSITIVE_Q,         1,  1);
    add(env, "negative?",            NEGATIVE_Q,         1,  1);
    add(env, "odd?",                 ODD_Q,              1,  1);
    add(env, "even?",                EVEN_Q,             1,  1);
    add(env, "max",                  MAX,                1, -1);
    add(env, "min",                  MIN,                1, -1);
    add(env, "+",                    ADD,                0, -1);
    add(env, "*",                    MULTIPLY,           0, -1);
    add(env, "-",                    SUBTRACT,           1, -1);
    add(env, "/",                    DIVIDE,             1, -1);
    add(env, "abs",                  ABS,                1,  1);
    add(env, "quotient",             QUOTIENT,           2,  2);
    add(env, "remainder",            REMAINDER,          2,  2);
    add(env, "modulo",               MODULO,             2,  2);
    add(env, "gcd",                  GCD,                0, -1);
    add(env, "lcm",                  LCM,                0, -1);
    add(env, "numerator",            NUMERATOR,          1,  1);
    add(env, "denominator",          DENOMINATOR,        1,  1);
    add(env, "floor",                FLOOR,              1,  1);
    add(env, "ceiling",              CEILING,            1,  1);
    add(env, "truncate",             TRUNCATE,           1,  1);
    add(env, "round",                ROUND,              1,  1);
    add(env, "rationalize",          RATIONALIZE,        2,  2);
    add(env, "exp",                  EXP,                1,  1);
    add(env, "log",                  LOG,                1,  1);
    add(env, "sin",                  SIN,                1,  1);
    add(env, "cos",                  COS,                1,  1);
    add(env, "tan",                  TAN,                1,  1);
    add(env, "asin",                 ASIN,               1,  1);
    add(env, "acos",                 ACOS,               1,  1);
    add(env, "atan",                 ATAN,               1,  2);
    add(env, "sqrt",                 SQRT,               1,  1);
    add(env, "expt",                 EXPT,               2,  2);
    add(env, "exact->inexact",       EXACT_TO_INEXACT,   1,  1);
    add(env, "inexact->exact",       INEXACT_TO_EXACT,   1,  1);
    add(env, "number->string",       NUMBER_TO_STRING,   1,  2);
    add(env, "string->number",       STRING_TO_NUMBER,   1,  2);
    
    // 6.3.1 Booleans
    add(env, "not",                  NOT,                1,  1);
    add(env, "boolean?",             BOOLEAN_Q,          1,  1);
    
    // 6.3.2 Pairs and lists
    add(env, "pair?",                PAIR_Q,             1,  1);
    add(env, "cons",                 CONS,               2,  2);
    add(env, "car",                  CAR,                1,  1);
    add(env, "cdr",                  CDR,                1,  1);
    add(env, "set-car!",             SET_CAR,            2,  2);
    add(env, "set-cdr!",             SET_CDR,            2,  2);
    add(env, "null?",                NULL_Q,             1,  1);
    add(env, "list?",                LIST_Q,             1,  1);
    add(env, "list",                 LIST,               0, -1);
    add(env, "length",               LENGTH,             1,  1);
    add(env, "append",               APPEND,             0, -1);
    add(env, "reverse",              REVERSE,            1,  1);
    add(env, "list-tail",            LIST_TAIL,          2,  2);
    add(env, "list-ref",             LIST_REF,           2,  2);
    add(env, "memq",                 MEMQ,               2,  2);
    add(env, "memv",                 MEMV,               2,  2);
    add(env, "member",               MEMBER,             2,  2);
    add(env, "assq",                 ASSQ,               2,  2);
    add(env, "assv",                 ASSV,               2,  2);
    add(env, "assoc",                ASSOC,              2,  2);
    add(env, "caar",                 CAAR,               1,  1);
    add(env, "cadr",                 CADR,               1,  1);
    add(env, "cdar",                 CDAR,               1,  1);
    add(env, "cddr",                 CDDR,               1,  1);
    add(env, "caaar",                CAAAR,              1,  1);
    add(env, "caadr",                CAADR,              1,  1);
    add(env, "cadar",                CADAR,              1,  1);
    add(env, "caddr",                CADDR,              1,  1);
    add(env, "cdaar",                CDAAR,              1,  1);
    add(env, "cdadr",                CDADR,              1,  1);
    add(env, "cddar",                CDDAR,              1,  1);
    add(env, "cdddr",                CDDDR,              1,  1);
    add(env, "caaaar",               CAAAAR,             1,  1);
    add(env, "caaadr",               CAAADR,             1,  1);
    add(env, "caadar",               CAADAR,             1,  1);
    add(env, "caaddr",               CAADDR,             1,  1);
    add(env, "cadaar",               CADAAR,             1,  1);
    add(env, "cadadr",               CADADR,             1,  1);
    add(env, "caddar",               CADDAR,             1,  1);
    add(env, "cadddr",               CADDDR,             1,  1);
    add(env, "cdaaar",               CDAAAR,             1,  1);
    add(env, "cdaadr",               CDAADR,             1,  1);
    add(env, "cdadar",               CDADAR,             1,  1);
    add(env, "cdaddr",               CDADDR,             1,  1);
    add(env, "cddaar",               CDDAAR,             1,  1);
    add(env, "cddadr",               CDDADR,             1,  1);
    add(env, "cdddar",               CDDDAR,             1,  1);
    add(env, "cddddr",               CDDDDR,             1,  1);
    
    // 6.3.3 Symbols
    add(env, "symbol?",              SYMBOL_Q,           1,  1);
    add(env, "string->symbol",       STRING_TO_SYMBOL,   1,  1);
    add(env, "symbol->string",       SYMBOL_TO_STRING,   1,  1);
    
    // 6.3.4 Characters
    add(env, "char?",                CHAR_Q,             1,  1);
    add(env, "char=?",               CHAR_EQ,            2,  2);
    add(env, "char<?",               CHAR_LQ,            2,  2);
    add(env, "char>?",               CHAR_GQ,            2,  2);
    add(env, "char<=?",              CHAR_LEQ,           2,  2);
    add(env, "char>=?",              CHAR_GEQ,           2,  2);
    add(env, "char-ci=?",            CHAR_CI_EQ,         2,  2);
    add(env, "char-ci<?",            CHAR_CI_LQ,         2,  2);
    add(env, "char-ci>?",            CHAR_CI_GQ,         2,  2);
    add(env, "char-ci<=?",           CHAR_CI_LEQ,        2,  2);
    add(env, "char-ci>=?",           CHAR_CI_GEQ,        2,  2);
    add(env, "char-alphabetic?",     CHAR_ALPHABETIC_Q,  1,  1);
    add(env, "char-numeric?",        CHAR_NUMERIC_Q,     1,  1);
    add(env, "char-whitespace?",     CHAR_WHITESPACE_Q,  1,  1);
    add(env, "char-upper-case?",     CHAR_UPPER_CASE_Q,  1,  1);
    add(env, "char-lower-case?",     CHAR_LOWER_CASE_Q,  1,  1);
    add(env, "char->integer",        CHAR_TO_INTEGER,    1,  1);
    add(env, "integer->char",        INTEGER_TO_CHAR,    1,  1);
    add(env, "char-upcase",          CHAR_UPCASE,        1,  1);
    add(env, "char-downcase",        CHAR_DOWNCASE,      1,  1);
    
    // 6.3.5 Strings
    add(env, "string?",              STRING_Q,           1,  1);
    add(env, "make-string",          MAKE_STRING,        1,  2);
    add(env, "string",               STRING,             0, -1);
    add(env, "string-length",        STRING_LENGTH,      1,  1);
    add(env, "string-ref",           STRING_REF,         2,  2);
    add(env, "string-set!",          STRING_SET,         3,  3);
    add(env, "string=?",             STRING_EQ,          2,  2);
    add(env, "string<?",             STRING_LQ,          2,  2);
    add(env, "string>?",             STRING_GQ,          2,  2);
    add(env, "string<=?",            STRING_LEQ,         2,  2);
    add(env, "string>=?",            STRING_GEQ,         2,  2);
    add(env, "string-ci=?",          STRING_CI_EQ,       2,  2);
    add(env, "string-ci<?",          STRING_CI_LQ,       2,  2);
    add(env, "string-ci>?",          STRING_CI_GQ,       2,  2);
    add(env, "string-ci<=?",         STRING_CI_LEQ,      2,  2);
    add(env, "string-ci>=?",         STRING_CI_GEQ,      2,  2);
    add(env, "substring",            SUBSTRING,          3,  3);
    add(env, "string-append",        STRING_APPEND,      0, -1);
    add(env, "string->list",         STRING_TO_LIST,     1,  1);
    add(env, "list->string",         LIST_TO_STRING,     1,  1);
    add(env, "string-copy",          STRING_COPY,        1,  1);
    add(env, "string-fill!",         STRING_FILL,        2,  2);
    
    // 6.3.6 Vectors
    add(env, "vector?",              VECTOR_Q,           1,  1);
    add(env, "make-vector",          MAKE_VECTOR,        1,  2);
    add(env, "vector",               VECTOR,             0, -1);
    add(env, "vector-length",        VECTOR_LENGTH,      1,  1);
    add(env, "vector-ref",           VECTOR_REF,         2,  2);
    add(env, "vector-set!",          VECTOR_SET,         3,  3);
    add(env, "vector->list",         VECTOR_TO_LIST,     1,  1);
    add(env, "list->vector",         LIST_TO_VECTOR,     1,  1);
    add(env, "vector-fill!",         VECTOR_FILL,        2,  2);
  }

  /**
   * Create a new data operator as specified and add a binding
   * for it to the specified environment.
   *
   * @param   env       The environment for the new data operator.
   * @param   name      The name of the new data operator.
   * @param   opcode    The opcode of the new data operator.
   * @param   minArgs   The non-negative minimum number of arguments for
   *                    the new data operator.
   * @param   maxArgs   The non-negative maximum number of arguments for
   *                    the new data operator, or -1 if it takes an
   *                    unlimited maximum number of arguments.
   */
  private static void add(Environment env, String name, int opcode,
                          int minArgs, int maxArgs) {
    name     = name.intern();
    Symbol s = Symbol.intern(name);
    Data   v = new Data(name, opcode, minArgs, maxArgs);

    env.bind(s, v);
  }

}
