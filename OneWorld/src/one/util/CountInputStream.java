package one.util;
import java.io.IOException;
import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * An input stream class which can be used to wrap another
 * input stream, reporting the number of bytes passed through it.
 * This class does not support <code>reset()</code> and  will through a
 * <code>one.util.Bug</code> if <code>reset()</code> is called.
 *
 * bytes skipped using <code>skip()</code> are counted as read.  
 *
 * <p>NOTE: this uses static accessors, so is not a good general 
 *          purpose class.</p>
 */

public class CountInputStream extends FilterInputStream {
  /** 
   * The number of bytes read.
   */
  static int bytes;

  /**
   * A lock to serialize access to the bytes counter.
   */
  static Object lock = new Object();

  /**
   * Return the number of bytes read since the last clear.
   */
  public static int getReadCount() {
    synchronized(lock) {
      return bytes;
    }
  }

  /**
   * Attomically return the number of bytes read and clear
   * the read counter.
   */
  public static int clearReadCount() {
    int oldbytes;
    synchronized(lock) {
      oldbytes=bytes;
      bytes = 0;
    }
    return oldbytes;
  }

  
  public int read() throws IOException {
    int val;

    val = in.read();
    if (val == -1) {
      return -1;
    }
    synchronized(lock) {
      bytes++;
    }
    return val;
  }

  public int read(byte[] b) throws IOException {
    int ret;

    ret = in.read(b);
    if (ret == -1) {
      return -1;
    }
    synchronized(lock) {
      bytes+=ret;
    }
    return ret;
  }


  public int read(byte[] b,int off,int len) throws IOException {
    int ret;

    ret = in.read(b,off,len);
    if (ret == -1) {
      return -1;
    }
    synchronized(lock) {
      bytes+=ret;
    }
    return ret;
  }

  public long skip(long n) {
    long ret;

    ret = skip(n);
    if (ret == -1) {
      return -1;
    }
    synchronized(lock) {
      bytes+=ret;
    }
    return ret;
  } 

  /**
   * Not supported: will throw a {@link Bug} if called.
   */
  public void reset() {
    throw new one.util.Bug("Does not support reset");
  }

  /**
   * Create a new CountInputStream wrapping the given InputStream.
   *
   * @param in The InputStream to wrap.
   */
  public CountInputStream(InputStream in) {
    super(in);
  }
}
