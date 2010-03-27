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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import one.util.Guid;

import one.world.core.Tuple;
import one.world.core.TupleException;

/**
 * Definition of a converter. A converter converts between tuples and
 * binary streams backed by, for example, files.
 *
 * @see      Tuple
 *
 * @version  $Revision: 1.8 $
 * @author   Robert Grimm
 */
public interface Converter {

  /**
   * Definition of a tuple reader. A tuple reader provides a call-back
   * when converting a tuple. A converter can thus access additional
   * tuples that are symbolically referenced by the tuple being
   * converted.
   *
   * @see Converter#convert(Tuple,OutputStream,Converter.TupleReader)
   */
  public interface TupleReader {

    /**
     * Read the tuple with the specified ID.
     *
     * @param   id  The ID of the tuple to read.
     * @return      The tuple with the specified ID or <code>null</code>
     *              if no such tuple exists.
     * @throws  IOException
     *              Signals an exceptional condition while reading
     *              the tuple with the specified ID.
     */
    Tuple read(Guid id) throws IOException;

  }

  /**
   * Get the types of tuples this converter can convert to binary
   * data. The returned types must be valid tuple types.
   *
   * @return  The types of tuples this converter can convert, or
   *          an empty array if it cannot convert tuples to binary
   *          data. 
   */
  Class[] getTupleTypes();

  /**
   * Determine whether this converter can convert subtypes of the
   * specified tuple types.
   *
   * @return  <code>true</code> if this converter can convert subtypes
   *          of the tuple types returned by {@link #getTupleTypes()}.
   */
  boolean convertsSubtypes();

  /**
   * Get the MIME types this converter can convert to tuples. Each
   * MIME type can be just a MIME type, such as "<code>text</code>",
   * or a MIME type and subtype, such as "<code>text/xml</code>".
   * Alternatively, if this converter can convert all MIME types to
   * tuples, this method returns an array with the empty string "" as
   * its only entry.
   *
   * @return  The MIME types this converter can convert, or an empty
   *          array if it cannot convert binary data to tuples.
   */
  String[] getMimeTypes();

  /**
   * Determine whether this tuple is relative. A tuple is relative to
   * some other tuple if it is symbolically referenced by some other
   * tuple. For example, a {@link one.world.data.Chunk} whose
   * <code>previous</code> field is not <code>null</code> is relative,
   * while a chunk whose <code>previous</code> field is
   * <code>null</code> is not relative.
   *
   * @param   t    The tuple to test.
   * @return       <code>true</code> if the specified tuple is
   *               relative to some other tuple.
   * @throws  NullPointerException
   *               Signals that <code>t</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *               Signals that the specified tuple does not have a
   *               tuple type supported by this converter.
   * @throws  TupleException
   *               Signals that the specified tuple is malformed.
   */
  boolean isRelative(Tuple t) throws TupleException;

  /**
   * Size the binary data for the specified tuple. An implementation
   * of this method should return the size of the binary data if the
   * converter would convert the tuple. However, if it cannot
   * determine the size without performing the actual conversion, it
   * must return -1.
   *
   * @param   t    The tuple to size.
   * @return       The size of the binary data or -1 if the size cannot
   *               be determined without actually converting the tuple.
   * @throws  NullPointerException
   *               Signals that <code>t</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *               Signals that the specified tuple does not have a
   *               tuple type supported by this converter.
   * @throws  TupleException
   *               Signals that the specified tuple is malformed.
   */
  long size(Tuple t) throws TupleException;

  /**
   * Determine the MIME type of the binary data for the specified
   * tuple. An implementation of this method should return the MIME
   * type of the binary data if the converter would convert the
   * tuple. However, if it cannot determine the MIME type without
   * performing the actual conversion, it must return
   * <code>null</code>.
   *
   * @param   t    The tuple to type.
   * @return       The MIME type of the binary data or
   *               <code>null</code> if the type cannot be determined
   *               without actually converting the tuple.
   * @throws  NullPointerException
   *               Signals that <code>t</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *               Signals that the specified tuple does not have a
   *               tuple type supported by this converter.
   * @throws  TupleException
   *               Signals that the specified tuple is malformed.
   */
  String type(Tuple t) throws TupleException;

  /**
   * Convert the specified tuple to binary data, writing the resulting
   * data to the specified output stream. An implementation of this
   * method is expected to flush the output stream before returning.
   *
   * @see      Converter.TupleReader
   *
   * @param    t       The tuple to convert.
   * @param    out     The output stream for the binary data.
   * @param    reader  The tuple reader for accessing tuples that are
   *                   symbolically referenced by the specified tuple.
   * @return           The MIME type for the binary data.
   * @throws   NullPointerException
   *                   Signals that <code>t</code> or <code>out</code>
   *                   is <code>null</code>.
   * @throws   IllegalArgumentException
   *                   Signals that the specified tuple does not have a
   *                   tuple type supported by this converter, or that
   *                   the specified tuple is relative to some other
   *                   tuple.
   * @throws   IllegalStateException
   *                   Signals that a symbolically referenced tuple
   *                   does not exist or has the wrong type.
   * @throws   TupleException
   *                   Signals that the specified tuple is malformed.
   * @throws   IOException
   *                   Signals an exceptional condition while accessing
   *                   the specified output stream or while reading
   *                   another tuple.
   */
  String convert(Tuple t, OutputStream out, Converter.TupleReader reader)
    throws TupleException, IOException;

  /**
   * Convert the data accessible through the specified input stream to
   * one or more tuples.
   *
   * <p>The name passed to this method is the name of the binary data,
   * if such a name is available. For example, when converting a file
   * into a tuple, the name is the name of the file, without any path
   * information but with the file name extension.</p>
   *
   * <p>The returned object must either be a valid tuple or a
   * <code>java.util.Iterator</code> over one or more valid tuples. If
   * the returned object is an iterator over tuples, the caller of
   * this method can only close the specified input stream after the
   * last tuple has been returned from the iterator (so that tuples
   * can be constructed on demand). Exceptional conditions while
   * lazily constructing a tuple need to be signaled through an
   * illegal state exception on invocation of the iterator's
   * <code>next()</code> method.</p>
   *
   * @param   in        The input stream for the binary data.
   * @param   length    The number of bytes in the input stream.
   * @param   name      The name for the binary data or <code>null</code>
   *                    if no name is available.
   * @param   mimeType  The MIME type for the binary data.
   * @return            The corresponding tuple(s).
   * @throws  NullPointerException
   *                    Signals that <code>in</code> or
   *                    <code>mimeType</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                    Signals that <code>length</code> is a negative
   *                    number, that this converter cannot convert
   *                    binary data of the specified length, or that
   *                    the specified MIME type is not supported by
   *                    this converter.
   * @throws  IOException
   *                    Signals an exceptional condition while
   *                    accessing the specified input stream or that
   *                    the binary data contained in the stream is
   *                    malformed for the specified MIME type.
   * @throws  ClassNotFoundException
   *                    Signals that a class required during
   *                    conversion could not be found.
   */
  Object convert(InputStream in, long length, String name, String mimeType)
    throws IOException, ClassNotFoundException;

}
