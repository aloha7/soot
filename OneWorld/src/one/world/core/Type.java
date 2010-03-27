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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import one.util.Bug;
import one.util.Guid;

/**
 * Implementation of a type. This class defines the type system for
 * tuples, that is, for tuple fields as well as the tuples themselves.
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public final class Type implements java.io.Serializable {

  // =====================================================================
  //                     The tuple information record
  // =====================================================================

  /** Implementation of a tuple information record. */
  static final class Info {

    /** The no-arg constructor. */
    Constructor constructor;

    /** The field map, mapping field names to reflection fields. */
    HashMap     fields;

    /**
     * Create a new information record.
     *
     * @param  constructor  The constructor.
     * @param  fields       The field map.
     */
    Info(Constructor constructor, HashMap fields) {
      this.constructor = constructor;
      this.fields      = fields;
    }
  }


  // =====================================================================
  //                              Constants
  // =====================================================================

  /** The serial version ID for this class. */


  /** The event class. */
  static final Class    CLASS_EVENT                = Event.class;

  /** The throwable class. */
  static final Class    CLASS_THROWABLE            = Throwable.class;

  /** The parameter types for no parameters.*/
  static final Class[]  TYPES_NONE                 = new Class[] { };

  /** The parameter types for an object output stream. */
  static final Class[]  TYPES_OBJECT_OUTPUT_STREAM =
    new Class[] { java.io.ObjectOutputStream.class };

  /** The parameter types for object output. */
  static final Class[]  TYPES_OBJECT_OUTPUT        =
    new Class[] { java.io.ObjectOutput.class };

  /** The parameter types for an object input stream. */
  static final Class[]  TYPES_OBJECT_INPUT_STREAM  =
    new Class[] { java.io.ObjectInputStream.class };

  /** The parameter types for object input. */
  static final Class[]  TYPES_OBJECT_INPUT         =
    new Class[] { java.io.ObjectInput.class };

  /** The parameter types for a string. */
  static final Class[]  TYPES_STRING               =
    new Class[] { String.class };

  /** The parameter types for a string and an object. */
  static final Class[]  TYPES_STRING_OBJECT        =
    new Class[] { String.class, Object.class };

  /** The field descriptor for the ID field. */
  static final Field    FIELD_ID;
  
  /** The field descriptor for the meta-data field. */
  static final Field    FIELD_META_DATA;

  
  // =====================================================================
  //                             Static fields
  // =====================================================================

  /** The lock protecting validTuples. */
  static final Object   lock;

  /**
   * The map of valid tuple classes. This map maps the class of a
   * valid tuple type to a tuple information record.
   */
  static final HashMap  validTuples;

  /** The field mapping for dynamic tuples. */
  static final HashMap  dynamicTupleMap;

  /** The set of valid field types. */
  static final HashSet  validFieldTypes;

  /** Initialize static fields. */
  static {
    lock        = new Object();
    validTuples = new HashMap();

    // Set up tuple in validTuples.
    Class   k = Tuple.class;
    HashMap m = new HashMap();

    try {
      FIELD_ID        = k.getField(Tuple.ID);
      FIELD_META_DATA = k.getField(Tuple.META_DATA);
    } catch (NoSuchFieldException x) {
      throw new Bug("Statically typed fields missing from tuple");
    }

    m.put(Tuple.ID, FIELD_ID);
    m.put(Tuple.META_DATA, FIELD_META_DATA);
    validTuples.put(k, new Info(null, m));

    // Set up dynamic tuple in validTuples.
    Field f;

    k = DynamicTuple.class;
    m = new HashMap();

    try {
      f = k.getField(Tuple.ID);
      m.put(Tuple.ID, f);
      f = k.getField(Tuple.META_DATA);
      m.put(Tuple.META_DATA, f);
      f = k.getField(Tuple.SOURCE);
      m.put(Tuple.SOURCE, f);
      f = k.getField(Tuple.CLOSURE);
      m.put(Tuple.CLOSURE, f);
    } catch (NoSuchFieldException x) {
      throw new Bug("Field missing from DynamicTuple (" + x + ")");
    }
    validTuples.put(k, new Info(null, m));
    dynamicTupleMap = m;

    // Initialized validFieldTypes.
    validFieldTypes = new HashSet();

    validFieldTypes.add(Boolean.TYPE);
    validFieldTypes.add(Boolean.class);
    validFieldTypes.add(Byte.TYPE);
    validFieldTypes.add(Byte.class);
    validFieldTypes.add(Short.TYPE);
    validFieldTypes.add(Short.class);
    validFieldTypes.add(Character.TYPE);
    validFieldTypes.add(Character.class);
    validFieldTypes.add(Integer.TYPE);
    validFieldTypes.add(Integer.class);
    validFieldTypes.add(Long.TYPE);
    validFieldTypes.add(Long.class);
    validFieldTypes.add(Float.TYPE);
    validFieldTypes.add(Float.class);
    validFieldTypes.add(Double.TYPE);
    validFieldTypes.add(Double.class);
    validFieldTypes.add(String.class);
    validFieldTypes.add(java.net.InetAddress.class);
    validFieldTypes.add(Guid.class);
    validFieldTypes.add(Box.class);
  }


  // =====================================================================
  //                                Fields
  // =====================================================================

  // =====================================================================
  //                              Constructors
  // =====================================================================

  private Type() {}


  // =====================================================================
  //                           Tuple validation
  // =====================================================================

  /**
   * Validate the specified tuple type. This method validates that the
   * specified class represents a valid class of tuples. It checks
   * that <code>k</code> itself adheres to the tuple specification. It
   * also checks that all fields of <code>k</code> that are tuples
   * adhere to the tuple specification.
   *
   * @see     Tuple
   *
   * @param   k  The tuple class to validate.
   * @throws  MalformedTupleException
   *             Signals that <code>k</code> is not a valid tuple class.
   */
  public static void validate(final Class k) throws MalformedTupleException {
    // Have we already validated this class?
    boolean valid;

    synchronized (lock) {
      valid = validTuples.containsKey(k);
    }

    if (valid) { return; }

    // Do the work.
    Object r = validateFully(k);
    if (null != r) {
      throw (MalformedTupleException)r;
    }
  }

  /**
   * Fully validate the specified tuple type. This method ensures that
   * the specified class uses default serialization to serialize its
   * state. It also examines all fields of the specified class to
   * verify that they conform to the tuple specification and collects
   * all field descriptors into a hash table. Finally, it validates
   * fields that are also tuples.
   *
   * @param   k         The tuple type to validate fully.
   * @return            <code>null</code> if <code>k</code> is a valid
   *                    tuple type or a malformed tuple exception
   *                    indicating why <code>k</code> is invalid.
   */
  private static MalformedTupleException validateFully(final Class k) {
    // Do the actual validation.
    return (MalformedTupleException)AccessController.doPrivileged(new
      PrivilegedAction() {
        public Object run() {
          return validateFully(k, null);
        }
      });
  }

  /**
   * Fully validate the specified tuple type. This method ensures that
   * the specified class uses default serialization to serialize its
   * state. It also examines all fields of the specified class to
   * verify that they conform to the tuple specification and collects
   * all field descriptors into a hash table. It recursively invokes
   * itself to validate fields that are also tuples. This method is
   * package private to avoid synthetic accessors.
   *
   * @param   k         The tuple type to validate fully.
   * @param   checking  The tuple types being checked under recursive
   *                    invocations of this method, which should be
   *                    <code>null</code> at the top-level.
   * @return            <code>null</code> if <code>k</code> is a valid
   *                    tuple type or a malformed tuple exception
   *                    indicating why <code>k</code> is invalid.
   */
  static MalformedTupleException validateFully(final Class k,
                                               HashSet checking) {
    // Make sure class represents a tuple.
    if (! Tuple.class.isAssignableFrom(k)) {
      return new MalformedTupleException("Not a tuple (" + k + ")");
    }

    // Get the no-arg constructor.
    Constructor constructor;
    try {
      constructor = k.getDeclaredConstructor(TYPES_NONE);
    } catch (NoSuchMethodException x) {
      return new MalformedTupleException("No no-arg constructor (" + k + ")");
    }

    // Make sure it is accessible.
    if (! Modifier.isPublic(k.getModifiers())) {
      constructor.setAccessible(true);
    }

    // The mapping of field names to field descriptors (instance fields only).
    HashMap names = new HashMap();

    // Add field descriptors for ID and meta-data fields.
    names.put(Tuple.ID,        FIELD_ID       );
    names.put(Tuple.META_DATA, FIELD_META_DATA);

    // Walk the chain of superclasses up to Tuple.
    Class klass = k;

    do {
      /*
       * Make sure class has no writeObject(), writeExternal(),
       * writeReplace(), readObject(), readExternal(), or
       * readResolve() method.
       */
      boolean invalid = false;

      try {                                                 // writeObject()
        Method m = klass.getDeclaredMethod("writeObject",
                                           TYPES_OBJECT_OUTPUT_STREAM);
        if (Void.TYPE.equals(m.getReturnType())) {
          invalid = true;
        }
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Serialized through writeObject() ("
                                           + klass +")");
      }

      try {                                                 // writeExternal()
        Method m = klass.getDeclaredMethod("writeExternal",
                                           TYPES_OBJECT_OUTPUT);
        if (Modifier.isPublic(m.getModifiers()) &&
            Void.TYPE.equals(m.getReturnType())) {
          invalid = true;
        }
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Serialized through writeExternal()"
                                           + " (" + klass +")");
      }

      try {                                                 // writeReplace()
        Method m = klass.getDeclaredMethod("writeReplace", TYPES_NONE);
        if (Object.class.equals(m.getReturnType())) {
          invalid = true;
        }
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Serialized through writeReplace() ("
                                           + klass +")");
      }

      try {                                                 // readObject()
        Method m = klass.getDeclaredMethod("readObject",
                                           TYPES_OBJECT_INPUT_STREAM);
        if (Void.TYPE.equals(m.getReturnType())) {
          invalid = true;
        }
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Deserialized through readObject() ("
                                           + klass +")");
      }

      try {                                                 // readExternal()
        Method m = klass.getDeclaredMethod("readExternal",
                                           TYPES_OBJECT_INPUT);
        if (Modifier.isPublic(m.getModifiers()) &&
            Void.TYPE.equals(m.getReturnType())) {
          invalid = true;
        }
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Deserialized through readExternal()"
                                           + " (" + klass +")");
      }

      try {                                                 // readResolve()
        Method m = klass.getDeclaredMethod("readResolve", TYPES_NONE);
        if (Object.class.equals(m.getReturnType())) {
          invalid = true;
        }
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Deserialized through readResolve()"
                                           + " (" + klass +")");
      
      }

      /*
       * Make sure class does not reimplement clone().
       */
      try {
        Method m = klass.getDeclaredMethod("clone", TYPES_NONE);
        invalid  = true;
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Reimplements clone() ("+klass+")");
      }

      /*
       * Make sure class does not reimplement get(), set(),
       * hasField(), getType(), or fields().
       */

      try {
        Method m = klass.getDeclaredMethod("get", TYPES_STRING);
        invalid  = true;
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Reimplements get() ("+klass+")");
      }

      try {
        Method m = klass.getDeclaredMethod("set", TYPES_STRING_OBJECT);
        invalid  = true;
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Reimplements set() ("+klass+")");
      }

      try {
        Method m = klass.getDeclaredMethod("hasField", TYPES_STRING);
        invalid  = true;
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Reimplements hasField() (" + klass
                                           + ")");
      }

      try {
        Method m = klass.getDeclaredMethod("getType", TYPES_STRING);
        invalid  = true;
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Reimplements getType() (" + klass
                                           + ")");
      }

      try {
        Method m = klass.getDeclaredMethod("fields", TYPES_NONE);
        invalid  = true;
      } catch (NoSuchMethodException x) {
        // Nothing to do.
      }
      if (invalid) {
        return new MalformedTupleException("Reimplements fields() (" + klass
                                           + ")");
      }

      // Determine whether the class is public.
      boolean isPublic = Modifier.isPublic(klass.getModifiers());

      // Validate all fields.
      Field[] fields = klass.getDeclaredFields();

      for (int i=0; i<fields.length; i++) {
        Field  f    = fields[i];
        int    mod  = f.getModifiers();
        Class  type = f.getType();
        String name = f.getName();

        if (Modifier.isStatic(mod)) {                 // Static fields.
          if (! Modifier.isFinal(mod)) {
            // Static fields must be final.
            return new MalformedTupleException("Non-final static field (" + f
                                               + ")");

          } else if ((! validFieldTypes.contains(type)) &&
                     (! Object.class.equals(type)) &&
                     (! Class.class.equals(type))) {
            /*
             * Static fields must be of a recognized immutable type.
             *
             * Note that this test also catches a ObjectStreamField[]
             * serialPersistentFields field which remaps fields for
             * serialization.
             */
            return new MalformedTupleException("Invalid type (" + type
                                               + ") for static field (" + f
                                               + ")");
          }

        } else {                                      // Instance fields.

          if (! Modifier.isPublic(mod)) {
            // Instance fields must be public.
            return new MalformedTupleException("Non-public field (" + f + ")");

          } else if (Modifier.isFinal(mod)) {
            // Instance fields must not be final.
            return new MalformedTupleException("Final field (" + f + ")");

          } else if (Modifier.isTransient(mod)) {
            // Instance fields must not be transient.
            return new MalformedTupleException("Transient field (" + f + ")");

          } else if (names.containsKey(name)) {
            // Field names must be unique.
            return new MalformedTupleException("Duplicate field name (" + f
                                               + ")");

          } else if (Tuple.class.isAssignableFrom(type)) {
            // Tuple fields must be well-formed.
            boolean valid;
            
            synchronized (lock) {
              valid = validTuples.containsKey(type);
            }

            if ((! valid) &&
                (! k.equals(type)) &&
                ((null == checking) || (! checking.contains(type)))) {
              // The field type has not been encountered before
              // and it is not currently under consideration.

              // Mark k as currently being checked.
              if (null == checking) {
                checking = new HashSet();
              }
              checking.add(k);

              // Fully validate the field type.
              MalformedTupleException x = validateFully(type, checking);
              if (null != x) {
                return x;
              }
            }
          }

          // Make sure field is accessible.
          if (! isPublic) {
            f.setAccessible(true);
          }

          // Remember field descriptor.
          names.put(name, f);
        }
      }

      klass = klass.getSuperclass();
    } while (! klass.equals(Tuple.class));

    // All done.
    synchronized (lock) {
      validTuples.put(k, new Info(constructor, names));
    }
    return null;
  }

  /**
   * Get the mapping from field names to field descriptors for the
   * specified tuple type. For dynamically typed tuples, this method
   * only returns descriptors for the statically typed ID, meta-data,
   * source, and closure fields.
   *
   * @param   k    The tuple type.
   * @return       The mapping from field names to field descriptors.
   * @throws  Bug  Signals that <code>k</code> is not a tuple or
   *               that <code>k</code> is a malformed tuple.
   */
  static Map getFieldMap(final Class k) {
    Info info;

    synchronized (lock) {
      info = (Info)validTuples.get(k);
    }

    // Did we validate this tuple already?
    if (null == info) {
      MalformedTupleException x = validateFully(k);

      if (null != x) {
        throw new Bug(x.getMessage());
      }

      synchronized (lock) {
        info = (Info)validTuples.get(k);
      }
    }

    // Done.
    return info.fields;
  }

  /**
   * Remove all mappings from the field map for classes loaded by the
   * specified class loader.
   *
   * @param   loader  The class loader.
   */
  static void clean(final ClassLoader loader) {
    if (null == loader) {
      return;
    }

    // Iterate over the classes and remove those with a matching class
    // loader.
    synchronized (lock) {
      Iterator iter = validTuples.keySet().iterator();

      while (iter.hasNext()) {
        Class k = (Class)iter.next();

        if (k.getClassLoader().equals(loader)) {
          iter.remove();
        }
      }
    }
  }

}
