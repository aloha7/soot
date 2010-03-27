package one.util;
import java.io.OutputStream;
import java.io.IOException;

/**
 * An output stream class which can be used to wrap another output stream,
 * reporting the number of bytes passing through it.  
 * 
 * <p>NOTE: this uses static accessors, so is not a good general purpose
 *          class</p>.
 */
public class CountOutputStream extends OutputStream {
  /** The wrapped OutputStream */
  private OutputStream wrapped;

  /** The number of bytes written. */
  static int bytes;

  /** A lock to serialize access to the bytes variable */
  static Object lock = new Object();

  /**
   * Return the number of bytes written since the last clear.
   *
   * @return The number of bytes written.
   */
  public static int getWriteCount() {
    synchronized(lock) {
      return bytes;
    }
  }

  /**
   * Return the number of bytes written siince the last clear and
   * atomically clear the bytes counter.
   *
   * @return The number of bytes written.
   */
  public static int clearWriteCount() {
    int oldbytes;
    synchronized(lock) {
      oldbytes=bytes;
      bytes = 0;
    }
    return oldbytes;
  }

  public void flush() throws IOException{
    wrapped.flush();
  }

  public void write(byte[] b,int off, int len) throws IOException {
    synchronized(lock) {
      bytes+=len;  
    }
    wrapped.write(b,off,len);
  }
  
  public void write(int b) throws IOException {
    synchronized(lock) {
      bytes++;
    }
    wrapped.write(b);
  }

  /**
   * Create a new CountOutputStream wrapping the specified OutputStream.
   *
   * @param wrapped The OutputStream to be wrapped.
   */
  public CountOutputStream(OutputStream wrapped) {
    this.wrapped = wrapped;
  }

  public void close() throws IOException{
    wrapped.close();
  }
}
