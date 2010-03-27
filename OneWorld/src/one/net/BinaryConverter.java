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

import java.util.Iterator;
import java.util.NoSuchElementException;

import one.util.BufferOutputStream;
import one.util.Guid;

import one.world.Constants;

import one.world.core.Tuple;
import one.world.core.TupleException;

import one.world.data.BinaryData;
import one.world.data.Chunk;

/**
 * Implementation of a binary converter. A binary conterter converts
 * between arbitrary MIME types and binary data tuples. If the binary
 * data is smaller than {@link Constants#CHUNKING_THRESHOLD} when
 * converting to a tuple, this converter produces a single binary data
 * tuple. Otherwise, it returns a list of chunks. Note that this
 * converter does not convert any subtypes of <code>BinaryData</code>
 * back to streams.
 *
 * @see  BinaryData
 * @see  Chunk
 *
 * @version  $Revision: 1.8 $
 * @author   Robert Grimm
 */
public final class BinaryConverter implements Converter {

  /** The array of tuple types. */
  private Class[]  tupleTypes;

  /** The array of MIME types. */
  private String[] mimeTypes;

  /** Create a new binary converter. */
  public BinaryConverter() {
    tupleTypes = new Class[]  { BinaryData.class };
    mimeTypes  = new String[] { ""               };
  }

  /**
   * Get the types of tuples this converter can convert to binary
   * data, which are binary data tuples.
   */
  public Class[] getTupleTypes() {
    return tupleTypes;
  }

  /**
   * Determine whether this converter can convert subtypes of the
   * specified tuple types, which it does.
   */
  public boolean convertsSubtypes() {
    return true;
  }

  /**
   * Get the MIME types this converter can convert to tuples, which
   * are all MIME types.
   */
  public String[] getMimeTypes() {
    return mimeTypes;
  }

  /** Determine whether the specified tuple is relative. */
  public boolean isRelative(Tuple t) throws TupleException {
    if (null == t) {
      throw new NullPointerException("Null tuple");
    } else if (! (t instanceof BinaryData)) {
      throw new IllegalArgumentException("Not a binary data tuple ("+t+")");
    }

    t.validate();

    if (t instanceof Chunk) {
      return (null != ((Chunk)t).previous);
    } else {
      return false;
    }
  }

  /** Size the specified tuple. */
  public long size(Tuple t) throws TupleException {
    if (null == t) {
      throw new NullPointerException("Null tuple");
    } else if (! (t instanceof BinaryData)) {
      throw new IllegalArgumentException("Not a binary data tuple ("+t+")");
    }

    t.validate();

    if (t instanceof Chunk) {
      return ((Chunk)t).length;
    } else {
      return ((BinaryData)t).data.length;
    }
  }

  /** Type the specified tuple. */
  public String type(Tuple t) throws TupleException {
    if (null == t) {
      throw new NullPointerException("Null tuple");
    } else if (! (t instanceof BinaryData)) {
      throw new IllegalArgumentException("Not a binary data tuple ("+t+")");
    }

    return ((BinaryData)t).type;
  }

  /**
   * Convert the specified tuple to binary data, writing the resulting
   * data to the specified output stream.
   */
  public String convert(Tuple t, OutputStream out, Converter.TupleReader reader)
    throws TupleException, IOException {

    // Consistency checks.
    if (null == t) {
      throw new NullPointerException("Null tuple");
    } else if (null == out) {
      throw new NullPointerException("Null output stream");
    } else if (! (t instanceof BinaryData)) {
      throw new IllegalArgumentException("Not a binary data tuple (" + t + ")");
    }

    t.validate();

    if (t instanceof Chunk) {
      Chunk  chunk = (Chunk)t;
      String type  = chunk.type;

      if (null != chunk.previous) {
        throw new IllegalArgumentException("Chunk not first chunk in list of " +
                                           "chunks (" + chunk + ")");
      }

      do {
        // Write the data.
        out.write(chunk.data);

        if (null == chunk.next) {
          // We are done.
          break;
        }

        // Get the next chunk.
        t = reader.read(chunk.next);

        // Consistency checks.
        if (null == t) {
          throw new IllegalStateException("Chunk " + chunk.next + " missing");
        } else if (! (t instanceof Chunk)) {
          throw new IllegalStateException("Tuple with ID " + chunk.next +
                                          " not a chunk (" + t + ")");
        }

        chunk = (Chunk)t;
      } while (true);

      out.flush();
      return type;

    } else {
      BinaryData bd = (BinaryData)t;
      
      out.write(bd.data);
      out.flush();
      return bd.type;
    }
  }

  /**
   * Convert the data accessible through the specified input stream to
   * tuple(s).
   */
  public Object convert(final InputStream in, final long length,
                        final String name, final String mimeType)
    throws IOException {

    // Consistency checks.
    if (null == in) {
      throw new NullPointerException("Null input stream");
    } else if (null == mimeType) {
      throw new NullPointerException("Null MIME type");
    } else if (0 > length) {
      throw new IllegalArgumentException("Negative length (" + length + ")");
    }

    if (Constants.CHUNKING_THRESHOLD > length) {
      // Pump the bytes from the input stream into a buffer and create a
      // new binary data tuple with that buffer.
      BufferOutputStream out    = new BufferOutputStream((int)length);
      long               pumped = NetUtilities.pump(in, out, length);
      
      if (pumped != length) {
        throw new EOFException("Unexpected end-of-file after " + pumped + 
                               " bytes");
      } else {
        BinaryData bd = new BinaryData(null, mimeType, out.getBytes());
        
        // Patch the name.
        if (null == name) {
          bd.name = "Binary data";
        } else {
          bd.name = name;
        }

        return bd;
      }

    } else {
      return new Iterator() {
          final String actualName    = ((null == name)? "Binary data" : name);
          long         toGo          = length;
          Guid         previousChunk = null;
          Guid         nextChunk     = new Guid();

          /** Determine whether we have another chunk. */
          public boolean hasNext() {
            return (0 < toGo);
          }

          /** Get the next chunk. */
          public Object next() {
            // Do we have data for another chunk?
            if (0 >= toGo) {
              throw new NoSuchElementException("No more tuples for binary " +
                                               "conversion");
            }

            // Determine the number of bytes for the next chunk.
            final int pump   = ((Constants.CHUNKING_THRESHOLD > toGo) ?
                                (int)toGo : Constants.CHUNKING_THRESHOLD);
            final int pumped;

            // Create the byte array.
            BufferOutputStream out = new BufferOutputStream(pump);
            try {
              pumped = (int)NetUtilities.pump(in, out, pump);
            } catch (IOException x) {
              throw new IllegalStateException(x.toString());
            }

            if (pumped != pump) {
              throw new IllegalStateException("Unexpected end-of-file after " +
                                              pumped + " bytes");
            } else {
              // Create the new chunk.
              Guid currentChunk  = nextChunk;
              nextChunk          = ((Constants.CHUNKING_THRESHOLD == pump)?
                                    new Guid() : null);
              Chunk chunk        = new
                Chunk(currentChunk, actualName, mimeType, out.getBytes(),
                      previousChunk, nextChunk, length);
              previousChunk      = currentChunk;
              toGo              -= pump;

              // Done.
              return chunk;
            }
          }

          /** Remove the current chunk, which is impossible. */
          public void remove() {
            throw new
              UnsupportedOperationException("Unable to remove tuple while " +
                                            "converting binary data");
          }
        };
    }
  }

}
