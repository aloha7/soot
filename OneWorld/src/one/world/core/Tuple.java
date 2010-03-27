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

import java.io.ObjectStreamClass;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import one.util.Bug;
import one.util.Guid;

import one.world.io.Query;

import one.world.rep.DiscoveredResource;
import one.world.rep.NamedResource;
import one.world.rep.RemoteReference;

import one.world.util.NullHandler;

/**
 * Abstract base class for tuples. Tuples are the core data
 * structures. They can either be statically typed or dynamically
 * typed.
 *
 * <p><b>Statically Typed Tuples</b></p>
 *
 * <p>Statically typed tuples are Java objects that directly or
 * indirectly inherit from this base class. The data for statically
 * typed tuples is provided by their public, non-static, non-final,
 * non-transient fields. Tuples must not have any non-public, final,
 * or transient fields. Furthermore, subclasses of a tuple may not
 * introduce any instance fields that have the same name as a field of
 * a superclass.</p>
 *
 * <p>The types for non-static fields are restricted. They can be of
 * any primitive Java type, the boxed version of a primitive types, a
 * string, a {@link Class}, a {@link Throwable}, a {@link Guid}, an
 * {@link java.net.InetAddress}, an event handler, a tuple, a {@link
 * Box} containing an arbitrary serialized object, or a
 * one-dimensional array of one of the previously listed types. When
 * declaring a field to be some event handler, that field should not
 * be declared to be a specific event handler (unless the event
 * handler is a {@link SymbolicHandler}) but always an {@link
 * EventHandler}. Otherwise, the tuple cannot be passed between
 * concurrency and protection domains, because event handlers are
 * wrapped when being passed across concurrency and protection
 * domains.</p>
 *
 * <p>Note that the field type restrictions are currently only
 * enforced when passing tuples between protection domains. This may
 * change in a future release of <i>one.world</i>.</p>
 *
 * <p>Statically typed tuples must not have any static fields, unless
 * they are final and of a primitive type, the boxed version of a
 * primitive type, a string, a class, a GUID, an Internet address, a
 * box, or an object (but not a subclass of it).</p>
 *
 * <p>Serialization for statically typed tuples must be
 * straight-forward. In other words, statically typed tuples must not
 * have a <code>serialPersistentFields</code> field and must not
 * implement a <code>writeObject()</code>,
 * <code>writeExternal()</code>, <code>writeReplace()</code>,
 * <code>readObject()</code>, <code>readExternal()</code>, or
 * <code>readResolve</code> method.</p>
 *
 * <p><b>Dynamically Typed Tuples</b></p>
 *
 * <p>Dynamically typed tuples are instances of class {@link
 * DynamicTuple}. They implement a mapping from field names to values.
 * Mappings can be dynamically added and removed from a dynamic tuple,
 * and values are dynamically typed.
 *
 * <p><b>Required Fields</b></p>
 *
 * <p>By subclassing from this class, all tuples have a public,
 * non-transient, non-static field named "<code>id</code>" of type
 * {@link one.util.Guid}, which specifies the ID of the tuple, and a
 * public, non-transient, non-static field named
 * "<code>metaData</code>" of type {@link DynamicTuple}, which
 * specifies the meta-data for the tuple. All tuples (must) also have
 * a public no-argument constructor.</p>
 *
 * <p>Since dynamically typed tuples are also {@link Event events},
 * they have four statically typed fields, for the ID
 * (<code>id</code>), the meta-data (<code>metaData</code>), the
 * source (<code>source</code>), and the closure
 * (<code>closure</code>).</p>
 *
 * <p><b>Accessing Tuple Fields</b></p>
 *
 * <p>Statically and dynamically typed tuples alike implement the same
 * uniform operations to access their fields. Though, the fields of
 * statically typed tuples can be accessed directly as well.</p>
 *
 * <p><b>Remarks</b></p>
 *
 * <p>The methods implemented by this class automatically work for all
 * subclasses that implement valid tuples. If a subclass does not
 * follow the tuple specification, invoking any of the methods defined
 * by this class will result in a {@link Bug}.</p>
 *
 * <p>Even though the {@link #get(String)}, {@link
 * #set(String,Object)}, {@link #hasField(String)}, {@link
 * #getType(String)}, and {@link #fields()} methods are not declared
 * to be final (since the implementation of dynamic tuple, for
 * example, needs to reimplement them), they must not be overriden by
 * statically typed tuples. Statically typed tuples that override, for
 * example, the <code>get()</code> method defined by this class do not
 * validate.</p>
 *
 * <p>Note that even if the class of a tuple is declared to not be
 * public, all its fields are still accessible through the methods
 * implemented by this class. There is no way to restrict access to
 * the fields of a tuple, besides not passing it out.</p>
 *
 * @version  $Revision: 1.27 $
 * @author   Robert Grimm
 */
public abstract class Tuple implements Cloneable, java.io.Serializable {

  // =====================================================================
  //                              Constants
  // =====================================================================

  /** The serial version ID for this class. */
  static final long serialVersionUID = -2311385468198189841L;

  /** The name of the ID field. */
  public static final String ID        = "id";

  /** The name of the meta-data field. */
  public static final String META_DATA = "metaData";

  /** The name of the source field. */
  public static final String SOURCE    = "source";

  /** The name of the closure field. */
  public static final String CLOSURE   = "closure";

  /** The empty object array. */
  static final Object[]      ARGS_NONE = new Object[] { };


  // =====================================================================
  //                                Fields
  // =====================================================================

  /**
   * The ID for this tuple.
   *
   * @serial  Must not be <code>null</code>.
   */
  public Guid         id;

  /**
   * The meta-data for this tuple. The fields of the dynamic tuple
   * should be named using the same naming conventions as those used
   * for Java package names or system properties.
   *
   * @serial
   */
  public DynamicTuple metaData;


  // =====================================================================
  //                              Constructors
  // =====================================================================

  /**
   * Create a new tuple. This constructor creates a new tuple with a
   * freshly created ID.
   */
  public Tuple() {
    id = new Guid();
  }

  /**
   * Create a new tuple. This constructor creates a new tuple with the
   * specified ID.
   *
   * @param  id  The ID for the new tuple.
   */
  public Tuple(Guid id) {
    this.id = id;
  }


  // =====================================================================
  //                              Cloning
  // =====================================================================

  /**
   * Make a shallow copy of this tuple. The copy contains the same
   * values as this tuple, including its ID and its meta-data.
   *
   * @return  A shallow copy of this tuple.
   */
  public Object clone() {
    Tuple t;
    
    try {
      t = (Tuple)super.clone();
    } catch (CloneNotSupportedException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    }

    return t;
  }


  // =====================================================================
  //                              Projection
  // =====================================================================

  /**
   * Project the specified event into the projection domain with the
   * specified class loader. This method copies mutable values (tuples
   * and arrays) using the corresponding classes of the specified
   * class loader. Immutable values and non-symbolic event handlers
   * are not copied.
   *
   * @param   e       The event to project.
   * @param   loader  The class loader to use for the projection.
   * @param   prot    The ID of the target protection domain.
   * @return          The projected event.
   * @throws  IllegalStateException
   *                  Signals that a class necessary class could not
   *                  be found, that a found class is incompatible
   *                  with the source class, or that a found class
   *                  cannot be instantiated.
   */
  final static Event project(final Event e, final ClassLoader loader,
                             final Guid prot) {

    try {
      return (Event)AccessController.doPrivileged(new
        PrivilegedExceptionAction() {
          public Object run() throws Exception {
            return project1(e, loader, prot);
          }
        });
    } catch (PrivilegedActionException x) {
      Exception xx = x.getException();

      throw (RuntimeException)xx;
    }
  }

  /**
   * Project the specified object into the projection domain with the
   * specified class loader. This method copies mutable values (tuples
   * and arrays) using the corresponding classes of the specified
   * class loader. Immutable values and non-symbolic event handlers
   * are not copied.
   *
   * @param   o       The object to project.
   * @param   loader  The class loader to use for the projection.
   * @param   prot    The ID of the target protection domain.
   * @return          The projected object.
   * @throws  IllegalStateException
   *                  Signals that a class necessary class could not
   *                  be found, that a found class is incompatible
   *                  with the source class, or that a found class
   *                  cannot be instantiated.
   */
  final static Object project(final Object o, final ClassLoader loader,
                              final Guid prot) {

    try {
      return AccessController.doPrivileged(new
        PrivilegedExceptionAction() {
          public Object run() throws Exception {
            return project1(o, loader, prot);
          }
        });
    } catch (PrivilegedActionException x) {
      Exception xx = x.getException();

      throw (RuntimeException)xx;
    }
  }

  /**
   * Project the specified object into the protection domain with the
   * specified class loader. This method needs to be privileged. This
   * method is package private to avoid synthetic accessors.
   *
   * @param   o       The object to project.
   * @param   loader  The class loader to project with.
   * @param   prot    The ID of the target protection domain.
   * @return          The projected object.
   * @throws  IllegalStateException
   *                  Signals that a class necessary class could not
   *                  be found, that a found class is incompatible
   *                  with the source class, or that a found class
   *                  cannot be instantiated.
   */
  static Object project1(final Object o,
                         final ClassLoader loader,
                         final Guid prot) {

    // Get null out of the way.
    if (null == o) {
      return o;
    }

    // Get the class.
    Class k = o.getClass();

    // Do the projection voodoo.
    if (o instanceof Tuple) {
      // Return the projected tuple.
      return project1((Tuple)o, loader, prot);

    } else if (Type.validFieldTypes.contains(k) ||
               (Throwable.class.isAssignableFrom(k) &&
                (! (k.getClassLoader() instanceof ProtectionDomain.Loader)))) {
      // The object is effectively immutable. Return it.
      return o;

    } else if (o instanceof EventHandler) {

      if (o instanceof SymbolicHandler) {
        // Null handlers, remote references, and named resources are
        // effectively immutable. Discovered resources contain a
        // mutable query and need to be copied. All other symbolic
        // handlers we reject.
        if ((o == NullHandler.NULL) ||
            (o instanceof RemoteReference) ||
            (o instanceof NamedResource)) {
          return o;
        } else if (o instanceof DiscoveredResource) {
          DiscoveredResource dr = (DiscoveredResource)o;
          return new DiscoveredResource((Query)project1(dr.query, loader, prot),
                                        dr.matchAll);
        }

      } else {
        // Non-symbolic event handlers are simply returned. They are
        // effectively immutable since they are wrapped by a
        // concurrency domain call wrapper.
        return o;
      }

    } else if (o instanceof Class) {
      // Return the projected class.
      return project1((Class)o, loader, prot);

    } else if (k.isArray()) {
      // Arrays need some special treatment, depending on the
      // component type.
      Class  comp = k.getComponentType();
      int    l    = Array.getLength(o);
      Object o2;

      if (Type.validFieldTypes.contains(comp) ||
          Throwable.class.isAssignableFrom(comp)) {
        // The component type is immutable. Return a shallow copy.
        o2 = Array.newInstance(comp, l);
        System.arraycopy(o, 0, o2, 0, l);
        return o2;

      } else if (Tuple.class.isAssignableFrom(comp) ||
                 EventHandler.class.isAssignableFrom(comp) ||
                 Class.class.equals(comp) ||
                 Object.class.equals(comp)) {
        // Project the component type and then all array elements.
        Class comp2 = project1(comp, loader, prot);
        o2          = Array.newInstance(comp2, l);
        for (int i=0; i<l; i++) {
          Object value = project1(Array.get(o, i), loader, prot);

          try {
            Array.set(o2, i, value);
          } catch (IllegalArgumentException x) {
            throw new IllegalStateException("Protection domain " + prot +
                                            ": Unable to assign value (" +
                                            value + ") to array of type " +
                                            comp2);
          }
        }
        return o2;
      }
    }

    // We don't know this type of object.
    throw new Bug("Unable to copy tuple field value (" + o + ")");
  }

  /**
   * Project the specified tuple into the protection domain with the
   * specified class loader. This method needs to be privileged. This
   * method is package private to avoid synthetic accessors.
   *
   * @param   o       The tuple to project.
   * @param   loader  The class loader to project with.
   * @param   prot    The ID of the target protection domain.
   * @return          The projected tuple.
   * @throws  IllegalStateException
   *                  Signals that a class necessary class could not
   *                  be found, that a found class is incompatible
   *                  with the source class, or that a found class
   *                  cannot be instantiated.
   */
  static Tuple project1(final Tuple t,
                        final ClassLoader loader,
                        final Guid prot) {

    // Nested calls to this method may pass in null.
    if (null == t) {
      return t;
    }

    // ----------------------- Dynamic tuples --------------------------
    if (t instanceof DynamicTuple) {
      // Dynamic tuples.
      DynamicTuple dt1 = (DynamicTuple)t;
      DynamicTuple dt2 = (DynamicTuple)dt1.clone();

      dt2.metaData = (DynamicTuple)project1(dt2.metaData, loader, prot);
      dt2.source   = (EventHandler)project1(dt2.source,   loader, prot);
      dt2.closure  = project1(dt2.closure,  loader, prot);

      // Project dynamically typed fields.
      Iterator iter = dt2.map.keySet().iterator();
      while (iter.hasNext()) {
        Object o = iter.next();

        dt2.map.put(o, project1(dt2.map.get(o), loader, prot));
      }

      return dt2;
    }

    // Static tuples.
    Class k1 = t.getClass();
    Class k2 = project1(k1, loader, prot);

    // ----------- Static tuples in same protection domain -------------
    if (k1.equals(k2)) {
      // Both protection domains agree on the same class. We assume
      // that the tuple class is in fact valid.
      Tuple t2 = (Tuple)t.clone();
      
      // Project all non-primitive fields.
      Iterator iter = Type.getFieldMap(k1).values().iterator();
      while (iter.hasNext()) {
        Field f = (Field)iter.next();
        
        if (! f.getType().isPrimitive()) {
          try {
            f.set(t2, project1(f.get(t), loader, prot));
          } catch (IllegalAccessException x) {
            throw new Bug("Access to field (" + f + ") denied");
          }
        }
      }

      // Done.
      return t2;
    }

    // -------- Static tuples in different protection domains ----------

    // Make sure the target class is a valid tuple class.
    Type.Info info;

    synchronized (Type.lock) {
      info = (Type.Info)Type.validTuples.get(k2);
    }

    if (null == info) {
      MalformedTupleException x = Type.validateFully(k2, null);
      if (null != x) {
        throw new IllegalStateException("Protection domain " + prot +
                                        ": " + x.getMessage());
      }

      synchronized (Type.lock) {
        info = (Type.Info)Type.validTuples.get(k2);
      }
    }

    // Create the tuple.
    Tuple t2;
    try {
      t2 = (Tuple)info.constructor.newInstance(ARGS_NONE);
    } catch (InstantiationException x) {
      throw new IllegalStateException("Protection domain " + prot +
                                      ": Abstract tuple (" + k2 + ")");
    } catch (IllegalAccessException x) {
      throw new Bug("Access to constructor (" + info.constructor + ") denied");
    } catch (IllegalArgumentException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    } catch (InvocationTargetException x) {
      throw new IllegalStateException("Protection domain " + prot +
                                      ": Unable to instantiate tuple " +
                                      k2 + " (" + x.getTargetException() +
                                      ")");
    }

    // Project all fields.
    Iterator iter = Type.getFieldMap(k1).values().iterator();
    Map      m2   = Type.getFieldMap(k2);

    while (iter.hasNext()) {
      Field f1 = (Field)iter.next();
      Field f2 = (Field)m2.get(f1.getName());

      // If the target class does not have a field, we skip the
      // field. The assumption is that the source class is an older
      // version of the target class.
      if (null != f2) {
        Class f1Type = f1.getType();
        if (f1Type.isPrimitive()) {
          // For efficiency reasons, we treat booleans, ints, and
          // longs separately.
          if (Boolean.TYPE.equals(f1Type)) {
            boolean value;
            try {
              value = f1.getBoolean(t);
            } catch (IllegalAccessException x) {
              throw new Bug("Access to field (" + f1 + ") denied");
            }

            try {
              f2.setBoolean(t2, value);
            } catch (IllegalAccessException x) {
              throw new Bug("Access to field (" + f2 + ") denied");
            } catch (IllegalArgumentException x) {
              throw new IllegalStateException("Protection domain " + prot +
                                              ": Unable to assign value (" +
                                              value + ") to field (" + f2 +
                                              ")");
            }
            continue;

          } else if (Integer.TYPE.equals(f1Type)) {
            int value;
            try {
              value = f1.getInt(t);
            } catch (IllegalAccessException x) {
              throw new Bug("Access to field (" + f1 + ") denied");
            }

            try {
              f2.setInt(t2, value);
            } catch (IllegalAccessException x) {
              throw new Bug("Access to field (" + f2 + ") denied");
            } catch (IllegalArgumentException x) {
              throw new IllegalStateException("Protection domain " + prot +
                                              ": Unable to assign value (" +
                                              value + ") to field (" + f2 +
                                              ")");
            }
            continue;

          } else if (Long.TYPE.equals(f1Type)) {
            long value;
            try {
              value = f1.getLong(t);
            } catch (IllegalAccessException x) {
              throw new Bug("Access to field (" + f1 + ") denied");
            }

            try {
              f2.setLong(t2, value);
            } catch (IllegalAccessException x) {
              throw new Bug("Access to field (" + f2 + ") denied");
            } catch (IllegalArgumentException x) {
              throw new IllegalStateException("Protection domain " + prot +
                                              ": Unable to assign value (" +
                                              value + ") to field (" + f2 +
                                              ")");
            }
            continue;
          }

          // Fall through for all other primitive types.
        }
        
        // Get the field value.
        Object value;
        try {
          value = f1.get(t);
        } catch (IllegalAccessException x) {
          throw new Bug("Access to field (" + f1 + ") denied");
        }

        // Project the field value.
        value = project1(value, loader, prot);

        // Set the projected field value.
        try {
          f2.set(t2, value);
        } catch (IllegalAccessException x) {
          throw new Bug("Access to field (" + f2 + ") denied");
        } catch (IllegalArgumentException x) {
          throw new IllegalStateException("Protection domain " + prot +
                                          ": Unable to assign value (" +
                                          value + ") to field (" + f2 +
                                          ")");
        }
      }
    }

    // Done.
    return t2;
  }

  /**
   * Project the specified class into the protection domain with the
   * specified class loader. This method needs to be privileged.
   *
   * @param   k       The class to project.
   * @param   loader  The class loader to project with.
   * @param   prot    The ID of the target protection domain.
   * @return          The projected class.
   * @throws  IllegalStateException
   *                  Signals that a class necessary class could not
   *                  be found, that a found class is incompatible
   *                  with the source class, or that a found class
   *                  cannot be instantiated.
   */
  private static Class project1(final Class k,
                                final ClassLoader loader,
                                final Guid prot) {
    Class k2;

    try {
      k2 = Class.forName(k.getName(), true, loader);
    } catch (ClassNotFoundException x) {
      // Create the corresponding illegal state exception.
      throw new IllegalStateException("Protection domain " + prot +
                                      ": No such class (" + k.getName() + ")");
    }

    // Make sure that the serial version UIDs match.
    if (! k.equals(k2)) {
      long id1 = ObjectStreamClass.lookup(k ).getSerialVersionUID();
      long id2 = ObjectStreamClass.lookup(k2).getSerialVersionUID();

      if (id1 != id2) {
        throw new IllegalStateException("Protection domain " + prot +
                                        ": Serial version UID mismatch for " +
                                        "class " + k.getName());
      }
    }

    return k2;
  }


  // =====================================================================
  //                                Equality
  // =====================================================================

  /**
   * Get a hashcode for this tuple.
   *
   * @return    A hashcode for this tuple.
   */
  public final int hashCode() {
    return hashCode(this);
  }

  /**
   * Get a hashcode for the specified object. If the specified object
   * is <code>null</code>, this method returns 0. If the specified
   * object is a tuple, this method returns the sum of the hashcodes
   * of all field values, as determined by recursively calling
   * itself. If the specified object is an array, this method returns
   * the sum of the hashcodes of all entries, as determined by
   * recursively calling itself. Finally, for all other objects, this
   * method returns the result of invoking the object's
   * <code>hashCode()</code> method.
   *
   * @param   o  The object.
   * @return     The corresponding hashcode.
   */
  private static int hashCode(final Object o) {
    if (null == o) {
      return 0;

    } else if (o instanceof DynamicTuple) {
      DynamicTuple dt   = (DynamicTuple)o;
      int          hash = 0;

      // Ignore id and metaData fields, but add in hashcodes of source
      // and closure.
      hash += hashCode(dt.source);
      hash += hashCode(dt.closure);

      // Add in hashcodes of dynamically typed fields.
      Iterator iter = dt.map.values().iterator();
      while (iter.hasNext()) {
        hash += hashCode(iter.next());
      }

      return hash;

    } else if (o instanceof Tuple) {
      int hash = 0;

      // Iterate over fields.
      Iterator iter = Type.getFieldMap(o.getClass()).values().iterator();
      while (iter.hasNext()) {
        Field  f    = (Field)iter.next();
        String name = f.getName();

        // Ignore ID and meta-data fields.
        if (ID.equals(name) || META_DATA.equals(name)) {
          continue;
        }

        try {
          hash += hashCode(f.get(o));
        } catch (IllegalAccessException x) {
          throw new Bug("Access to field (" + f + ") denied");
        }
      }

      return hash;

    } else if (o.getClass().isArray()) {
      int l    = Array.getLength(o);
      int hash = 0;

      // Iterate over array entries.
      for (int i=0; i<l; i++) {
        hash += hashCode(Array.get(o, i));
      }

      return hash;

    } else {
      // Let object do the work.
      return o.hashCode();
    }
  }

  /**
   * Determine whether this tuple equals the specified object. This
   * tuple equals the specified object, if the specified object is a
   * tuple of the same type and all fields of this tuple, besides the
   * <code>id</code> and <code>metaData</code> fields, are equal to
   * the corresponding fields of the specified object.
   *
   * @param   o  The object to compare to.
   * @return     <code>true</code> if this tuple equals the specified
   *             object.
   */
  public final boolean equals(final Object o) {
    return equals(this, o);
  }

  /**
   * Determine whether the specified objects are equal. This method
   * compares tuples by comparing their fields. It ignores the
   * <code>id</code> and <code>metaData</code> fields. This method
   * compares arrays by comparing their entries.
   *
   * @param   o1  The first object to compare.
   * @param   o2  The second object to compare.
   * @return      <code>true</code> if the two objects are equal.
   */
  private static boolean equals(final Object o1, final Object o2) {
    if (o1 == o2) {                               // Identity.
      return true;

    } else if (null == o1) {                      // o1 == null.
      return (null == o2);

    } else if (null == o2) {                      // o2 == null.
      return false;

    } else if (o1 instanceof DynamicTuple) {      // Dynamic tuples.
      if (! (o2 instanceof DynamicTuple)) {
        return false;
      }

      DynamicTuple dt1 = (DynamicTuple)o1;
      DynamicTuple dt2 = (DynamicTuple)o2;

      // Make sure the two dynamic tuples have the same source and
      // closure and the same number of dynamically typed fields.
      if (! equals(dt1.source,  dt2.source))  { return false; }
      if (! equals(dt1.closure, dt2.closure)) { return false; }
      if (dt1.map.size() != dt2.map.size())   { return false; }
      
      // Make sure the two dynamic tuples have the same dynamically
      // typed fields with equal values.
      Iterator iter = dt1.map.keySet().iterator();
      while (iter.hasNext()) {
        Object key = iter.next();

        if (! dt2.map.containsKey(key))                   { return false; }
        if (! equals(dt1.map.get(key), dt2.map.get(key))) { return false; }
      }

      // Done.
      return true;

    } else if (o1 instanceof Tuple) {             // Tuples.
      if (! o1.getClass().equals(o2.getClass())) {
        return false;
      }

      // Compare the two tuples field by field.
      Iterator iter = Type.getFieldMap(o1.getClass()).values().iterator();
      while (iter.hasNext()) {
        Field  f     = (Field)iter.next();
        String name  = f.getName();

        // Ignore ID and meta-data fields.
        if (ID.equals(name) || META_DATA.equals(name)) {
          continue;
        }

        try {
          if (! equals(f.get(o1), f.get(o2))) { return false; }
        } catch (IllegalAccessException x) {
          throw new Bug("Access to field (" + f + ") denied");
        }
      }

      // Done.
      return true;

    } else if (o1.getClass().isArray()) {         // Arrays.
      Class k1 = o1.getClass();
      Class k2 = o2.getClass();

      // Make sure o2 also is an array of the same component type.
      if (! k2.isArray()) {
        return false;
      } else if (! k1.getComponentType().equals(k2.getComponentType())) {
        return false;
      }

      int l = Array.getLength(o1);

      // Make sure the two arrays have the same lengths.
      if (Array.getLength(o2) != l) {
        return false;
      }

      // Make sure the entries are equal.
      for (int i=0; i<l; i++) {
        if (! equals(Array.get(o1, i), Array.get(o2, i))) {
          return false;
        }
      }

      // Done.
      return true;

    } else {                                      // All other objects.
      // Let o1 do the work.
      return o1.equals(o2);
    }
  }


  // =====================================================================
  //                             Field access
  // =====================================================================

  /**
   * Get the value of the specified field. This method returns the
   * value of the specified field or <code>null</code> if this tuple
   * does not have a field with the specified name.
   *
   * @param   name  The name of the field.
   * @return        The value of the specified field, or
   *                <code>null</code> if this tuple has no such field.
   */
  public Object get(final String name) {
    Field f = (Field)Type.getFieldMap(getClass()).get(name);

    if (null == f) {
      return null;
    } else {
      try {
        return f.get(this);
      } catch (IllegalAccessException x) {
        throw new Bug("Access to field (" + f + ") denied");
      }
    }
  }

  /**
   * Set the specified field to the specified value. This method sets
   * the value of the field with the specified name to the specified
   * value. If this tuple is a dynamically typed tuple and does not
   * have a field with the specified name, the field is added to the
   * tuple.
   *
   * @param   name   The name of the field.
   * @param   value  The new value for the field.
   * @throws  IllegalArgumentException
   *                 Signals that a statically typed tuples does not
   *                 have a field with the specified name or that the
   *                 specified value is of the wrong type.
   */
  public void set(final String name, final Object value) {
    Field f = (Field)Type.getFieldMap(getClass()).get(name);

    if (null == f) {
      throw new IllegalArgumentException("No such field (" + name + ")");
    } else {
      try {
        f.set(this, value);
      } catch (IllegalAccessException x) {
        throw new Bug("Access to field (" + f + ") denied");
      }
    }
  }

  /**
   * Remove the specified field from this tuple. For dynamically typed
   * tuples, this method removes the specified field from the
   * tuple if it exists.
   *
   * @param   name  The name of the field to remove.
   * @return        The value of the removed field.
   * @throws  IllegalArgumentException
   *                Signals that the field cannot be removed because
   *                it is a statically typed field.
   */
  public final Object remove(final String name) {
    if (this instanceof DynamicTuple) {
      if (Type.dynamicTupleMap.containsKey(name)) {
        throw new IllegalArgumentException("Unable to remove field (" + name +
                                           "from tuple (" + this + ")");
      } else {
        return ((DynamicTuple)this).map.remove(name);
      }
    } else {
      throw new IllegalArgumentException("Unable to remove field (" + name +
                                         "from tuple (" + this + ")");
    }
  }


  // =====================================================================
  //                           Tuple structure
  // =====================================================================

  /**
   * Determine whether this tuple has a field with the specified name.
   *
   * @param   name  The name of the field to test for.
   * @return        <code>true</code> if this tuple has a field with
   *                the specified name.
   */
  public boolean hasField(final String name) {
    return Type.getFieldMap(getClass()).containsKey(name);
  }

  /**
   * Get the declared type of the field with the specified name. The
   * declared type of a dynamic tuple's dynamically typed fields is
   * <code>java.lang.Object</code>.
   *
   * @param   name  The name of the field.
   * @retrun        The declared type fo the specified field, or
   *                <code>null</code> if this tuple has no such
   *                field.
   */
  public Class getType(final String name) {
    Field f = (Field)Type.getFieldMap(getClass()).get(name);

    if (null == f) {
      return null;
    } else {
      return f.getType();
    }
  }

  /**
   * Get a list of this tuple's field names. Modifications to the list
   * have no effect on the corresponding fields.
   *
   * @return  A list of this tuple's field names.
   */
  public List fields() {
    return new ArrayList(Type.getFieldMap(getClass()).keySet());
  }


  // =====================================================================
  //                           Meta-data access
  // =====================================================================

  /**
   * Get the value of the meta-data field with the specified name.
   *
   * <p>This convenience method is equivalent to:<pre>
   *   return ((null == metaData)?
   *           null :
   *           metaData.get(name));
   * </pre></p>
   *   
   * @param   name  The name of the meta-data field.
   * @return        The value of the corresponding field, or
   *                <code>null</code> if this tuple does not have
   *                a meta-data field with the specified field.
   */
  public final Object getMetaData(final String name) {
    return ((null == metaData)? null : metaData.get(name));
  }

  /**
   * Set the meta-data field with the specified name to the specified
   * value.
   *
   * <p>This convenience method is equivalent to:<pre>
   *   if (null == metaData) {
   *     metaData = new DynamicTuple();
   *   }
   *   metaData.set(name, value);
   * </pre></p>
   * 
   * @param   name   The name of the meta-data field.
   * @param   value  The new value for the specified field.
   */
  public final void setMetaData(final String name, final Object value) {
    if (null == metaData) {
      metaData = new DynamicTuple();
    }

    metaData.set(name, value);
  }

  /**
   * Determine whether this tuple has a meta-data field with the
   * specified name.
   *
   * <p>This convenience method is equivalent to:<pre>
   *   return ((null == metaData)?
   *           false :
   *           metaData.hasField(name));
   * </pre></p>
   *
   * @param   name  The name of the meta-data field.
   * @return        <code>true</code> if this tuple has a meta-data
   *                field with the specified name.
   */
  public final boolean hasMetaData(final String name) {
    return ((null == metaData)? false : metaData.hasField(name));
  }


  // =====================================================================
  //                             Validation
  // =====================================================================

  /**
   * Validate this tuple. This method ensures that this tuple is
   * well-formed. Subclasses that overwrite this method must first
   * call the superclass's <code>validate()</code> method.
   *
   * <p>This method ensures that this tuple's class is a valid tuple
   * class. It also ensures that this tuple's ID is not
   * <code>null</code>.  This tuple's meta-data is not validated.</p>
   * 
   * @see     MalformedTupleException
   * @see     InvalidTupleException
   * @see     Type#validate(Class)
   *
   * @throws  TupleException  Signals that the tuple is either malformed
   *                          (i.e., does not conform to the tuple
   *                          specification) or invalid (i.e., does not
   *                          conform to the semantic constraints of its
   *                          type).
   */
  public void validate() throws TupleException {
    Type.validate(getClass());

    if (null == id) {
      throw new InvalidTupleException("Null ID for tuple (" + this + ")");
    }
  }


  // =====================================================================
  //                                Wrapping
  // =====================================================================

  /**
   * Wrap this tuple. This method applies the specified wrapper to all
   * event handlers referenced by this tuple (and any nested
   * tuples). The tuple is modified in place. Note that if the result
   * of wrapping an event handler cannot be assigned back to the
   * field, this method will throw a {@link Bug}.
   *
   * @param  wrapper  The wrapper to apply on all event handlers.
   */
  public final void wrap(Wrapper wrapper) {
    if (this instanceof DynamicTuple) {
      DynamicTuple dt = (DynamicTuple)this;

      // Wrap statically typed fields.
      if (null != dt.metaData) {
        dt.metaData.wrap(wrapper);
      }

      dt.source = wrapper.wrap(dt.source);

      if (wrapArray(dt.closure, wrapper)) {
        if (dt.closure instanceof Tuple) {
          ((Tuple)dt.closure).wrap(wrapper);
        }
        if (dt.closure instanceof EventHandler) {
          dt.closure = wrapper.wrap((EventHandler)dt.closure);
        }
      }

      // Wrap dynamically typed fields.
      Iterator iter = dt.map.keySet().iterator();

      while (iter.hasNext()) {
        String name = (String)iter.next();
        Object o    = dt.map.get(name);

        if (wrapArray(o, wrapper)) {
          if (o instanceof Tuple) {
            ((Tuple)o).wrap(wrapper);
          }
          if (o instanceof EventHandler) {
            dt.map.put(name, wrapper.wrap((EventHandler)o));
          }
        }
      }

    } else {
      Iterator iter = Type.getFieldMap(getClass()).values().iterator();

      Field  f = null;
      try {
        while (iter.hasNext()) {
          f        = (Field)iter.next();
          Object o = f.get(this);

          if (wrapArray(o, wrapper)) {
            if (o instanceof Tuple) {
              ((Tuple)o).wrap(wrapper);
            }
            if (o instanceof EventHandler) {
              f.set(this, wrapper.wrap((EventHandler)o));
            }
          }
        }
      } catch (IllegalAccessException x) {
        throw new Bug("Access to field (" + f + ") denied");
      } catch (IllegalArgumentException x) {
        throw new Bug("Unable to assign wrapped event handler back to " +
                      " field (" + f.getName() + ") for tuple (" + this + ")");
      }
    }
  }

  /**
   * Wrap the specified object as an array, using the specified wrapper.
   *
   * @param   o        The object to wrap as an array.
   * @param   wrapper  The wrapper to use.
   * @return           <code>true</code> if the specified object may
   *                   be a tuple or an event handler.
   */
  private static boolean wrapArray(Object o, Wrapper wrapper) {
    if (null == o) { return false; }

    Class k = o.getClass();

    if (! k.isArray()) { return true; }

    k = k.getComponentType();

    if (k.isPrimitive()) { return false; }

    int l = Array.getLength(o);
    try {
      for (int i=0; i<l; i++) {
        Object element = Array.get(o, i);

        if (wrapArray(element, wrapper)) {
          if (element instanceof Tuple) {
            ((Tuple)element).wrap(wrapper);
          }
          if (element instanceof EventHandler) {
            Array.set(o, i, wrapper.wrap((EventHandler)element));
          }
        }
      }
    } catch (IllegalArgumentException x) {
      throw new Bug("Unable to assign wrapped event handler back to array " +
                    "entry (" + o + ")");
    }

    return false;
  }

  /**
   * Determine whether this tuple contains at least one non-symbolic
   * event handler. This method is useful for {@link one.world.rep},
   * which requires that events do not contain non-symbolic event
   * handlers.
   *
   * @see  SymbolicHandler
   *
   * @return  <code>true</code> if this tuple contains at least one
   *          non-symbolic event handler.
   */
  public final boolean containsNonSymbolicHandler() {
    if (this instanceof DynamicTuple) {
      DynamicTuple dt = (DynamicTuple)this;

      // Test statically typed fields.
      if (null != dt.metaData) {
        if (dt.metaData.containsNonSymbolicHandler()) {
          return true;
        }
      }

      if (null != dt.source) {
        if (! (dt.source instanceof SymbolicHandler)) {
          return true;
        }
      }

      if (containsNonSymbolicHandler(dt.closure)) {
        return true;
      }

      // Test dynamically typed fields.
      Iterator iter = dt.map.keySet().iterator();
      while (iter.hasNext()) {
        if (containsNonSymbolicHandler(dt.map.get(iter.next()))) {
          return true;
        }
      }

    } else {
      Iterator iter = Type.getFieldMap(getClass()).values().iterator();

      Field f = null;
      try {
        while (iter.hasNext()) {
          f = (Field)iter.next();

          if (containsNonSymbolicHandler(f.get(this))) {
            return true;
          }
        }
      } catch (IllegalAccessException x) {
        throw new Bug("Access to field (" + f + ") denied");
      } catch (IllegalArgumentException x) {
        throw new Bug("Unable to assign wrapped event handler back to " +
                      " field (" + f.getName() + ") for tuple (" + this + ")");
      }
    }

    return false;
  }

  /**
   * Determine whether the specified object contains at least one
   * non-symbolic event handler.
   *
   * @param   o  The object to test.
   * @return     <code>true</code> if the specified object contains
   *             at least one non-symbolic event handler.
   */
  private static boolean containsNonSymbolicHandler(Object o) {
    if (null != o) {
      Class k = o.getClass();

      if (k.isArray()) {
        k = k.getComponentType();

        if (! k.isPrimitive()) {
          int l = Array.getLength(o);

          for (int i=0; i<l; i++) {
            if (containsNonSymbolicHandler(Array.get(o, i))) {
              return true;
            }
          }
        }

      } else {
        if (o instanceof Tuple) {
          if (((Tuple)o).containsNonSymbolicHandler()) {
            return true;
          }
        }
        if (o instanceof EventHandler) {
          if (! (o instanceof SymbolicHandler)) {
            return true;
          }
        }
      }
    }

    return false;
  }


  // =====================================================================
  //                           String representation
  // =====================================================================

  /**
   * Get a string representation for this tuple.
   *
   * @return  A string representation for this tuple.
   */
  public String toString() {
    return "#[Tuple " + getClass().getName() + " " + id + "]";
  }

}
