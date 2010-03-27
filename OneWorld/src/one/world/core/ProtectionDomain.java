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

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import one.util.Guid;

/**
 * Implementation of a protection domain.
 *
 * <p>Applications running on top of <i>one.world</i> are limited in
 * what resources provided by the Java platform they can access.  For
 * example, applications are generally expected to rely on structured
 * I/O for their storage and communication needs instead of accessing
 * files and networking sockets directly.</p>
 *
 * <p>In general, <i>one.world</i> relies on Java's platform security
 * to limit applications' access to "forbidden" resources. This
 * ensures that applications do not have access to such resources in
 * the common case. At the same time, relying on Java's platform
 * security also makes it possible to grant special access rights to
 * applications that have a legitimate need to access such
 * resources. For example, a web server can be granted the right to
 * listen for and to accept TCP connections on port 80.</p>
 *
 * <p>To ensure complete safety and security, <i>one.world</i> does
 * impose the following additional restrictions on applications:<ul>
 *
 * <li>The following classes in the <code>java.lang</code> package
 * cannot be accessed by application code: <code>Compiler</code>,
 * <code>InheritableThreadLocal</code>, <code>Runtime</code>,
 * <code>System</code>, <code>Thread</code>, <code>ThreadGroup</code>,
 * and <code>ThreadLocal</code>. Note that some of the functionality
 * of <code>System</code> is available through {@link
 * one.world.util.SystemUtilities}.</li>
 *
 * <li><code>Timer</code> and <code>TimerTask</code> in the
 * <code>java.util</code> package and <code>Timer</code> in the
 * <code>javax.swing</code> package cannot be accessed by application
 * code. Note that <i>one.world</i> provides its own {@link
 * one.world.util.Timer timed notification service}.</li>
 *
 * <li>Application code must not catch <code>Throwable</code>,
 * <code>Error</code>, or <code>ThreadDeath</code> exceptions.</li>
 *
 * <li>Tuples that provide own implementation of
 * <code>validate()</code> must invoke the superclass's
 * <code>validate()</code> before performing any other
 * computation.</li>
 *
 * </ul></p>
 *
 * <p>Each protection domain has its own {@link ClassLoader class
 * loader} to load the application's classes. However, system classes,
 * that is, classes in the <code>one.world.*</code>,
 * <code>one.util</code>, and <code>one.net</code> packages, are
 * loaded by the core class loader and shared between all protection
 * domains.</p>
 *
 * <p>When an application is {@link Environment#unload(Guid,Guid)
 * unloaded}, the class loader for the protection domain is recreated.
 * As a result, when an application is reloaded into the same
 * protection domain, the application's classes are loaded again.</p>
 *
 * <p>When sending an event to an event handler in a different
 * protection domain, the event is "projected" into that protection
 * domain by using the corresponding classes of the targeted
 * protection domain and by copying mutable data, even if the class is
 * loaded by the core class loader.</p>
 *
 * @version  $Revision: 1.13 $
 * @author   Robert Grimm 
 */
public final class ProtectionDomain extends Domain
  implements java.io.Serializable {

  /** The serial version ID for this class. */
  static final long serialVersionUID = -8284065987540224045L;

  // =======================================================================
  //                          The class loader
  // =======================================================================

  /** Implementation of a protection domain class loader. */
  final class Loader extends java.net.URLClassLoader {

    /** Create a new class loader. */
    Loader() {
      super(CLASS_PATH);
    }

    /**
     * Get the ID for this protection domain class loader.
     *
     * @return  The ID for this class loader's protection domain.
     */
    public Guid getId() {
      return id;
    }

    /**
     * Load the specified class. Before invoking the superclass's
     * implementation of this method, this method ensures that the
     * specified class is not one of the forbidden classes.
     */
    protected Class loadClass(String name, boolean resolve)
      throws ClassNotFoundException {

      // Make sure the class is a legal class.
      if (FORBIDDEN.contains(name)) {
        throw new ClassNotFoundException("Class " + name + " not available " +
                                         "for applications in one.world");
      }

      // Determine the package name.
      String pack = "";
      int    idx  = name.lastIndexOf('.');

      if (-1 != idx) {
        pack = name.substring(0, idx);
      }

      // Java platform classes and one.world core classes are loaded
      // relative to the main class loader.
      if (CORE.contains(pack)       ||
          pack.startsWith("java.")  ||
          pack.startsWith("javax.")   ) {
        return super.loadClass(name, resolve);
      }

      // Everything else is loaded by this class loader.
      Class k = findLoadedClass(name);
      if (k == null) {
        k = findClass(name);
      }
      if (resolve) {
        resolveClass(k);
      }
      return k;
    }

  }

  /** The class path as an array of URLs. */
  static final URL[] CLASS_PATH;

  /** Populate the class path array. */
  static {
    ArrayList       list = new ArrayList();
    StringTokenizer tok  = new
      StringTokenizer(System.getProperty("java.class.path"),
                      File.pathSeparator);

    while (tok.hasMoreTokens()) {
      try {
        list.add((new File(tok.nextToken())).toURL());
      } catch (MalformedURLException x) {
        throw new IllegalStateException("Malformed class path (" + x + ")");
      }
    }

    CLASS_PATH = (URL[])list.toArray(new URL[list.size()]);
  }


  // =======================================================================
  //                          Instance fields
  // =======================================================================

  /** The lock protecting loader initialization. */
  transient          Object      lock;

  /** 
   * The class loader for this protection domain. This field should
   * only be accessed directly, when it is certain that a class loader
   * has already been allocated for this protection domain.
   */
  transient volatile ClassLoader loader;

  /**
   * The flag for whether this protection domain has a class loader.
   *
   * @serial  Must be <code>true</code> iff <code>loader</code> is
   *          not <code>null</code>.
   */
  boolean                        hasLoader;
   
  // =======================================================================
  //                            Constructor
  // =======================================================================

  /**
   * Create a new protection domain with the specified ID.
   *
   * @param  id  The ID for the new protection domain.
   */
  ProtectionDomain(Guid id) {
    super(id);
    lock = new Object();
  }


  // =======================================================================
  //                              Finalization
  // =======================================================================

  /** Finalize this protection domain. */
  protected void finalize() {
    // Remove all validated tuple classes from the cache of validated
    // tuple classes, so that the class loader can be garbage
    // collected.
    Type.clean(loader);
  }


  // =======================================================================
  //                             Serialization
  // =======================================================================

  /**
   * Serialize this protection domain.
   *
   * @serialData     The default fields, followed by this protection
   *                 domain's ID.
   * @throws      IllegalStateException
   *                 Signals that his protection domain is being
   *                 serialized outside a check-point.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (lock) {
      out.defaultWriteObject();
    }

    // Resolve the ID of this protection domain.
    Guid id2 = Environment.resolveId(id);

    // Sanity check.
    if (null == id2) {
      throw new IllegalStateException("Attempting to serialize protection " +
                                      "domain outside check-point ("+this+")");
    }

    // Write out the resolved ID.
    out.writeObject(id2);
  }

  /** Deserialize a protection domain. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // Read in the ID.
    id = (Guid)in.readObject();
    
    // There is no need to restore the lock, because this protection
    // domain is going to be resolved away anyway.
  }

  /**
   * Resolve this protection domain reference during
   * deserialization.
   */
  private Object readResolve() throws ObjectStreamException {
    ProtectionDomain prot = Environment.resolveProtection(id);

    // Make sure the loader is initialized.
    if (hasLoader) {
      prot.initializeLoader();
    }

    return prot;
  }


  // =======================================================================
  //                          Class loader management
  // =======================================================================

  /**
   * Initialized this protection domain. If no class loader has been
   * allocated for this protection domain before, this method
   * allocates a new class loader.
   */
  void initializeLoader() {
    synchronized (lock) {
      if (null == loader) {
        loader    = new Loader();
        hasLoader = true;
      }
    }
  }


  // =======================================================================
  //                          Public functionality
  // =======================================================================

  /** Return a string representation of this protection domain. */
  public String toString() {
    return "#[Protection domain " + id.toString() + "]";
  }


  // =======================================================================
  //                   Forbidden classes and core packages
  // =======================================================================

  /** The set of forbidden classes in otherwise accessible packages. */
  private static final HashSet FORBIDDEN;

  /** The set of one.world core packages. */
  private static final HashSet CORE;

  static {
    FORBIDDEN = new HashSet(22);

    FORBIDDEN.add("java.lang.Compiler");
    FORBIDDEN.add("java.lang.InheritableThreadLocal");
    FORBIDDEN.add("java.lang.Runtime");
    FORBIDDEN.add("java.lang.System");
    FORBIDDEN.add("java.lang.Thread");
    FORBIDDEN.add("java.lang.ThreadGroup");
    FORBIDDEN.add("java.lang.ThreadLocal");
    FORBIDDEN.add("java.util.Timer");
    FORBIDDEN.add("java.util.TimerTask");
    FORBIDDEN.add("javax.swing.Timer");

    CORE = new HashSet(24);

    CORE.add("one.gui");
    CORE.add("one.net");
    CORE.add("one.util");
    CORE.add("one.world");
    CORE.add("one.world.binding");
    CORE.add("one.world.core");
    CORE.add("one.world.data");
    CORE.add("one.world.env");
    CORE.add("one.world.io");
    CORE.add("one.world.rep");
    CORE.add("one.world.transaction");
    CORE.add("one.world.util");
    //The following avoids a class not found issue in jdk1.4b3
    CORE.add("sun.reflect");
  }

}
