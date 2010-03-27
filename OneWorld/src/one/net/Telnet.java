/*
 * Copyright (c) 2001, University of Washington, Department of
 * Computer Science and Engineering.
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

package one.net;

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.net.ServerSocket;
import java.net.Socket;

import one.util.Bug;

/**
 * Implementation of a very simple line-oriented telnet connection.
 * This class exposes methods to read a line (or password) from the
 * telnet connection and to write to the telnet connection. The
 * methods reading from the connection are not thread-safe, though the
 * methods writing to the connection are. This class does not
 * implement a flush method, because all writes to the connection are
 * immediately flushed.
 *
 * <p>To consistently work with fully featured telnet clients (Linux)
 * as well as rather limited telnet clients (Windows 2000), this class
 * echoes characters as they are typed and maintains its own internal
 * buffer. Editing support includes backspace and delete
 * ("ESC&nbsp;[&nbsp;3&nbsp;~" as a terminal escape sequence), the
 * left and right arrow keys ("ESC&nbsp;[&nbsp;D" and
 * "ESC&nbsp;[&nbsp;C"), and the insert key
 * ("ESC&nbsp;[&nbsp;2&nbsp;~") to toggle between insert and overwrite
 * mode. It also maintains a history of typed lines, which is
 * accessible through the up and down arrow keys ("ESC&nbsp;[&nbsp;A"
 * and "ESC&nbsp;[&nbsp;B").</p>
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public class Telnet implements NetConstants {

  // =======================================================================
  //                           Protocol codes
  // =======================================================================

  /** The end-of-file code. */
  static final int EOF               = 236;

  /** The supsend current process code. */
  static final int SUSP              = 237;

  /** The abort process code. */
  static final int ABORT             = 238;

  /** The end of record code. */
  static final int EOR               = 239;

  /** The suboption end code. */
  static final int SE                = 240;

  /** The no operation code. */
  static final int NOP               = 241;

  /** The data mark code. */
  static final int DM                = 242;

  /** The break code. */
  static final int BRK               = 243;

  /** The interrupt process code. */
  static final int IP                = 244;

  /** The abort output code. */
  static final int AO                = 245;

  /** The are you there code. */
  static final int AYT               = 246;

  /** The escape character code. */
  static final int EC                = 247;

  /** The erase line code. */
  static final int EL                = 248;

  /** The go ahead code. */
  static final int GA                = 249;

  /** The suboption begin code. */
  static final int SB                = 250;

  /** The will option negotiation code. */
  static final int WILL              = 251;

  /** The won't option negotiation code. */
  static final int WONT              = 252;

  /** The do option negotiation code. */
  static final int DO                = 253;

  /** The don't option negotiation code. */
  static final int DONT              = 254;

  /** The interpret as command code. */
  static final int IAC               = 255;


  // =======================================================================
  //                               Options
  // =======================================================================

  /** The echo option ID. */
  static final int ECHO              =   1;

  /** The suppress go ahead option ID. */
  static final int SUPPRESS_GO_AHEAD =   3;

  /** The status option ID. */
  static final int STATUS            =   5;

  /** The status is sub-option code. */
  static final int STATUS_IS         =   0;

  /** The status send sub-option code. */
  static final int STATUS_SEND       =   1;

  /** The timing mark option ID. */
  static final int TIMING_MARK       =   6;


  // =======================================================================
  //                             Line buffer
  // =======================================================================

  /** Implementation of a line buffer. */
  class Buffer {
      
    /** The default length of a newly created buffer. */
    private static final int DEFAULT_LENGTH = 80;
      
    /** The actual bytes. */
    private byte[]  bytes;
      
    /** The current cursor position. */
    private int     pos;
      
    /** The length, that is, the number of valid bytes in the buffer. */
    private int     len;
      
    /** Create a new buffer. */
    Buffer() {
      bytes   = new byte[DEFAULT_LENGTH];
    }

    /** 
     * Move the current cursor position to the end of this buffer.
     * Note that this method does not echo the move operation.
     */
    void eol() {
      pos = len;
    }
      
    /**
     * Clear this buffer.
     *
     * @throws  IOException
     *             Signals an exceptional condition while interacting
     *             with the other side.
     */
    void clear() throws IOException {
      echoClear();

      pos = 0;
      len = 0;
    }
      
    /**
     * Insert the specified byte at the current cursor position.
     *
     * @param   i  The byte to insert.
     * @throws  IOException
     *             Signals an exceptional condition while interacting
     *             with the other side.
     */
    void insert(int i) throws IOException {
      // Make sure we have space.
      ensureSpace();
      
      // Shift the remainder of this buffer if necessary.
      if (pos < len) {
        System.arraycopy(bytes, pos, bytes, pos + 1, len - pos);
      }

      // Actually insert the byte.
      bytes[pos] = (byte)i;
      pos++;
      len++;
      
      // Update other side.
      echoInsert();
    }
    
    /**
     * Overwrite the byte at the current cursor position with the
     * specified byte.
     *
     * @param   i  The byte to overwrite the current cursor position
     *             with.
     * @throws  IOException
     *             Signals an exceptional condition while interacting
     *             with the other side.
     */
    void overwrite(int i) throws IOException {
      // If we are at the end of the buffer, make sure we have space.
      if (pos >= len) {
        ensureSpace();
        len++;
      }
      
      // Actually overwrite the byte.
      bytes[pos] = (byte)i;
      pos++;

      // Update the other side.
      echo(i);
    }
    
    /** Ensure that this buffer has space for adding one more byte. */
    private void ensureSpace() {
      if (bytes.length <= len) {
        byte[] newBytes = new byte[bytes.length * 2];
        System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
        bytes           = newBytes;
      }
    }
    
    /**
     * Move the current cursor position to the left.
     *
     * @throws  IOException
     *             Signals an exceptional condition while interacting
     *             with the other side.
     */
    void left() throws IOException {
      if (0 == pos) {
        ringBell();
      } else {
        pos--;
        echo(CHAR_BACKSPACE);
      }
    }
    
    /**
     * Move the current cursor position to the right.
     *
     * @throws  IOExexception
     *             Signals an exceptional condition while interacting
     *             with the other side.
     */
    void right() throws IOException {
      if (pos >= len) {
        ringBell();
      } else {
        pos++;
        echo(bytes[pos-1]);
      }
    }
    
    /**
     * Delete the byte to the left of the current cursor position.
     *
     * @throws  IOException
     *             Signals an exceptional condition while interacting
     *             with the other side.
     */
    void backspace() throws IOException {
      // Are we at the beginnig of this buffer?
      if (0 == pos) {
        ringBell();
        return;
      }
      
      // Shift the remainder of this buffer if necessary.
      if (pos < len) {
        System.arraycopy(bytes, pos, bytes, pos - 1, len - pos);
      }
      
      // Actually delete the byte.
      pos--;
      len--;
      
      // Update other side.
      echoBackspace();
    }
    
    /**
     * Delete the byte at the current cursor position.
     *
     * @throws  IOException
     *             Signals an exceptional condition while interacting
     *             with the other side.
     */
    void delete() throws IOException {
      // Are we at the end of this buffer?
      if (pos >= len) {
        ringBell();
        return;
      }
      
      // Shift the remainder of this buffer.
      System.arraycopy(bytes, pos + 1, bytes, pos, len - pos + 1);
      
      // Actually delete the byte.
      len--;
      
      // Update other side.
      echoDelete();
    }

    /**
     * Echo the specified character.
     *
     * @param   i  The character to echo.
     * @throws  IOException
     *             Signals an exceptional condition while interacting
     *             with the other side.
     */
    void echo(int i) throws IOException {
      synchronized (lock) {
        if (echo) {
          out.write(i);
          out.flush();
        }
      }
    }

    /**
     * Echo this buffer.
     *
     * @throws   IOException
     *              Signals an exceptional condition while interacting 
     *              with the other side.
     */
    void echoAll() throws IOException {
      synchronized (lock) {
        if (echo) {
          for (int i=0; i<len; i++) {
            out.write(bytes[i]);
          }
          out.flush();
        }
      }
    }

    /**
     * Echo this buffer before it is cleared.
     *
     * @throws   IOException
     *              Signals an exceptional condition while interacting
     *              with the other side.
     */
    void echoClear() throws IOException {
      synchronized (lock) {
        if (echo) {
          if (0 == len) {
            return;
          }
          for (int i=pos; i<len; i++) {
            out.write(bytes[i]);
          }
          out.write(CHAR_BACKSPACE);
          for (int i=1; i<len; i++) {
            out.write(' ');
            out.write(CHAR_BACKSPACE);
            out.write(CHAR_BACKSPACE);
          }
          out.write(' ');
          out.write(CHAR_BACKSPACE);
          out.flush();
        }
      }
    }

    /**
     * Echo this buffer after an insert operation.
     *
     * @throws   IOException
     *              Signals an exceptional condition while interacting
     *              with the other side.
     */
    void echoInsert() throws IOException {
      synchronized (lock) {
        if (echo) {
          for (int i=pos-1; i<len; i++) {
            out.write(bytes[i]);
          }
          for (int i=pos; i<len; i++) {
            out.write(CHAR_BACKSPACE);
          }
          out.flush();
        }
      }
    }

    /**
     * Echo this buffer after a backspace operation.
     *
     * @throws   IOException
     *              Signals an exceptional condition while interacting
     *              with the other side.
     */
    void echoBackspace() throws IOException {
      synchronized (lock) {
        if (echo) {
          out.write(CHAR_BACKSPACE);
          for (int i=pos; i<len; i++) {
            out.write(bytes[i]);
          }
          out.write(' ');
          for (int i=pos; i<=len; i++) {
            out.write(CHAR_BACKSPACE);
          }
          out.flush();
        }
      }
    }

    /**
     * Echo this buffer after a delete operation.
     *
     * @throws   IOException
     *              Signals an exceptional condition while interacting
     *              with the other side.
     */
    void echoDelete() throws IOException {
      synchronized (lock) {
        if (echo) {
          for (int i=pos; i<len; i++) {
            out.write(bytes[i]);
          }
          out.write(' ');
          for (int i=pos; i<=len; i++) {
            out.write(CHAR_BACKSPACE);
          }
          out.flush();
        }
      }
    }

    /**
     * Get a string representation of the contents of this buffer.
     *
     * @return  A string representation of the contents of this buffer.
     */
    public String toString() {
      try {
        return new String(bytes, 0, len, ENCODING_ASCII);
      } catch (UnsupportedEncodingException x) {
        throw new Bug("ASCII character encoding not supported");
      }
    }

    /**
     * Get a string representation of the contents of this
     * buffer. Buffer contents that do not represent ASCII characters
     * are escaped.
     *
     * @return  A string representation of the contents of this buffer
     *          with non-ASCII characters escaped.
     */
    public String toEscapedString() {
      StringBuffer buf = new StringBuffer(len);
      
      // Iterate over the bytes in the buffer.
      for (int i=0; i<len; i++) {
        if ((CHAR_SPACE <= bytes[i]) && (CHAR_DELETE > bytes[i])) {
          // Printable character.
          buf.append((char)bytes[i]);

        } else {
          // Escape character.
          buf.append('\\');
          String s = String.valueOf(bytes[i]);
          int    l = s.length();
          if (2 >= l) {
            buf.append('0');
            if (1 >= l) {
              buf.append('0');
            }
          }
          buf.append(s);
        }
      }

      return buf.toString();
    }

  }

  // =======================================================================
  //                            Instance fields
  // =======================================================================

  /** The lock for this telnet connection. */
  Object           lock;

  /** The socket for this telnet connection. */
  Socket           socket;
  
  /** The input stream for this telnet connection. */
  InputStream      in;
  
  /** The output stream for this telnet connection. */
  OutputStream     out;

  /** The writer for this telnet connection. */
  Writer           writer;
  
  /** The flag for whether to echo sent bytes. */
  boolean          echo;

  /** The flag for whether this connection is in insert mode. */
  boolean          insert;

  /** The current buffer. */
  Buffer           buf;

  /**
   * Flag for whether this telnet connection has reached the
   * end-of-file condition, that is, whether it has been closed.
   */
  volatile boolean eof;

  /** The history of buffers. */
  Buffer[]         history;

  /** The current head of the history. */
  int              historyHead;

  /** The current tail of the history. */
  int              historyTail;

  /** The current index into the history. */
  int              historyIndex;

  // =======================================================================
  //                             Constructor
  // =======================================================================

  /**
   * Create a new telnet connection for the specified socket.
   *
   * @param   socket  The client socket for the new telnet connection.
   * @param   historySize
   *                  The size of the buffer history.
   * @throws  NullPointerException
   *                  Signals that <code>socket</code> is
   *                  <code>null</code>.
   * @throws  IllegalArgumentException
   *                  Signals a negative history buffer size.
   * @throws  IOException
   *                  Signals an exceptional condition while
   *                  interacting with the other side.
   */
  public Telnet(Socket socket, int historySize) throws IOException {
    if (null == socket) {
      throw new NullPointerException("Null socket");
    } else if (0 > historySize) {
      throw new IllegalArgumentException("Negative buffer history size");
    }

    // Set up instance fields.
    lock         = new Object();
    this.socket  = socket;
    in           = socket.getInputStream();
    out          = socket.getOutputStream();
    writer       = new OutputStreamWriter(out, ENCODING_ASCII);
    echo         = true;
    insert       = true;
    buf          = new Buffer();
    eof          = false;
    history      = new Buffer[historySize];
    historyHead  = 0;
    historyTail  = -1;
    historyIndex = -1;

    // Send options.
    send(WILL, ECHO);
    send(WILL, SUPPRESS_GO_AHEAD);
  }


  // =======================================================================
  //                          Instance methods
  // =======================================================================

  /**
   * Test whether this telnet connection is open.
   *
   * @return  <code>true</code> if this telnet connection is
   *          open.
   */
  public boolean isOpen() {
    synchronized (lock) {
      return (! eof);
    }
  }

  /**
   * Read a password from this telnet connection. Note that reading
   * from a telnet connection is not thread-safe; only one read method
   * may be invoked on the same connection at a time.
   *
   * @return     The read-in password.
   * @throws  IOException
   *             Signals an exceptional condition while interacting
   *             with the other side.
   */
  public String readPassword() throws IOException {
    if (eof) {
      return null;
    }
    
    echo            = false;
    boolean success = fillBuffer(false);
    echo            = true;

    if (success) {
      return buf.toString();
    } else {
      return null;
    }
  }

  /**
   * Read a line from this telnet connection. Note that reading from a
   * telnet connection is not thread-safe; only one read method may be
   * invoked on the same connection at a time.
   *
   * @return     The read-in line.
   * @throws  IOException
   *             Signals an exceptional condition while interacting
   *             with the other side.
   */
  public String readLine() throws IOException {
    if (eof) {
      return null;
    } else if (fillBuffer(true)) {
      return buf.toString();
    } else {
      return null;
    }
  }

  /**
   * Read a line from this telnet connection and escape unprintable
   * characters. Note that reading from a telnet connection is not
   * thread-safe; only one read method may be invoked on the same
   * connection at a time.
   *
   * @return     The read-in line, with unprintable characters escaped
   *             in decimal form.
   * @throws  IOException
   *             Signals an exceptional condition while interacting
   *             with the other side.
   */
  public String readEscapedLine() throws IOException {
    if (eof) {
      return null;
    } else if (fillBuffer(true)) {
      return buf.toEscapedString();
    } else {
      return null;
    }
  }

  /**
   * Fill the buffer with a line.
   *
   * @param   visible  Flag for whether the read-in line is visible in
   *                   the history buffer.
   * @return           Flag for whether a line was successfully read.
   * @throws  IOException
   *                   Signals an exceptional condition while interacting
   *                   with the other side.
   */
  private boolean fillBuffer(boolean visible) throws IOException {
    // Create new line buffer.
    buf = new Buffer();

    // Add into history.
    if (visible) {
      addToHistory(buf);
    }

    int i = in.read();
    while (true) {
      switch (i) {

      case -1:
        // End-of-file
        close();
        return false;

      case CHAR_BACKSPACE:
        buf.backspace();
        break;

      case '\n':
        if (visible) {
          trimHistory();
        }
        return true;

      case '\r':
        i = in.read();

        if (('\n' == i) || (0 == i)) {
          if (visible) {
            trimHistory();
          }
          return true;
        } else {
          if (insert) {
            buf.insert('\r');
          } else {
            buf.overwrite('\r');
          }
        }
        continue;

      case CHAR_ESCAPE:
        i = in.read();
        if ('[' == i) {
          i = in.read();

          // Process VT100 terminal emulation
          switch (i) {
          case '2':
            i = in.read();
            if ('~' == i) {
              // Toggle between insert and overwrite mode.
              insert = (! insert);
              break;
            } else {
              if (insert) {
                buf.insert(CHAR_ESCAPE);
                buf.insert('[');
                buf.insert('2');
                buf.insert(i);
              } else {
                buf.overwrite(CHAR_ESCAPE);
                buf.overwrite('[');
                buf.overwrite('2');
                buf.overwrite(i);
              }
            }
            break;
          case '3':
            i = in.read();
            if ('~' == i) {
              // Delete.
              buf.delete();
            } else {
              if (insert) {
                buf.insert(CHAR_ESCAPE);
                buf.insert('[');
                buf.insert('3');
                buf.insert(i);
              } else {
                buf.overwrite(CHAR_ESCAPE);
                buf.overwrite('[');
                buf.overwrite('3');
                buf.overwrite(i);
              }
            }
            break;
          case 'A':
            // Move backwards (up) in history.
            if (visible) {
              moveUp();
            } else {
              ringBell();
            }
            break;
          case 'B':
            // Move forwards (down) in history.
            if (visible) {
              moveDown();
            } else {
              ringBell();
            }
            break;
          case 'C':
            buf.right();
            break;
          case 'D':
            buf.left();
            break;
          case 'K':
            buf.clear();
            break;
          default:
            if (insert) {
              buf.insert(CHAR_ESCAPE);
              buf.insert('[');
              buf.insert(i);
            } else {
              buf.overwrite(CHAR_ESCAPE);
              buf.overwrite('[');
              buf.overwrite(i);
            }
          }
        } else {
          if (insert) {
            buf.insert(CHAR_ESCAPE);
            buf.insert(i);
          } else {
            buf.overwrite(CHAR_ESCAPE);
            buf.overwrite(i);
          }
        }
        break;

      case CHAR_DELETE:
        buf.delete();
        break;

      case IAC:
        i = in.read();
        switch (i) {

        case EOF:
          close();
          return false;

        case AYT:
          writeLine("Yes");
          break;
          
        case EL:
          buf.clear();
          break;
          
        case SB:
          processSuboption();
          break;
          
        case WILL:
        case WONT:
        case DO:
        case DONT:
          negotiate(i);
          break;

        default:
          // Ignore.
        }
        break; // End IAC

      default:
        if (insert) {
          buf.insert(i);
        } else {
          buf.overwrite(i);
        }
        break;
      }

      i = in.read();
    }
  }

  /**
   * Perform option negotiation. The IAC (WILL | WONT | DO | DONT)
   * initiating option negotiation must have been consume from this
   * connection's input stream. However, the option code is still in
   * the input stream.
   *
   * @param   negotiation  The negotiation code.
   * @throws  IOException  Signals an exceptional condition while
   *                       interacting with the other side.
   */
  private void negotiate(int negotiation) throws IOException {
    int option = in.read();

    switch (option) {

    case ECHO:
      // Optimistically ignored on the assumption that the other side
      // has favorably responded to the IAC WILL ECHO sent in the
      // constructor.
      return;

    case SUPPRESS_GO_AHEAD:
      // Optimistically ignored on the assumption that the other side
      // has favorably repsonded to the IAC WILL SUPPRESS-GO-AHEAD in
      // the constructor.
      return;

    default:
      if ((WILL == negotiation) || (WONT == negotiation)) {
        send(DONT, option);
      } else {
        send(WONT, option);
      }
    }
  }

  /**
   * Send the specified option negotiation.
   *
   * @param   negotiation  The negotiation code.
   * @param   option       The option code.
   * @throws  IOException  Signals an exceptional condition while
   *                       sending.
   */
  void send(int negotiation, int option) throws IOException {
    synchronized (lock) {
      out.write(IAC);
      out.write(negotiation);
      out.write(option);
      out.flush();
    }
  }

  /**
   * Perform suboption negotiation. The IAC SB initiating suboption
   * negotiation must have been consumed from this connection's input
   * stream.
   *
   * @throws  IOException  Signals an exceptional condition while
   *                       interacting with the other side.
   */
  private void processSuboption() throws IOException {
    // Simply consume everything until we see IAC SE.
    int prev;
    int curr = in.read();

    do {
      prev = curr;
      curr = in.read();
    } while ((IAC != prev) && (SE != curr));
  }

  /**
   * Add the specified buffer into the buffer history. This method
   * also resets the current history index.
   *
   * @param   buf  The buffer to add into the buffer history.
   */
  private void addToHistory(Buffer buf) {
    if (historyHead == historyTail) {
      // Is the history full?
      historyHead++;
      if (history.length <= historyHead) {
        historyHead = 0;
      }

    } else if (0 > historyTail) {
      // Is the history empty?
      historyTail = 0;
      historyHead = 0;
    }

    // Add buffer.
    history[historyTail] = buf;

    // Set history index to current tail.
    historyIndex = historyTail;

    // Adjust tail of history.
    historyTail++;
    if (history.length <= historyTail) {
      historyTail = 0;
    }
  }

  /**
   * Move forwards (up) in history.
   *
   * @throws  IOException
   *             Signals an exceptional condition while interacting
   *             with the other side.
   */
  private void moveUp() throws IOException {
    // Note that the history is never empty. It always contains at
    // least the buffer for the current line.
    if (historyIndex == historyHead) {
      // No more entries in history.
      ringBell();

    } else {
      // Adjust history index.
      historyIndex--;
      if (0 > historyIndex) {
        historyIndex = history.length - 1;
      }

      // Clear current buffer from line and move cursor to
      // end-of-line.
      buf.echoClear();
      buf.eol();

      // Set up new buffer and display it.
      buf = history[historyIndex];
      buf.echoAll();
    }
  }

  /**
   * Move backwards (down) in history.
   *
   * @throws  IOException
   *             Signals an exceptional condition while interacting
   *             with the other side.
   */
  private void moveDown() throws IOException {
    // Note that the history is never empty. It always contains at
    // least the buffer for the current line.
    int index = historyIndex;

    index++;
    if (history.length <= index) {
      index = 0;
    }

    if (index == historyTail) {
      // No more entries in history.
      ringBell();

    } else {
      // Clear current buffer from line and move cursor to
      // end-of-line.
      buf.echoClear();
      buf.eol();

      // Set up new buffer and display it.
      buf = history[index];
      buf.echoAll();

      // Adjust history index.
      historyIndex = index;
    }
  }

  /** Trim the history at the current history index. */
  private void trimHistory() {
    int index = historyIndex;

    index++;
    if (history.length <= index) {
      index = 0;
    }

    historyTail = index;
  }

  /**
   * Ring the bell.
   *
   * @throws  IOException
   *             Signals an exceptional condition while interacting
   *             with the other side.
   */
  void ringBell() throws IOException {
    synchronized (lock) {
      out.write(CHAR_BELL);
      out.flush();
    }
  }

  /**
   * Write an array of characters.
   *
   * @see  Writer#write(char[])
   */
  public void write(char[] cbuf) throws IOException {
    synchronized (lock) {
      if (eof) {
        throw new EOFException("Telnet connection closed");
      }

      writer.write(cbuf);
      writer.flush();
    }
  }

  /**
   * Write a portion of an array of characters.
   *
   * @see  Writer#write(char[],int,int)
   */
  public void write(char[] cbuf, int off, int len) throws IOException {
    synchronized (lock) {
      if (eof) {
        throw new EOFException("Telnet connection closed");
      }

      writer.write(cbuf, off, len);
      writer.flush();
    }
  }

  /**
   * Write a character.
   *
   * @see  Writer#write(int)
   */
  public void write(int c) throws IOException {
    synchronized (lock) {
      if (eof) {
        throw new EOFException("Telnet connection closed");
      }

      writer.write(c);
      writer.flush();
    }
  }

  /**
   * Write a string.
   *
   * @see  Writer#write(String)
   */
  public void write(String s) throws IOException {
    synchronized (lock) {
      if (eof) {
        throw new EOFException("Telnet connection closed");
      }

      writer.write(s);
      writer.flush();
    }
  }

  /**
   * Write a portion of a string.
   *
   * @see  Writer#write(String,int,int)
   */
  public void write(String s, int off, int len) throws IOException {
    synchronized (lock) {
      if (eof) {
        throw new EOFException("Telnet connection closed");
      }

      writer.write(s, off, len);
      writer.flush();
    }
  }

  /**
   * Write a new line.
   *
   * @throws   IOException
   *              Signals an exceptional condition while interacting
   *              with the other side.
   */
  public void writeLine() throws IOException {
    synchronized (lock) {
      if (eof) {
        throw new EOFException("Telnet connection closed");
      }
      
      out.write('\r');
      out.write('\n');
      out.flush();
    }
  }

  /**
   * Write a string and then terminate the line.
   *
   * @param   s  The string to write.
   * @throws  IOException
   *             Signal an exceptional condition while interacting
   *             with the other side.
   */
  public void writeLine(String s) throws IOException {
    synchronized (lock) {
      if (eof) {
        throw new EOFException("Telnet connection closed");
      }

      writer.write(s);
      writer.write(CRLF);
      writer.flush();
    }
  }

  /**
   * Close this telnet connection. Closing an already closed
   * connection has no effect.
   */
  public void close() {
    synchronized (lock) {
      if (eof) {
        return;
      }

      // Close connections and socket.
      try {
        in.close();
      } catch (IOException x) {
        // Ignore.
      }
      try {
        out.close();
      } catch (IOException x) {
        // Ignore.
      }
      try {
        socket.close();
      } catch (IOException x) {
        // Ignore.
      }
      
      // Done.
      eof = true;
    }
  }
  

  // =======================================================================
  //                             Main
  // =======================================================================

  /**
   * Run a test telnet server. This method opens a server socket at
   * the default telnet port (23) or at the specified port (if
   * <code>args</code> has a first entry that parses as an integer)
   * and accepts a single telnet connection. It reads line by line
   * from the telnet connection and prints each line to standard
   * output. It terminates on an end-of-file condition. Unprintable
   * characters are printed in escaped decimal form.
   *
   * @param   args  The arguments, the first of which is interpreted
   *                as the port number.
   */
  public static void main(String[] args) {
    int port = PORT_TELNET;

    // Parse optional port argument.
    if ((null != args) && (1 <= args.length)) {
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException x) {
        // Ignore.
      }
    }

    // Accept connection and process it.
    Telnet telnet = null;

    try {
      ServerSocket serverSocket = new ServerSocket(port);
      Socket       socket       = serverSocket.accept();
      serverSocket.close();
      telnet                    = new Telnet(socket, 10);
      String       s;

      // Read password.
      telnet.write("Password: ");
      s = telnet.readPassword();
      if (null == s) {
        System.out.println("Connection closed");
        return;
      } else {
        System.out.println("Password: " + s);
      }
      
      if (telnet.isOpen()) {
        telnet.writeLine();
      }

      while (telnet.isOpen()) {
        // Read line.
        telnet.write("> ");
        s = telnet.readEscapedLine();

        // Process line.
        if (null == s) {
          System.out.println("Connection closed");
          return;

        } else {
          System.out.println("Received: " + s);
          telnet.writeLine();
          telnet.writeLine("I got a line");
        }
      }

    } catch (IOException x) {
      System.out.println("*** " + x);
    } finally {
      if (null != telnet) {
        telnet.close();
      }
    }
  }

}
