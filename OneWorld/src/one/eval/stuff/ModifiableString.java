/*
 * ========================================================================
 *  Copyright 1999 Robert Grimm.
 *  All rights reserved.
 *  See COPYRIGHT file for a full description.
 * ========================================================================
 */

package one.eval.stuff;

import java.util.Arrays;

import one.eval.BadArgumentException;
import one.eval.BadPairStructureException;
import one.eval.BadTypeException;
import one.eval.Pair;
import one.util.NegativeSizeException;

/**
 * Implementation of a modifiable string. In contrast to constant Java
 * strings, modifiable strings, as their name implies, are
 * modifiable. In other words, the length of a modifiable string is
 * fixed at creation time, but the individual characters can be
 * changed.
 *
 * <p>Note that operations on modifiable strings are not synchronized.
 *
 * @see      String
 *
 * @author   &copy; Copyright 1999 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public final class ModifiableString
  implements Comparable, java.io.Serializable {

  /** The empty modifiable string. */
  public static final ModifiableString EMPTY = new ModifiableString();

  /**
   * The characters of a modifiable string.
   *
   * @serial The array of characters representing the modifiable
   *         string. The length of this array is the length of the
   *         modifiable string and each character in the array is a
   *         character of the modifiable string. <code>chars</code>
   *         must not be null.
   */
  protected char[] chars;

  /**
   * Create a new empty modifiable string.
   */
  private ModifiableString() {
    chars = new char[0];
  }

  /**
   * Create a new modifiable string that contains the characters from
   * the specified array of characters, starting at index
   * <code>start</code> inclusive and ending at index <code>end</code>
   * exclusive.
   *
   * @param   chars  The array of characters that contains the region
   *                 of characters of the new modifiable string.
   * @param   start  The start index (inclusive).
   * @param   end    The end index (exclusive).
   * @throws  IndexOutOfBoundsException
   *                 Signals an invalid index into <code>chars</code>, or
   *                 that <code>end < start</code>.
   */
  private ModifiableString(char[] chars, int start, int end) {
    if ((start < 0) || (start > chars.length)) {
      throw new IndexOutOfBoundsException("Illegal start index " + start);
    } else if ((end < start) || (end > chars.length)) {
      throw new IndexOutOfBoundsException("Illegal end index " + end);
    }

    int l      = end - start;
    this.chars = new char[l];

    System.arraycopy(chars, start, this.chars, 0, l);
  }

  /**
   * Create a new modifibale string of the specified length.
   *
   * @param   length  The length of the new modifiable string.
   * @throws  NegativeSizeException
   *                  Signals that <code>length</code> is negative.
   */
  private ModifiableString(int length) {
    if (length < 0) throw new NegativeSizeException(Integer.toString(length));
    chars = new char[length];
  }

  /**
   * Create a new modifiable string of the specified length that is
   * filled with the specified character.
   *
   * @param   length  The length of the new modifiable string.
   * @param   c       The character to fill the new string with.
   * @throws  NegativeSizeException
   *                  Signals that <code>length</code> is negative.
   */
  private ModifiableString(int length, char c) {
    this(length);
    fill(c);
  }

  /**
   * Create a new modifiable string that is a copy of the specified
   * array of characters.
   *
   * @param   a  The array of characters.
   * @return     A new modifiable string that is a copy of the
   *             specified array of characters.
   */
  public static ModifiableString create(char[] a) {
    if ((null == a) || (0 == a.length)) {
      return EMPTY;
    } else {
      return new ModifiableString(a, 0, a.length);
    }
  }

  /**
   * Create a new modifiable string that is a copy fo the specified
   * region of the specified array of characters.
   *
   * @param   a      The array of characters that contains the
   *                 characters for the new modifiable string.
   * @param   start  The start index (inclusive).
   * @param   end    The end index (exclusive).
   * @return         A new modifiable string that is a copy of the
   *                 specified region of the specified array of
   *                 characters.
   * @throws  IndexOutOfBoundsException
   *                 Signals an invalid index into the specified
   *                 array of characters, or that
   *                 <code>end < start</code>.
   */
  public static ModifiableString create(char[] a, int start, int end) {
    if ((start == end) && (a.length == end)) {
      return EMPTY;
    } else {
      return new ModifiableString(a, start, end);
    }
  }

  /**
   * Create a new modifiable string that is a copy of the specified
   * Java string.
   *
   * @param   s  The Java string to copy.
   * @return     A new modifiable string that is a copy of the
   *             specified Java string.
   */
  public static ModifiableString create(String s) {
    int l = s.length();

    if (0 == l) {
      return EMPTY;
    } else {
      ModifiableString result = new ModifiableString(l);
      s.getChars(0, l, result.chars, 0);
      return result;
    }
  }

  /**
   * Create a new modifiable string that is a copy of the specified
   * region of the specified Java string.
   *
   * @param   s      The Java string that contains the characters
   *                 for the new modifiable string.
   * @param   start  The start index (inclusive).
   * @param   end    The end index (exclusive).
   * @return         A new modifiable string that is a copy of the
   *                 specified region of the specified Java string.
   * @throws  IndexOutOfBoundsException
   *                 Signals an invalid index into the specified
   *                 Java string, or that <code>end < start</code>.
   */
  public static ModifiableString create(String s, int start, int end) {
    int l = s.length();
    
    if ((start < 0) || (start > l)) {
      throw new IndexOutOfBoundsException("Illegal start index " + start);
    } else if ((end < start) || (end > l)) {
      throw new IndexOutOfBoundsException("Illegal end index " + end);
    } else if (start == end) {
      return EMPTY;
    }

    ModifiableString result = new ModifiableString(end - start);
    s.getChars(start, end, result.chars, 0);

    return result;
  }

  /**
   * Create a new modifiable string that is a copy of the specified
   * modifiable string. Same as <code>s.clone()</code>.
   *
   * @see     #clone()
   *
   * @param   s  The modifiable string to copy.
   * @return     A new modifiable string that is a copy of the
   *             specified modifiable string, or the empty string
   *             if the specified string is the empty string.
   */
  public static ModifiableString create(ModifiableString s) {
    return (ModifiableString)s.clone();
  }

  /**
   * Create a new modifiable string that is a copy of the specified
   * region of the specified modifiable string. Same as
   * <code>s.substring(start, end)</code>.
   *
   * @see     #substring(int,int)
   *
   * @param   s      The modifiable string that contains the characters
   *                 for the new modifiable string.
   * @param   start  The start index (inclusive).
   * @param   end    The end index (exclusive).
   * @return         A new modifiable string that is a copy of the
   *                 specified region of the specified modifiable
   *                 string, or the empty string if
   *                 <code>start == end</code>.
   * @throws  IndexOutOfBoundsException
   *                Signals an invalid index into the specified string,
   *                or that <code>end < start</code>.
   */
  public static ModifiableString create(ModifiableString s,
                                        int start, int end) {
    return s.substring(start, end);
  }

  /**
   * Create a new modifiable string of the specified length that is
   * filled with the specified character.
   *
   * @param   length  The length of the new modifiable string.
   * @param   c       The character to fill the new string with.
   * @return          A new modifiable string of the specified
   *                  length and filled with the specified
   *                  character.
   * @throws  NegativeSizeException
   *                  Signals that <code>length</code> is negative.
   */
  public static ModifiableString create(int length, char c) {
    if (0 == length) {
      return EMPTY;
    } else {
      return new ModifiableString(length, c);
    }
  }

  /**
   * Create a new modifiable string that is the result of appending
   * all characters in the specified list together.
   *
   * @param   p  The pair starting the list of characters.
   * @return     A new modifiable string that is the result of
   *             appending all characters in the specified list.
   * @throws  BadPairStructureException
   *             Signals that the list starting at the specified
   *             pair is not a proper list.
   * @throws  BadTypeException
   *             Signals that an element in the specified list
   *             is not a character.
   */
  public static ModifiableString createFromCharacterList(Pair p)
    throws BadPairStructureException, BadTypeException {
    if (Pair.EMPTY_LIST == p) return EMPTY;

    int              l = p.length(); // Signals if not a proper list.
    ModifiableString s = new ModifiableString(l);
    int              i = 0;
    do {
      Object o = p.car();
      if (! (o instanceof Character)) {
        throw new BadTypeException("Not a character", o);
      }
      s.chars[i] = ((Character)o).charValue();

      i++;
      p = (Pair)p.cdr();
    } while (Pair.EMPTY_LIST != p);

    return s;
  }

  /**
   * Create a new modifiable string that is the result of appending
   * all strings in the specified list together. Both modifiable
   * and Java strings are considered as strings by this method.
   *
   * @param   p  The pair starting the list of strings.
   * @return     A new modifiable string that is the result of
   *             appending all strings in the specified list.
   * @throws  BadPairStructureException
   *             Signals that the list starting at the specified
   *             pair is not a proper list.
   * @throws  BadTypeException
   *             Signals that an element in the specified list
   *             is not a string.
   */
  public static ModifiableString createFromStringList(Pair p)
    throws BadPairStructureException, BadTypeException {
    if (Pair.EMPTY_LIST == p) {
      return EMPTY;
    } else if (! p.isList()) {
      throw new BadPairStructureException("Not a list", p);
    }

    Pair head   = p;
    int  length = 0;

    // Determine length of result string.
    do {
      Object o = p.car();
      if (o instanceof String) {
        length += ((String)o).length();
      } else if (o instanceof ModifiableString) {
        length += ((ModifiableString)o).chars.length;
      } else {
        throw new BadTypeException("Not a string", o);
      }

      p = (Pair)p.cdr();
    } while (Pair.EMPTY_LIST != p);
    
    if (0 == length) return EMPTY;

    // Create string.
    ModifiableString result = new ModifiableString(length);

    // Copy strings in list into result.
    length = 0;
    p      = head;

    do {
      Object o = p.car();
      if (o instanceof String) {
        String s = (String)o;
        int    l = s.length();
        s.getChars(0, l, result.chars, length);
        length += l;
      } else {
        ModifiableString s = (ModifiableString)o;
        int              l = s.chars.length;
        System.arraycopy(s.chars, 0, result.chars, length, l);
        length += l;
      }

      p = (Pair)p.cdr();
    } while (Pair.EMPTY_LIST != p);

    // Done.
    return result;
  }

  /**
   * Clone this string.
   *
   * @return  A newly allocated string of the same length and
   *          with the same characters as this string, or
   *          this string if this string is the empty string.
   */
  public Object clone() {
    if (0 == chars.length) {
      return this;
    } else {
      return new ModifiableString(chars, 0, chars.length);
    }
  }

  /**
   * Return the length of this string.
   *
   * @return  The length of this string.
   */
  public int length() {
    return chars.length;
  }

  /**
   * Return the character at the specified index.
   *
   * @param   index                      The index of the character.
   * @return                             The character at the specified
   *                                     index.
   * @throws  IndexOutOfBoundsException  Signals an invalid index.
   */
  public char charAt(int index) {
    return chars[index];
  }

  /**
   * Replace the character at the specified index with the specified
   * character.
   *
   * @param   index                      The index of the character.
   * @param   c                          The new character at the
   *                                     specified index.
   * @return                             The old character at the
   *                                     specified index.
   * @throws  IndexOutOfBoundsException  Signals an invalid index.
   */
  public char setCharAt(int index, char c) {
    char oldChar = chars[index];
    chars[index] = c;
    return oldChar;
  }

  /**
   * Fill this string with the specified character.
   *
   * @param   c  The character to fill this string with.
   * @return     This string.
   */
  public ModifiableString fill(char c) {
    Arrays.fill(chars, c);
    return this;
  }

  /**
   * Create a newly allocated substring, starting at the specified
   * <code>start</code> index (inclusive) and ending at the specified
   * <code>end</code> index (exclusive).
   *
   * @param  start  The start index (inclusive).
   * @param  end    The end index (exclusive).
   * @return        The substring of this string starting at
   *                <code>start</code> (inclusive) and ending at
   *                <code>end</code> (exclusive), or the empty string
   *                if <code>start == end</code>.
   * @throws  IndexOutOfBoundsException
   *                Signals an invalid index into this string, or that
   *                <code>end < start</code>.
   */
  public ModifiableString substring(int start, int end) {
    if ((start == end) && (chars.length == end)) {
      return EMPTY;
    } else {
      return new ModifiableString(chars, start, end);
    }
  }

  /**
   * Compare this string with the specified object. If the specified
   * object is another modifiable string, this method behaves as
   * described for Java strings, otherwise it throws a
   * <code>ClassCastException</code>.
   *
   * @see     String#compareTo(String)
   * 
   * @param   o                   The object to compare to.
   * @return                      A negative integer if this string
   *                              is lexographically less than the
   *                              specified string, zero if this
   *                              string equals the specified string,
   *                              and a positive integer if this
   *                              string is lexographically greater
   *                              than the specified string.
   * @throws  ClassCastException  Signals that the specified object
   *                              is not a modifiable string.
   */
  public int compareTo(Object o) {
    return compareTo((ModifiableString)o);
  }

  /**
   * Compare this string with the specified string. This method
   * compares this modifiable string with the specified modifiable
   * string as described for Java strings.
   *
   * @see     String#compareTo(String)
   *
   * @param   s                   The string to compare to.
   * @return                      A negative integer if this string
   *                              is lexographically less than the
   *                              specified string, zero if this
   *                              string equals the specified string,
   *                              and a positive integer if this
   *                              string is lexographically greater
   *                              than the specified string.
   */
  public int compareTo(ModifiableString s) {
    char[] chars2 = s.chars;
    int    l1     = chars.length;
    int    l2     = chars2.length;
    for (int i=0; i<l1 && i<l2; i++) {
      char c1 = chars[i];
      char c2 = chars2[i];
      if (c1 != c2) {
	return c1 - c2;
      }
    }

    return l1 - l2;
  }

  /**
   * Compare this string with the specified modifiable string,
   * ignoring case considerations. This method behaves as described
   * for Java strings.
   *
   * @see        String#compareToIgnoreCase(String)
   *
   * @param   s  The modifiable string to compare to.
   * @return     A negative integer if this string is lexographically
   *             less than the specified string, zero if this string
   *             equals the specified string, and a positive integer if
   *             this string is lexographically greater than the
   *             specified string, all ignoring case considerations.
   */
  public int compareToIgnoreCase(ModifiableString s) {
    char[] chars2 = s.chars;
    int    l1     = chars.length;
    int    l2     = chars2.length;
    for (int i=0; i<l1 && i<l2; i++) {
      char c1 = chars[i];
      char c2 = chars2[i];
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
    }

    return l1 - l2;
  }

  /**
   * Compare this string with the specified object for
   * equality. Returns <code>true</code> iff the specified object
   * denotes a modifiable string of the same length and with the same
   * characters in the same positions.
   *
   * @param   o  The object to compare to.
   * @return     <code>true</code> iff the specified object is another
   *             modifiable string of the same length with the same
   *             characters in the same positions.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ModifiableString)) return false;
    return (0 == compareTo(o));
  }

  /**
   * Return a hash code for this string. The hash code for a
   * modifiable string is computed as described for Java strings.
   *
   * @see     String#hashCode()
   *
   * @return  A hash code value for this string.
   */
  public int hashCode() {
    int h = 0;

    for (int i=0; i<chars.length; i++) {
      h = 31*h + chars[i];
    }

    return h;
  }

  /**
   * Convert this string to a new character array.
   *
   * @return  A new character array that contains the characters
   *          of this string in order.
   */
  public char[] toCharArray() {
    return (char[])chars.clone();
  }

  /**
   * Convert this string to a new pair-based list of characters.
   *
   * @return  A new pair-based list of characters that contains the
   *          characters of this string in order.
   */
  public Pair toCharacterList() {
    Pair result = Pair.EMPTY_LIST;

    for (int i=chars.length-1; i>=0; i--) {
      result = new Pair(new Character(chars[i]), result);
    }

    return result;
  }

  /**
   * Return a Java string that is a copy of this string.
   *
   * @return  A Java string that is a copy of this string.
   */
  public String toString() {
    return (new String(chars)).intern();
  }

  /**
   * Cast the specified object to a modifiable string.
   *
   * @param   o  The object to cast.
   * @return     The specified object as a modifiable string.
   * @throws  BadArgumentException
   *             Signals that <code>o</code> is a Java string.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is not a modifiable string.
   */
  public static ModifiableString toModifiableString(Object o)
    throws BadArgumentException, BadTypeException {
    if (o instanceof ModifiableString) {
      return (ModifiableString)o;
    } else if (o instanceof String) {
      throw new BadArgumentException("Constant string", o);
    } else {
      throw new BadTypeException("Not a string", o);
    }
  }

  /**
   * Convert the specified object to a Java string.
   *
   * @param   o  The object to convert.
   * @return     The specified object as a Java string.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is neither a Java
   *             string nor a modifiable string.
   */
  public static String toString(Object o) throws BadTypeException {
    if (o instanceof String) {
      return (String)o;
    } else if (o instanceof ModifiableString) {
      return (new String(((ModifiableString)o).chars)).intern();
    } else {
      throw new BadTypeException("Not a string", o);
    }
  }

  /**
   * Ensure that the specified object is a string and determine
   * whether it is a Java string. Both modifiable strings and
   * Java strings are considered strings by this method.
   *
   * @param   o  The object to check.
   * @return     <code>true</code> iff the specified object is
   *             a Java string.
   * @throws  BadTypeException
   *             Signals that <code>o</code> is neither a modifiable
   *             nor a Java string.
   */
  public static boolean treatAsJavaString(Object o) throws BadTypeException {
    if (o instanceof String) {
      return true;
    } else if (o instanceof ModifiableString) {
      return false;
    } else {
      throw new BadTypeException("Not a string", o);
    }
  }

  /**
   * Compare the specified string objects. Compares the two strings as
   * described for {@link String#compareTo(String)}. Both Java strings
   * and modifiable strings are considered strings by this method.
   * 
   * @param   o1  The first string object.
   * @param   o2  The second string object.
   * @return      A negative integer if the first string object is
   *              lexographically less than the second string object,
   *              zero if the first string object equals the second
   *              string object, and a positive integer if the first
   *              string object is lexographically greater than the
   *              second string object.
   * @throws  BadTypeException
   *              Signals that one of the specified object is neither
   *              a Java nor a modifiable string.
   */
  public static int compareStrings(Object o1, Object o2)
    throws BadTypeException {
    if (o1 instanceof String) {
      if (o2 instanceof String) {
        return ((String)o1).compareTo((String)o2);
      } else if (o2 instanceof ModifiableString) {
        String           s1 = (String)o1;
        ModifiableString s2 = (ModifiableString)o2;
        int              l1 = s1.length();
        int              l2 = s2.chars.length;
        for (int i=0; i<l1 && i<l2; i++) {
          char c1 = s1.charAt(i);
          char c2 = s2.chars[i];
          if (c1 != c2) {
            return c1 - c2;
          }
        }
        return l1 - l2;
      } else {
        throw new BadTypeException("Not a string", o2);
      }
    } else if (o1 instanceof ModifiableString) {
      if (o2 instanceof String) {
        ModifiableString s1 = (ModifiableString)o1;
        String           s2 = (String)o2;
        int              l1 = s1.chars.length;
        int              l2 = s2.length();
        for (int i=0; i<l1 && i<l2; i++) {
          char c1 = s1.chars[i];
          char c2 = s2.charAt(i);
          if (c1 != c2) {
            return c1 - c2;
          }
        }
        return l1 - l2;
      } else if (o2 instanceof ModifiableString) {
        return ((ModifiableString)o1).compareTo(o2);
      } else {
        throw new BadTypeException("Not a string", o2);
      }
    } else {
      throw new BadTypeException("Not a string", o1);
    }
  }

  /**
   * Compare the specified string objects, ignoring case
   * considerations. Compares the two strings as described for {@link
   * String#compareToIgnoreCase(String)}. Both Java strings and
   * modifiable strings are considered strings by this method.
   * 
   * @param   o1  The first string object.
   * @param   o2  The second string object.
   * @return      A negative integer if the first string object is
   *              lexographically less than the second string object,
   *              zero if the first string object equals the second
   *              string object, and a positive integer if the first
   *              string object is lexographically greater than the
   *              second string object, all ignoring case
   *              considerations.
   * @throws  BadTypeException
   *              Signals that one of the specified object is neither
   *              a Java nor a modifiable string.
   */
  public static int compareStringsIgnoreCase(Object o1, Object o2)
    throws BadTypeException {
    if (o1 instanceof String) {
      if (o2 instanceof String) {
        return ((String)o1).compareToIgnoreCase((String)o2);
      } else if (o2 instanceof ModifiableString) {
        String           s1 = (String)o1;
        ModifiableString s2 = (ModifiableString)o2;
        int              l1 = s1.length();
        int              l2 = s2.chars.length;
        for (int i=0; i<l1 && i<l2; i++) {
          char c1 = s1.charAt(i);
          char c2 = s2.chars[i];
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
        }
        return l1 - l2;
      } else {
        throw new BadTypeException("Not a string", o2);
      }
    } else if (o1 instanceof ModifiableString) {
      if (o2 instanceof String) {
        ModifiableString s1 = (ModifiableString)o1;
        String           s2 = (String)o2;
        int              l1 = s1.chars.length;
        int              l2 = s2.length();
        for (int i=0; i<l1 && i<l2; i++) {
          char c1 = s1.chars[i];
          char c2 = s2.charAt(i);
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
        }
        return l1 - l2;
      } else if (o2 instanceof ModifiableString) {
        return ((ModifiableString)o1).compareToIgnoreCase((ModifiableString)o2);
      } else {
        throw new BadTypeException("Not a string", o2);
      }
    } else {
      throw new BadTypeException("Not a string", o1);
    }
  }

}
