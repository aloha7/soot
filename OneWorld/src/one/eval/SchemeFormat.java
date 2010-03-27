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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import java.util.List;
import java.util.ArrayList;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

import java.net.URL;

/**
 * Implementation of formatting for Scheme objects.
 *
 * <p>The language format implemented by this class generally follows
 * the lexical structure for Scheme defined in &sect;7.1.1 of
 * R<sup><font size="-1">5</font></sup>RS. In particular, it strictly
 * enforces the rules for valid identifiers, which require that an
 * identifier not start with a number. Furthermore, number formats may
 * not contain white space characters. Note that the default case for
 * identifiers is the lower case.</p>
 *
 *
 * <p><b>Characters and Strings</b></p>
 *
 * <p>To better support Unicode characters as well as the conventions
 * for writing Java strings, the language format implemented by this
 * class supports additional character names and escape sequences over
 * those defined in R<sup><font size="-1">5</font></sup>RS. The
 * following table provides an overview of the mapping between
 * characters, character names and escape sequences. It is followed by
 * a detailed description of how character and string literals are
 * read by {@link #read(Reader)} and written by {@link
 * #write(Object,Writer,boolean)}.</p>
 *
 * <p><table border="1" cellpadding="3" cellspacing="0" width="100%">
 * <tr bgcolor="#CCCCFF" id="TableHeadingColor">
 * <td align=center><font size="+1">
 * <b>Character</b></font></td>
 * <td align=center><font size="+1">
 * <b>Character Name</b></font></td>
 * <td align=center><font size="+1">
 * <b>Escape Sequence</b></font></td>
 * </tr>
 * <tr bgcolor="white" id="TableRowColor">
 * <td>Space &nbsp;</td> <td><code>space</code></td>
 * <td><i>none</i></td>
 * </tr>
 * <tr bgcolor="white" id="TableRowColor">
 * <td>Backspace <code>BS</code></td> <td><code>backspace</code></td>
 * <td><code>&#092;b</code></td>
 * </tr>
 * <tr bgcolor="white" id="TableRowColor">
 * <td>Horizontal tab <code>HT</code></td> <td><code>tab</code></td>
 * <td><code>&#092;t</code></td>
 * </tr>
 * <tr bgcolor="white" id="TableRowColor">
 * <td>Line feed <code>LF</code></td>
 * <td><code>newline</code> or <code>linefeed</code></td>
 * <td><code>&#092;n</code></td>
 * </tr>
 * <tr bgcolor="white" id="TableRowColor">
 * <td>Form feed <code>FF</code></td> <td><code>formfeed</code</td>
 * <td><code>&#092;f</code></td>
 * </tr>
 * <tr bgcolor="white" id="TableRowColor">
 * <td>Carriage return <code>CR</code></td> <td><code>carriagereturn</code></td>
 * <td><code>&#092;r</code></td>
 * </tr>
 * <tr bgcolor="white" id="TableRowColor">
 * <td>Double quote <code>"</code></td> <td><i>none</i></td>
 * <td><code>&#092;"</code></td>
 * </tr>
 * <tr bgcolor="white" id="TableRowColor">
 * <td>Single quote <code>'</code></td> <td><i>none</i></td>
 * <td><code>&#092;'</code></td>
 * </tr>
 * <tr bgcolor="white" id="TableRowColor">
 * <td>Backslash <code>&#092;</code></td> <td><i>none</i></td>
 * <td><code>&#092;&#092;</code></td>
 * </tr>
 * <tr bgcolor="white" id="TableRowColor">
 * <td>Unicode character <i>xxxx</i> (where <i>x</i> is a hexadecimal
 * digit)</td>
 * <td><code>unicode-</code><i>xxxx</i></td>
 * <td><code>&#092;u</code><i>xxxx</i></td>
 * </tr>
 * </table></p>
 *
 * <p>When reading a character literal, the language format
 * implemented by this class recognizes any Unicode character as a
 * character. That is, any Unicode character can follow
 * "<code>#&#092;</code>" and is correctly recognized as the character
 * for this character literal. Furthermore, all character names
 * specified in above table are recognized.</p>
 *
 * <p>When reading a string literal, the language format implemented
 * by this class recognizes any Unicode character as a character of
 * that string. As a result, strings may span several lines and the
 * line-termination characters are recognized as characters of that
 * string. The language format implemented by this class also
 * recognizes all escape sequences specified in above table. Both the
 * double quote and the backslash characters must be escaped to appear
 * in a string.</p>
 *
 * <p>When writing a quoted character, the language format implemented
 * by this class uses the character itself for all Unicode characters
 * that are not in a <code>Cx</code> category, besides the
 * '<code>&nbsp;</code>' (space) character. It uses the character name
 * for the space, backspace, horizontal tab, line feed (using the
 * <code>newline</code> character name), form feed, and carriage
 * return characters. And, it uses the unicode character name for all
 * other characters.</p>
 *
 * <p>When writing a quoted string, the language format implemented by
 * this class uses the character itself for all Unicode characters
 * that are not in a <code>Cx</code> category, unless the character is
 * a double quote, single quote, or backslash, in which case it writes
 * the escape sequence for that character as specified in above
 * table. For all other characters, it writes the Unicode escape
 * sequence.</p>
 *
 *
 * <p><b>External Representation of Arbitrary Objects</b></p>
 *
 * <p>The Scheme standard defines the external representation of
 * numbers, booleans, pairs as well as lists, symbols, characters,
 * strings, and vectors. The language format implemented by this class
 * reads and writes the external representation for these types as
 * specified (with extensions for characters and strings, as described
 * above). Additionally, the language format implemented by this class
 * provides an external representation for all other objects,
 * including Scheme objects, such as input or output ports, as well as
 * arbitrary Java objects. However, it only supports writing the
 * external representation, but not reading it. This external
 * representation is of the form
 * <code>#[</code><i>keyword</i>&nbsp;<i>string</i><code>]</code>,
 * where <i>keyword</i> describes the type of the object and
 * <i>string</i> is a string representation of the object, typically
 * obtained by calling its <code>toString()</code> method.</p>
 *
 *
 * <p><b>Subclasses</b></p>
 *
 * <p>Subclasses of this class which customize or extend the Scheme
 * language format implemented by this class must implement the {@link
 * #read2(Reader,int)} and/or {@link
 * #write1(Object,Writer,boolean,int)} methods.</p>
 *
 * @see      #read(Reader)
 * @see      #write(Object,Writer,boolean)
 * @see      #isPrintable(int)
 *
 * @author   &copy; Copyright 1998-2000 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public class SchemeFormat extends Format {

  // ============================= Constants ================================

  /** The <code>+</code> symbol. */
  protected static final Symbol      PLUS            = Symbol.intern("+");

  /** The <code>-</code> symbol. */
  protected static final Symbol      MINUS           = Symbol.intern("-");

  /** The <code>...</code> symbol. */
  protected static final Symbol      DOTDOTDOT       = Symbol.intern("...");

  /** The <code>quote</code> symbol. */
  protected static final Symbol      QUOTE           = Symbol.intern("quote");

  /** The <code>quasiquote</code> symbol. */
  protected static final Symbol      QUASIQUOTE      =
    Symbol.intern("quasiquote");

  /** The <code>unquote</code> symbol. */
  protected static final Symbol      UNQUOTE         = Symbol.intern("unquote");

  /** The <code>unquote-splicing</code> symbol. */
  protected static final Symbol      SPLICING        =
    Symbol.intern("unquote-splicing");

  /**
   * The dot object, which is used internally by this class to
   * indicate that a single dot has been read.
   */
  protected static final Object      DOT             = new Object();

  /** The space character. */
  protected static final Character   SPACE           = new Character(' ');

  /** The back-space character. */
  protected static final Character   BACKSPACE       = new Character('\b');

  /** The tab character. */
  protected static final Character   TAB             = new Character('\t');

  /** The newline character. */
  protected static final Character   NEWLINE         = new Character('\n');

  /** The carriage-return character. */
  protected static final Character   CARRIAGE_RETURN = new Character('\r');

  // ============================ Constructor ===============================

  /** Create a new Scheme format. */
  public SchemeFormat() {
    // Nothing to do.
  }

  // ================================ I/O ===================================

  /**
   * Read the external representation of a Scheme object from the
   * specified reader and return that object. Requires the use of
   * <code>mark(int)</code> and <code>reset()</code> to correctly
   * parse the character stream from the specified reader.
   *
   * <p>The implementation of this method verifies that the specified
   * reader supports <code>mark(int)</code> and <code>reset()</code>
   * and then returns the result of calling <code>read1(Reader)</code>
   * on the specified reader. Any I/O exceptions thrown by
   * <code>read1(Reader)</code> are caught and rethrown as a wrapped
   * exception.</p>
   *
   * @see     java.io.Reader#mark(int)
   * @see     java.io.Reader#reset()
   * @see     #read1(Reader)
   *
   * @param   in  The reader to read the external representation of a
   *              Scheme object from.
   * @return      The corresponding Scheme object, or {@link #EOF} if
   *              the end of file position is reached before
   *              encountering a Scheme object.
   * @throws  BadArgumentException
   *              Signals that the specified reader does not
   *              support <code>mark(int)</code> and
   *              <code>reset()</code>.
   * @throws  BadFormatException
   *              Signals that the external representation of a
   *              Scheme object does not adhere to the expected
   *              format.
   * @throws  WrappedException
   *              Signals an exceptional condition when accessing the
   *              specified reader.
   */
  public final Object read(Reader in)
    throws BadArgumentException, BadFormatException, WrappedException {

    if (! in.markSupported()) {
      throw new BadArgumentException("Mark/reset not supported by",
                                     in);
    }

    try {
      return read1(in);
    } catch (IOException x) {
      throw new WrappedException("Unable to read from", x, in);
    }
  }

  /**
   * Read the external representation of a Scheme object from the
   * specified reader and return that object. The specified reader
   * must support <code>mark(int)</code> and <code>reset()</code>.
   *
   * <p>The implementation of this method consumes any available
   * atmosphere, that is, white space and comments. It then reads the
   * first non-whitespace, non-comment character from the specified
   * reader and returns the result of calling
   * <code>read2(Reader,int)</code> with the specified reader and the
   * read-in character. If the object returned by <code>read2</code>
   * is the <code>DOT</code> object, this method throws a bad format
   * exception signalling a single dot token that does not designate
   * an improper list.</p>
   *
   * @see     java.io.Reader#mark(int)
   * @see     java.io.Reader#reset()
   * @see     #read2(Reader,int)
   * @see     #DOT
   *
   * @param   in  The reader to read the external representation of a
   *              Scheme object from.
   * @return      The corresponding Scheme object, or {@link #EOF} if
   *              the end of file position is reached before
   *              encountering a Scheme object.
   * @throws  BadFormatException
   *              Signals that the external representation of a
   *              Scheme object does not adhere to the expected
   *              format.
   * @throws  IOException
   *              Signals an exceptional condition when accessing the
   *              specified reader.
   */
  protected final Object read1(Reader in)
    throws BadFormatException, IOException {

    int    i = consumeAtmosphere(in);
    Object o = read2(in, i);
    if (DOT == o) {
      throw new BadFormatException(
        "Single dot \'.\' that does not designate an improper list", in);
    } else {
      return o;
    }
  }

  /**
   * Read the external representation of a Scheme object from the
   * specified reader and return that object, using the specified
   * character as the first character of the external representation
   * of the object. The specified reader must support
   * <code>mark(int)</code> and <code>reset()</code>. The specified
   * character must be the start character of a token, that is, the
   * specified character must not be atmosphere.
   *
   * <p>The implementation of this method first tests whether the
   * specified character is -1, indicating an end-of-file condition.
   * If this is the case, it returns the end-of-file
   * object. Otherwise, it reads the rest of the external
   * representation of the object from the specified reader, calling
   * itself and <code>read1(Reader)</code> as necessary, and returns
   * that object.</p>
   *
   * <p>A subclass of this class which customizes or extends how
   * objects are read by this class must override this method. The
   * overriding method should first test whether the specified
   * character is -1 and, if so, return the end-of-file object. If the
   * specified character is not -1, it should determine whether it
   * recognizes the external representation in the specified reader
   * and, if so, return the corresponding object. If it does not
   * recognize the external representation in the specified reader, it
   * should restore the specified reader to the state it was in on
   * call to the <code>read2</code> method and return the result of
   * invoking the same method in its superclass.</p>
   *
   * @see     java.io.Reader#mark(int)
   * @see     java.io.Reader#reset()
   * @see     #consumeAtmosphere(Reader)
   * @see     #EOF
   * @see     #read1(Reader)

   * @param   in  The reader to read the external representation of a
   *              Scheme object from.
   * @param   i   The last character read from the reader that has not
   *              been consumed.
   * @return      The corresponding Scheme object, or {@link #EOF} if
   *              the end of file position is reached before
   *              encountering a Scheme object, or {@link #DOT} if a
   *              single dot token was read.
   * @throws  BadFormatException
   *              Signals that the external representation of a
   *              Scheme object does not adhere to the expected
   *              format.
   * @throws  IOException
   *              Signals an exceptional condition when accessing
   *              the specified reader.
   */
  protected Object read2(Reader in, int i)
    throws BadFormatException, IOException {

    if (-1 == i) {                       // =============== EOF.
      return EOF;

    } else if (isSymbolStart(i)) {       // =============== Symbols.
      StringBuffer buf = new StringBuffer();

      do {
        buf.append((char)i);
        in.mark(1);
        i = in.read();
      } while (isSymbolPart(i));
      in.reset();
      if ((! isDelimiter(i)) && ('#' != i)) {
        throw new BadFormatException("Illegal character \'" + ((char)i) +
                                     "\' in symbol", in);
      }

      return Symbol.intern(buf.toString().toLowerCase());

    } else if (('0' <= i) && ('9' >= i)) { // =============== Numbers.
      return readNumber(in, i);

    } else {
      Object o;
      
      switch (i) {
      case '(':                          // =============== Pairs and lists.
        i = consumeAtmosphere(in);
        if (')' == i) return Pair.EMPTY_LIST;

        // Read in first car element.
        o = read2(in, i);
        if (EOF == o) {
          throw new BadFormatException(
            "Unexpected end of file when reading pair/list", in);
        } else if (DOT == o) {
          throw new BadFormatException(
            "Single dot \'.\' that does not designate an improper list", in);
        }

        // Start pair/list.
        Pair head = new Pair(o, Pair.EMPTY_LIST);
        Pair tail = head;

        // Read rest of pair/list.
        do {
          i = consumeAtmosphere(in);
          if (')' == i) return head; // Done with list.

          // Read next car element.
          o = read2(in, i);
          if (EOF == o) {
            throw new BadFormatException(
              "Unexpected end of file when reading pair/list", in);
          } else if (DOT == o) {
            // Handle improper lists.
            o = read1(in);
            if (EOF == o) {
              throw new BadFormatException(
                "Unexpected end of file when reading pair/list", in);
            }
            i = consumeAtmosphere(in);
            if (')' != i) {
              throw new BadFormatException(
                "Missing closing parenthesis for improper list", in);
            } else {
              tail.cdr = o; // tail points to a fresh pair.
              return head;
            }
          }

          // Add new car element to list.
          Pair p   = new Pair(o, Pair.EMPTY_LIST);
          tail.cdr = p; // tail points to a fresh pair.
          tail     = p;
        } while (true);

      case '#':                          // =============== # forms.
        in.mark(1);
        i = in.read();
        if (-1 == i) {
          throw new BadFormatException("Unexpected end of file after \'#\'",
                                       in);
        }

        switch (i) {
        case 'f':                        // =============== Booleans.
        case 'F':
          return Boolean.FALSE;
        case 't':
        case 'T':
          return Boolean.TRUE;

        case 'i':                        // =============== More numbers.
        case 'I':
        case 'e':
        case 'E':
        case 'b':
        case 'B':
        case 'o':
        case 'O':
        case 'd':
        case 'D':
        case 'x':
        case 'X':
          in.reset();
          return readNumber(in, '#');

        case '\\':                       // =============== Characters.
          i = in.read();
          if (-1 == i) {
            throw new BadFormatException(
              "Unexpected end of file after character start \"#\\\"", in);
          } else if (! ((('a' <= i) && ('z' >= i)) ||
                        (('A' <= i) && ('Z' >= i)))) {
            // We know it's just a character.
            return new Character((char)i);
          }

          // Read in entire character or character name.
          StringBuffer buf = new StringBuffer();
          do {
            buf.append((char)i);
            in.mark(1);
            i = in.read();
          } while ((! isDelimiter(i)) && ('.' != i) && ('#' != i));
          in.reset();

          String s = buf.toString();
          if (s.length() == 1) {
            return new Character(s.charAt(0));

          } else if (s.equalsIgnoreCase("space")) {
            return SPACE;
          } else if (s.equalsIgnoreCase("backspace")) {
            return BACKSPACE;
          } else if (s.equalsIgnoreCase("tab")) {
            return TAB;
          } else if (s.equalsIgnoreCase("newline")) {
            return NEWLINE;
          } else if (s.equalsIgnoreCase("linefeed")) {
            return NEWLINE;
          } else if (s.equalsIgnoreCase("formfeed")) {
            return new Character('\f');
          } else if (s.equalsIgnoreCase("carriagereturn")) {
            return CARRIAGE_RETURN;
          } else if ((s.length() != 12) ||
                     (! s.toLowerCase().startsWith("unicode-"))) {
            throw new BadFormatException("Unrecognized character name \"" +
                                         s + "\"", in);
          }

          // Process Unicode character name.
          try {
            i = Integer.parseInt(s.substring(8, 12), 16);
          } catch (NumberFormatException x) {
            throw new BadFormatException("Illegal Unicode specification \"" +
              s.substring(8, 12) + "\" in character name", in);
          }
          return new Character((char)i);

        case '(':                        // =============== Vectors.
          i = consumeAtmosphere(in);
          if (')' == i) return Vector.EMPTY;

          {
            ArrayList a = new ArrayList();
            do {
              o = read2(in, i);

              if (EOF == o) {
                throw new BadFormatException(
                  "Unexpected end of file when reading vector", in);
              } else if (DOT == o) {
                throw new BadFormatException(
                  "Single dot \'.\' that does not designate improper list", in);
              } 
            
              a.add(o);
              i = consumeAtmosphere(in);
              if (')' == i) return Vector.create(a);
            } while (true);
          }

        case '[':                        // =============== Internal form.
          throw new BadFormatException(
            "Unable to read printed form of internal object or ellipsis", in);

        default:                         // =============== Bad # form.
          throw new BadFormatException("Unrecognized # form: \"#" + ((char)i) +
                                       "\"", in);
        }

      case '\"':                         // =============== Strings.
        StringBuffer buf = new StringBuffer();

        i = in.read();
        do {
          if ('\"' == i) {
            // Done.
            return buf.toString().intern();
          } else if (-1 == i) {
            // Unexpected EOF.
            throw new BadFormatException(
              "Unexpected end of file when reading string", in);
          } else if ('\\' == i) {
            // Handle escape sequence
            i = in.read();
            if (-1 == i) {
              throw new BadFormatException(
                "Unexpected end of file when reading string", in);
            } else {
              switch (i) {
              case 'b':
                buf.append('\b');
                break;
              case 't':
                buf.append('\t');
                break;
              case 'n':
                buf.append('\n');
                break;
              case 'f':
                buf.append('\f');
                break;
              case 'r':
                buf.append('\r');
                break;
              case '\"':
                buf.append('\"');
                break;
              case '\'':
                buf.append('\'');
                break;
              case '\\':
                buf.append('\\');
                break;
              case 'u':
                // Read in the Unicode spec.
                char[] chars = new char[4];
                for (int j=0; j<4; j++) {
                  i = in.read();
                  if (-1 == i) {
                    throw new BadFormatException(
                      "Unexpected end of file when reading string", in);
                  }
                  chars[j] = (char)i;
                }
                
                // Parse the Unicode spec.
                String s = new String(chars);
                try {
                  i = Integer.parseInt(s, 16);
                } catch (NumberFormatException x) {
                  throw new BadFormatException(
                    "Illegal Unicode specification \"" + s +
                    "\" in string escape sequence", in);
                }

                // Append resulting char.
                buf.append((char)i);
                break;
              default:
                throw new BadFormatException(
                  "Unrecognized string escape sequence \"\\" +
                  ((char)i) + "\"", in);
              }
            }
          } else {
            buf.append((char)i);
          }
          i = in.read();
        } while (true);

      case '\'':                         // =============== quote.
        o = read1(in);
        if (EOF == o) {
          throw new BadFormatException(
            "Unexpected end of file after \'\'\'", in);
        } else {
          return Pair.cons(QUOTE, Pair.cons(o, Pair.EMPTY_LIST));
        }

      case '`':                          // =============== quasiquote.
        o = read1(in);
        if (EOF == o) {
          throw new BadFormatException(
            "Unexpected end of file after \'`\'", in);
        } else {
          return Pair.cons(QUASIQUOTE, Pair.cons(o, Pair.EMPTY_LIST));
        }

      case ',':                          // =============== unquote.
        Symbol sym = UNQUOTE;
        in.mark(1);
        i = in.read();
        if ('@' == i) {
          sym = SPLICING;
        } else {
          in.reset();
        }

        o = read1(in);
        if (EOF == o) {
          throw new BadFormatException("Unexpected end of file after " +
                                       ((UNQUOTE == sym)? "\',\'" : "\",@\""),
                                       in);
        } else {
          return Pair.cons(sym, Pair.cons(o, Pair.EMPTY_LIST));
        }

      case '+':                          // =============== +, +i, pos. numbers.
        in.mark(1);
        i = in.read();
        in.reset();
        if (isDelimiter(i)) {
          return PLUS;
        } else {
          return readNumber(in, '+');
        }

      case '-':                          // =============== -, -i, neg. numbers.
        in.mark(1);
        i = in.read();
        in.reset();
        if (isDelimiter(i)) {
          return MINUS;
        } else {
          return readNumber(in, '-');
        }

      case '.':                          // =============== ., ..., numbers.
        in.mark(1);
        i = in.read();
        if ('.' != i) {
          in.reset();
          if (isDelimiter(i)) {
            return DOT;
          } else {
            return readNumber(in, '.');
          }
        }
        i = in.read();
        if ('.' != i) {
          throw new BadFormatException(
            "Illegal token starting with \"..\"", in);
        }
        return DOTDOTDOT;

      default:                           // =============== That's it... (;
        throw new BadFormatException("Unrecognized token start character \'"
                                     + ((char)i) + "\'", in);
      }
    }
  }

  /**
   * Read a number from the specified reader, using the specified
   * character as the first character of the external representation
   * of the number. This method reads in all characters from the
   * specified reader until encountering a delimiter, which is
   * returned to the reader and returns the result of calling {@link
   * Math#parse(String)} with the collected string. The specified
   * reader must support {@link java.io.Reader#mark(int)} and {@link
   * java.io.Reader#reset()}.
   *
   * @see     #isDelimiter(int)
   *
   * @param   in  The reader to read the external representation of
   *              a number from.
   * @param   i   The first character of the external representation
   *              of the number
   * @return      The corresponding number.
   * @throws  BadFormatException
   *              Signals that the external representation of the
   *              number does not have the expected format.
   * @throws  IOException
   *              Signals an exceptional condition when accessing
   *              the specified reader.
   */
  protected static Object readNumber(Reader in, int i)
    throws BadFormatException, IOException {

    StringBuffer buf = new StringBuffer();

    // Read in number.
    do {
      buf.append((char)i);
      in.mark(1);
      i = in.read();
    } while (! isDelimiter(i));
    in.reset();

    try {
      return Math.parse(buf.toString());
    } catch (BadFormatException x) {
      x.setCausingObject(in);  // Fix the cause...
      throw x;                 // ...and rethrow.
    }
  }

  /**
   * Consume atmosphere. This method consumes all white space and
   * comments up to and including the first character that is not
   * white space and not part of a comment and returns this
   * character. Note that this method does not mark the specified
   * reader before reading from it.
   *
   * @param   in  The reader to read from.
   * @return      The first character that is not white space and
   *              not part of a comment, or -1 if the end of reader
   *              has been reached.
   * @throws  IOException
   *              Signals an exceptional condition when reading
   *              from the specified reader.
   */
  protected static int consumeAtmosphere(Reader in) throws IOException {
    int i;

    i = in.read();
    while ((';' == i) || Character.isWhitespace((char)i)) {
      if (';' == i) {
        do {
          i = in.read();
        } while ((-1 != i) && ('\n' != i) && ('\r' != i));
        if (-1 == i) break;
      }
      i = in.read();
    }

    return i;
  }

  /**
   * Write the external representation of the specified Scheme object
   * to the specified writer. This method recursively walks the
   * specified Scheme object and writes its external representation to
   * the specified writer.
   *
   * <p>To prevent this method from looping forever when writing
   * circular data structures, it uses two heuristics. First, it
   * correctly detects circular cdr chains for lists and, after
   * writing all unique elements in the list to the specified writer
   * at least once, ends the list with "<code>#[...]</code>".  Second,
   * it uses the recursion limit to constrain the depth of the
   * recursive walk. If the current recursion limit is reached, it
   * prints "<code>#[...]</code>" instead of the current subobject and
   * does not continue the recursive traversal of that subobject.</p>
   *
   * <p>Note that (proper and improper) lists are walked linearily
   * along the cdr chain, so that only their car elements increase the
   * current recursion depth.</p>
   *
   * <p>The implementation of this method calls <code>write1</code>
   * with the specified object, writer, quoted flag, and a recursion
   * depth of 0. Any I/O exceptions thrown by <code>write1</code> are
   * caught and rethrown as a wrapped exception.</p>
   *
   * @see     #write1(Object,Writer,boolean,int)
   *
   * @param   o       The Scheme object to write.
   * @param   out     The writer to write the external representation
   *                  of the specified Scheme object to.
   * @param   quoted  Flag to indicate whether characters and strings
   *                  should be quoted, or output verbatim.
   * @throws  WrappedException
   *                  Signals an exceptional condition when writing to
   *                  the specified writer.
   */
  public final void write(Object o, Writer out, boolean quoted)
    throws WrappedException {

    try {
      write1(o, out, quoted, 0);
    } catch (IOException x) {
      throw new WrappedException("Unable to write to", x, out);
    }
  }

  /**
   * Write the external representation of the specified Scheme object
   * to the specified writer. Limits the recursion depth to the
   * current recursion limit {@link #getRecursionLimit()}.
   *
   * <p>The implementation of this method first tests whether the
   * current recursion depth is larger than the recursion limit. If
   * this is the case, it writes "<code>#[...]</code>" to the
   * specified writer. Otherwise, it writes the external
   * representation of the specified object to the specified writer,
   * calling itself recursively if necessary.</p>
   *
   * <p>A subclass of this class which customizes or extends how
   * objects are written by this class must override this method. The
   * overriding method should have the following
   * structure:<blockquote><pre>
   * void write1(Object o, Writer out, boolean quoted, int rec)
   *   throws IOException {
   *
   *   if (rec > getRecursionLimit()) {
   *     out.write("#[...]");
   *   } else if (...) {
   *     ...
   *   } else {
   *     super.write1(o, out, quoted, rec);
   *   }
   * }
   * </pre></blockquote>The elided section should test whether the
   * specified object is recognized by the implementation and, if so,
   * write its external representation to the specified writer.</p>
   *
   * @see     #write(Object,Writer,boolean)
   *
   * @param   o       The Scheme object whose external representation to
   *                  write.
   * @param   out     The writer to write the external representation of
   *                  the specified Scheme object to.
   * @param   quoted  Flag to indicate whether characters and strings
   *                  should be quoted, or output verbatim.
   * @param   rec     The current recursion depth.
   * @throws  IOException
   *                  Signals an exceptional condition when writing to
   *                  the specified writer.
   */
  protected void write1(Object o, Writer out, boolean quoted, int rec)
    throws IOException {

    if (rec > getRecursionLimit()) {              // Recursion limit.
      out.write("#[...]");
    } else if (Pair.EMPTY_LIST == o) {            // The empty list.
      out.write("()");
    } else if (o instanceof Number) {             // Numbers.
      if ((o instanceof Double) || (o instanceof Float)) {
        double d = ((Number)o).doubleValue();
        if (Double.isNaN(d)) {
          out.write("#[NaN]");
        } else if (Double.isInfinite(d)) {
          if (0.0D < d) {
            out.write("#[+Infinity]");
          } else {
            out.write("#[-Infinity]");
          }
        } else {
          out.write(o.toString());
        }
      } else {
        out.write(o.toString());
      }
    } else if (o instanceof Boolean) {            // Booleans.
      if (((Boolean)o).booleanValue()) {
        out.write("#t");
      } else {
        out.write("#f");
      }
    } else if (o instanceof Pair) {               // Pairs and lists.
      Pair p = (Pair)o;
      int  l = p.estimateLength();
      int  i = 0;

      out.write('(');
      do {
        write1(p.car, out, quoted, rec + 1);

        o = p.cdr;
        if (Pair.EMPTY_LIST == o) {
          out.write(')');
          return;
        } else if (! (o instanceof Pair)) {
          out.write(" . ");
          write1(o, out, quoted, rec);
          out.write(')');
          return;
        } else {
          p = (Pair)o;
          i++;
          if (i >= l) {
            out.write(" #[...])");
            return;
          } else {
            out.write(' ');
          }
        }
      } while(true);
    } else if (o instanceof Symbol) {             // Symbols.
      out.write(o.toString());
    } else if (o instanceof Character) {          // Characters.
      char c = ((Character)o).charValue();
      if (quoted) {
        out.write("#\\");

        if ((' ' != c) && isPrintable(c)) {
          out.write(c);
        } else {
          switch (c) {
          case ' ':
            out.write("space");
            break;
          case '\b':
            out.write("backspace");
            break;
          case '\t':
            out.write("tab");
            break;
          case '\n':
            out.write("newline");
            break;
          case '\f':
            out.write("formfeed");
            break;
          case '\r':
            out.write("carriagereturn");
            break;
          default:
            out.write("unicode-");
            out.write(Character.forDigit(((c >> 12) & 0x0f), 16));
            out.write(Character.forDigit(((c >>  8) & 0x0f), 16));
            out.write(Character.forDigit(((c >>  4) & 0x0f), 16));
            out.write(Character.forDigit(((c >>  0) & 0x0f), 16));
          }
        }
      } else {
        out.write(c);
      }
    } else if (o instanceof String) {             // Strings.
      String s = (String) o;
      if (quoted) {
        out.write('\"');
        int l = s.length();
        for (int i=0; i<l; i++) {
          char c = s.charAt(i);
          if ('\"' == c) {
            out.write("\\\"");
          } else if ('\'' == c) {
            out.write("\\\'");
          } else if ('\\' == c) {
            out.write("\\\\");
          } else if (isPrintable(c)) {
            out.write(c);
          } else {
            switch (c) {
            case '\b':
              out.write("\\b");
              break;
            case '\t':
              out.write("\\t");
              break;
            case '\n':
              out.write("\\n");
              break;
            case '\f':
              out.write("\\f");
              break;
            case '\r':
              out.write("\\r");
              break;
            default:
              out.write("\\u");
              out.write(Character.forDigit(((c >> 12) & 0x0f), 16));
              out.write(Character.forDigit(((c >>  8) & 0x0f), 16));
              out.write(Character.forDigit(((c >>  4) & 0x0f), 16));
              out.write(Character.forDigit(((c >>  0) & 0x0f), 16));
            }
          }
        }
        out.write('\"');
      } else {
        out.write(s);
      }
    } else if (o instanceof StringBuffer) {
      StringBuffer s = (StringBuffer)o;
      if (quoted) {
        out.write('\"');
        int l = s.length();
        for (int i=0; i<l; i++) {
          char c = s.charAt(i);
          if ('\"' == c) {
            out.write("\\\"");
          } else if ('\'' == c) {
            out.write("\\\'");
          } else if ('\\' == c) {
            out.write("\\\\");
          } else if (isPrintable(c)) {
            out.write(c);
          } else {
            switch (c) {
            case '\b':
              out.write("\\b");
              break;
            case '\t':
              out.write("\\t");
              break;
            case '\n':
              out.write("\\n");
              break;
            case '\f':
              out.write("\\f");
              break;
            case '\r':
              out.write("\\r");
              break;
            default:
              out.write("\\u");
              out.write(Character.forDigit(((c >> 12) & 0x0f), 16));
              out.write(Character.forDigit(((c >>  8) & 0x0f), 16));
              out.write(Character.forDigit(((c >>  4) & 0x0f), 16));
              out.write(Character.forDigit(((c >>  0) & 0x0f), 16));
            }
          }
        }
        out.write('\"');
      } else {
        out.write(s.toString());
      }
    } else if (o instanceof List) {               // Vectors.
      List v = (List)o;
      int  l = v.size();

      if (0 == l) {
        out.write("#()");
      } else {
        out.write("#(");
        for (int i=0; i<l-1; i++) {
          write1(v.get(i), out, quoted, rec + 1);
          out.write(' ');
        }
        write1(v.get(l-1), out, quoted, rec + 1);
        out.write(')');
      }
    } else if (o instanceof Reader) {             // Input ports.
      out.write("#[input-port ");
      out.write(o.toString());
      out.write(']');
    } else if (o instanceof Writer) {             // Output ports.
      out.write("#[output-port ");
      out.write(o.toString());
      out.write(']');
    } else if (o instanceof ObjectInput) {        // Object input.
      out.write("#[object-input ");
      out.write(o.toString());
      out.write(']');
    } else if (o instanceof ObjectOutput) {       // Object output.
      out.write("#[object-output ");
      out.write(o.toString());
      out.write(']');
    } else if (o instanceof InputStream) {        // Input streams.
      out.write("#[input-stream ");
      out.write(o.toString());
      out.write(']');
    } else if (o instanceof OutputStream) {       // Output streams.
      out.write("#[output-stream ");
      out.write(o.toString());
      out.write(']');
    } else if ((o instanceof Applicable) ||       // Applicables.
               (o instanceof Environment) ||      // Environments.
               (o instanceof Promise) ||          // Promises.
               (EOF == o) ||                      // EOF.
                                                  // Uninitialized binding.
               (Environment.Uninitialized.MARKER == o)) {
      out.write(o.toString());
    } else if (o instanceof Class) {              // Classes.
      Class k = (Class)o;
      if (k.isArray()) {
        out.write("#[class ");
        out.write(Cast.getName(k));
        out.write(']');
      } else {
        out.write("#[");
        out.write(k.toString());
        out.write(']');
      }
    } else if ((o instanceof Field) ||            // Fields.
               (o instanceof Method) ||           // Methods.
               (o instanceof Constructor) ||      // Constructors.
               (o instanceof URL)) {              // URLs.
      out.write("#[");
      out.write(o.toString());
      out.write(']');
    } else if (o instanceof Throwable) {          // Throwables.
      out.write("#[throwable ");
      out.write(o.toString());
      out.write(']');
    } else if (o.getClass().isArray()) {           // Arrays.
      int l = java.lang.reflect.Array.getLength(o);

      if (0 == l) {
        out.write("#()");
      } else {
        out.write("#(");
        for (int i=0; i<l-1; i++) {
          write1(java.lang.reflect.Array.get(o, i), out, quoted, rec + 1);
          out.write(' ');
        }
        write1(java.lang.reflect.Array.get(o, l-1), out, quoted, rec + 1);
        out.write(')');
      }
    } else {                                      // Everything else.
      out.write("#[java-object ");
      out.write(o.getClass().getName());
      out.write(' ');
      out.write(o.toString());
      out.write(']');
    }
  }

  // ============================== Helpers =================================

  /**
   * Test whether the specified character is a valid Scheme
   * delimiter.
   *
   * <p>Valid delimiters are white space characters as defined for
   * <code>Character.isWhitespace(char)</code>, '<code>(</code>'
   * (opening parenthesis), '<code>)</code>' (closing parenthesis),
   * '<code>"</code>' (double quote), '<code>;</code>' (semicolon),
   * '<code>'</code>' (single quote), '<code>`</code>' (backquote),
   * '<code>,</code>' (comma), and <code>-1</code> (end of file).</p>
   *
   * @param   c  The character to test.
   * @return     <code>true</code> iff the specified character
   *             is a legal delimiter between two Scheme tokens.
   */
  public static boolean isDelimiter(int c) {
                                 // These are defined in 7.1.1 of R5RS.
    return (Character.isWhitespace((char)c) || 
            (c == '(') ||
            (c == ')') ||
            (c == '\"') ||
            (c == ';') ||
            (c == '\'') ||      // These three are always valid, too.
            (c == '`') ||
            (c == ',') ||
            (c == -1));         // EOF always is a valid delimiter, too.
  }

  /**
   * Test whether the specified character is a valid character
   * to start a Scheme symbol.
   *
   * @param   c  The character to test.
   * @return     <code>true</code> iff the specified character
   *             is a valid start character for a symbol.
   */
  public static boolean isSymbolStart(int c) {
    return (((c >= 'a') && (c <= 'z')) ||
            ((c >= 'A') && (c <= 'Z')) ||
            (c == '!') ||
            ((c >= '$') && (c <= '&')) ||
            (c == '*') ||
            (c == '/') ||
            (c == ':') ||
            ((c >= '<') && (c <= '?')) ||
            ((c >= '^') && (c <= '_')) ||
            (c == '~'));
  }

  /**
   * Test whether the specified character is a valid character
   * to appear within a Scheme symbol.
   *
   * @param   c  The character to test.
   * @return     <code>true</code> iff the specified character
   *             is a valid character to appear within a symbol.
   */
  public static boolean isSymbolPart(int c) {
    return (((c >= 'a') && (c <= 'z')) ||
            ((c >= 'A') && (c <= 'Z')) ||
            ((c >= '0') && (c <= '9')) ||
            (c == '!') ||
            ((c >= '$') && (c <= '&')) ||
            ((c >= '*') && (c <= '+')) ||
            ((c >= '-') && (c <= '/')) ||
            (c == ':') ||
            ((c >= '<') && (c <= '@')) ||
            ((c >= '^') && (c <= '_')) ||
            (c == '~'));
  }

  /**
   * Test whether the specified character is printable.
   *
   * <p>This method considers all Unicode characters that are not in a
   * <code>Cx</code> category to be printable. In other words, all
   * characters that are not in the <code>Cc</code> (Control),
   * <code>Cf</code> (Format), <code>Cs</code> (Surrogate),
   * <code>Co</code> (Private Use), or <code>Cn</code> (Unassigned)
   * categories are printable.</p>
   *
   * @param   c  The character to test.
   * @return     <code>true</code> iff the specified character
   *             is a printable character.
   */
  public static boolean isPrintable(int c) {
    int category = Character.getType((char)c);

    return ((category != Character.CONTROL) &&
            (category != Character.FORMAT) &&
            (category != Character.SURROGATE) &&
            (category != Character.PRIVATE_USE) &&
            (category != Character.UNASSIGNED));
  }

}
