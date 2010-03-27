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

package one.math;

import java.math.BigInteger;
import java.math.BigDecimal;

/**
 * Implementation of rational numbers with practically unlimited size
 * and precision.
 *
 * @author   &copy; Copyright 1999 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public class Rational extends java.lang.Number implements Comparable {

  /** Zero as a rational. */
  public static final Rational ZERO = new Rational(BigInteger.ZERO,
                                                   BigInteger.ONE);

  /** One as a rational. */
  public static final Rational ONE = new Rational(BigInteger.ONE,
                                                  BigInteger.ONE);

  /** Two as a big integer. */
  protected static final BigInteger BIG_TWO = BigInteger.valueOf(2);

  /** Ten as a big integer. */
  protected static final BigInteger BIG_TEN = BigInteger.valueOf(10);

  /** One half as a rational. */
  public static final Rational ONE_HALF = new Rational(BigInteger.ONE,
                                                       BIG_TWO);

  /**
   * The numerator of this rational.
   *
   * @serial  Must not be <code>null</code>
   */
  protected BigInteger numerator;

  /**
   * The denominator of this rational.
   *
   * @serial  Must not be <code>null</code>
   */
  protected BigInteger denominator;

  /**
   * Create a new rational with the specified numerator and
   * denominator.
   *
   * @param   numerator    The numerator for the new rational.
   * @param   denominator  The denominator for the new rational.
   */
  private Rational(BigInteger numerator, BigInteger denominator) {
    this.numerator   = numerator;
    this.denominator = denominator;
  }

  /**
   * Return a rational with the specified numerator and denominator.
   *
   * @param   numerator    The numerator of the rational.
   * @param   denominator  The denominator of the rational.
   * @return               A rational with the specified numerator
   *                       and denominator.
   * @throws  NullPointerException
   *                       Signals that either <code>numerator</code>
   *                       or <code>denominator</code> is
   *                       <code>null</code>.
   * @throws  IllegalArgumentException
   *                       Signals that <code>denominator</code> is
   *                       zero.
   */
  public static Rational valueOf(BigInteger numerator,
                                 BigInteger denominator) {
    if ((null == numerator) || (null == denominator)) {
      throw new NullPointerException();
    } else if (denominator.signum() == 0) {
      throw new IllegalArgumentException("Zero denominator");
    } else if (numerator.signum() == 0) {
      return ZERO;
    } else {
      return (new Rational(numerator, denominator)).normalize();
    }
  }

  /**
   * Return a rational with the specified numerator and denominator.
   *
   * @param   numerator    The numerator of the rational.
   * @param   denominator  The denominator of the rational.
   * @return               A rational with the specified numerator
   *                       and denominator.
   * @throws  IllegalArgumentException
   *                       Signals that <code>denominator</code> is
   *                       zero.
   */
  public static Rational valueOf(long numerator, long denominator) {
    if (0 == denominator) {
      throw new IllegalArgumentException("Zero denominator");
    } else if (0 == numerator) {
      return ZERO;
    } else {
      return (new Rational(BigInteger.valueOf(numerator),
                           BigInteger.valueOf(denominator))).normalize();
    }
  }

  /**
   * Return a rational with the same value as the specified
   * big integer.
   *
   * @param   n  The big integer value to convert into a
   *             rational.
   * @return     A rational with the same value as the
   *             specified big integer.
   * @throws  NullPointerException
   *             Signals that <code>n</code> is <code>null</code>.
   */
  public static Rational valueOf(BigInteger n) {
    if (null == n) {
      throw new NullPointerException();
    } else {
      return new Rational(n, BigInteger.ONE);
    }
  }

  /**
   * Return a rational with the same value as the specified
   * long.
   *
   * @param   n  The long value to convert to a rational.
   * @return     A rational with the same value as the specified
   *             long.
   */
  public static Rational valueOf(long n) {
    return new Rational(BigInteger.valueOf(n), BigInteger.ONE);
  }

  /**
   * Return a rational with the same value as the specified
   * big decimal.
   *
   * @param   n  The big decimal value to convert to a rational.
   * @return     A rational with the same value as the specified
   *             big decimal.
   */
  public static Rational valueOf(BigDecimal n) {
    Rational r = new Rational(n.unscaledValue(), BIG_TEN.pow(n.scale()));
    return r.normalize();
  }

  /**
   * Return a rational with the same value as the specified
   * double. The returned rational preserves all available information
   * of the specified double, or, in other words, it does not lose any
   * precision over the specified double.
   *
   * @param   d  The double to convert into a rational.
   * @return     A rational with the same value as the
   *             specified double, or 0 if the specified
   *             double is not-a-number or an infinity.
   */
  public static Rational valueOf(double d) {
    if (Double.isNaN(d) || Double.isInfinite(d) || (0.0D == d)) {
      return ZERO;
    } else {
      long bits = Double.doubleToLongBits(d);

      int  s = (((bits >> 63) == 0) ? 1 : -1);
      int  e = (int)((bits >> 52) & 0x7ffL);
      long m = ((e == 0) ?
                ((bits & 0xfffffffffffffL) << 1)
                : ((bits & 0xfffffffffffffL) | 0x10000000000000L));
      
      e -= 1075;
      m  = m * s;

      BigInteger num = BigInteger.valueOf(m);
      BigInteger den = ((0 == e)?
                        BigInteger.ONE
                        : BIG_TWO.shiftLeft(Math.abs(e)-1));

      Rational r;
      if (0 <= e) {
        r = new Rational(num.multiply(den), BigInteger.ONE);
      } else {
        r = new Rational(num, den);
      }

      return r.normalize();
    }
  }

  /**
   * Return a rational with the same value as the specified number. If
   * the specified number is a Byte, Short, Integer, Long, BigInteger,
   * BigDecimal, Rational, Float, or Double, this method returns a
   * rational that preserves all available information of the
   * specified number. If the specified number is any other number,
   * this method returns the result of converting
   * <code>n.doubleValue()</code> to a rational (with not-a-number and
   * both positive and negative infinity converting to zero).
   *
   * @param   n  The number to convert into a rational.
   * @return     A rational with the same value as the
   *             specified number.
   */
  public static Rational valueOf(java.lang.Number n) {
    if ((n instanceof Integer) || (n instanceof Long) ||
        (n instanceof Short) || (n instanceof Byte)) {
      return valueOf(n.longValue());

    } else if (n instanceof BigInteger) {
      return valueOf((BigInteger)n);

    } else if (n instanceof BigDecimal) {
      return valueOf((BigDecimal)n);

    } else if (n instanceof Rational) {
      return (Rational)n;

    } else {
      return valueOf(n.doubleValue());
    }
  }
  
  /**
   * Return the numerator of this rational.
   *
   * @return  The numerator of this rational.
   */
  public BigInteger numerator() {
    return numerator;
  }

  /**
   * Return the denominator of this rational.
   *
   * @return  The denominator of this rational.
   */
  public BigInteger denominator() {
    return denominator;
  }

  /**
   * Determine whether this rational is zero.
   *
   * @return  <code>true</code> if this rational is zero.
   */
  public boolean isZero() {
    return (numerator.signum() == 0);
  }

  /**
   * Determine whether this rational represents an integer.
   *
   * @return  <code>true</code> if this rational is an integer.
   */
  public boolean isInteger() {
    return (denominator.compareTo(BigInteger.ONE) == 0);
  }

  /**
   * Return the floor of this rational.
   *
   * @return  The floor of this rational.
   */
  public BigInteger floor() {
    if (isInteger()) {
      return numerator;
    } else {
      if (numerator.signum() < 0) {
        return numerator.divide(denominator).subtract(BigInteger.ONE);
      } else {
        return numerator.divide(denominator);
      }
    }
  }

  /**
   * Return the ceiling of this rational
   *
   * @return  The ceiling of this rational.
   */
  public BigInteger ceil() {
    if (isInteger()) {
      return numerator;
    } else {
      if (numerator.signum() < 0) {
        return numerator.divide(denominator);
      } else {
        return numerator.divide(denominator).add(BigInteger.ONE);
      }
    }
  }

  /**
   * Return the closest integer to this rational, rounding
   * to even when this rational is halfway between two
   * integers.
   *
   * @return  The closest integer to this rational, rounding
   *          to even when this rational is halfway between
   *          two integers.
   */
  public BigInteger rint() {
    if (isInteger()) {
      return numerator;

    } else {
      BigInteger[] n    = numerator.divideAndRemainder(denominator);
      Rational     r    = (new Rational(n[1].abs(), denominator)).normalize();
      int          diff = r.compareTo(ONE_HALF);

      if (diff < 0) {
        return n[0];

      } else if (diff > 0) {
        if (numerator.signum() < 0) {
          return n[0].subtract(BigInteger.ONE);
        } else {
          return n[0].add(BigInteger.ONE);
        }

      } else {
        if (n[0].mod(BIG_TWO).equals(BigInteger.ZERO)) {
          return n[0];
        } else if (numerator.signum() < 0) {
          return n[0].subtract(BigInteger.ONE);
        } else {
          return n[0].add(BigInteger.ONE);
        }
      }
    }
  }

  /**
   * Add the specified rational to this rational.
   *
   * @param   val  The rational to add to this rational.
   * @return       A rational that is the sum of this rational
   *               and the specified rational.
   */
  public Rational add(Rational val) {
    if (this.denominator.equals(val.denominator)) {
      Rational r = new Rational(this.numerator.add(val.numerator),
                                this.denominator);
      return r.normalize();
    } else {
      BigInteger lcm  = lcm(this.denominator, val.denominator);
      BigInteger num1 = this.numerator.multiply(lcm.divide(this.denominator));
      BigInteger num2 = val.numerator.multiply(lcm.divide(val.denominator));

      Rational r = new Rational(num1.add(num2), lcm);

      return r.normalize();
    }
  }

  /**
   * Subtract the specified rational from this rational.
   *
   * @param   val  The rational to subtract from this rational.
   * @return       A rational that is the the result of subtracting
   *               the specified rational from this rational.
   */
  public Rational subtract(Rational val) {
    if (this.denominator.equals(val.denominator)) {
      Rational r = new Rational(this.numerator.subtract(val.numerator),
                                this.denominator);
      return r.normalize();
    } else {
      BigInteger lcm  = lcm(this.denominator, val.denominator);
      BigInteger num1 = this.numerator.multiply(lcm.divide(this.denominator));
      BigInteger num2 = val.numerator.multiply(lcm.divide(val.denominator));

      Rational r = new Rational(num1.subtract(num2), lcm);

      return r.normalize();
    }
  }    

  /**
   * Multiply the specified ration with this rational.
   *
   * @param   val  The rational to multiply with this rational.
   * @return       A rational that is the product of this rational
   *               and the specified rational.
   */
  public Rational multiply(Rational val) {
    Rational r = new Rational(this.numerator.multiply(val.numerator),
                              this.denominator.multiply(val.denominator));

    return r.normalize();
  }

  /**
   * Divide this rational by the specified rational.
   *
   * @param   val  The rational to divide this rational by.
   * @return       A rational that is the result of dividing
   *               this rational by the specified rational.
   * @throws  ArithmeticException
   *               Signals that <code>val</code> is zero.
   */
  public Rational divide(Rational val) {
    if (val.isZero()) {
      throw new ArithmeticException("Unable to divide by zero");
    } else {
      Rational r = new Rational(this.numerator.multiply(val.denominator),
                                this.denominator.multiply(val.numerator));

      return r.normalize();
    }
  }

  /**
   * Invert this rational.
   *
   * @return     A rational whose numerator is the denominator of this
   *             rational and whose denominator is the numerator of this
   *             rational.
   * @throws  ArithmeticException
   *             Signals that this rational is zero.
   */
  public Rational invert() {
    if (isZero()) {
      throw new ArithmeticException("Unable to divide by zero");
    } else {
      return new Rational(denominator, numerator);
    }
  }

  /**
   * Raise this rational to the specified power. Note that
   * <code>0<sup><font size="-1">exponent</font></sup></code> is
   * <code>1</code> if <code>exponent == 0</code> and <code>0</code>
   * otherwise.
   *
   * @param   exponent  The power to raise this rational to.
   * @return            A rational that is the result of raising
   *                    this rational to the specified power.
   */
  public Rational pow(int exponent) {
    if (0 == exponent) {
      // Result is always one.
      return ONE;

    } else if (isZero()) {
      // Per R5RS, section 6.2.5 definition of expt.
      return ZERO;

    } else if (0 < exponent) {
      Rational r = new Rational(numerator.pow(exponent),
                                denominator.pow(exponent));
      return r.normalize();

    } else if (Integer.MIN_VALUE == exponent) {
      // abs(Integer.MIN_VALUE) == (Integer.MAX_VALUE + 1)
      Rational r = new Rational(denominator.pow(Integer.MAX_VALUE),
                                numerator.pow(Integer.MAX_VALUE));
      r.numerator   = r.numerator.multiply(denominator);
      r.denominator = r.denominator.multiply(numerator);
      return r.normalize();

    } else {
      Rational r = new Rational(denominator.pow(Math.abs(exponent)),
                                numerator.pow(Math.abs(exponent)));
      return r.normalize();
    }
  }

  /**
   * Return the absolute value of this rational.
   *
   * @return  The absolute value of this rational if this rational
   *          is negative, or this rational if this rational is
   *          non-negative.
   */
  public Rational abs() {
    if (-1 == numerator.signum()) {
      return new Rational(numerator.abs(), denominator);
    } else {
      return this;
    }
  }
  
  /**
   * Negate this rational.
   *
   * @return  A rational that has the opposite sign of this rational.
   */
  public Rational negate() {
    return new Rational(numerator.negate(), denominator);
  }

  /**
   * Return the sign of this rational
   *
   * @return  The sign of this rational, that is, -1 if this
   *          rational is negative, 0 if this rational is zero,
   *          or +1 if this rational is positive.
   */
  public int signum() {
    return numerator.signum();
  }

  /**
   * Compare this rational with the specified object.
   *
   * @param   val  The rational to which this rational is to be
   *               compared.
   * @return       A negative number if this rational is numerically
   *               less than the specified rational, zero if this rational
   *               is equal to the specified rational, or a positive
   *               number if this rational is numerically greater than
   *               the specified rational.
   */
  public int compareTo(Rational val) {
    if (this == val) {
      return 0;

    } else if ((this.numerator == val.numerator) &&
               (this.denominator == val.denominator)) {
      return 0;

    } else if (this.denominator.equals(val.denominator)) {
      return this.numerator.compareTo(val.numerator);

    } else {
      BigInteger lcm  = lcm(this.denominator, val.denominator);
      BigInteger num1 = this.numerator.multiply(lcm.divide(this.denominator));
      BigInteger num2 = val.numerator.multiply(lcm.divide(val.denominator));

      return num1.compareTo(num2);
    }
  }

  /**
   * Compare this rational with the specified object.
   *
   * @param   o  The rational to which this rational is to be
   *             compared.
   * @return     A negative number if this rational is numerically
   *             less than the specified rational, zero if this rational
   *             is equal to the specified rational, or a positive
   *             number if this rational is numerically greater than
   *             the specified rational.
   * @throws  ClassCastException
   *             Signals that <code>o</code> is not a rational.
   */
  public int compareTo(Object o) {
    return this.compareTo((Rational)o);
  }
  
  /**
   * Determine whether this rational equals the specified object.
   *
   * @param   o  The object to compare to.
   * @return     <code>true</code> if the specified object is
   *             a rational with the same value as this rational.
   */
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    } else if (o instanceof Rational) {
      Rational val = (Rational)o;

      if ((this.numerator == val.numerator) &&
          (this.denominator == val.denominator)) {
        return true;
      } else {
        return ((this.numerator.compareTo(val.numerator) == 0) &&
                (this.denominator.compareTo(val.denominator) == 0));
      }
    } else {
      return false;
    }
  }

  /**
   * Determine the minimum of this rational and the specified
   * rational.
   *
   * @param   val  The rational with which the minimum is to be
   *               computed.
   * @return       The minimum of this rational and the specified
   *               rational. If the are equal, either may be returned.
   */
  public Rational min(Rational val) {
    if (this.denominator.equals(val.denominator)) {
      if (this.numerator.compareTo(val.numerator) < 0) {
        return this;
      } else {
        return val;
      }
    } else {
      BigInteger lcm  = lcm(this.denominator, val.denominator);
      BigInteger num1 = this.numerator.multiply(lcm.divide(this.denominator));
      BigInteger num2 = val.numerator.multiply(lcm.divide(val.denominator));

      if (num1.compareTo(num2) < 0) {
        return this;
      } else {
        return val;
      }
    }
  }

  /**
   * Determine the maximum of this rational and the specified
   * rational.
   *
   * @param   val  The rational with which the maximum is to be
   *               computed.
   * @return       The maximum of this rational and the specified
   *               rational. If they are equal, either may be
   *               returned.
   */
  public Rational max(Rational val) {
    if (this.denominator.equals(val.denominator)) {
      if (this.numerator.compareTo(val.numerator) < 0) {
        return val;
      } else {
        return this;
      }
    } else {
      BigInteger lcm  = lcm(this.denominator, val.denominator);
      BigInteger num1 = this.numerator.multiply(lcm.divide(this.denominator));
      BigInteger num2 = val.numerator.multiply(lcm.divide(val.denominator));

      if (num1.compareTo(num2) < 0) {
        return val;
      } else {
        return this;
      }
    }
  }

  /**
   * Return the simplest rational number differing from this rational
   * by no more than the specified rational. This method essentially
   * implements the <code>rationalize</code> Scheme function defined
   * in &sect;6.2.5 of R<sup><font size="-1">5</font></sup>RS, although
   * only for rational numbers. The code for this method is a
   * translation from <code>ratize.scm</code> in Aubrey Jaffer's SLIB,
   * who credits Alan Bawden with the algorithm.
   *
   * @see <a href="http://www-swiss.ai.mit.edu/~jaffer/SLIB.html">SLIB</a>
   *
   * @param   delta  The delta from this rational which specifies
   *                 the range <blockquote><pre>
   * this - abs(delta) <= result <= this + abs(delta)
   * </pre></blockquote> for the resulting simplest rational.
   * @return         The simplest rational number differing from this
   *                 rational by no more than the specified rational.
   */
  public Rational simplify(Rational delta) {
    if (delta.isZero()) {
      // Zero delta only leaves this rational.
      return this;
    } else {
      delta      = delta.abs();
      Rational x = this.subtract(delta);
      Rational y = this.add(delta);

      if (x.signum() > 0) {
        return simplest(x, y);
      } else if (y.signum() < 0) {
        return simplest(y.negate(), x.negate()).negate();
      } else {
        return ZERO;
      }
    }
  }

  /**
   * Return the simplest rational number within the specified range of
   * rational numbers, including both limits. Provides the essential
   * functionality for <code>simplify</code>. Both limits must be
   * positive numbers, and <code>r1 <= r2</code> must hold.
   *
   * @see     #simplify(Rational)
   *
   * @param   x  The positive lower limit of the range.
   * @param   y  The positive upper limit of the range.
   * @return     The simplest rational number within the
   *             specified range of rational numbers.
   */
  private static Rational simplest(Rational x, Rational y) {
    Rational fx = new Rational(x.floor(), BigInteger.ONE);
    Rational fy = new Rational(y.floor(), BigInteger.ONE);

    if (fx.compareTo(x) >= 0) {
      return fx;

    } else if (fx.compareTo(fy) == 0) {
      Rational newx = y.subtract(fy).invert();
      Rational newy = x.subtract(fx).invert();
      Rational r    = simplest(newx, newy);
      return fx.add(r.invert());

    } else {
      return fx.add(ONE);
    }
  }

  /**
   * Return a hash code for this rational.
   *
   * @return  A hash code for this rational.
   */
  public int hashCode() {
    return numerator.hashCode() + (31 * denominator.hashCode());
  }

  /**
   * Return a string representing this rational with the specified
   * radix. The resulting string is of the form
   * "[<code>-</code>]<i>numerator</i><code>/</code><i>denominator</i>".
   *
   * @see     java.math.BigInteger#toString(int)
   * 
   * @return  A string representing this rational.
   */
  public String toString(int radix) {
    return numerator.toString(radix) + "/" + denominator.toString(radix);
  }

  /**
   * Return a string representing this rational in the decimal
   * system. The resulting string is of the form
   * "[<code>-</code>]<i>numerator</i><code>/</code><i>denominator</i>".
   *
   * @return  A string representing this rational.
   */
  public String toString() {
    return toString(10);
  }

  /**
   * Convert this rational to a byte. Resolves this rational by big
   * integer division and casts the result to a byte, potentially
   * narrowing it.
   *
   * @return This rational converted to a byte.
   */
  public byte byteValue() {
    if (isInteger()) {
      return numerator.byteValue();
    } else {
      return numerator.divide(denominator).byteValue();
    }
  }

  /**
   * Convert this rational to a short. Resolves this rational by big
   * integer division and casts the result to a short, potentially
   * narrowing it.
   *
   * @return This rational converted to a short.
   */
  public short shortValue() {
    if (isInteger()) {
      return numerator.shortValue();
    } else {
      return numerator.divide(denominator).shortValue();
    }
  }

  /**
   * Convert this rational to an int. Resolves this rational by big
   * integer division and casts the result to an int, potentially
   * narrowing it.
   *
   * @return This rational converted to an int.
   */
  public int intValue() {
    if (isInteger()) {
      return numerator.intValue();
    } else {
      return numerator.divide(denominator).intValue();
    }
  }

  /**
   * Convert this rational to a long. Resolves this rational by big
   * integer division and casts the result to a long, potentially
   * narrowing it.
   *
   * @return  This rational converted to a long.
   */
  public long longValue() {
    if (isInteger()) {
      return numerator.longValue();
    } else {
      return numerator.divide(denominator).longValue();
    }
  }

  /**
   * Convert this rational to a float. Returns the result of converting
   * this rational to a double cast to a float.
   *
   * @return  This rational converted to a float.
   */
  public float floatValue() {
    return (float)doubleValue();
  }

  /**
   * Convert this rational to a double. Returns the result of dividing
   * the numerator of this rational converted to a double by the
   * denominator of this rational converted to a double.
   *
   * @return  This rational converted to a double.
   */
  public double doubleValue() {
    return (numerator.doubleValue()/denominator.doubleValue());
  }

  /**
   * Normalize this rational. A rational in normal form has a positive
   * denominator, and both the numerator and the denominator have the
   * smallest integer value for a rational of the same mathematical
   * value. All rationals returned by methods in this class are
   * guaranteed to be in normal form.
   *
   * @return  This rational.
   */
  protected Rational normalize() {
    BigInteger gcd = numerator.gcd(denominator);
    if (gcd.compareTo(BigInteger.ONE) != 0) {
      numerator      = numerator.divide(gcd);
      denominator    = denominator.divide(gcd);
    }

    if (-1 == denominator.signum()) {
      numerator   = numerator.negate();
      denominator = denominator.abs();
    }

    return this;
  }

  /**
   * Calculate the least common multiple (lcm) for the two specified
   * big integers.
   *
   * @param   n1  The first number.
   * @param   n2  The second number.
   * @return      The lcm of the two specified numbers.
   */
  protected static BigInteger lcm(BigInteger n1, BigInteger n2) {
    BigInteger gcd = n1.gcd(n2);
      
    if (gcd.compareTo(BigInteger.ZERO) == 0) {
      return BigInteger.ZERO;
    } else if (gcd.compareTo(BigInteger.ONE) == 0) {
      return n1.multiply(n2);
    } else {
      return n1.multiply(n2.divide(gcd));
    }
  }

}
