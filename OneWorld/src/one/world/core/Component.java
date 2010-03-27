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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import one.util.Bug;
import one.util.Guid;

import one.world.util.Timer;

/**
 * The superclass of all components. Components implement services and
 * export as well as import event handlers. Components are always
 * instantiated within a specified environment and can be linked
 * against components in the same environment as well as other
 * environments.
 *
 * <p>Note that events passed between components cannot reference
 * components.</p>
 *
 * @version  $Revision: 1.31 $
 * @author   Robert Grimm
 */
public abstract class Component implements java.io.Serializable {

  /** Serial version ID for this class. */
  static final long serialVersionUID = -2327452889273575875L;

  // =======================================================================
  //                    Managing imported event handlers
  // =======================================================================

  /**
   * The record for an exported handler that is linked to an imported
   * event handler.
   */
  public static final class HandlerReference implements java.io.Serializable {

    /** Serial version ID for this class. */
    static final long serialVersionUID = 4667676409594053128L;

    /**
     * The name of the exported event handler.
     *
     * @serial  Must not be <code>null</code>.
     */
    final String       name;

    /**
     * The component of the exported event handler.
     *
     * @serial  Must not be <code>null</code>.
     */
    final Component    component;

    /**
     * The effective event handler.
     *
     * @serial  Must not be <code>null</code>.
     */
    final EventHandler handler;

    /**
     * Create a new handler reference. The specified name, component,
     * and event handler must not be <code>null</code>.
     *
     * @param   name       The name of the exported event handler.
     * @param   component  The component of the exported event handler.
     * @param   handler    The effective handler.
     */
    HandlerReference(String name, Component component, EventHandler handler) {
      this.name      = name;
      this.component = component;
      this.handler   = handler;
    }
    
    /** Get a hash code for this handler reference. */
    public int hashCode() {
      return (name.hashCode() + component.hashCode());
    }

    /**
     * Determine whether this handler reference equals the specified
     * object. This handler reference equals the specified object, if
     * the specified object is a handler reference with the same name
     * and the same component.
     */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (! (o instanceof HandlerReference)) return false;
      HandlerReference other = (HandlerReference)o;
      return (name.equals(other.name) && component.equals(other.component));
    }

    /**
     * Get the name of this handler reference.
     *
     * @return  The name of this handler reference.
     */
    public String getName() {
      return name;
    }

    /**
     * Get the component of this handler reference.
     *
     * @return  The component of this handler reference.
     */
    public Component getComponent() {
      return component;
    }

    /**
     * Get the effective event handler for this handler reference.
     *
     * @return     The effective event handler for this handler
     *             reference.
     * @throws  SecurityException
     *             Signals that the caller does not have permission
     *             to manage environment.
     */
    public EventHandler getEventHandler() {
      Environment.ensurePermission();

      return handler;
    }

  }

  // =======================================================================

  /**
   * The manager for imported event handlers. This class is returned
   * by the {@link Component#declareImported(ImportedDescriptor)}
   * method and lets a component send events to imported event
   * handlers. While this class implements the
   * <code>QueuedEventHandler</code> interface, not all imported event
   * handlers need to be queued event handlers. As a result, invoking
   * the {@link #handleFirst(Event)} or {@link #handleForced(Event)}
   * methods of an instance of this class may result in an illegal
   * state exception.
   */
  public final class Importer
    implements QueuedEventHandler, java.io.Serializable {

    /** Serial version ID for this class. */
    static final long serialVersionUID = -1100557006064329979L;    

    /**
     * The name of the imported event handler.
     *
     * @serial  Must not be <code>null</code>.
     */
    private final     String  name;

    /**
     * The flag for whether this importer may only link
     * one exported event handler.
     *
     * @serial
     */
    private final     boolean linkOne;
    
    /**
     * The currently imported event handlers. This field must either
     * be <code>null</code>, an instance of
     * <code>HandlerReference</code>, or an instance of
     * <code>ArrayList</code> filled with handler references.
     */
    private transient Object  references;

    /**
     * The lock for this importer.
     */
    private transient Object  lock;
    
    /** 
     * Create a new importer. The specified name must not be
     * <code>null</code>.
     *
     * @param  name     The name of the imported event.
     * @param  linkOne  The flag for whether this importer may
     *                  only link one exported event handler.
     */
    Importer(String name, boolean linkOne) {
      this.name    = name;
      this.linkOne = linkOne;
      lock         = new Object();
    }

    /**
     * Serialize this importer. This method filters out handler
     * references to event handlers that are exported by components
     * which are not in any of the environments currently being
     * serialized.
     *
     * @serialData  The default fields, followed by zero or more
     *              handler references, followed by
     *              <code>Boolean.FALSE</code>, all while holding
     *              the monitor for <code>this</code>.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
      synchronized (lock) {
        // Write default fields.
        out.defaultWriteObject();

        // Filter and write handler references.
        if (references instanceof HandlerReference) {
          HandlerReference ref = (HandlerReference)references;

          if (Environment.isInSerializationSet(ref.component.env.id)) {
            out.writeObject(ref);
          } else if (ref.component instanceof Environment) {
            Guid ident = ((Environment)ref.component).id;

            if (Environment.isInSerializationSet(ident)) {
              out.writeObject(ref);
            }
          }

        } else if (references instanceof ArrayList) {
          Iterator iter = ((ArrayList)references).iterator();

          while (iter.hasNext()) {
            HandlerReference ref = (HandlerReference)iter.next();

            if (Environment.isInSerializationSet(ref.component.env.id)) {
              out.writeObject(ref);
            } else if (ref.component instanceof Environment) {
              Guid ident = ((Environment)ref.component).id;

              if (Environment.isInSerializationSet(ident)) {
                out.writeObject(ref);
              }
            }
          }
        }

        // Marker object that we are done.
        out.writeObject(Boolean.FALSE);
      }
    }

    /** Deserialize an importer. */
    private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {

      in.defaultReadObject();

      Object o;

      o = in.readObject();
      while (! o.equals(Boolean.FALSE)) {
        // Add in handler reference.
        if (null == references) {
          references = o;

        } else if (references instanceof HandlerReference) {
          ArrayList l = new ArrayList();

          l.add(references);
          l.add(o);

          references = l;

        } else {
          ((ArrayList)references).add(o);
        }

        // Read next object.
        o = in.readObject();
      }

      lock = new Object();
    }
    
    /**
     * Add the specified exported event handler. Adding an already
     * added event handler has no effect. The specified name,
     * component, and event handler must not be <code>null</code>.
     *
     * @param   name       The name of the exported event handler.
     * @param   component  The component of the exported event handler.
     * @param   handler    The effective event handler.
     * @throws  LinkingException
     *                     Signals that this importer can only link
     *                     against one event handler.
     */
    void add(String name, Component component, EventHandler handler) {

      HandlerReference reference = new HandlerReference(name, component,
                                                        handler);

      synchronized (lock) {
        if (null == references) {
          references = reference;

        } else if (references instanceof HandlerReference) {
          if (references.equals(reference)) {
            return;
          } else if (linkOne) {
            throw new LinkingException("Imported event handler already linked ("
                                       + name + ")");
          }

          ArrayList l = new ArrayList();

          l.add(references);
          l.add(reference);

          references = l;

        } else {
          ArrayList l = (ArrayList)references;

          if (l.contains(reference)) {
            return;
          }

          l.add(reference);
        }
      }
    }

    /**
     * Remove the specified event handler. Removing a not currently
     * imported event handler has no effect. The specified name and
     * component must not be <code>null</code>.
     *
     * @param   name       The name of the exported event hander.
     * @param   component  The component of the exported event handler.
     */
    void remove(String name, Component component) {
      // The following reference is only used for finding the
      // corresponding exported event handler, so null for the
      // effective handler is ok.
      HandlerReference reference = new HandlerReference(name, component, null);

      synchronized (lock) {
        if (null == references) {
          // Nothing to remove

        } else if (references instanceof HandlerReference) {
          if (references.equals(reference)) {
            references = null;
          }

        } else {
          ArrayList l = (ArrayList)references;
          int       i = l.indexOf(reference);

          if (-1 != i) {
            l.remove(i);
          }
        }
      }
    }

    /**
     * Clear all event handlers from this importer. Calling this
     * method effectively unlinks all exported event handlers from
     * this event handler.
     */
    void clear() {
      synchronized (lock) {
        references = null;
      }
    }

    /**
     * Get the name of the imported event handler.
     *
     * @return  The name of the imported event handler.
     */
    public String getName() {
      return name;
    }

    /**
     * Get the component of the imported event handler.
     *
     * @return     The component of the imported event handler.
     */
    public Component getComponent() {
      return Component.this;
    }

    /**
     * Test whether this importer is linked.
     *
     * @return  <code>true</code> if this importer is linked.
     */
    public boolean isLinked() {
      synchronized (lock) {
        return (null != references);
      }
    }

    /**
     * Get the number of exported event handlers currently linked by
     * this importer.
     *
     * @return  The number of exported event handlers currently linked
     *          by this importer.
     */
    public int getLinkedNumber() {
      synchronized (lock) {
        if (null == references) {
          return 0;
        } else if (references instanceof HandlerReference) {
          return 1;
        } else {
          return ((ArrayList)references).size();
        }
      }
    }

    /**
     * Get the references for this importer. This method returns a
     * list of handler references representing the exported event
     * handlers currently linked to the imported event handler
     * represented by this importer.
     *
     * @see     Component.HandlerReference
     * @return     A list of handler references.
     */
    public List getHandlers() {
      synchronized (lock) {
        if (null == references) {
          return Collections.EMPTY_LIST;

        } else if (references instanceof HandlerReference) {
          ArrayList l = new ArrayList(1);
          l.add(references);
          return l;

        } else {
          return (ArrayList)((ArrayList)references).clone();
        }
      }
    }

    /**
     * Test whether this importer is a queued event handler. Even
     * though this class implements the
     * <code>QueuedEventHandler</code> interface, not all event
     * handlers imported by this importer need to be queued event
     * handers. This method returns <code>true</code> if all imported
     * event handlers are queued event handlers.
     *
     * @return     <code>true</code> if this importer is linked to only
     *             queued event handlers.
     * @throws  NotLinkedException
     *             Signals that this importer is not linked.
     */
    public boolean isQueued() {
      synchronized (lock) {
        if (null == references) {
          throw new NotLinkedException("Imported event handler " + name +
                                       " not linked");

        } else if (references instanceof HandlerReference) {
          return (((HandlerReference)references).handler instanceof
                  QueuedEventHandler);

        } else {
          ArrayList l = (ArrayList)references;
          int       s = l.size();

          for (int i=0; i<s; i++) {
            if (! (((HandlerReference)l.get(i)).handler instanceof
                   QueuedEventHandler)) {
              return false;
            }
          }

          return true;
        }
      }
    }

    /**
     * Test whether this importer effectively is a wrapped event
     * handler. This method returns <code>true</code> if all event
     * handlers imported by this importer are wrapped event handlers.
     *
     * @see     Domain#isWrapped(EventHandler)
     *
     * @return     <code>true</code> if this importer is linked to only
     *             wrapped event handlers.
     * @throws  NotLinkedException
     *             Signals that this importer is not linked.
     */
    public boolean isWrapped() {
      synchronized (lock) {
        if (null == references) {
          throw new NotLinkedException("Imported event handler " + name +
                                       " not linked");

        } else if (references instanceof HandlerReference) {
          HandlerReference handler = (HandlerReference)references;

          return Domain.isWrapped(handler.handler);

        } else {
          ArrayList l = (ArrayList)references;
          int       s = l.size();

          for (int i=0; i<s; i++) {
            HandlerReference handler = (HandlerReference)l.get(i);

            if (! Domain.isWrapped(handler.handler)) {
              return false;
            }
          }

          return true;
        }
      }
    }

    /**
     * Test whether this importer effectively is active. This method
     * returns <code>true</code> if all event handlers imported by
     * this importer are active.
     *
     * @see     Domain#isActive(EventHandler)
     *
     * @return     <code>true</code> if this importer is linked to only
     *             wrapped event handlers.
     * @throws  NotLinkedException
     *             Signals that this importer is not linked.
     */
    public boolean isActive() {
      synchronized (lock) {
        if (null == references) {
          throw new NotLinkedException("Imported event handler " + name +
                                       " not linked");

        } else if (references instanceof HandlerReference) {
          HandlerReference handler = (HandlerReference)references;

          return Domain.isActive(handler.handler);

        } else {
          ArrayList l = (ArrayList)references;
          int       s = l.size();

          for (int i=0; i<s; i++) {
            HandlerReference handler = (HandlerReference)l.get(i);

            if (! Domain.isActive(handler.handler)) {
              return false;
            }
          }

          return true;
        }
      }
    }

    /** Handle the specified event. */
    public void handle(Event e) {
      Object r = references;

      if (r instanceof HandlerReference) {
        // The common case, handled first and withou a lock.
        ((HandlerReference)r).handler.handle(e);
        return;

      } else if (null == r) {
        // The handler is not linked.
        throw new NotLinkedException("Imported event handler " + name +
                                     " not linked");

      } else {
        // It was a list of importer references a while ago, so now we
        // synchronize and and do everything over again.
        synchronized (lock) {
          if (null == references) {
            throw new NotLinkedException("Imported event handler " + name +
                                         " not linked");

          } else if (references instanceof HandlerReference) {
            r = references;

          } else {
            // Copy references, so that we can walk the list outside
            // the monitor.
            r = ((ArrayList)references).clone();
          }
        }

        // Invoke handlers outside monitor.
        if (r instanceof HandlerReference) {
          ((HandlerReference)r).handler.handle(e);

        } else {
          ArrayList l = (ArrayList)r;
          int       s = l.size();

          for (int i=0; i<s; i++) {
            ((HandlerReference)l.get(i)).handler.handle(e);
          }
        }
      }
    }

    /**
     * Handle the specified event first.
     *
     * <p>Even though this class implements the
     * <code>QueuedEventHandler</code> interface, not all event
     * handlers imported by this importer need to be queued event
     * handlers. This method signals an illegal state exception if any
     * of the currently imported event handlers is not a queued event
     * handler.</p>
     *
     * @see     #isQueued()
     *
     * @throws  IllegalStateException
     *            Signals that this importer is not solely linked to
     *            queued event handler(s).
     */
    public void handleFirst(Event e) {
      Object r = references;

      if (r instanceof HandlerReference) {
        // The common case, handled first and without a lock.
        EventHandler eh = ((HandlerReference)r).handler;

        if (eh instanceof QueuedEventHandler) {
          ((QueuedEventHandler)eh).handleFirst(e);
          return;
        } else {
          throw new IllegalStateException("Imported event handler " + name +
                                          " not a queued event handler");
        }

      } else if (null == r) {
        // The handler is not linked.
        throw new NotLinkedException("Imported event handler " + name +
                                     " not linked");

      } else {
        // It was a list of importer references a while ago, so now we
        // synchronize and and do everything over again.
        synchronized (lock) {
          if (null == references) {
            throw new NotLinkedException("Imported event handler " + name +
                                         " not linked");

          } else if (references instanceof HandlerReference) {
            r = references;

          } else {
            // Copy references, so that we can walk the list outside
            // the monitor.
            r = ((ArrayList)references).clone();
          }
        }

        // Invoke handlers outside monitor.
        if (r instanceof HandlerReference) {
          EventHandler eh = ((HandlerReference)r).handler;

          if (eh instanceof QueuedEventHandler) {
            ((QueuedEventHandler)eh).handleFirst(e);
          } else {
            throw new IllegalStateException("Imported event handler " + name +
                                            " not a queued event handler");
          }

        } else {
          ArrayList l = (ArrayList)r;
          int       s = l.size();

          // First check...
          for (int i=0; i<s; i++) {
            HandlerReference hr = (HandlerReference)l.get(i);

            if (! (hr.handler instanceof QueuedEventHandler)) {
              throw new IllegalStateException("Imported event handler " + name +
                                              " not a queued event handler");
            }
          }

          // ...then invoke.
          for (int i=0; i<s; i++) {
            HandlerReference hr = (HandlerReference)l.get(i);

            ((QueuedEventHandler)hr.handler).handleFirst(e);
          }
        }
      }
    }

    /**
     * Forcibly handle the specified event.
     *
     * <p>Even though this class implements the
     * <code>QueuedEventHandler</code> interface, not all event
     * handlers imported by this importer need to be queued event
     * handlers. This method signals an illegal state exception if any
     * of the currently imported event handlers is not a queued event
     * handler.</p>
     *
     * @see     #isQueued()
     *
     * @throws  IllegalStateException
     *            Signals that this importer is not solely linked to
     *            queued event handler(s).
     */
    public void handleForced(Event e) {
      Object r = references;

      if (r instanceof HandlerReference) {
        // The common case, handled first and without a lock.
        EventHandler eh = ((HandlerReference)r).handler;

        if (eh instanceof QueuedEventHandler) {
          ((QueuedEventHandler)eh).handleForced(e);
          return;
        } else {
          throw new IllegalStateException("Imported event handler " + name +
                                          " not a queued event handler");
        }

      } else if (null == r) {
        // The handler is not linked.
        throw new NotLinkedException("Imported event handler " + name +
                                     " not linked");

      } else {
        // It was a list of importer references a while ago, so now we
        // synchronize and and do everything over again.
        synchronized (lock) {
          if (null == references) {
            throw new NotLinkedException("Imported event handler " + name +
                                         " not linked");

          } else if (references instanceof HandlerReference) {
            r = references;

          } else {
            // Copy references, so that we can walk the list outside
            // the monitor.
            r = ((ArrayList)references).clone();
          }
        }

        // Invoke handlers outside monitor.
        if (r instanceof HandlerReference) {
          EventHandler eh = ((HandlerReference)r).handler;

          if (eh instanceof QueuedEventHandler) {
            ((QueuedEventHandler)eh).handleForced(e);
          } else {
            throw new IllegalStateException("Imported event handler " + name +
                                            " not a queued event handler");
          }

        } else {
          ArrayList l = (ArrayList)r;
          int       s = l.size();

          // First check...
          for (int i=0; i<s; i++) {
            HandlerReference hr = (HandlerReference)l.get(i);

            if (! (hr.handler instanceof QueuedEventHandler)) {
              throw new IllegalStateException("Imported event handler " + name +
                                              " not a queued event handler");
            }
          }

          // ...then invoke.
          for (int i=0; i<s; i++) {
            HandlerReference hr = (HandlerReference)l.get(i);

            ((QueuedEventHandler)hr.handler).handleForced(e);
          }
        }
      }
    }

  }


  // =======================================================================
  //          Internal records for keeping track of event handlers
  // =======================================================================

  /**
   * The base class for maintaining relevant internal state on
   * imported and exported event handlers.
   */
  static class EventHandlerRecord implements java.io.Serializable {

    /** Serial version ID for this class. */
    static final long serialVersionUID = 835671251515782546L;

    /**
     * The descriptor for this event handler.
     *
     * @serial  Must be a valid event handler descriptor.
     */
    final EventHandlerDescriptor descriptor;

    /**
     * Create a new event handler record. The event handler descriptor
     * must be a valid descriptor.
     *
     * @param  descriptor  The descriptor for the corresponding
     *                     event handler.
     */
    EventHandlerRecord(EventHandlerDescriptor descriptor) {
      this.descriptor = descriptor;
    }
  }

  // =======================================================================

  /** The internal state for an imported event handler. */
  static final class ImportedRecord extends EventHandlerRecord {

    /** Serial version ID for this class. */
    static final long serialVersionUID = -8038187400840906141L;

    /**
     * The importer for this imported event handler.
     *
     * @serial  Must not be <code>null</code>.
     */
    final Importer importer;

    /**
     * Create a new imported event handler. The descriptor must be a
     * valid descriptor for an imported event handler.
     *
     * @param  descriptor  The descriptor for the imported event handler.
     * @param  importer    The importer for the imported event handler.
     */
    ImportedRecord(ImportedDescriptor descriptor, Importer importer) {
      super(descriptor);
      this.importer = importer;
    }

  }

  // =======================================================================

  /** The internal state for an exported event handler. */
  static final class ExportedRecord extends EventHandlerRecord {

    /** Serial version ID for this class. */
    static final long serialVersionUID = 3083755522730147160L;

    /**
     * The actual event handler.
     *
     * @serial  Must not be <code>null</code>.
     */
    final EventHandler handler;
    

    /**
     * Create a new exported event handler. The descriptor must be a
     * valid descriptor for an exported event handler and the actual
     * event handler must not be <code>null</code>.
     *
     * @param  descriptor  The descriptor for the exported event handler.
     * @param  handler     The actual event handler.
     */
    ExportedRecord(ExportedDescriptor descriptor,
                   EventHandler       handler) {
      super(descriptor);
      this.handler = handler;
    }
  }


  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The environment for this component.
   *
   * @serial  Must not be <code>null</code>.
   */
  transient Environment    env;

  /**
   * The mapping from event handler names to their implementation.
   *
   * @serial  Must not be <code>null</code>. The keys in this mapping
   *          must be strings representing the names of imported and
   *          exported event handlers and the values must be the
   *          corresponding instances of <code>ImportedRecord</code>
   *          and <code>ExportedRecord</code>.
   */
  private final HashMap    handlers;

  /**
   * Flag for whether this component is sealed. It is impossible to
   * add or remove declared event handlers from a sealed component or
   * to link or unlink event handlers from a component.
   *
   * @serial
   */
  private boolean          sealed;


  // =======================================================================
  //                            Constructors
  // =======================================================================

  /**
   * Create a new component. This constructor is called from within
   * the constructor for environments, which needs to patch up the
   * <code>env</code> field itself.
   */
  Component() {
    // Environments have only three handlers: main, request, and
    // monitor.
    handlers = new HashMap(6);
  }

  /**
   * Create a new component.
   *
   * @param   env  The environment for this component.
   * @throws  NullPointerException
   *               Signals that <code>env</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *               Signals that the specified environment is
   *               multi-threaded, even though components of this
   *               class are not thread safe, or that the protection
   *               domain for this component is not the environment's
   *               protection domain.
   * @throws  IllegalStateException
   *               Signals that the specified environment is being
   *               or has been destroyed, or that the system has
   *               shut down.
   */
  public Component(Environment env) {
    if (null == env) {
      throw new NullPointerException("Null environment");
    }

    // Register component.
    env.register(this);

    this.env = env;
    handlers = new HashMap();
  }


  // =======================================================================
  //                           Serialization
  // =======================================================================

  /**
   * Serialize this component.
   *
   * @serialData  The default fields followed by the environment field
   *              while holding the monitor for <code>handlers</code>.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    synchronized (handlers) {
      out.defaultWriteObject();
      if (this instanceof Environment) {
        out.writeObject(null); // Don't write root env.
      } else {
        out.writeObject(env);
      }
    }
  }

  /** Deserialize a component. */
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {

    in.defaultReadObject();
    env = (Environment)in.readObject();
    // Sanity check.
    if ((null == env) && (! (this instanceof Environment))) {
      throw new Bug("Serialized component with null environment pointer " +
                    "not an environment (" + this + ")");
    }

    // Note that components are not registered during deserialization.
  }


  // =======================================================================
  //                        Hashcode and equality
  // =======================================================================

  /**
   * Determine whether this component equals the specified object. A
   * component equals another object if and only if that object is the
   * same object as the component. This restriction is necessary for
   * internal book-keeping and reasonable because components implement
   * services and not data structures.
   */
  public final boolean equals(Object o) {
    return (this == o);
  }

  /** Get a hash code for this component. */
  public final int hashCode() {
    return super.hashCode();
  }


  // =======================================================================
  //                        Component properties
  // =======================================================================

  /**
   * Get the component descriptor for this component.
   *
   * @return  The component descriptor for this component.
   */
  public abstract ComponentDescriptor getDescriptor();


  // =======================================================================
  //                           Environments
  // =======================================================================

  /**
   * Get the environment for this component.
   *
   * @return  The environment for this component.
   */
  public final Environment getEnvironment() {
    return env;
  }


  // =======================================================================
  //                 Imported and exported event handlers
  // =======================================================================

  /**
   * Declare the specified imported event handler.
   *
   * <p>Actual components, that is, concrete implementations of this
   * class, use this method to declare an imported event handler.  The
   * specified descriptor must be a valid event handler descriptor and
   * should be statically allocated within the actual component, yet
   * not be visible outside that class.</p>
   *
   * <p>While the returned event handler implements the queued event
   * handler interface, not all of the linked event handlers need to
   * be queued event handlers. If not all of the linked event handlers
   * are queued event handlers, invoking the
   * <code>handleFirst()</code> or <code>handleForced()</code> methods
   * results in an illegal state exception.</p>
   *
   * <p>The returned event handler throws a {@link NotLinkedException}
   * on invocation of its event handling methods if this event handler
   * has not been linked against an exported event handler.</p>
   *
   * <p>Furthermore, the returned event handler may also throw the
   * following runtime exceptions on invocation of its event handling
   * methods:<ul>
   *
   * <li>A {@link NoBufferSpaceException} if the event is being sent
   * to an environment whose event queue is full.</li>
   *
   * <li>A {@link NotActiveException} if the event is being sent to
   * an environment that is not currently active.</li>
   *
   * <li>An <code>IllegalStateException</code> if the event is being
   * sent to an environment that has no loaded code (for example,
   * because it has been unloaded).</li>
   *
   * </ul>For each of these exceptional conditions, <i>one.world</i>
   * always attempts to deliver the exceptional condition as an
   * exceptional event to the source of the event. However, if the
   * exceptional event cannot be delivered, for example, because the
   * originating environment's event queue is also full, the
   * exceptional condition is signaled directly through a regular Java
   * exception.</p>
   *
   * @see     #undeclare
   *
   * @param   descriptor  The descriptor for the imported event handler.
   * @return              The event handler for invoking the imported
   *                      event handler.
   * @throws  NullPointerException
   *                      Signals that <code>descriptor</code> is
   *                      <code>null</code>.
   * @throws  IllegalArgumentException
   *                      Signals that <code>descriptor</code> is
   *                      invalid or that this component already has
   *                      an event handler with the specified name.
   * @throws  IllegalStateException
   *                      Signals that this method has been invoked
   *                      on a sealed component.
   */
  protected final Importer declareImported(ImportedDescriptor descriptor) {
    // Perform basic consistency checking.
    if (null == descriptor) {
      throw new NullPointerException("Null imported event handler descriptor");
    } else {
      try {
        descriptor.validate();
      } catch (TupleException x) {
        throw new IllegalArgumentException("Invalid imported event handler "
                                           + "descriptor: " + x.getMessage());
      }
    }

    // Try to add new imported event handler.
    Importer       importer = new Importer(descriptor.name, descriptor.linkOne);
    ImportedRecord record   = new ImportedRecord(descriptor, importer);

    synchronized (handlers) {
      if (sealed) {
        throw new IllegalStateException("Unable to declare event handler for" +
                                        " sealed component (" + this + ")");
      } else if (handlers.containsKey(descriptor.name)) {
        throw new IllegalArgumentException("Duplicate event handler (" +
                                           descriptor.name + ")");
      }

      handlers.put(descriptor.name, record);
    }

    return record.importer;
  }

  /**
   * Declare the specified exported event handler.
   *
   * <p>Actual components, that is, concrete implementations of this
   * class, use this method to declare an exported event handler.  The
   * specified descriptor must be a valid event handler descriptor and
   * should be statically allocated within the actual component, yet
   * not be visible outside that class.</p>
   *
   * @see     #undeclare
   *
   * @param   descriptor  The descriptor for the exported event handler.
   * @param   handler     The actual exported event handler.
   * @return              <code>handler</code>.
   * @throws  NullPointerException
   *                      Signals that <code>descriptor</code> or
   *                      <code>handler</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                      Signals that <code>descriptor</code> is
   *                      invalid or that this component already has
   *                      an event handler with the specified name.
   * @throws  IllegalStateException
   *                      Signals that this method has been invoked on
   *                      a sealed component.
   */
  protected final EventHandler declareExported(ExportedDescriptor descriptor,
                                               EventHandler handler) {
    // Perform basic consistency checking.
    if (null == descriptor) {
      throw new NullPointerException("Null exported event handler descriptor");
    } else if (null == handler) {
      throw new NullPointerException("Null event handler for exported event " +
                                     "handler");
    } else {
      try {
        descriptor.validate();
      } catch (TupleException x) {
        throw new IllegalArgumentException("Invalid exported event handler "
                                           + "descriptor: " + x.getMessage());
      }
    }

    // Try to add new exported event handler.
    ExportedRecord record = new ExportedRecord(descriptor, handler);

    synchronized (handlers) {
      if (sealed) {
        throw new IllegalStateException("Unable to declare event handler for" +
                                        " sealed component (" + this + ")");
      } else if (handlers.containsKey(descriptor.name)) {
        throw new IllegalArgumentException("Duplicate event handler (" +
                                           descriptor.name + ")");
      }

      handlers.put(descriptor.name, record);
    }

    return handler;
  }


  /**
   * Undeclare the event handler with the specified name. This method
   * removes a previously declared imported or exported event handler
   * from this component.
   *
   * @see     #declareImported
   * @see     #declareExported
   *
   * @param   name  The name of the event handler to undeclare.
   * @throws  NullPointerException
   *                Signals that <code>name</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                Signals that this component has no imported or
   *                exported event handler with the specified name.
   * @throws  IllegalStateException
   *                Signals that this method has been invoked on a
   *                sealed component.
   */
  protected final void undeclare(String name) {
    // Basic consistency check.
    if (null == name) {
      throw new NullPointerException("Null name");
    }

    synchronized (handlers) {
      if (sealed) {
        throw new IllegalStateException("Unable to undeclare event handler " +
                                        "for sealed component (" + this + ")");
      } else if (! handlers.containsKey(name)) {
        throw new IllegalArgumentException("Unrecognized event handler name ("
                                           + name + ")");
      } else {
        handlers.remove(name);
      }
    }
  }

  /**
   * Replace the main imported event handler, the request exported
   * event handler, and the monitor imported event handler with the
   * specified event handlers. This method is called by
   * <code>Environment</code> when restoring a saved check-point to
   * install the saved event handlers instead of the current ones.
   *
   * @param   main     The new main imported event handler.
   * @param   request  The new request exported event handler.
   * @param   monitor  The new monitor imported event handler.
   * @throws  NullPointerException
   *                   Signals a <code>null</code> event handler.
   */
  final void replace(Importer main, EventHandler request, Importer monitor) {
    if (null == main) {
      throw new NullPointerException("Null main imported event handler");
    } else if (null == request) {
      throw new NullPointerException("Null request exported event handler");
    } else if (null == monitor) {
      throw new NullPointerException("Null monitor imported event handler");
    } else if (! (this instanceof Environment)) {
      throw new Bug("Component.replace() called on component that is not " +
                    "an environment");
    }

    synchronized (handlers) {
      ImportedRecord mainRecord    = (ImportedRecord)handlers.get("main");
      ExportedRecord requestRecord = (ExportedRecord)handlers.get("request");
      ImportedRecord monitorRecord = (ImportedRecord)handlers.get("monitor");

      handlers.put("main",    new
        ImportedRecord((ImportedDescriptor)mainRecord.descriptor,    main));
      handlers.put("request", new
        ExportedRecord((ExportedDescriptor)requestRecord.descriptor, request));
      handlers.put("monitor", new
        ImportedRecord((ImportedDescriptor)monitorRecord.descriptor, monitor));
    }
  }
  
  /**
   * Get a list of the names of the event handlers for this component.
   * This method returns a list of the names of the event handlers
   * imported and exported by this component.
   *
   * @return  A list of the names of the event handlers for this
   *          component.
   */
  public final List eventHandlers() {
    synchronized (handlers) {
      return new ArrayList(handlers.keySet());
    }
  }

  /**
   * Determine whether this component has an event handler with
   * the specified name.
   *
   * @return  <code>true</code> if this component has an event handler
   *          with the specified name.
   */
  public final boolean hasEventHandler(String name) {
    synchronized (handlers) {
      return handlers.containsKey(name);
    }
  }

  /**
   * Get the event handler descriptor for the event handler with the
   * specified name.
   *
   * @param   name  The name of the event handler.
   * @return        The corresponding event handler descriptor.
   * @throws  NullPointerException
   *                Signals that <code>name</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                Signals that this component does not have
   *                an event handler with the specified name.
   */
  public final EventHandlerDescriptor getDescriptor(String name) {
    if (null == name) {
      throw new NullPointerException("Null event handler name");
    }

    EventHandlerRecord record;
    synchronized (handlers) {
      record = (EventHandlerRecord)handlers.get(name);
    }

    if (null == record) {
      throw new IllegalArgumentException("No such event handler ("+name+")");
    } else {
      EventHandlerDescriptor desc =
        (EventHandlerDescriptor)record.descriptor.clone();
      if (null != desc.types) {
        desc.types = (Class[])desc.types.clone();
      }
      if (null != desc.conditions) {
        desc.conditions = (Class[])desc.conditions.clone();
      }
      return desc;
    }
  }


  // =======================================================================
  //                                Linking
  // =======================================================================

  /**
   * Link the specified event handler imported by this component with
   * the specified event handler exported by the specified component.
   * Linking an imported event handler with an exported event handler
   * it is already linked with to has no effect.
   *
   * @param   imported   The name of the event handler imported by this
   *                     component.
   * @param   exported   The name of the event handler exported by
   *                     <code>component</code>.
   * @param   component  The component exporting event handler
   *                     <code>exported</code>.
   * @throws  LinkingException
   *                     Signals that an event handler does not exist,
   *                     that an environment is not being linked
   *                     against a component within the same
   *                     environment, that an event handler is not
   *                     imported/exported when an exported/imported
   *                     event handler is required, that
   *                     <code>imported</code> has already been linked
   *                     and cannot be linked again, or that this
   *                     component is sealed.
   */
  public final void link(String imported, String exported,
                         Component component) {

    link(imported, exported, component, false);
  }

  /**
   * Link the specified event handler imported by this component with
   * the specified event handler exported by the specified component.
   * Linking an imported event handler with an exported event handler
   * it is already linked with has no effect.
   *
   * <p>If the component exporting the exported event handler is in a
   * different concurrency domain than this component, all invocations
   * to the exported event handler must go through the animator for
   * the exported event handler's concurrency domain. If
   * <code>forced</code> is <code>true</code>, invocations to the
   * exported event handler always go through an animator, even if
   * both the imported and exported event handlers are in the same
   * concurrency domain.</p>
   *
   * <p>Note that if either the imported event handler or the exported
   * event handler specify that they need to be forcibly linked,
   * invocations to the exported event handler will always go through
   * an animator, even if this method is invoked with
   * <code>false</code> for <code>forced</code>.</p>
   *
   * @see     EventHandlerDescriptor#linkForced
   *
   * @param   imported   The name of the event handler imported by this
   *                     component.
   * @param   exported   The name of the event handler exported by
   *                     <code>component</code>.
   * @param   component  The component exporting event handler
   *                     <code>exported</code>.
   * @param   forced     Flag to indicate whether the exported event
   *                     handler should forcibly be accessed through
   *                     the corresponding animator.
   * @throws  LinkingException
   *                     Signals that an event handler does not exist,
   *                     that an environment is not being linked
   *                     against a component within the same
   *                     environment, that an event handler is not
   *                     imported/exported when an exported/imported
   *                     event handler is required, that
   *                     <code>imported</code> has already been linked
   *                     and cannot be linked again, or that this
   *                     component is sealed.
   */
  public final void link(String imported, String exported,
                         Component component, boolean forced) {
    // Environments can only be linked against components within the
    // same environment.
    if (this instanceof Environment) {
      if (this != component.env) {
        throw new LinkingException("Environments can only be linked against " +
                                   "components within the same environment");
      }
    } else if (component instanceof Environment) {
      if (component != env) {
        throw new LinkingException("Environments can only be linked against " +
                                   "components within the same environment");
      }
    }

    // Get the imported and exported record.
    ExportedRecord exportt = getExported(exported, component);
    ImportedRecord importt = getImported(imported, this);
    
    // Determine the real value for forced.
    forced = forced ||
      exportt.descriptor.linkForced || importt.descriptor.linkForced;

    // Set up the wrapper, if necessary.
    Wrapper        wrapper = env.getWrapper(component.env, forced);
    EventHandler   handler = exportt.handler;

    if (null != wrapper) {
      handler              = wrapper.wrap(handler);
    }

    // Do the actual linking.
    importt.importer.add(exported, component, handler);
  }

  /**
   * Unlink the specified event handler exported by the specified
   * component from the event handler imported by this component.
   * Unlinking an imported event handler from an exported event
   * handler it is not linked with has no effect.
   *
   * @param   imported   The name of the event handler imported by this
   *                     component.
   * @param   exported   The name of the event handler exported by
   *                     <code>component</code>.
   * @param   component  The component exporting event handler
   *                     <code>exported</code>.
   * @throws  LinkingException
   *                     Signals that an event handler does not exist,
   *                     that an event handler is not imported/exported
   *                     when an exported/imported event handler is
   *                     required, that <code>imported</code> has
   *                     not been linked against <code>exported</code>,
   *                     or that this component is sealed.
   */
  public final void unlink(String imported, String exported,
                           Component component) {
    getExported(exported, component); // Just to check it exists.
    getImported(imported, this).importer.remove(exported, component);
  }

  /**
   * Determine whether this component is sealed.
   *
   * @see     #seal()
   *
   * @return  <code>true</code> if this component is sealed.
   */
  public final boolean isSealed() {
    synchronized (handlers) {
      return sealed;
    }
  }

  /**
   * Seal this component. A sealed component cannot be linked or
   * unlinked again. Sealing an already sealed component has no
   * effect.
   *
   * @throws  LinkingException
   *             Signals that this component is not fully linked.
   */
  public final void seal() {
    synchronized (handlers) {
      if (sealed) {
        return;
      } else if (! isFullyLinked()) {
        throw new LinkingException("Component not fully linked");
      }

      sealed = true;
    }
  }

  /**
   * Determine whether the specified imported event handler is linked.
   *
   * @param   name  The name of the imported event handler.
   * @return        <code>true</code> if the imported event handler is
   *                linked.
   * @throws  IllegalArgumentException
   *                Signals that this component does not have an
   *                event handler with the specified name or that the
   *                specified event handler is exported.
   */
  public final boolean isLinked(String name) {
    EventHandlerRecord record;

    synchronized (handlers) {
      record = (EventHandlerRecord)handlers.get(name);
    }

    if (null == record) {
      throw new IllegalArgumentException("No such imported event handler (" +
                                         name + ")");
    } else if (record instanceof ExportedRecord) {
      throw new IllegalArgumentException("Exported event handler instead of " +
                                         "imported event handler (" + name +
                                         ")");
    }

    return ((ImportedRecord)record).importer.isLinked();
  }

  /**
   * Determine whether this component is fully linked. This component
   * is fully linked if every imported event handler is linked against
   * at least one exported event handler.
   *
   * @return  <code>true</code> if this component is fully linked.
   */
  public final boolean isFullyLinked() {
    synchronized (handlers) {
      Iterator iter = handlers.values().iterator();

      while (iter.hasNext()) {
        EventHandlerRecord record = (EventHandlerRecord)iter.next();

        if (record instanceof ImportedRecord) {
          if (! ((ImportedRecord)record).importer.isLinked()) {
            return false;
          }
        }
      }
    }

    return true;
  }

  /**
   * Get the number of event handlers currently linked to the imported
   * event handler with the specified name.
   *
   * @param   name  The name of the imported event handler.
   * @return        The number of event handlers currently linked to the
   *                imported event handler.
   * @throws  NullPointerException
   *                Signals that <code>name</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                Signals that this component does not have an event
   *                handler with the specified name or that the the
   *                event handler with the specified name is exported.
   */
  public final int getLinkedNumber(String name) {
    if (null == name) {
      throw new NullPointerException("Null event handler name");
    }

    EventHandlerRecord record;
    synchronized (handlers) {
      record = (EventHandlerRecord)handlers.get(name);
    }

    if (null == record) {
      throw new IllegalArgumentException("No such event handler ("+name+")");
    } else if (record instanceof ExportedRecord) {
      throw new IllegalArgumentException("Exported event handler instead of " +
                                         "imported event handler ("+name+")");
    } else {
      return ((ImportedRecord)record).importer.getLinkedNumber();
    }
  }

  /**
   * Get a list of event handlers currently linked to the imported
   * event handler with the specified name. The returned list is a
   * list of references to exported event handlers.
   *
   * @see           Component.HandlerReference
   *
   * @param   name  The name of the imported event handler.
   * @return        A list of handler references.
   * @throws  NullPointerException
   *                Signals that <code>name</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                Signals that this component does not have an event
   *                handler with the specified name or that the the
   *                event handler with the specified name is exported.
   */
  public final List getLinkedHandlers(String name) {
    if (null == name) {
      throw new NullPointerException("Null event handler name");
    }

    EventHandlerRecord record;
    synchronized (handlers) {
      record = (EventHandlerRecord)handlers.get(name);
    }

    if (null == record) {
      throw new IllegalArgumentException("No such event handler ("+name+")");
    } else if (record instanceof ExportedRecord) {
      throw new IllegalArgumentException("Exported event handler instead of " +
                                         "imported event handler ("+name+")");
    } else {
      return ((ImportedRecord)record).importer.getHandlers();
    }
  }

  /**
   * Wrap the specified event handler. If the specified event handler
   * has not yet been wrapped, this method returns a wrapped version
   * of the specified event handler that uses this component's
   * concurrency domain.  If the specified event handler has already
   * been wrapped, it is simply returned.
   *
   * @param   handler  The event handler to be wrapped.
   * @return           The wrapped event handler.
   */
  protected final EventHandler wrap(EventHandler handler) {
    if (! ConcurrencyDomain.isWrapped(handler)) {
      return env.getWrapper(env, true).wrap(handler);
    } else {
      return handler;
    }
  }


  // =======================================================================
  //                                 Time
  // =======================================================================

  /**
   * Get the timer for this component's environment. Each environment
   * has its own timer component, which is allocated on demand. Note
   * that the implementation of this method is not just returning the
   * value of a field. Components that use the timer repeatedly should
   * probably call this method once in their constructor and then
   * store the returned reference in a private field.
   *
   * @return   The timer for this component's environment.
   */
  protected final Timer getTimer() {
    return (Timer)AccessController.doPrivileged(new PrivilegedAction() {
        public Object run() {
          return env.getTimer1();
        }
      });
  }


  // =======================================================================
  //                                 Threads
  // =======================================================================

  /**
   * Execute the specified runnable in a new thread. Some applications
   * (such as a HTTP server supporting persistent connections) have a
   * legitimate need to spawn their own threads. While this practice
   * is generally discouraged, this method lets applications spawn a
   * new thread if they have the proper permission.
   *
   * @param   r  The runnable.
   * @throws  NullPointerException
   *             Signals that <code>r</code> is <code>null</code>.
   * @throws  SecurityException
   *             Signals that the caller does not have permission to
   *             spawn new threads.
   */
  protected final void run(Runnable r) {
    SecurityManager security = System.getSecurityManager();
    if (null != security) {
      security.checkPermission(SystemPermission.SPAWN_THREAD);
    }
    if (null == r) {
      throw new NullPointerException("Null runnable");
    }

    new Thread(r, "#[Application thread " + env.id + "]").start();
  }


  // =======================================================================
  //                                 Boxes
  // =======================================================================

  /**
   * Unbox the specified box. This method returns the object embedded
   * in the specified box. Calling this method on the same box twice
   * results in distinct objects.
   *
   * @param   box  The box to unbox.
   * @return       The embedded object.
   * @throws  IOException
   *               Signals an exceptional condition while deserializing
   *               the embedded object.
   * @throws  ClassNotFoundException
   *               Signals that a class necessary for instantiating the
   *               embedded object could not be found.
   */
  protected final Object unbox(Box box)
    throws IOException, ClassNotFoundException {

    // Get the class loader for this protection domain.
    final ClassLoader    loader = env.protection.loader;

    // Create an object input stream that uses the class loader to
    // resolve classes.
    ByteArrayInputStream b      = new ByteArrayInputStream(box.bytes);
    ObjectInputStream    in     = new ObjectInputStream(b) {
        protected Class resolveClass(ObjectStreamClass k)
          throws IOException, ClassNotFoundException {
          
          return Class.forName(k.getName(), false, loader);
        }
      };

    // Read and return the embedded object.
    return in.readObject();
  }


  // =======================================================================
  //                              Static helpers
  // =======================================================================

  /**
   * Get the specified exported event handler record.
   *
   * @param   name       The name of the exported event handler.
   * @param   component  The component of the exported event handler.
   * @return             The corresponding exported event handler record.
   * @throws  LinkingException
   *                     Signals that the specified component does
   *                     not have an exported event handler with the
   *                     specified name.
   */
  private static ExportedRecord getExported(String name, Component component) {
    Object record;

    synchronized (component.handlers) {
      record = component.handlers.get(name);
    }

    if (null == record) {
      throw new LinkingException("No such exported event handler ("+name+")");
    } else if (record instanceof ImportedRecord) {
      throw new LinkingException("Imported event handler instead of exported "
                                 + "event handler (" + name + ")");
    }

    return (ExportedRecord)record;
  }

  /**
   * Get the specified imported event handler record. This method also
   * ensure that the specified component is not sealed.
   *
   * @param   name         The name of the imported event handler.
   * @param   component    The component of the imported event handler.
   * @return               The corresponding imported event handler record.
   * @throws  LinkingException
   *                       Signals that the specified component does not
   *                       have an imported event handler with the
   *                       specified name or that the specified
   *                       component is sealed.
   */
  private static ImportedRecord getImported(String name, Component component) {
    Object record;

    synchronized (component.handlers) {
      if (component.sealed) {
        throw new LinkingException("Component sealed");
      }

      record = component.handlers.get(name);
    }

    if (null == record) {
      throw new LinkingException("No such imported event handler ("+name+")");
    } else if (record instanceof ExportedRecord) {
      throw new LinkingException("Exported event handler instead of imported "
                                 + "event handler (" + name + ")");
    }

    return (ImportedRecord)record;
  }

}
