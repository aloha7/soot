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

import java.math.BigInteger;
import java.math.BigDecimal;

import one.util.Bug;

import one.math.Rational;

/**
 * Implementation of the operations for the Scheme numeric tower. This
 * class does not implement a specific datatype, but rather provides a
 * set of static "helper" methods that operate on numbers. These
 * methods are collected in this class, to isolate the code that deals
 * with numbers as much as possible from other code in this package.
 *
 * <p>Generally, any subclass of <code>java.lang.Number</code> is
 * considered a number by this class. However, it only "knows" to
 * correctly use instances of class <code>java.lang.Byte</code>,
 * <code>java.lang.Short</code>, <code>java.lang.Integer</code>,
 * <code>java.lang.Long</code>, {@link BigInteger}, {@link Rational},
 * <code>java.lang.Float</code>, and
 * <code>java.lang.Double</code>. Support for {@link BigDecimal} is
 * rudimentary. Some methods that take only one number as their
 * argument, return a <code>BigDecimal</code> as a result. But,
 * usually, instances of <code>BigDecimal</code> are treated as
 * doubles, that is, they are converted to doubles using {@link
 * BigDecimal#doubleValue()} before any computation is performed on a
 * <code>BigDecimal</code>. All other subclasses of
 * <code>java.lang.Number</code>, which are effectively "unknown", are
 * also treated as doubles.</p>
 *
 * <p>Instances of <code>Byte</code>, <code>Short</code>,
 * <code>Integer</code>, <code>Long</code>, <code>BigInteger</code>,
 * and <code>Rational</code> are considered exact numbers. Instances
 * of <code>Float</code>, <code>Double</code>, <code>BigDecimal</code>
 * and all unknown subtypes of <code>java.lang.Number</code> are
 * considered inexact numbers. Note that unkown subtypes of
 * <code>java.lang.Number</code> are also considered complex, real and
 * rational, just like doubles.</p>
 *
 * <p>The preferred representations for numbers are
 * <code>Integer</code>, <code>Long</code>, <code>BigInteger</code>,
 * <code>Rational</code>, and <code>Double</code>. While most methods
 * generally accept any number, the result usually is an instance of a
 * preferred representation. <code>Float</code> is not a preferred
 * representation because most routines in <code>java.lang.Math</code>
 * only work on instances of <code>Double</code>.</p>
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class Math {

  /** Nothing to construct. */
  private Math() {
  }

  /** Flag for unspecified exactness. */
  private static final int   UNSPECIFIED = 0;

  /** Flag for exact exactness. */
  private static final int   EXACT       = 1;

  /** Flag for inexact exactness. */
  private static final int   INEXACT     = 2;

  /** Two as a big integer. */
  private static final BigInteger BIG_TWO = BigInteger.valueOf(2);

  /** Zero as a big decimal. */
  private static final BigDecimal DECIMAL_ZERO = BigDecimal.valueOf(0);

  /** Zero as a double. */
  private static final Double DOUBLE_ZERO = new Double(0.0D);

  /** One as a double. */
  private static final Double DOUBLE_ONE  = new Double(1.0D);

  /** Flag for <code>=</code> comparison. */
  public static final int COMPARE_EQUAL         = 1;

  /** Flag for <code>&lt;</code> comparison. */
  public static final int COMPARE_LESS          = 2;

  /** Flag for <code>&gt;</code> comparison. */
  public static final int COMPARE_GREATER       = 3;

  /** Flag for <code>&lt;=</code> comparison. */
  public static final int COMPARE_LESS_EQUAL    = 4;

  /** Flag for <code>&gt;=</code> comparison. */
  public static final int COMPARE_GREATER_EQUAL = 5;

  /** Flag for <code>max</code> computation. */
  public static final int COMPUTE_MAX      = 1;

  /** Flag for <code>min</code> computation. */
  public static final int COMPUTE_MIN      = 2;

  /** Flag for <code>+</code> computation. */
  public static final int COMPUTE_ADD      = 3;

  /** Flag for <code>*</code> computation. */
  public static final int COMPUTE_MULTIPLY = 4;

  /** Flag for <code>-</code> computation. */
  public static final int COMPUTE_SUBTRACT = 5;

  /** Flag for <code>/</code> computation. */
  public static final int COMPUTE_DIVIDE   = 6;

  /** The integer cateogry. */
  public static final int CATEGORY_INT         = 1;
  
  /** The long category. */
  public static final int CATEGORY_LONG        = 2;

  /** The big integer category. */
  public static final int CATEGORY_BIG_INT     = 3;

  /** The rational category. */
  public static final int CATEGORY_RATIONAL    = 4;

  /** The big decimal category. */
  public static final int CATEGORY_BIG_DECIMAL = 5;

  /** The double category. */
  public static final int CATEGORY_DOUBLE      = 6;

  /** The not-a-number category. */
  public static final int CATEGORY_NAN         = 7;

  // ==================================================================
  //                              Packing.
  // ==================================================================

  /**
   * Pack the specified big integer. If possible, returns
   * a numerically equivalent Long or Integer.
   *
   * @param   n  The big integer to pack.
   */
  public static Number pack(BigInteger n) {
    if ((n.compareTo(Cast.BIG_LONG_MAX_VALUE) <= 0) &&
        (n.compareTo(Cast.BIG_LONG_MIN_VALUE) >= 0)) {
      long l = n.longValue();
      return Cast.box(l);
    } else {
      return n;
    }
  }

  /**
   * Pack the specified rational. If possible, returns
   * a numerically equivalent BigInteger, Long, or Integer.
   *
   * @param   r  The rational to pack.
   */
  public static Number pack(Rational r) {
    if (r.isInteger()) {
      return pack(r.numerator());
    } else {
      return r;
    }
  }

  // ==================================================================
  //                              Parsing.
  // ==================================================================

  /**
   * Parse a number from the specified string with a default radix of
   * 10. If the external representation of the number includes an
   * explicit radix specification, that specification overrides the
   * default radix.
   *
   * @param   s  The external representation of a number as a
   *             string.
   * @return     The corresponding number, represented as an
   *             <code>Integer</code>, <code>Long</code>,
   *             <code>BigInteger</code>, or <code>Rational</code>
   *             for exact numbers, and represented as a
   *             <code>Float</code> or <code>Double</code> for
   *             inexact numbers.
   * @throws  BadFormatException
   *             Signals that the external representation of the
   *             number does not have the expected format.
   */
  public static Number parse(String s) throws BadFormatException {
    try {
      return parse(s, 10);
    } catch (BadArgumentException x) {
      // Must never happen.
      throw new Bug("Unexpected exception: " + x.toString());
    }
  }

  /**
   * Parse a number from the specified string using the specified
   * radix.  If the external representation of the number includes an
   * explicit radix specification, that specification overrides the
   * specified radix.
   *
   * @param   s  The external representation of a number as a
   *             string.
   * @param   radix
   *             The radix for the number. Must be 2, 8, 10, or 16.
   * @return     The corresponding number, represented as an
   *             <code>Integer</code>, <code>Long</code>,
   *             <code>BigInteger</code>, or <code>Rational</code>
   *             for exact numbers, and represented as a
   *             <code>Float</code> or <code>Double</code> for
   *             inexact numbers.
   * @throws  BadFormatException
   *             Signals that the external representation of the
   *             number does not have the expected format.
   * @throws  BadArgumentException
   *             Signals that the specified radix is not 2, 8, 10,
   *             or 16.
   */
  public static Number parse(String s, int radix)
    throws BadFormatException, BadArgumentException {

    // Check radix.
    if ((radix != 2) && (radix != 8) && (radix != 10) && (radix != 16)) {
      throw new BadArgumentException("Illegal radix: " + radix);
    }

    // Remove unnecessary whitespace.
    s = s.trim();

    try {
      return parse1(s, radix);
    } catch (BadFormatException x) {
      x.setCausingObject(s); // Fix the cause...
      throw x;               // ...and rethrow.
    }
  }

  /**
   * Parse a number from the specified string using the specified
   * radix. If the external representation of the number includes an
   * explicit radix specification, that specification overrides the
   * specified radix.
   *
   * @param   s  The external representation of the number as a
   *             string with no leading or trailing spaces.
   * @param   radix
   *             The radix for the number. Must be 2, 8, 10, or 16.
   * @return     The corresponding number.
   * @throws  BadFormatException
   *             Signals that the external representation of the
   *             number does not have the expected format.
   */
  private static Number parse1(String s, int radix)
    throws BadFormatException {

    // Variable declarations.
    int     index  = 0;               // Index into the string.
    int     length = s.length();      // Length of the string.
    int     exact  = UNSPECIFIED;     // Exactness of result.
    char    c;                        // Current char.

    // Get first char.
    if (index >= length) {
      throw new BadFormatException("Empty number format");
    } else {
      c = s.charAt(index++);
    }

    // Handle prefix (radix + exactness) first.
    if ('#' == c) {
      boolean seenRadix = false;

      // Something must follow #.
      if (index >= length) {
        throw new BadFormatException("Incomplete prefix in number format");
      } else {
        c = s.charAt(index++);
      }

      switch(c) {
      case 'i':
      case 'I':
        exact     = INEXACT;
        break;
      case 'e':
      case 'E':
        exact     = EXACT;
        break;
      case 'b':
      case 'B':
        radix     = 2;
        seenRadix = true;
        break;
      case 'o':
      case 'O':
        radix     = 8;
        seenRadix = true;
        break;
      case 'd':
      case 'D':
        radix     = 10;
        seenRadix = true;
        break;
      case 'x':
      case 'X':
        radix     = 16;
        seenRadix = true;
        break;
      default:
        throw new BadFormatException("Unrecognized # form \"#" + c +
                                     " in number format");
      }

      // There could be another # form,
      // and there must be at least one more char.
      if (index >= length) {
        throw new BadFormatException("Incomplete prefix in number format");
      } else {
        c = s.charAt(index++);
      }

      if ('#' == c) {

        // Something must follow #.
        if (index >= length) {
          throw new BadFormatException("Incomplete prefix in number format");
        } else {
          c = s.charAt(index++);
        }

        if (UNSPECIFIED != exact) {
          switch(c) {
          case 'b':
          case 'B':
            radix = 2;
            break;
          case 'o':
          case 'O':
            radix = 8;
            break;
          case 'd':
          case 'D':
            radix = 10;
            break;
          case 'x':
          case 'X':
            radix = 16;
            break;
          default:
            throw new BadFormatException("Unrecognized # form \"#" +
                                         c + "\" after \"#" +
                                         ((EXACT == exact)? 'e' : 'i')
                                         + "\" in number format");
          }
        } else if (seenRadix) {
          switch(c) {
          case 'i':
          case 'I':
            exact = INEXACT;
            break;
          case 'e':
          case 'E':
            exact = EXACT;
            break;
          default:
            char c2 = ((radix == 2)? 'b' : ((radix == 8)? 'o'
                                            : ((radix == 10)? 'd' : 'x')));
            throw new BadFormatException("Unrecognized # form \"#" +
                                         c + "\" after \"#" +
                                         c2 + "\" in number format");
          }
        } else {
          // Must never happen.
          throw new Bug("Invalid internal state");
        }

        // Get next char. There must be one.
        if (index >= length) {
          throw new BadFormatException("No number in number format");
        } else {
          c = s.charAt(index++);
        }
      }
    }

    // Now, we are ready to parse the <complex R> form.
    index--; // Adjust index for the consumed character.

    /*
     * Since we currently do not support numbers with a non-zero
     * imaginary part, live is somewhat simple here. We just
     * check that the number has no imaginary part and fire
     * off the parser for the <real R> form. Once, we support
     * non-zero imaginary parts, we need to do some more
     * sophisticated scanning here and potentially fire off
     * parseReal() twice.
     */
    
    // Check that number has no imaginary part.
    c = s.charAt(length - 1);
    if (('i' == c) || ('I' == c)) {
      throw new BadFormatException("Imaginary numbers are not supported");
    } else {
      for (int i=index; i<length; i++) {
        if ('@' == s.charAt(i)) {
          throw new BadFormatException("Imaginary numbers are not supported");
        }
      }
    }

    // Parse the real part.
    Number num;

    if (0 == index) {
      num = parseReal(s, radix, exact);
    } else {
      num = parseReal(s.substring(index, length), radix, exact);
    }

    // Check that the inexact number make sense.
    if ((num instanceof Double) || (num instanceof Float)) {
      double d = num.doubleValue();

      if (Double.isNaN(d) || Double.isInfinite(d)) {
        throw new BadFormatException(
          "Unable to represent specified number as an inexact number");
      }
    }

    return num;
  }

  /**
   * Parse the specified number format as a real number with the
   * specified radix and specified exactness and return the
   * corresponding number. The number format may have any format that
   * can be produced using the &lt;real <i>R</i>&gt; production in
   * section 7.1.1. of R5RS. All violations of that format are
   * detected.
   *
   * @param   s      The string containing the number format for
   *                 the real number.
   * @param   radix  The radix for the number. Must be 2, 8, 10 or
   *                 16.
   * @param   exact  The exactness of the number. Must be
   *                 {@link #UNSPECIFIED}, {@link #EXACT}, or
   *                 {@link #INEXACT}.
   * @return         The corresponding number.
   * @throws  BadFormatException
   *                 Signals that the external representation of the
   *                 number does not have the expected format.
   */
  private static Number parseReal(String s, int radix, int exact)
    throws BadFormatException {

    int length = s.length();
    if (0 == length) throw new BadFormatException("Empty number format");

    /*
     * Scan real number. Ensures that the real number has a valid
     * number format and collects the information necessary to
     * actually convert the external representation into a number.
     */

    boolean seenPlus   = false;  // Seen a leading '+' ?
    int     dotPos     = -1;     // Position of decimal dot, or -1 if none.
    int     expPos     = -1;     // Position of exponent marker, or -1 if none.
    boolean normalExp  = true;   // Is exponent marker 'e' ?
    boolean doublePrec = true;   // Is exponent marker for double precision ?
    int     slashPos   = -1;     // Position of rational slash, or -1 if none.
    boolean seenHash   = false;  // Seen hash for this number component?
    boolean hashed     = false;  // Ever seen hash for this number?
    boolean seenDigit  = false;  // Seen digit for this number component?

    for (int i=0; i<length; i++) {
      char c = s.charAt(i);

      switch (c) {

      case '+':
        if (0 == i) {
          // May be the first character.
          seenPlus = true;
        } else if (-1 != expPos) {
          // May be the first character in an exponent.
          if (expPos + 1 != i) {
            throw new BadFormatException("Unrecognized \'+\' in number format");
          }
        } else {
          // May not appear anywhere else.
          throw new BadFormatException("Unrecognized \'+\' in number format");
        }
        break;

      case '-':
        if (0 == i) {
          // May be the first character.
        } else if (-1 != expPos) {
          // May be the first character in an exponent.
          if (expPos + 1 != i) {
            throw new BadFormatException("Unrecognized \'-\' in number format");
          }
        } else {
          // May not appear anywhere else.
          throw new BadFormatException("Unrecognized \'-\' in number format");
        }
        break;

      case '.':
        if (-1 != dotPos) {
          // May only appear once.
          throw new BadFormatException(
            "Unrecognized second \'.\' in number format");
        } else if (-1 != expPos) {
          // May not appear in exponent.
          throw new BadFormatException(
            "Unrecognized \'.\' in exponent of number format");
        } else if (-1 != slashPos) {
          // May not appear in rational
          throw new BadFormatException(
            "Unrecognized \'.\' in rational number format");
        } else if (10 != radix) {
          // Radix must be 10.
          throw new BadFormatException(
            "Illegal radix " + radix + " for decimal number format");
        } else {
          dotPos = i;
        }
        break;

      case 'e':
      case 'E':
        if (16 == radix) {                 // Is digit in radix 16.
          if (seenHash) {
            throw new BadFormatException(
              "Hash \'#\' followed by digit in number format");
          } else {
            seenDigit = true;
          }
        } else if (! seenDigit) {          // Is exponent marker.
          // Must follow a digit.
          throw new BadFormatException(
            "Unrecognized exponent marker \'" + c +
            "\' before digit in number format");
        } else if (-1 != expPos) {
          // May only appear once.
          throw new BadFormatException(
            "Unrecognized second exponent marker \'" + c + 
            "\' in number format");
        } else if (-1 != slashPos) {
          // May not appear in rational
          throw new BadFormatException(
            "Unrecognized exponent marker \'" + c +
            "\' in rational of number format");
        } else if (10 != radix) {
          // Radix must be 10.
          throw new BadFormatException(
            "Illegal radix " + radix + " for decimal number format");
        } else {
          expPos    = i;
          seenDigit = false; // Exponent must have digit
          seenHash  = false; // Reset hash scan or digits won't be recognized.
        }
        break;

      case 'f':
      case 'F':
        if (16 == radix) {                 // Is digit in radix 16.
          if (seenHash) {
            throw new BadFormatException(
              "Hash \'#\' followed by digit in number format");
          } else {
            seenDigit = true;
            break;
          }
        }
        // Fall through.
      case 's':
      case 'S':
        if (! seenDigit) {
          // Must follow a digit.
          throw new BadFormatException(
            "Unrecognized exponent marker \'" + c +
            "\' before digit in number format");
        } else if (-1 != expPos) {
          // May only appear once.
          throw new BadFormatException(
            "Unrecognized second exponent marker \'" + c + 
            "\' in number format");
        } else if (-1 != slashPos) {
          // May not appear in rational
          throw new BadFormatException(
            "Unrecognized exponent marker \'" + c +
            "\' in rational number format");
        } else if (10 != radix) {
          // Radix must be 10.
          throw new BadFormatException("Illegal radix " + radix +
                                       " for decimal number format");
        } else {
          expPos     = i;
          normalExp  = false;
          doublePrec = false;
          seenDigit  = false; // Exponent must have digit.
          seenHash   = false; // Reset hash scan or digits won't be recognized.
        }
        break;

      case 'd':
      case 'D':
        if (16 == radix) {                 // Is digit in radix 16.
          if (seenHash) {
            throw new BadFormatException(
              "Hash \'#\' followed by digit in number format");
          } else {
            seenDigit = true;
            break;
          }
        }
        // Fall through.
      case 'l':
      case 'L':
        if (! seenDigit) {
          // Must follow a digit.
          throw new BadFormatException(
            "Unrecognized exponent marker \'" + c +
            "\' before digit in number format");
        } else  if (-1 != expPos) {
          // May only appear once.
          throw new BadFormatException(
            "Unrecognized second exponent marker \'" + c + 
            "\' in number format");
        } else if (-1 != slashPos) {
          // May not appear in rational
          throw new BadFormatException(
            "Unrecognized exponent marker \'" + c +
            "\' in rational number format");
        } else if (10 != radix) {
          // Radix must be 10.
          throw new BadFormatException("Illegal radix " + radix +
                                       " for decimal number format");
        } else {
          expPos     = i;
          normalExp  = false;
          seenDigit  = false; // Exponent must have digit.
          seenHash   = false; // Reset hash scan or digits won't be recognized.
        }
        break;

      case '/':
        if (-1 != slashPos) {
          // May only appear once.
          throw new BadFormatException (
            "Unrecognized second slash \'/\' in rational number format");
        } else if ((-1 != dotPos) || (-1 != expPos)) {
          // May not appear in decimal format.
          throw new BadFormatException (
            "Unrecognized slash \'/\' in decimal number format");
        } else {
          slashPos  = i;
          seenDigit = false; // Reset digit scan.
          seenHash  = false; // Reset hash scan.
        }
        break;

      case '#':
        if (-1 != expPos) {
          // May not appear in exponent.
          throw new BadFormatException(
            "Unrecognized \'#\' in exponent of number format");
        } else if (! seenDigit) {
          // Must follow leading digit.
          throw new BadFormatException(
            "Unrecognized \'#\' before digit in number format");
        } else {
          seenHash = true;
          hashed   = true;
        }
        break;

      default:
        if (isDigit(c, radix)) {
          if (seenHash) {
            throw new BadFormatException(
              "Hash \'#\' followed by digit in number format");
          } else {
            seenDigit = true;
          }
        } else {
          throw new BadFormatException(
            "Unrecognized character \'" + c + "\' in number format");
        }
      }
    } // End of scan loop

    if (! seenDigit) {
      // Ensure that last number component has a digit.
      throw new BadFormatException("Missing digit in number format");

    } else if (UNSPECIFIED == exact) {
      // Set unspecified exactness.

      if (hashed || (-1 != dotPos) || (-1 != expPos)) {
        // Inexact if number contains hash mark or is decimal.
        exact = INEXACT;
      } else {
        exact = EXACT;
      }
    }

    // Now, we are ready to do the actual conversion from text to number.
    if ((-1 != dotPos) || (-1 != expPos)) {
      // Process decimal format.
      if (hashed || (! normalExp)) {
        return parseDecimal(normalize(s, 0, length), exact,
                            dotPos, expPos, doublePrec);
      } else {
        return parseDecimal(s, exact, dotPos, expPos, doublePrec);
      }

    } else {
      // Process integer or rational format.
      if (-1 != slashPos) {
        // Rational.
        if (! hashed) {
          if (seenPlus) {
            return parseRational(s.substring(1), radix, exact, slashPos);
          } else {
            return parseRational(s, radix, exact, slashPos);
          }
        } else {
          return parseRational(normalize(s, (seenPlus? 1 : 0), length),
                               radix, exact, slashPos);
        }

      } else {
        // Integer.
        if (! hashed) {
          if (seenPlus) {
            return parseInteger(s.substring(1), radix, exact);
          } else {
            return parseInteger(s, radix, exact);
          }
        } else {
          return parseInteger(normalize(s, (seenPlus? 1 : 0), length),
                              radix, exact);
        }
      }
    }
  }

  /**
   * Parse the specified number format as a decimal with the specified
   * exactness and return the corresponding number. The specified
   * string must be a valid floating point literal as specified in
   * &sect;3.10.2 of the Java Language Specification with an optional
   * leading sign marker. It may not contain any hash characters or
   * any exponent marker other than '<code>e</code>' (or
   * '<code>E<code>').
   *
   * @param   s           The string containing the number format for
   *                      the real number.
   * @param   exact       The exactness of the number. Must be
   *                      {@link #EXACT} or {@link INEXACT}.
   * @param   dotPos      The index of the decimal dot in the
   *                      specified number format, or -1 if the
   *                      number format does not contain a decimal
   *                      dot.
   * @param   expPos      The index of the exponent marker in the
   *                      specified number format, or -1 if the
   *                      number format does not contain an
   *                      exponent marker.
   * @param   doublePrec  <code>true</code> indicates that the
   *                      number should have double precision if
   *                      it is actually represented as an inexact
   *                      floating point number.
   * @return              The corresponding number, represented
   *                      as a float or a double for inexact numbers,
   *                      and represented as an Integer, Long, 
   *                      BigInteger, or Rational for exact numbers.
   * @throws  BadFormatException
   *                      Signals that the exponent for an exact
   *                      decimal number format is too large.
   */
  private static Number parseDecimal(String s, int exact, int dotPos,
                                     int expPos, boolean doublePrec)
    throws BadFormatException {

    if (INEXACT == exact) {
      // Process the inexact representation.
      try {
        double d = Double.parseDouble(s);
        if (doublePrec) {
          return Cast.box(d);
        } else {
          return Cast.box((float)d);
        }
      } catch (NumberFormatException x) {
        // Must never happen.
        throw new Bug("Unexpected exception: " + x.toString());
      }

    } else {
      // Process the exact representation.

      /*
       * The strategy is as follows: Translate the decimal format into
       * an integer or rational format and then call on the parser for
       * that format. While this strategy results in extra string
       * manipulation, it has the clear advantage that it clearly
       * avoids redundant implementations of the same functionality,
       * and preserves all specified digits as both integers and
       * rationals are of practically unlimited precision.
       *
       * (1) Identify position of last non-zero digit.
       * (2) Copy all digits (including an optional '-' but not '+')
       *     before that position into a new buffer. Omit the decimal
       *     dot if present.
       * (3) Calculate the basic exponent resulting from step 2
       *     and add in the optional explicit exponent.
       * (4) Now, if the total exponent is non-negative, the resulting
       *     number is an integer. We patch the buffer with necessary
       *     zeroes and pass the result to the integer parser. If the
       *     total exponent is negative, the resulting number is a
       *     rational. We patch the buffer with the appropriate
       *     denominator and pass the result to the rational parser.
       */

      int length    = s.length();
      int beforeExp = ((-1 == expPos)? length - 1 : expPos - 1);

      // Identify position of last non-zero digit.
      int lastSigPos = -1;
      for (int i=beforeExp; i>=0; i--) {
        char c = s.charAt(i);

        if (('0' != c) && ('.' != c) && ('+' != c) && ('-' != c)) {
          lastSigPos = i;
          break;
        }
      }

      if (-1 == lastSigPos) {
        // Nothing but zeroes, dots, signs, and exponent is a zero,
        // no matter how you look at it.
        return Cast.ZERO;
      }

      // Determine the basic exponent, that is, the exponent resulting
      // from copying only significant digits without the decimal dot.
      int basicExp = 0;

      if (-1 == dotPos) {
        // No decimal dot, measure distance from beforeExp.
        basicExp = beforeExp - lastSigPos;
      } else {
        // We have a decimal dot, measure their difference.
        if (lastSigPos < dotPos) {
          basicExp = dotPos - 1 - lastSigPos;
        } else {
          basicExp = dotPos - lastSigPos;
        }
      }

      // Extract explicit exponent.
      int exp = 0;
      if (-1 != expPos) {
        try {
          exp = Integer.parseInt(s.substring((('+' == s.charAt(expPos + 1))?
                                              (expPos + 2) : (expPos + 1)),
                                             length));
        } catch (NumberFormatException x) {
          throw new BadFormatException(
            "Exponent too large for exact decimal number format");
        }
      }

      // Calculate total exponent.
      exp += basicExp;

      // Fill buffer with significant digits.
      StringBuffer buf = new StringBuffer(lastSigPos + java.lang.Math.abs(exp)
                                          + 2);
      for (int i=0; i<=lastSigPos; i++) {
        char c = s.charAt(i);

        if (('+' != c) && ('.' != c)) {
          buf.append(c);
        }
      }

      // Patch up number and send it off.
      if (exp >= 0) {
        for (int i=0; i<exp; i++) {
          buf.append('0');
        }

        return parseInteger(buf.toString(), 10, exact);

      } else {
        int slashPos = buf.length();
        buf.append("/1");
        for (int i=0; i>exp; i--) {
          buf.append('0');
        }

        return parseRational(buf.toString(), 10, exact, slashPos);
      }
    }
  }
 
  /**
   * Parse the specified number format as a rational with the
   * specified radix and specified exactness and return the
   * corresponding number. The specified string may start with an
   * optional minus sign as its first character but must otherwise
   * contain only characters that are legal digits for the specified
   * radix as well as the slash character at the specified
   * position. The specified string must not be <code>null</code> or
   * of any length smaller than 3. It must contain two non-empty
   * numerical components separated by the slash character.
   *
   * @param   s         The string containing the number format
   *                    for the rational number.
   * @param   radix     The radix for the number. Must be 2, 8, 10,
   *                    or 16.
   * @param   exact     The exactness of the number. Must be
   *                    {@link #EXACT} or {@link #INEXACT}.
   * @param   slashPos  The index of the rational slash in the
   *                    specified number format.
   * @return            The corresponding number, represented
   *                    as a double for inexact numbers, and
   *                    represented as a Integer, Long, BigInteger,
   *                    or Rational for exact numbers.
   * @throws  BadFormatException
   *                    Signals a zero denominator.
   */
  private static Number parseRational(String s, int radix, int exact,
                                      int slashPos)
    throws BadFormatException {

    Number n1 = parseInteger(s.substring(0, slashPos), radix, exact);
    Number n2 = parseInteger(s.substring(slashPos + 1, s.length()),
                             radix, exact);

    /*
     * Note that in the presence of big integers the result of
     * parseInteger is exact if (EXACT == exact). So, we don't
     * need to worry about enforcing exactness in this method,
     * only about preserving it.
     */

    if ((n1 instanceof Integer) || (n1 instanceof BigInteger)) {
      // Return an exact rational.

      // The two numbers can be integers or big integers.
      if ((n1 instanceof BigInteger) || (n2 instanceof BigInteger)) {

        BigInteger i1 = ((n1 instanceof BigInteger)? ((BigInteger)n1)
          : BigInteger.valueOf(((Integer)n1).longValue()));
        BigInteger i2 = ((n2 instanceof BigInteger)? ((BigInteger)n2)
          : BigInteger.valueOf(((Integer)n2).longValue()));

        if (i2.signum() == 0) {
          throw new BadFormatException(
            "Zero denominator in rational number format");

        } else if (i1.mod(i2).signum() == 0) {
          return pack(i1.divide(i2));

        } else {
          return Rational.valueOf(i1, i2);
        }
      
      } else {
        int i1 = n1.intValue();
        int i2 = n2.intValue();
      
        if (0 == i2) {
          throw new BadFormatException(
            "Zero denominator in rational number format");

        } else if ((i1 % i2) == 0) {
          return Cast.box(i1 / i2);

        } else {
          return Rational.valueOf(i1, i2);
        }
      }

    } else {
      // Return an inexact rational.

      double d1 = n1.doubleValue();
      double d2 = n2.doubleValue();

      if (0.0D == d2) {
        throw new BadFormatException(
          "Zero denominator in rational number format");
      } else {
        return Cast.box(d1/d2);
      }
    }
  }

  /**
   * Parse the specified number format as an integer with the
   * specified radix and specified exactness and return the
   * corresponding number. The specified string may start with an
   * optional minus sign as its first character but must otherwise
   * contain only characters that are legal digits for the specified
   * radix. The specified string must not be <code>null</code> or of
   * zero length and must contain at least one digit.
   *
   * @param   s      The string containing the number format for
   *                 the integer number.
   * @param   radix  The radix for the number. Must be 2, 8, 10, or
   *                 16.
   * @param   exact  The exactness of the number. Must be
   *                 {@link #EXACT} or {@link #INEXACT}.
   * @return         The corresponding number, represented as an
   *                 Integer or BigInteger for exact numbers, and
   *                 represented as a Double for inexact numbers.
   */
  private static Number parseInteger(String s, int radix, int exact) {

    try {
      int i = Integer.parseInt(s, radix); // Try to parse as an int.
      if (INEXACT == exact) {
        return Cast.box((double)i);
      } else {
        return Cast.box(i);
      }
    } catch (NumberFormatException x) {
      /*
       * If, as specified for this method, the string is not null or
       * of zero length, contains only legal digits for the specified
       * radix, with an optional leading minus sign, and the radix is
       * 2, 8, 10, or 16, this exception means that the number
       * specified by the string does not fit into an int. We
       * therefore use a big integer.
       *
       * Note that we parse into a big integer, even if the end result
       * is inexact, because the double parser does not like radices
       * besides 10 and rolling our own (I tried) may introduce
       * numerical inaccuracies.
       */

      BigInteger i;

      try {
        i = new BigInteger(s, radix);
      } catch (NumberFormatException xx) {
        // Must never happen.
        throw new Bug("Unexpected exception: " + xx.toString());
      }

      if (INEXACT == exact) {
        return Cast.box(i.doubleValue());
      } else {
        return i;
      }
    }
  }

  // ==================================================================
  //                           Unparsing.
  // ==================================================================

  /**
   * Return a string representing the specified number in the
   * decimal system.
   *
   * @param   n  The number to convert to a string.
   * @return     A string representing the specified number.
   */
  public static String toString(Number n) {
    try {
      return toString(n, 10);
    } catch (IllegalArgumentException x) {
      // Must never happen.
      throw new Bug("Unexpected exception: " + x.toString());
    }
  }

  /**
   * Return a string representing the specified number with the
   * specified radix. The specified radix must be 2, 8, 10, or 16
   * for exact numbers, and it must be 10 for inexact numbers.
   *
   * @param   n  The number to convert to a string.
   * @param   radix
   *             The radix for the external representation.
   * @return     A string representing the specified number with
   *             the specified radix.
   * @throws  IllegalArgumentException
   *             Signals that the radix is not 2, 8, 10, or 16
   *             for exact numbers, or that it is not 10 for
   *             inexact numbers.
   */
  public static String toString(Number n, int radix) {

    // Check radix.
    if ((radix != 2) && (radix != 8) && (radix != 10) && (radix != 16)) {
      throw new IllegalArgumentException("Illegal radix " + radix);
    }

    if ((n instanceof Integer) || (n instanceof Long) ||
        (n instanceof Short) || (n instanceof Byte)) {
      long l = n.longValue();
      return Long.toString(l, radix);

    } else if (n instanceof BigInteger) {
      return ((BigInteger)n).toString(radix);

    } else if (n instanceof Rational) {
      return ((Rational)n).toString(radix);

    } else if (n instanceof BigDecimal) {
      if (10 != radix) {
        throw new IllegalArgumentException("Illegal radix " + radix +
                                           " for inexact number");
      }

      return n.toString();

    } else {
      if (10 != radix) {
        throw new IllegalArgumentException("Illegal radix " + radix +
                                           " for inexact number");
      }

      double d = n.doubleValue();

      if (Double.isNaN(d)) {
        return "#[NaN]";
      } else if (Double.isInfinite(d)) {
        if (0.0 < d) {
          return "#[+Infinity]";
        } else {
          return "#[-Infinity]";
        }
      } else {
        return n.toString();
      }
    }
  }

  // ==================================================================
  //                      Tests and Comparisons.
  // ==================================================================

  /**
   * Determine whether the specified object represents a number.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents a number.
   */
  public static boolean isNumber(Object o) {
    return (o instanceof Number);
  }

  /**
   * Determine whether the specified object represents an exact
   * number.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents an inexact number.
   */
  public static boolean isExact(Object o) {
    return ((o instanceof Integer) || (o instanceof BigInteger) || 
            (o instanceof Rational) || (o instanceof Long) ||
            (o instanceof Short) || (o instanceof Byte));
  }

  /**
   * Determine whether the specified object represents an inexact
   * number.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents an inexact number.
   */
  public static boolean isInexact(Object o) {
    return ((o instanceof Number) &&
            (! ((o instanceof Integer) || (o instanceof BigInteger) ||
                (o instanceof Rational) || (o instanceof Long) ||
                (o instanceof Short) || (o instanceof Byte))));
  }

  /**
   * Determine whether the specified object represents a complex
   * number.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specfieid object
   *             represents a complex number.
   */
  public static boolean isComplex(Object o) {
    return isNumber(o);
  }

  /**
   * Determine whether the specified object represents a real
   * number.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents a real number.
   */
  public static boolean isReal(Object o) {
    return isNumber(o);
  }

  /**
   * Determine whether the specified object represents a
   * rational number.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents a rational number.
   */
  public static boolean isRational(Object o) {
    return isNumber(o);
  }

  /**
   * Determine whether the specified object represents an
   * integer number.
   *
   * @param    o  The object to test.
   * @return      <code>true</code> iff the specified object
   *              represents an integer number.
   */
  public static boolean isInteger(Object o) {
    if ((o instanceof Integer) || (o instanceof BigInteger) || 
        (o instanceof Long) || (o instanceof Short) ||
        (o instanceof Byte)) {
      return true;

    } else if (o instanceof Rational) {
      return ((Rational)o).isInteger();

    } else if (o instanceof BigDecimal) {
      BigDecimal d = (BigDecimal)o;
      return (d.compareTo(d.setScale(0, BigDecimal.ROUND_DOWN)) == 0);

    } else if (o instanceof Number) {
      double d = ((Number)o).doubleValue();
      return (java.lang.Math.ceil(d) == d);
      
    } else {
      return false;
    }
  }

  /**
   * Determine whether the specified numbers, in order, are related to
   * each other according to the specified type of comparison. The
   * specified pair must start a proper list of length at least 2, and
   * the specified type must be one of the comparison identifiers
   * defined by this class.
   *
   * @see     #COMPARE_EQUAL
   * @see     #COMPARE_LESS
   * @see     #COMPARE_GREATER
   * @see     #COMPARE_LESS_EQUAL
   * @see     #COMPARE_GREATER_EQUAL
   *
   * @param   type  The type of the comparison.
   * @param   p     The pair starting the proper list of numbers to
   *                compare.
   * @return        <code>true</code> iff the specified numbers
   *                are related to each other according to the
   *                specified comparison.
   * @throws  BadTypeException
   *                Signals that an object on the specified list
   *                is not a number.
   * @throws  NullPointerException
   *                Signals that the specified list is not at
   *                least of length 2.
   * @throws  ClassCastException
   *                Signals that the specified list is not a proper
   *                list, but rather ends with a dotted pair.
   * @throws  IllegalArgumentException
   *                Signals that the specified list contains at
   *                least <code>Integer.MAX_VALUE</code> elements and
   *                may thus be circular, or that <code>type</code>
   *                is illegal.
   */
  public static boolean compare(int type, Pair p) throws BadTypeException {

    if ((COMPARE_EQUAL > type) || (COMPARE_GREATER_EQUAL < type)) {
      throw new IllegalArgumentException("Illegal type specifier " +
                                         type + " for comparison");
    }

    int category = categorize(p);

    switch (category) {

    case CATEGORY_INT:
    case CATEGORY_LONG:
      // First value.
      long l1 = ((Number)p.car).longValue();

      p = (Pair)p.cdr;

      do {
        long l2 = ((Number)p.car).longValue();

        switch (type) {
        case COMPARE_EQUAL:
          if (l1 != l2) { return false; } else { break; }
        case COMPARE_LESS:
          if (l1 >= l2) { return false; } else { l1 = l2; break; }
        case COMPARE_GREATER:
          if (l1 <= l2) { return false; } else { l1 = l2; break; }
        case COMPARE_LESS_EQUAL:
          if (l1 > l2)  { return false; } else { l1 = l2; break; }
        case COMPARE_GREATER_EQUAL:
          if (l1 < l2)  { return false; } else { l1 = l2; break; }
        default:
          // Must never happen.
          throw new Bug("Illegal comparison type " + type);
        }

        // Next.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      return true;

    case CATEGORY_BIG_INT:
      // First value.
      Object     o  = p.car;
      BigInteger i1 = ((o instanceof BigInteger)? (BigInteger)o
                       : BigInteger.valueOf(((Number)o).longValue()));

      p = (Pair)p.cdr;

      do {
        o             = p.car;
        BigInteger i2 = ((o instanceof BigInteger) ? (BigInteger)o
          : BigInteger.valueOf(((Number)o).longValue()));

        switch (type) {
        case COMPARE_EQUAL:
          if (i1.compareTo(i2) != 0) { return false; } else { break; }
        case COMPARE_LESS:
          if (i1.compareTo(i2) >= 0) { return false; } else { i1 = i2; break; }
        case COMPARE_GREATER:
          if (i1.compareTo(i2) <= 0) { return false; } else { i1 = i2; break; }
        case COMPARE_LESS_EQUAL:
          if (i1.compareTo(i2) >  0) { return false; } else { i1 = i2; break; }
        case COMPARE_GREATER_EQUAL:
          if (i1.compareTo(i2) <  0) { return false; } else { i1 = i2; break; }
        default:
          // Must never happen.
          throw new Bug("Illegal comparison type " + type);
        }

        // Next.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      return true;

    case CATEGORY_RATIONAL:
      // First value.
      o           = p.car;
      Rational r1 = Rational.valueOf((Number)o);

      p = (Pair)p.cdr;

      do {
        o           = p.car;
        Rational r2 = Rational.valueOf((Number)o);

        switch (type) {
        case COMPARE_EQUAL:
          if (r1.compareTo(r2) != 0) { return false; } else { break; }
        case COMPARE_LESS:
          if (r1.compareTo(r2) >= 0) { return false; } else { r1 = r2; break; }
        case COMPARE_GREATER:
          if (r1.compareTo(r2) <= 0) { return false; } else { r1 = r2; break; }
        case COMPARE_LESS_EQUAL:
          if (r1.compareTo(r2) >  0) { return false; } else { r1 = r2; break; }
        case COMPARE_GREATER_EQUAL:
          if (r1.compareTo(r2) <  0) { return false; } else { r1 = r2; break; }
        default:
          // Must never happen.
          throw new Bug("Illegal comparsion type " + type);
        }

        // Next.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      return true;

    case CATEGORY_BIG_DECIMAL:
    case CATEGORY_DOUBLE:
      // First value.
      double d1 = ((Number)p.car).doubleValue();

      p = (Pair)p.cdr;

      do {
        double d2 = ((Number)p.car).doubleValue();

        switch (type) {
        case COMPARE_EQUAL:
          if (d1 != d2) { return false; } else { break; }
        case COMPARE_LESS:
          if (d1 >= d2) { return false; } else { d1 = d2; break; }
        case COMPARE_GREATER:
          if (d1 <= d2) { return false; } else { d1 = d2; break; }
        case COMPARE_LESS_EQUAL:
          if (d1 > d2)  { return false; } else { d1 = d2; break; }
        case COMPARE_GREATER_EQUAL:
          if (d1 < d2)  { return false; } else { d1 = d2; break; }
        default:
          // Must never happen.
          throw new Bug("Illegal comparison type " + type);
        }

        // Next.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      return true;

    case CATEGORY_NAN:
      return false;

    default:
      // Must never happen.
      throw new Bug("Illegal category " + category);
    }
  }

  /**
   * Determine whether the specified object represents zero.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             is a number representing zero.
   */
  public static boolean isZero(Object o) {
    if ((o instanceof Integer) || (o instanceof Long) ||
        (o instanceof Byte) || (o instanceof Short)) {
      long l = ((Number)o).longValue();
      return (0 == l);

    } else if (o instanceof BigInteger) {
      return (((BigInteger)o).signum() == 0);

    } else if (o instanceof Rational) {
      return ((Rational)o).isZero();

    } else if (o instanceof BigDecimal) {
      return (((BigDecimal)o).signum() == 0);

    } else if (o instanceof Number) {
      double d = ((Number)o).doubleValue();
      return (0.0D == d);

    } else {
      return false;
    }
  }

  /**
   * Determine whether the specified object represents a
   * positive number.
   * 
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents a positive number.
   */
  public static boolean isPositive(Object o) {
    if ((o instanceof Integer) || (o instanceof Long) ||
        (o instanceof Byte) || (o instanceof Short)) {
      long l = ((Number)o).longValue();
      return (0 < l);

    } else if (o instanceof BigInteger) {
      return (((BigInteger)o).signum() > 0);

    } else if (o instanceof Rational) {
      return (((Rational)o).signum() > 0);

    } else if (o instanceof BigDecimal) {
      return (((BigDecimal)o).signum() > 0);

    } else if (o instanceof Number) {
      double d = ((Number)o).doubleValue();
      return (0.0D < d);

    } else {
      return false;
    }
  }

  /**
   * Determine whether the specified object represents a
   * negative number.
   * 
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents a negative number.
   */
  public static boolean isNegative(Object o) {
    if ((o instanceof Integer) || (o instanceof Long) ||
        (o instanceof Byte) || (o instanceof Short)) {
      long l = ((Number)o).longValue();
      return (0 > l);

    } else if (o instanceof BigInteger) {
      return (((BigInteger)o).signum() < 0);

    } else if (o instanceof Rational) {
      return (((Rational)o).signum() < 0);

    } else if (o instanceof BigDecimal) {
      return (((BigDecimal)o).signum() < 0);

    } else if (o instanceof Number) {
      double d = ((Number)o).doubleValue();
      return (0.0D > d);

    } else {
      return false;
    }
  }

  /**
   * Determine whether the specified object represents an
   * odd integer.
   * 
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents an odd integer.
   */
  public static boolean isOdd(Object o) {

    if ((o instanceof Integer) || (o instanceof Long) ||
        (o instanceof Byte) || (o instanceof Short)) {
      long l = ((Number)o).longValue();
      return ((l % 2) != 0);

    } else if (o instanceof BigInteger) {
      return (! ((BigInteger)o).mod(BIG_TWO).equals(BigInteger.ZERO));

    } else if (o instanceof Rational) {
      Rational r = (Rational)o;
      if (r.isInteger()) {
        BigInteger i = r.numerator();
        return (! i.mod(BIG_TWO).equals(BigInteger.ZERO));
      } else {
        return false;
      }

    } else if (o instanceof BigDecimal) {
      BigDecimal d = (BigDecimal)o;
      if (d.compareTo(d.setScale(0, BigDecimal.ROUND_DOWN)) == 0) {
        BigInteger i = d.unscaledValue();
        return (! i.mod(BIG_TWO).equals(BigInteger.ZERO));
      } else {
        return false;
      }

    } else if (o instanceof Number) {
      double d = ((Number)o).doubleValue();

      if (java.lang.Math.ceil(d) == d) {
        return ((d % 2.0D) != 0.0D);
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Determine whether the specified object represents an
   * even integer.
   * 
   * @param   o  The object to test.
   * @return     <code>true</code> iff the specified object
   *             represents an even integer.
   */
  public static boolean isEven(Object o) {

    if ((o instanceof Integer) || (o instanceof Long) ||
        (o instanceof Byte) || (o instanceof Short)) {
      long l = ((Number)o).longValue();
      return ((l % 2) == 0);

    } else if (o instanceof BigInteger) {
      return ((BigInteger)o).mod(BIG_TWO).equals(BigInteger.ZERO);

    } else if (o instanceof Rational) {
      Rational r = (Rational)o;
      if (r.isInteger()) {
        BigInteger i = r.numerator();
        return i.mod(BIG_TWO).equals(BigInteger.ZERO);
      } else {
        return false;
      }

    } else if (o instanceof BigDecimal) {
      BigDecimal d = (BigDecimal)o;
      if (d.compareTo(d.setScale(0, BigDecimal.ROUND_DOWN)) == 0) {
        BigInteger i = d.unscaledValue();
        return i.mod(BIG_TWO).equals(BigInteger.ZERO);
      } else {
        return false;
      }

    } else if (o instanceof Number) {
      double d = ((Number)o).doubleValue();

      if (java.lang.Math.ceil(d) == d) {
        return ((d % 2.0D) == 0.0D);
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Determine whether the specified object represents not-a-number.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff <code>o</code> represents
   *             not-a-number.
   */
  public static boolean isNaN(Object o) {
    if (isInexact(o) && (! (o instanceof BigDecimal))) {
      double d = ((Number)o).doubleValue();
      return Double.isNaN(d);
    } else {
      return false;
    }
  }

  /**
   * Determine whether the specified object represents an infinity,
   * either positive or negative.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> iff <code>o</code> represents
   *             an infinity.
   */
  public static boolean isInfinite(Object o) {
    if (isInexact(o) && (! (o instanceof BigDecimal))) {
      double d = ((Number)o).doubleValue();
      return Double.isInfinite(d);
    } else {
      return false;
    }
  }

  /**
   * Test whether the two specified objects represent equal
   * numbers. Two numbers are equal according to this method if they
   * are either both exact or both inexact and if they have the same
   * numerical value.
   *
   * @param   o1  The first object to compare.
   * @param   o2  The second object to compare.
   * @return      <code>true</code> iff both objects are numbers
   *              and both numbers are either exact or inexact
   *              and both numbers have the same numerical value.
   */
  public static boolean equalExact(Object o1, Object o2) {
    if (isExact(o1) && isExact(o2)) {
      if ((o1 instanceof Rational) || (o2 instanceof Rational)) {
        Rational r1 = Rational.valueOf((Number)o1);
        Rational r2 = Rational.valueOf((Number)o2);

        return r1.equals(r2);

      } else if ((o1 instanceof BigInteger) || (o2 instanceof BigInteger)) {
        BigInteger i1 = ((o1 instanceof BigInteger)? ((BigInteger)o1)
          : BigInteger.valueOf(((Number)o1).longValue()));
        BigInteger i2 = ((o2 instanceof BigInteger)? ((BigInteger)o2)
          : BigInteger.valueOf(((Number)o2).longValue()));

        return (i1.compareTo(i2) == 0);

      } else {
        long l1 = ((Number)o1).longValue();
        long l2 = ((Number)o2).longValue();

        return (l1 == l2);
      }

    } else if (isInexact(o1) && isInexact(o2)) {

      if ((o1 instanceof BigDecimal) && (o2 instanceof BigDecimal)) {
        return (((BigDecimal)o1).compareTo((BigDecimal)o2) == 0);

      } else {
        double d1 = ((Number)o1).doubleValue();
        double d2 = ((Number)o2).doubleValue();

        return (d1 == d2);
      }
    } else {
      return false;
    }
  }

  // ==================================================================
  //                                Casts.
  // ==================================================================

  /**
   * Ensure that the specified object is a number.
   *
   * @param   o  The object to test.
   * @return     The object <code>o</code>.
   * @throws  BadTypeException
   *             Signals that <code>o</code> does not represent
   *             a number.
   */
  public static Number toNumber(Object o) throws BadTypeException {
    if (isNumber(o)) {
      return (Number)o;
    } else {
      throw new BadTypeException("Not a number", o);
    }
  }

  /**
   * Ensure that the specified number is an integer.
   *
   * @param   n  The number to test.
   * @return     The number <code>n</code>, or the corresponding
   *             big integer if <code>n</code> is an integer in
   *             rational representation.
   * @throws  BadArgumentException
   *             Signals that <code>n</code> does not represent
   *             an integer number.
   */
  public static Number toInteger(Number n)
    throws BadArgumentException {

    if ((n instanceof Integer) || (n instanceof BigInteger) ||
        (n instanceof Long) || (n instanceof Short) ||
        (n instanceof Byte)) {
      return n;

    } else if (n instanceof Rational) {
      Rational r = (Rational)n;
      if (r.isInteger()) {
        return r.numerator();
      } else {
        throw new BadArgumentException("Not an integer", n);
      }

    } else if (n instanceof BigDecimal) {
      BigDecimal d = (BigDecimal)n;
      if (d.compareTo(d.setScale(0, BigDecimal.ROUND_DOWN)) == 0) {
        return d;
      } else {
        throw new BadArgumentException("Not an integer", n);
      }

    } else {
      double d = n.doubleValue();
      if (java.lang.Math.ceil(d) == d) {
        return n;
      } else {
        throw new BadArgumentException("Not an integer", n);
      }

    }
  }

  /**
   * Convert the specified object to a big integer.
   *
   * @param   o  The object to convert.
   * @return     A big integer that has the same value as the
   *             specified number.
   * @throws  BadTypeException
   *             Signals that <code>o</code> does not represent
   *             a number.
   * @throws  BadArgumentException
   *             Signals that <code>o</code> does not represent
   *             an integer number.
   */
  public static BigInteger toBigInteger(Object o)
    throws BadTypeException, BadArgumentException {

    if ((o instanceof Integer) || (o instanceof Long) ||
        (o instanceof Short) || (o instanceof Byte)) {
      return BigInteger.valueOf(((Number)o).longValue());

    } else if (o instanceof BigInteger) {
      return (BigInteger)o;

    } else if (o instanceof Rational) {
      Rational r = (Rational)o;
      if (r.isInteger()) {
        return r.numerator();
      } else {
        throw new BadArgumentException("Not an integer", o);
      }

    } else if (o instanceof BigDecimal) {
      BigDecimal d = (BigDecimal)o;
      if (d.compareTo(d.setScale(0, BigDecimal.ROUND_DOWN)) == 0) {
        return Rational.valueOf(d).numerator();
      } else {
        throw new BadArgumentException("Not an integer", o);
      }

    } else if (o instanceof Number) {
      double d = ((Number)o).doubleValue();
      if (java.lang.Math.ceil(d) == d) {
        return Rational.valueOf(d).numerator();
      } else {
        throw new BadArgumentException("Not an integer", o);
      }

    } else {
      throw new BadTypeException("Not a number", o);
    }
  }

  /*
   * Convert the specified double to a float.
   *
   * @param   n  The double number to convert.
   * @return     The corresponding float.
   * @throws  BadArgumentException
   *             Signals that the specified double does not fit
   *             into a float.
   */
  public static float toFloat(double n) throws BadArgumentException {
    // Get rid of NaN and infinities.
    if (Double.isNaN(n) || Double.isInfinite(n)) {
      return (float)n;
    }

    // Float value.
    float  f     = (float)n;
    int    fbits = Float.floatToIntBits(f);
    int    fe    = ((fbits >> 23) & 0xff);
    int    fm    = (fe == 0) ?
      (fbits & 0x7fffff) << 1 :
      (fbits & 0x7fffff) | 0x800000;
    
    // Unpack as double value.
    double d     = n;
    long   dbits = Double.doubleToLongBits(d);
    int    de    = (int)((dbits >> 52) & 0x7ffL);
    long   dm    = (de == 0) ?
      (dbits & 0xfffffffffffffL) << 1 :
      (dbits & 0xfffffffffffffL) | 0x10000000000000L;
      
    // Normalize exponents.
    fe -= 150;
    de -= 1075;
      
    // Compare the two.
    if ((fe == de) && (fm == dm)) {
      return f;
    } else {
      throw new BadArgumentException("Unable to convert to float", Cast.box(n));
    }
  }

  /**
   * Categorize the numbers which are the elements of the specified
   * proper list. Returns a category identifier which identifies the
   * least common representation of all numbers on the specified
   * list. The following categories are defined: <ol>
   * <li><code>CATEGORY_INT</code>: All numbers are of type
   * <code>Byte</code>, <code>Short</code>, or <code>Integer</code>.
   * </li>
   * <li><code>CATEGORY_LONG</code>: All numbers are either in the
   * <code>CATEGORY_INT</code> category or of type <code>LONG</code>.
   * </li>
   * <li><code>CATEGORY_BIG_INT</code>: All numbers are either in
   * the <code>CATEGORY_LONG</code> category or of type
   * <code>BigInteger</code>.
   * </li>
   * <li><code>CATEGORY_RATIONAL</code>: All numbers are either in
   * the <code>CATEGORY_BIG_INT</code> category or of type
   * <code>Rational</code>.
   * </li>
   * <li><code>CATEGORY_BIG_DECIMAL</code>: All numbers are either
   * in the <code>CATEGORY_RATIONAL</code> category or of type
   * <code>BigDecimal</code>.
   * </li>
   * <li><code>CATEGORY_DOUBLE</code>: All numbers are either
   * in the <code>CATEGORY_BIG_DECIMAL</code> category or of type
   * <code>Float</code> or <code>Double</code> or of some unknown
   * subtype of <code>java.lang.Number</code>, but do not represent
   * not-a-number.
   * </li>
   * <li><code>CATEGORY_NAN</code>: All numbers are either in the
   * <code>CATEGORY_DOUBLE</code> cateogory or are of type
   * <code>Float</code> or <code>Double</code> and represent
   * not-a-number.
   * </li>
   * </ol>
   *
   * @see     #CATEGORY_INT
   * @see     #CATEGORY_LONG
   * @see     #CATEGORY_BIG_INT
   * @see     #CATEGORY_RATIONAL
   * @see     #CATEGORY_BIG_DECIMAL
   * @see     #CATEGORY_DOUBLE
   * @see     #CATEGORY_NAN
   * 
   * @param   p   The pair starting the proper list of numbers to
   *              categorize.
   * @return      The number category identifier.
   * @throws  BadTypeException
   *              Signals that an object on the specified list
   *              is not a number.
   * @throws  ClassCastException
   *              Signals that the specified list is not a proper
   *              list, but rather ends with a dotted pair.
   * @throws  IllegalArgumentException
   *              Signals that the specified list contains at least
   *              <code>Integer.MAX_VALUE</code> elements
   *              and may thus be circular.
   */
  public static int categorize(Pair p) throws BadTypeException {
    int category = CATEGORY_INT;
    int count    = 0;

    while (Pair.EMPTY_LIST != p) {
      Object o = p.car;

      if ((o instanceof Integer) ||
          (o instanceof Short) ||
          (o instanceof Byte)) {
        // No effect on category.

      } else if (o instanceof Long) {
        if (CATEGORY_LONG > category) {
          category = CATEGORY_LONG;
        }

      } else if (o instanceof BigInteger) {
        if (CATEGORY_BIG_INT > category) {
          category = CATEGORY_BIG_INT;
        }

      } else if (o instanceof Rational) {
        if (CATEGORY_RATIONAL > category) {
          category = CATEGORY_RATIONAL;
        }

      } else if (o instanceof BigDecimal) {
        if (CATEGORY_BIG_DECIMAL > category) {
          category = CATEGORY_BIG_DECIMAL;
        }

      } else if (o instanceof Number) {
        double d = ((Number)o).doubleValue();

        if (Double.isNaN(d)) {
          category = CATEGORY_NAN;
        } else if (CATEGORY_DOUBLE > category) {
          category = CATEGORY_DOUBLE;
        }

      } else {
        throw new BadTypeException("Not a number", o);
      }

      count++;
      if (Integer.MAX_VALUE == count) {
        throw new IllegalArgumentException("List too long");
      }

      p = (Pair)p.cdr;
    }

    return category;
  }

  /**
   * Convert the specified number to an inexact number.
   * 
   * @param   n  The number to convert.
   * @return     The corresponding inexact number.
   */
  public static Number toInexact(Number n) {
    if (isExact(n)) {
      return Cast.box(n.doubleValue());

    } else {
      return n;
    }
  }

  /**
   * Convert the specified number to an exact number.
   *
   * @param   n  The number to convert.
   * @param      The corresponding exact number, or 0 if the
   *             specified number is an inexact number representing
   *             either not-a-number or an infinity.
   */
  public static Number toExact(Number n) {
    if (isExact(n)) {
      return n;

    } else {
      return pack(Rational.valueOf(n));
    }
  }

  // ==================================================================
  //                       Numerical Computations.
  // ==================================================================

  /**
   * Perform the specified computation on the specified list of
   * numbers. The specified pair must start a proper list, and the
   * specified type must be one of the computation identifiers defined
   * by this class. The specified list may be the empty list if
   * <code>type</code> is <code>COMPUTE_ADD</code> or
   * <code>COMPUTE_MUTIPLY</code>. Otherwise, it must be at least of
   * length 1.
   *
   * @see      #COMPUTE_MAX
   * @see      #COMPUTE_MIN
   * @see      #COMPUTE_ADD
   * @see      #COMPUTE_MULTIPLY
   * @see      #COMPUTE_SUBTRACT
   * @see      #COMPUTE_DIVIDE
   *
   * @param    type  The type for this computation.
   * @param    p     The numbers to perform the computation on.
   * @throws   BadTypeException
   *                 Signals that an object on the specified
   *                 list is not a number.
   * @throws   ArithmeticException
   *                 Signals that a divisor in a divide operation
   *                 is zero.
   * @throws   ClassCastException
   *                 Signals that the specified list is not a proper
   *                 list, but rather ends with a dotted pair.
   * @throws   IllegalArgumentException
   *                 Signals that the specified list is too
   *                 short or too long (at least of length
   *                 <code>Integer.MAX_VALUE</code>), or that
   *                 the specified <code>type</code> is illegal.
   */
  public static Number compute(int type, Pair p)
    throws BadTypeException {

    if ((COMPUTE_MAX > type) || (COMPUTE_DIVIDE < type)) {
      throw new IllegalArgumentException("Illegal type specifier " +
                                         type + " for computation");
    } else if (Pair.EMPTY_LIST == p) {
      if (COMPUTE_ADD == type) {
        return Cast.ZERO;
      } else if (COMPUTE_MULTIPLY == type) {
        return Cast.ONE;
      } else {
        throw new IllegalArgumentException(
          "Illegal empty list for computation");
      }
    }

    int category = categorize(p);

    switch (category) {
    case CATEGORY_INT:
      if ((COMPUTE_MULTIPLY != type) && (COMPUTE_DIVIDE != type)) {
        // Multiplication overruns even longs to quickly,
        // and division needs to be exact using rationals.

        long l1 = ((Number)p.car).longValue();

        p = (Pair)p.cdr;

        if (Pair.EMPTY_LIST == p) {
          if (COMPUTE_SUBTRACT == type) {
            return Cast.box(0L - l1);
          } else {
            return Cast.box(l1);
          }
        }

        do {
          long l2 = ((Number)p.car).longValue();

          switch (type) {
          case COMPUTE_MAX:
            l1 = java.lang.Math.max(l1, l2);
            break;
          case COMPUTE_MIN:
            l1 = java.lang.Math.min(l1, l2);
            break;
          case COMPUTE_ADD:
            l1 = l1 + l2;
            break;
          case COMPUTE_SUBTRACT:
            l1 = l1 - l2;
            break;
          default:
            // Must never happen.
            throw new Bug("Illegal computation type " + type);
          }

          // Next.
          p = (Pair)p.cdr;
        } while (Pair.EMPTY_LIST != p);

        return Cast.box(l1);

      } // Fall through for multiplication and division.

    case CATEGORY_LONG:
    case CATEGORY_BIG_INT:
      if (COMPUTE_DIVIDE != type) {
        // Division needs to be exact using rationals.

        Object     o  = p.car;
        BigInteger i1 = ((o instanceof BigInteger)? (BigInteger)o
          : BigInteger.valueOf(((Number)o).longValue()));

        p = (Pair)p.cdr;

        if (Pair.EMPTY_LIST == p) {
          if (COMPUTE_SUBTRACT == type) {
            return pack(i1.negate());
          } else {
            return pack(i1);
          }
        }

        do {
          o             = p.car;
          BigInteger i2 = ((o instanceof BigInteger)? (BigInteger)o
            : BigInteger.valueOf(((Number)o).longValue()));

          switch (type) {
          case COMPUTE_MAX:
            i1 = i1.max(i2);
            break;
          case COMPUTE_MIN:
            i1 = i1.min(i2);
            break;
          case COMPUTE_ADD:
            i1 = i1.add(i2);
            break;
          case COMPUTE_MULTIPLY:
            i1 = i1.multiply(i2);
            break;
          case COMPUTE_SUBTRACT:
            i1 = i1.subtract(i2);
            break;
          default:
            // Must never happen.
            throw new Bug("Illegal computation type " + type);
          }

          // Next.
          p = (Pair)p.cdr;
        } while (Pair.EMPTY_LIST != p);

        return pack(i1);

      } // Fall through for division.

    case CATEGORY_RATIONAL:
      // First value.
      Object   o  = p.car;
      Rational r1 = Rational.valueOf((Number)o);

      p = (Pair)p.cdr;

      if (Pair.EMPTY_LIST == p) {
        if (COMPUTE_SUBTRACT == type) {
          return pack(r1.negate());
        } else if (COMPUTE_DIVIDE == type) {
          return pack(r1.invert());
        } else {
          return pack(r1);
        }
      }

      do {
        Rational r2 = Rational.valueOf((Number)p.car);

        switch (type) {
        case COMPUTE_MAX:
          r1 = r1.max(r2);
          break;
        case COMPUTE_MIN:
          r1 = r1.min(r2);
          break;
        case COMPUTE_ADD:
          r1 = r1.add(r2);
          break;
        case COMPUTE_MULTIPLY:
          r1 = r1.multiply(r2);
          break;
        case COMPUTE_SUBTRACT:
          r1 = r1.subtract(r2);
          break;
        case COMPUTE_DIVIDE:
          r1 = r1.divide(r2);
          break;
        default:
          // Must never happen.
          throw new Bug("Illegal computation type " + type);
        }

        // Next.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      return pack(r1);

    case CATEGORY_BIG_DECIMAL:
    case CATEGORY_DOUBLE:
      // First value.
      o         = p.car;
      double d1 = ((Number)o).doubleValue();

      p = (Pair)p.cdr;

      if (Pair.EMPTY_LIST == p) {
        if (COMPUTE_SUBTRACT == type) {
          return Cast.box(0.0D - d1);
        } else if (COMPUTE_DIVIDE == type) {
          return Cast.box(1.0D / d1);
        } else {
          return (Number)o;
        }
      }

      do {
        double d2 = ((Number)p.car).doubleValue();

        switch (type) {
        case COMPUTE_MAX:
          d1 = java.lang.Math.max(d1, d2);
          break;
        case COMPUTE_MIN:
          d1 = java.lang.Math.min(d1, d2);
          break;
        case COMPUTE_ADD:
          d1 = d1 + d2;
          break;
        case COMPUTE_MULTIPLY:
          d1 = d1 * d2;
          break;
        case COMPUTE_SUBTRACT:
          d1 = d1 - d2;
          break;
        case COMPUTE_DIVIDE:
          d1 = d1 / d2;
          break;
        default:
          // Must never happen.
          throw new Bug("Illegal computation type " + type);
        }

        // Next.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      return Cast.box(d1);

    case CATEGORY_NAN:
      return Cast.box(Double.NaN);

    default:
      // Must never happen.
      throw new Bug("Illegal category type " + category);
    }
  }

  /**
   * Return the absolute value of the specified number.
   *
   * @param   n  The number to take the absolute value of.
   * @return     The absolute value of the specified number
   */
  public static Number abs(Number n) {
    if ((n instanceof Integer) || (n instanceof Long) ||
        (n instanceof Short) || (n instanceof Byte)) {
      long l = n.longValue();

      if (0 <= l) {
        return n;
      } else if (Long.MIN_VALUE == l) {
        // Make sure we correctly handle that
        // abs(Long.MIN_VALUE) == Long.MAX_VALUE + 1.
        return BigInteger.valueOf(l).abs();
      } else {
        return Cast.box(-l);
      }

    } else if (n instanceof BigInteger) {
      return pack(((BigInteger)n).abs());

    } else if (n instanceof Rational) {
      return pack(((Rational)n).abs());

    } else if (n instanceof BigDecimal) {
      return ((BigDecimal)n).abs();

    } else {
      double d = n.doubleValue();

      if (0.0D < d) {
        return n;
      } else {
        return Cast.box(0.0D - d); // Turn -0.0 into +0.0.
      }

    }
  }

  /**
   * Return the quotient of the two specified integers.
   *
   * @param   n1  The first integer.
   * @param   n2  The second integer.
   * @return      The quotient of the two specified integers.
   * @throws  BadArgumentException
   *              Signals that <code>n1</code> or <code>n2</code>
   *              is not an integer, or that <code>n2</code>
   *              is zero.
   */
  public static Number quotient(Number n1, Number n2)
    throws BadArgumentException {

    n1 = toInteger(n1);
    n2 = toInteger(n2);

    if (isZero(n2)) {
      throw new BadArgumentException("Zero divisor", n2);

    } else if (isInexact(n1) || isInexact(n2)) {
      double d1 = n1.doubleValue();
      double d2 = n2.doubleValue();
      double d3 = d1 / d2;

      if (0.0D < d3) {
        return Cast.box(java.lang.Math.floor(d3));
      } else {
        return Cast.box(java.lang.Math.ceil(d3));
      }

    } else if ((n1 instanceof BigInteger) || (n2 instanceof BigInteger)) {
      BigInteger i1 = ((n1 instanceof BigInteger)? (BigInteger)n1
                       : BigInteger.valueOf(n1.longValue()));
      BigInteger i2 = ((n2 instanceof BigInteger)? (BigInteger)n2
                       : BigInteger.valueOf(n2.longValue()));

      return pack(i1.divide(i2));

    } else {
      long l1 = n1.longValue();
      long l2 = n2.longValue();

      return Cast.box(l1 / l2);
    }
  }

  /**
   * Return the remainder of dividing the two specified integers.
   *
   * @param   n1  The first integer.
   * @param   n2  The second integer.
   * @return      The remainder of dividing the two specified integers.
   * @throws  BadArgumentException
   *              Signals that <code>n1</code> or <code>n2</code>
   *              is not an integer, or that <code>n2</code> is
   *              zero.
   */
  public static Number remainder(Number n1, Number n2)
    throws BadArgumentException {

    n1 = toInteger(n1);
    n2 = toInteger(n2);

    if (isZero(n2)) {
      throw new BadArgumentException("Zero divisor", n2);

    } else if (isInexact(n1) || isInexact(n2)) {
      double d1 = n1.doubleValue();
      double d2 = n2.doubleValue();

      return Cast.box(d1 % d2);

    } else if ((n1 instanceof BigInteger) || (n2 instanceof BigInteger)) {
      BigInteger i1 = ((n1 instanceof BigInteger)? (BigInteger)n1
                       : BigInteger.valueOf(n1.longValue()));
      BigInteger i2 = ((n2 instanceof BigInteger)? (BigInteger)n2
                       : BigInteger.valueOf(n2.longValue()));

      return pack(i1.remainder(i2));

    } else {
      long l1 = n1.longValue();
      long l2 = n2.longValue();

      return Cast.box(l1 % l2);
    }
  }

  /**
   * Return the modulo of dividing the two specified integers.
   *
   * @param   n1  The first integer.
   * @param   n2  The second integer.
   * @return      The modulo of dividing the two specified integers.
   * @throws  BadArgumentException
   *              Signals that <code>n1</code> or <code>n2</code>
   *              is not an integer, or that <code>n2</code> is
   *              zero.
   */
  public static Number modulo(Number n1, Number n2)
    throws BadArgumentException {

    n1 = toInteger(n1);
    n2 = toInteger(n2);

    if (isZero(n2)) {
      throw new BadArgumentException("Zero divisor", n2);

    } else if (isInexact(n1) || isInexact(n2)) {
      double d1 = n1.doubleValue();
      double d2 = n2.doubleValue();
      double d3 = d1 % d2;
      
      if (((0.0D < d2) && (0.0D > d3)) ||
          ((0.0D > d2) && (0.0D < d3))) {
        return Cast.box(d3 + d2);
      } else {
        return Cast.box(d3);
      }

    } else if ((n1 instanceof BigInteger) || (n2 instanceof BigInteger)) {
      BigInteger i1 = ((n1 instanceof BigInteger)? (BigInteger)n1
                       : BigInteger.valueOf(n1.longValue()));
      BigInteger i2 = ((n2 instanceof BigInteger)? (BigInteger)n2
                       : BigInteger.valueOf(n2.longValue()));
      BigInteger i3 = i1.remainder(i2);

      if (((i2.signum() > 0) && (i3.signum() < 0)) ||
          ((i2.signum() < 0) && (i3.signum() > 0))) {
        return pack(i3.add(i2));
      } else {
        return pack(i3);
      }

    } else {
      long l1 = n1.longValue();
      long l2 = n2.longValue();
      long l3 = l1 % l2;

      if (((0 < l2) && (0 > l3)) ||
          ((0 > l2) && (0 < l3))) {
        return Cast.box(l3 + l2);
      } else {
        return Cast.box(l3);
      }
    }
  }

  /**
   * Return the greatest common divisor (gcd) for the specified list
   * of integers. Independent of whether the numbers are exact or
   * inexact, the gcd computation is performed using exact
   * numbers. Though, if any of the specified numbers is inexact, the
   * final result is inexact as well. The specified list must be
   * a proper list.
   *
   * @param    p     The list of integers for the gcd computation.
   * @throws   BadTypeException
   *                 Signals that an object on the specified
   *                 list is not a number.
   * @throws   BadArgumentException
   *                 Signals that a number on the specified list
   *                 is not an integer.
   * @throws   ClassCastException
   *                 Signals that the specified list is not a proper
   *                 list, but rather ends with a dotted pair.
   * @throws   IllegalArgumentException
   *                 Signals that the specified list is too
   *                 too long (at least of length
   *                 <code>Integer.MAX_VALUE</code>).
   */
  public static Number gcd(Pair p)
    throws BadArgumentException, BadTypeException {

    if (Pair.EMPTY_LIST == p) {
      return Cast.ZERO;
    }

    int     category = categorize(p);
    boolean exact    = true;

    switch (category) {
    case CATEGORY_INT:
      long l1 = ((Number)p.car).longValue();
      
      p = (Pair)p.cdr;

      if (Pair.EMPTY_LIST == p) {
        return Cast.box(java.lang.Math.abs(l1));
      }

      do {
        long l2 = ((Number)p.car).longValue();

        l1 = gcd(l1, l2);

        // Next.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      return Cast.box(l1);

    case CATEGORY_BIG_DECIMAL:
    case CATEGORY_DOUBLE:
      exact = false;
      // Fall through.

    case CATEGORY_LONG:
    case CATEGORY_BIG_INT:
      BigInteger i1 = toBigInteger(p.car);

      p = (Pair)p.cdr;

      if (Pair.EMPTY_LIST == p) {
        if (exact) {
          return pack(i1.abs());
        } else {
          return Cast.box(java.lang.Math.abs(i1.doubleValue()));
        }
      }

      do {
        BigInteger i2 = toBigInteger(p.car);

        i1 = i1.gcd(i2);

        // Next.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      if (exact) {
        return pack(i1);
      } else {
        return Cast.box(i1.doubleValue());
      }

    case CATEGORY_NAN:
      return Cast.box(Double.NaN);

    default:
      // Must never happen.
      throw new Bug("Illegal category " + category);
    }
  }

  /**
   * Return the least common multiple (lcm) for the specified list
   * of integers. Independent of whether the numbers are exact or
   * inexact, the lcm computation is performed using exact
   * numbers. Though, if any of the specified numbers is inexact, the
   * final result is inexact as well. The specified list must be
   * a proper list.
   *
   * @param    p     The list of integers for the lcm computation.
   * @throws   BadTypeException
   *                 Signals that an object on the specified
   *                 list is not a number.
   * @throws   BadArgumentException
   *                 Signals that a number on the specified list
   *                 is not an integer.
   * @throws   ClassCastException
   *                 Signals that the specified list is not a proper
   *                 list, but rather ends with a dotted pair.
   * @throws   IllegalArgumentException
   *                 Signals that the specified list is too
   *                 too long (at least of length
   *                 <code>Integer.MAX_VALUE</code>).
   */
  public static Number lcm(Pair p)
    throws BadArgumentException, BadTypeException {

    if (Pair.EMPTY_LIST == p) {
      return Cast.ONE;
    }

    int     category = categorize(p);
    boolean exact    = true;

    switch (category) {
    case CATEGORY_INT:
      long l1 = ((Number)p.car).longValue();
      
      p = (Pair)p.cdr;

      if (Pair.EMPTY_LIST == p) {
        return Cast.box(java.lang.Math.abs(l1));
      }

      do {
        long l2  = ((Number)p.car).longValue();
        long gcd = gcd(l1, l2);

        l1 = ((0 == gcd)? gcd : ((l2 / gcd) * l1));

        // Next.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      return Cast.box(java.lang.Math.abs(l1));

    case CATEGORY_BIG_DECIMAL:
    case CATEGORY_DOUBLE:
      exact = false;
      // Fall through.

    case CATEGORY_LONG:
    case CATEGORY_BIG_INT:
      BigInteger i1 = toBigInteger(p.car);

      p = (Pair)p.cdr;

      if (Pair.EMPTY_LIST == p) {
        if (exact) {
          return pack(i1.abs());
        } else {
          return Cast.box(java.lang.Math.abs(i1.doubleValue()));
        }
      }

      do {
        BigInteger i2  = toBigInteger(p.car);
        BigInteger gcd = i1.gcd(i2);

        i1 = ((gcd.signum() == 0)? gcd : i2.divide(gcd).multiply(i1));

        // Next.
        p = (Pair)p.cdr;
      } while (Pair.EMPTY_LIST != p);

      if (exact) {
        return pack(i1.abs());
      } else {
        return Cast.box(i1.abs().doubleValue());
      }

    case CATEGORY_NAN:
      return Cast.box(Double.NaN);

    default:
      // Must never happen.
      throw new Bug("Illegal category " + category);
    }
  }

  /**
   * Return the numerator of the specified number.
   *
   * @param   n  The number to take the numerator of.
   * @return     The numerator of the specified number.
   */
  public static Number numerator(Number n) {
    if ((n instanceof Integer) || (n instanceof BigInteger) ||
        (n instanceof Long) || (n instanceof Short) ||
        (n instanceof Byte)) {
      return n;

    } else if (n instanceof Rational) {
      return ((Rational)n).numerator();

    } else {
      Rational r = Rational.valueOf(n);
      if (n instanceof BigDecimal) {
        return new BigDecimal(r.denominator());
      } else {
        return Cast.box(r.numerator().doubleValue());
      }

    }
  }

  /**
   * Return the denominator of the specified number.
   *
   * @param   n  The number to take the denominator of.
   * @return     The denominator of the specified number.
   */
  public static Number denominator(Number n) {
    if ((n instanceof Integer) || (n instanceof BigInteger) ||
        (n instanceof Long) || (n instanceof Short) ||
        (n instanceof Byte)) {
      return Cast.ONE;

    } else if (n instanceof Rational) {
      return ((Rational)n).denominator();

    } else {
      Rational r = Rational.valueOf(n);
      if (n instanceof BigDecimal) {
        return new BigDecimal(r.denominator());
      } else {
        return Cast.box(r.denominator().doubleValue());
      }
    }
  }    

  /**
   * Return the floor of the specified number.
   *
   * @param   n  The number to take the floor of.
   * @return     The floor of the specified number.
   */
  public static Number floor(Number n) {
    if (isExact(n)) {
      if (n instanceof Rational) {
        return pack(((Rational)n).floor());
      } else {
        return n;
      }

    } else {
      if (n instanceof BigDecimal) {
        return ((BigDecimal)n).setScale(0, BigDecimal.ROUND_FLOOR);
      } else {
        return Cast.box(java.lang.Math.floor(n.doubleValue()));
      }
    }
  }

  /**
   * Return the ceiling of the specified number.
   *
   * @param   n  The number to take the ceiling of.
   * @return     The ceiling of the specified number.
   */
  public static Number ceil(Number n) {
    if (isExact(n)) {
      if (n instanceof Rational) {
        return pack(((Rational)n).ceil());
      } else {
        return n;
      }

    } else {
      if (n instanceof BigDecimal) {
        return ((BigDecimal)n).setScale(0, BigDecimal.ROUND_CEILING);
      } else {
        return Cast.box(java.lang.Math.ceil(n.doubleValue()));
      }
    }
  }

  /**
   * Truncate the specified number.
   *
   * @param   n  The number to truncate.
   * @return     The truncated number.
   */
  public static Number truncate(Number n) {
    if (isExact(n)) {
      if (n instanceof Rational) {
        Rational r = (Rational)n;
        if (r.signum() < 0) {
          return pack(r.ceil());
        } else {
          return pack(r.floor());
        }

      } else {
        return n;
      }
    } else {
      if (n instanceof BigDecimal) {
        BigDecimal d = (BigDecimal)n;

        if (d.compareTo(DECIMAL_ZERO) < 0) {
          return d.setScale(0, BigDecimal.ROUND_CEILING);
        } else {
          return d.setScale(0, BigDecimal.ROUND_FLOOR);
        }

      } else {
        double d = n.doubleValue();

        if (0.0D > d) {
          return Cast.box(java.lang.Math.ceil(d));
        } else {
          return Cast.box(java.lang.Math.floor(d));
        }
      }
    }
  }

  /**
   * Round the specified number.
   *
   * @param   n  The number to round.
   * @return     The rounded number.
   */
  public static Number rint(Number n) {
    if (isExact(n)) {
      if (n instanceof Rational) {
        return pack(((Rational)n).rint());
      } else {
        return n;
      }

    } else {
      if (n instanceof BigDecimal) {
        return ((BigDecimal)n).setScale(0, BigDecimal.ROUND_HALF_EVEN);
      } else {
        return Cast.box(java.lang.Math.rint(n.doubleValue()));
      }
    }
  }

  /**
   * Return the simplest rational number differing from the first
   * specified number by no more than the second specified number.
   *
   * @param   n1  The first number.
   * @param   n2  The second number.
   * @return      The simplest rational number differing from the
   *              first specified number by no more than the
   *              second specified number.
   */
  public static Number rationalize(Number n1, Number n2) {
    boolean inexact = false;

    if (isInexact(n1) || isInexact(n2)) {
      inexact = true;
    }
    
    Rational r1 = Rational.valueOf(n1);
    Rational r2 = Rational.valueOf(n2);
    Rational r3 = r1.simplify(r2);

    if (inexact) {
      return Cast.box(r3.doubleValue());
    } else {
      return pack(r3);
    }
  }

  /**
   * Return the first specified number raised to the power of the
   * second specified number. Note that <code>0<sup><font
   * size="-1">n2</font></sup></code> is <code>1</code> if <code>n2 ==
   * 0</code> and <code>0</code> otherwise.
   *
   * @param   n1  The first number.
   * @param   n2  The second number.
   * @return      <code>n1</code><sup><font
   *               size="-1"><code>n2</code></font></sup>.
   * @throws  BadArgumentException
   *              Signals that <code>n1 <= 0</code> and <code>n2</code> is
   *              not an integer.
   */
  public static Number expt(Number n1, Number n2)
    throws BadArgumentException {

    if (isInexact(n1) || isInexact(n2) || (! isInteger(n2))) {
      /*
       * If any of the numbers is inexact or the exponent is not an
       * integer, we produce an inexact result.
       */
      double d1 = n1.doubleValue();
      double d2 = n2.doubleValue();

      if (0.0D == d2) {
        return DOUBLE_ONE;
      } else if (0.0D == d1) {
        return DOUBLE_ZERO;
      } else {
        try {
          return Cast.box(java.lang.Math.pow(d1, d2));
        } catch (ArithmeticException x) {
          throw new BadArgumentException(
            "Unable to raise non-positive number to a non-integer power");
        }
      }

    } else {
      /**
       * Let's try to get an exact result.
       */
      int exp;
      try {
        exp = Cast.toInt(n2);
      } catch (BadTypeException x) {
        throw new Bug("Unexpected exception: " + x.toString());
      } catch (BadArgumentException x) {
        /*
         * If the exponent does not fit into an integer,
         * the result is way too large, so we produce an inexact
         * result.
         */
        double d1 = n1.doubleValue();
        double d2 = n2.doubleValue();

        if (0.0D == d2) {
          return Cast.ONE;
        } else if (0.0D == d1) {
          return Cast.ZERO;
        } else {
          try {
            return Cast.box(java.lang.Math.pow(d1, d2));
          } catch (ArithmeticException xx) {
            // Must never happen, since d2 is an integer.
            throw new Bug("Unexpected exception: " + xx.toString());
          }
        }
      }

      Rational r = Rational.valueOf(n1);

      return pack(r.pow(exp));
    }
  }

  /**
   * Calculate the greatest common divisor (gcd) for the two specified
   * long numbers.
   *
   * @param   a  The first number.
   * @param   b  The second number.
   * @return     The gcd of the two specified numbers.
   * @throws  IllegalArgumentException
   *             Signals that either <code>a</code> or <code>b</code>
   *             is <code>Long.MIN_VALUE</code>.
   */
  public static long gcd(long a, long b) {
    if ((Long.MIN_VALUE == a) || (Long.MIN_VALUE == b)) {
      throw new IllegalArgumentException("Long too small");
    }

    a = java.lang.Math.abs(a);
    b = java.lang.Math.abs(b);

    while (0 != b) {
      long olda = a;
      long oldb = b;

      a = oldb;
      b = (olda % oldb);
    }
    
    return a;
  }

  // ==================================================================
  //                               Helpers.
  // ==================================================================

  /**
   * Test whether the specified character is a digit within the
   * specified radix. The radix must be 2, 8, 10, or 16. Use
   * {@link java.lang.Character.digit(char,int)} to get the
   * corresponding numeric value.
   *
   * @param   c      The character to test.
   * @param   radix  The radix for the digit.
   * @return         <code>true</code> iff the specified character
   *                 is a digit within the specified radix.
   */
  private static boolean isDigit(char c, int radix) {
    if (10 >= radix) {
      return (('0' <= c) && ('0' + radix - 1 >= c));
    } else { // Must be radix 16
      return ((('0' <= c) && ('9' >= c)) ||
              (('a' <= c) && ('f' >= c)) ||
              (('A' <= c) && ('F' >= c)));
    }
  }

  /**
   * Create a new substring of the specified string which is a
   * normalized copy of the number format appearing in the original
   * string. This method simply copies all characters from the
   * specified region of the specified string, only that the hash mark
   * '<code>#</code>' is normalized to '<code>0</code>', and the
   * exponent markers '<code>s</code>', '<code>f</code>',
   * '<code>d</code>', and '<code>l</code>' are normalized to
   * '<code>e</code>'. <code>start</code> and <code>end</code> must be
   * valid indices for the specified string.
   *
   * @param   s      The string containing the number format.
   * @param   start  The start index in the specified string, inclusive.
   * @param   end    The end index in the specified string, exclusive.
   * @return         A copy of the specified region of the specified
   *                 string containing a normalized number format.
   * @throws  IndexOutOfBoundsException
   *                 Signals that either of the indices is not a valid
   *                 index into the specified string.
   */
  private static String normalize(String s, int start, int end) {
    StringBuffer buf = new StringBuffer(end - start);

    for (int i=start; i<end; i++) {
      char c = s.charAt(i);
      switch (c) {
      case '#':
        buf.append('0');
        break;
      case 's':
      case 'S':
      case 'f':
      case 'F':
      case 'd':
      case 'D':
      case 'l':
      case 'L':
        buf.append('e');
        break;
      default:
        buf.append(c);
      }
    }

    return buf.toString();
  }

}
