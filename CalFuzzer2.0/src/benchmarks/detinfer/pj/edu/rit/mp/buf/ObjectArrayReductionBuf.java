//******************************************************************************
//
// File:    ObjectArrayReductionBuf.java
// Package: benchmarks.detinfer.pj.edu.rit.mp.buf
// Unit:    Class benchmarks.detinfer.pj.edu.rit.mp.buf.ObjectArrayReductionBuf
//
// This Java source file is copyright (C) 2008 by Alan Kaminsky. All rights
// reserved. For further information, contact the author, Alan Kaminsky, at
// ark@cs.rit.edu.
//
// This Java source file is part of the Parallel Java Library ("PJ"). PJ is free
// software; you can redistribute it and/or modify it under the terms of the GNU
// General Public License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// PJ is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// A copy of the GNU General Public License is provided in the file gpl.txt. You
// may also obtain a copy of the GNU General Public License on the World Wide
// Web at http://www.gnu.org/licenses/gpl.html.
//
//******************************************************************************

package benchmarks.detinfer.pj.edu.rit.mp.buf;

import benchmarks.detinfer.pj.edu.rit.mp.Buf;
import benchmarks.detinfer.pj.edu.rit.mp.ObjectBuf;

import benchmarks.detinfer.pj.edu.rit.pj.reduction.ObjectOp;
import benchmarks.detinfer.pj.edu.rit.pj.reduction.Op;
import benchmarks.detinfer.pj.edu.rit.pj.reduction.ReduceArrays;

import benchmarks.detinfer.pj.edu.rit.util.Range;

/**
 * Class ObjectArrayReductionBuf provides a reduction buffer for class
 * {@linkplain ObjectArrayBuf}.
 *
 * @param  <T>  Data type of the objects in the buffer.
 *
 * @author  Alan Kaminsky
 * @version 12-Feb-2008
 */
class ObjectArrayReductionBuf<T>
	extends ObjectArrayBuf<T>
	{

// Hidden data members.

	ObjectOp<T> myOp;

// Exported constructors.

	/**
	 * Construct a new object array reduction buffer.
	 *
	 * @param  theArray  Array.
	 * @param  theRange  Range of array elements to include in the buffer.
	 * @param  op        Binary operation.
	 *
	 * @exception  NullPointerException
	 *     (unchecked exception) Thrown if <TT>op</TT> is null.
	 */
	public ObjectArrayReductionBuf
		(T[] theArray,
		 Range theRange,
		 ObjectOp<T> op)
		{
		super (theArray, theRange);
		if (op == null)
			{
			throw new NullPointerException
				("ObjectArrayReductionBuf(): op is null");
			}
		myOp = op;
		}

// Exported operations.

	/**
	 * Store the given item in this buffer.
	 * <P>
	 * The <TT>put()</TT> method must not block the calling thread; if it does,
	 * all message I/O in MP will be blocked.
	 *
	 * @param  i     Item index in the range 0 .. <TT>length()</TT>-1.
	 * @param  item  Item to be stored at index <TT>i</TT>.
	 */
	public void put
		(int i,
		 T item)
		{
		int off = myArrayOffset + i * myStride;
		myArray[off] = myOp.op (myArray[off], item);
		mySerializedItems = null;
		}

	/**
	 * Copy items from the given buffer to this buffer. The number of items
	 * copied is this buffer's length or <TT>theSrc</TT>'s length, whichever is
	 * smaller. If <TT>theSrc</TT> is this buffer, the <TT>copy()</TT> method
	 * does nothing.
	 *
	 * @param  theSrc  Source of items to copy into this buffer.
	 *
	 * @exception  ClassCastException
	 *     (unchecked exception) Thrown if <TT>theSrc</TT>'s item data type is
	 *     not the same as this buffer's item data type.
	 */
	public void copy
		(Buf theSrc)
		{
		if (theSrc == this)
			{
			}
		else if (theSrc instanceof ObjectArrayBuf)
			{
			ObjectArrayBuf<T> src = (ObjectArrayBuf<T>) theSrc;
			ReduceArrays.reduce
				(src.myArray, src.myRange, this.myArray, this.myRange, myOp);
			mySerializedItems = null;
			}
		else
			{
			ObjectBuf.defaultCopy ((ObjectBuf<T>) theSrc, this);
			mySerializedItems = null;
			}
		}

	/**
	 * Create a buffer for performing parallel reduction using the given binary
	 * operation. The results of the reduction are placed into this buffer.
	 *
	 * @param  op  Binary operation.
	 *
	 * @exception  ClassCastException
	 *     (unchecked exception) Thrown if this buffer's element data type and
	 *     the given binary operation's argument data type are not the same.
	 */
	public Buf getReductionBuf
		(Op op)
		{
		throw new UnsupportedOperationException();
		}

	}
