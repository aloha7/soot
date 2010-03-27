/*
 * Copyright (c) 2001, University of Washington, Department of
 * Computer Science and Engineering.
 * All rights reserved.
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

package one.world.core;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.lang.reflect.Field;

import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import one.util.Bug;
import one.util.Guid;

import one.world.util.NullHandler;

/**
 * Implementation of a dynamically typed tuple. A dynamically typed
 * tuple implements a mapping from field names to values. Fields can
 * be dynamically added and removed as well as modified.
 *
 * <p>Note that access to the dynamic fields of a dynamic tuple is
 * <i>not</i> synchronized. Concurrenct accesses <i>must</i> be
 * synchronized externally.</p>
 *
 * @see      Tuple
 *
 * @version  $Revision: 1.14 $
 * @author   Robert Grimm
 */
public final class DynamicTuple extends Event {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -6846564712876331247L;

  /** The field map. */
  transient HashMap map;

  /**
   * Create a new, empty dynamically typed tuple. This constructor
   * sets source field of the newly created dynamic tuple to the
   * canonical null handler, so that the new dynamic tuple validates.
   *
   * @see  NullHandler
   */
  public DynamicTuple() {
    super(NullHandler.NULL, null);
    map    = new HashMap();
  }

  /**
   * Create a new dynamically typed tuple with the specified source
   * and closure.
   *
   * @param   source   The source for the new dynamically typed tuple.
   * @param   closure  The closure for the new dynamically typed tuple.
   */
  public DynamicTuple(EventHandler source, Object closure) {
    super(source, closure);
    map = new HashMap();
  }

  /**
   * Serialize this dynamic tuple.
   *
   * @serialData  The number of field name, value mappings as an
   *              <code>int</code>, followed by as many
   *              <code>String</code>, <code>Object</code> pairs
   *              in no particular order.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    // Write id, source, closure.
    out.defaultWriteObject();

    // Write dynamic fields.
    Set keySet = map.keySet();
    int size   = keySet.size();

    out.writeInt(size);

    Iterator keys = keySet.iterator();
    while(keys.hasNext()) {
      Object name = keys.next();
      
      out.writeObject(name);
      out.writeObject(map.get(name));
    }
  }

  /** Deserialize a dynamic tuple. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // Create new map...
    map = new HashMap();

    // ...and repopulate it.
    int n = in.readInt();

    for (int i=0; i<n; i++) {
      String name  = (String)in.readObject();
      Object value = in.readObject();

      map.put(name, value);
    }
  }

  /** Make a shallow copy of this dynamic tuple. */
  public Object clone() {
    DynamicTuple dt = (DynamicTuple)super.clone();
    dt.map          = (HashMap)map.clone();

    return dt;
  }

  /** Get the value of the specified field. */
  public Object get(final String name) {
    Field f = (Field)Type.dynamicTupleMap.get(name);

    if (null == f) {
      return map.get(name);
    } else {
      try {
        return f.get(this);
      } catch (IllegalAccessException x) {
        throw new Bug("Access to field (" + f + ") denied");
      }
    }
  }

  /**
   * Get the value of the specified field. This method returns the
   * value of the specified field, if the field exists and if its
   * value has the specified type.
   *
   * <p>Note that this method cannot be used to access the four
   * statically typed fields &#150; <code>id</code>,
   * <code>metaData</code>, <code>source</code>, and
   * <code>closure</code> &#150; of a dynamic tuple.</p>
   *
   * @param   name       The name of the field.
   * @param   k          The required type for the field's value.
   * @param   allowNull  Flag for whether to allow <code>null</code>
   *                     values.
   * @throws  IllegalArgumentException
   *                     Signals that this dynamic tuple does not have
   *                     a field with the specified name, that the
   *                     field's value does not have the specified
   *                     type, or that the field's value is
   *                     <code>null</code> and <code>allowNull</code>
   *                     is <code>false</code>.
   */
  public final Object get(final String name, final Class k,
                          final boolean allowNull) {
    Object o = map.get(name);

    if (null == o) {
      if (map.containsKey(name)) {
        if (allowNull) {
          return o;
        } else {
          throw new IllegalArgumentException("Field "+name+" has null value");
        }

      } else {
        throw new IllegalArgumentException("No such field (" + name + ")");
      }

    } else {
      if (k.isInstance(o)) {
        return o;
      } else {
        throw new IllegalArgumentException("Value of field "+name+" not a "+
                                           k.getName());
      }
    }
  }

  /** Set the specified field to the specified value. */
  public void set(final String name, final Object value) {
    Field f = (Field)Type.dynamicTupleMap.get(name);

    if (null == f) {
      map.put(name, value);
    } else {
      try {
        f.set(this, value);
      } catch (IllegalAccessException x) {
        throw new Bug("Access to field (" + f + ") denied");
      }
    }
  }

  /** Determine whether this tuple has a field with the specified name. */
  public boolean hasField(final String name) {
    if (Type.dynamicTupleMap.containsKey(name)) {
      return true;
    } else {
      return map.containsKey(name);
    }
  }

  /** Get the declared type of the field with the specified name. */
  public Class getType(final String name) {
    Field f = (Field)Type.dynamicTupleMap.get(name);

    if (null == f) {
      if (map.containsKey(name)) {
        return Object.class;
      } else {
        return null;
      }
    } else {
      return f.getType();
    }
  }

  /** Get a list of this tuple's field names. */
  public List fields() {
    // First, the names of the dynamically types fields.
    List l = new ArrayList(map.keySet());

    // Then, add in the names of the statically typed fields.
    l.add(Tuple.ID);
    l.add(Tuple.META_DATA);
    l.add(Tuple.SOURCE);
    l.add(Tuple.CLOSURE);

    // Done.
    return l;
  }

  /** Validate this dynamic tuple. */
  public void validate() throws TupleException {
    super.validate();

    Iterator iter = map.values().iterator();

    while (iter.hasNext()) {
      Object o = iter.next();

      if (o instanceof Tuple) {
        ((Tuple)o).validate();
      }
    }
  }

  /** Get a string representation for this dynamic tuple. */
  public String toString() {
    return "#[DynamicTuple " + id + "]";
  }

}
