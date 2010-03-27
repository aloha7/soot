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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;

import one.util.Guid;

import one.world.binding.ResourceRevokedException;

import one.world.util.ExceptionHandler;
import one.world.util.NullHandler;

/**
 * The superclass of all domains.
 *
 * @version  $Revision: 1.11 $
 * @author   Robert Grimm
 */
public abstract class Domain {

  // =======================================================================
  //                            Call wrapper
  // =======================================================================

  /**
   * The wrapper for wrapping event handlers passed across concurrency
   * domains.
   */
  final static class Call implements Wrapper, java.io.Serializable {

    /** Serial version ID for this class. */
    static final long serialVersionUID = -2483038199152722648L;

    /** The event handler returned by a call wrapper. */
    final class Handler implements QueuedEventHandler, java.io.Serializable {

      /** Serial version ID for this class. */
      static final long serialVersionUID = 6052520645150987740L;
      
      /**
       * The actual event handler for this handler.
       *
       * @serial  Must not be <code>null</code>.
       */
      final EventHandler handler;
      
      /**
       * Create a new handler. The specified event handler must not be
       * <code>null</code>.
       *
       * @param  handler  The event handler for the new handler.
       */
      Handler(EventHandler handler) {
        this.handler = handler;
      }

      /**
       * Replace this handler with the canonical null handler if its
       * environment is not in the current serialization set. Request
       * handlers for environments are not replaced, if a request
       * handler's environment is in the serialization set.
       */
      private Object writeReplace() throws ObjectStreamException {
        if (! Environment.isInSerializationSet(concurrency.id)) {
          // The request handlers for the environments being
          // serialized are executing as part of the root environment
          // and are thus not in the serialization set. However, they
          // need to be serialized as well.
          if (handler instanceof Environment.RequestHandler) {
            Environment.RequestHandler rh = (Environment.RequestHandler)handler;
            if (Environment.isInSerializationSet(rh.getEnvironment().id)) {
              return this;
            }
          }

          // Return the canonical null handler.
          return NullHandler.NULL;
        }

        return this;
      }

      /**
       * Get the call-back wrapper for this event handler.
       *
       * @return  The call-back wrapper for this event handler.
       */
      Call getCallback() {
        return callback;
      }
      
      /**
       * Get the concurrency domain for this event handler.
       *
       * @return  The concurrency domain for this event handler.
       */
      ConcurrencyDomain getConcurrencyDomain() {
        return concurrency;
      }

      /**
       * Get the protection domain for this event handler.
       *
       * @return  The protection domain for this event handler.
       */
      ProtectionDomain getProtectionDomain() {
        return protection;
      }
      
      /**
       * Handle the specified event.
       *
       * <p>This method implements the following error handling
       * strategy. If this method cannot deliver the event because (1)
       * it cannot be projected, (2) the concurrency domain of the
       * targeted event handler is inactive, or (3) the animator's
       * queue is full, it attempts to deliver an exceptional event to
       * the source of the specified event. In order to avoid infinite
       * recursions (which did appear in a prior version of this
       * method), this method attempts to enqueue the exceptional
       * event directly in the animator of the source of the specified
       * event and does so forcibly. However, if the animator of the
       * source of the specified event is also inactive, it raises a
       * direct exception.</p>
       */
      public void handle(Event e) {
        // Wrap the event.
        e.wrap(callback);

        // Project the event.
        if (project) {
          ClassLoader loader = protection.loader;

          try {
            // Make sure the class loader exists.
            if (null == loader) {
              throw new IllegalStateException("Protection domain " +
                                              protection.id +
                                              ": No code loaded");
            }

            // Perform the actual projection.
            e = Tuple.project(e, loader, protection.id);
          } catch (IllegalStateException x) {
            raise(e.source, e.closure, x);
            return;
          }
        }

        // Deliver the event.
        Animator anim     = concurrency.anim;
        boolean  inactive = false;

        if (null == anim) {
          inactive = true;
        } else {
          try {
            if (anim.enqueue(handler, e)) {
              return;
            }
          } catch (IllegalStateException x) {
            inactive = true;
          }
        }

        if (inactive) {
          if (concurrency instanceof NestedConcurrencyDomain) {
            // A root environment service has been terminated.
            NestedConcurrencyDomain c = (NestedConcurrencyDomain)concurrency;

            raise(e.source, e.closure, new
              ResourceRevokedException(c.service));
          } else {
            raise(e.source, e.closure, new
              NotActiveException(concurrency.id.toString()));
          }

        } else {
          raise(e.source, e.closure, new
            NoBufferSpaceException(concurrency.id.toString()));
        }
      }

      /**
       * Handle the specified event first.
       *
       * <p>This method uses the same error handling strategy as
       * <code>handle(Event)</code>.</p>
       */
      public void handleFirst(Event e) {
        // Wrap the event.
        e.wrap(callback);

        // Project the event.
        if (project) {
          ClassLoader loader = protection.loader;

          try {
            // Make sure the class loader exists.
            if (null == loader) {
              throw new IllegalStateException("Protection domain " +
                                              protection.id +
                                              ": No code loaded");
            }

            // Perform the actual projection.
            e = Tuple.project(e, loader, protection.id);
          } catch (IllegalStateException x) {
            raise(e.source, e.closure, x);
            return;
          }
        }

        // Deliver the event.
        Animator anim     = concurrency.anim;
        boolean  inactive = false;

        if (null == anim) {
          inactive = true;
        } else {
          try {
            if (anim.enqueueFirst(handler, e)) {
              return;
            }
          } catch (IllegalStateException x) {
            inactive = true;
          }
        }

        if (inactive) {
          if (concurrency instanceof NestedConcurrencyDomain) {
            // A root environment service has been terminated.
            NestedConcurrencyDomain c = (NestedConcurrencyDomain)concurrency;

            raise(e.source, e.closure, new
              ResourceRevokedException(c.service));
          } else {
            raise(e.source, e.closure, new
              NotActiveException(concurrency.id.toString()));
          }

        } else {
          raise(e.source, e.closure, new
            NoBufferSpaceException(concurrency.id.toString()));

        }
      }

      /**
       * Forcibly handle the specified event.
       *
       * <p>This method uses the same error handling strategy as
       * <code>handle(Event)</code> if the concurrency domain of the
       * targeted event handler is inactive. If it is active, however,
       * the event is forcibly enqueued.</p>
       */
      public void handleForced(Event e) {
        // Wrap the event.
        e.wrap(callback);

        // Project the event.
        if (project) {
          ClassLoader loader = protection.loader;

          try {
            // Make sure the class loader exists.
            if (null == loader) {
              throw new IllegalStateException("Protection domain " +
                                              protection.id +
                                              ": No code loaded");
            }

            // Perform the actual projection.
            e = Tuple.project(e, loader, protection.id);
          } catch (IllegalStateException x) {
            raise(e.source, e.closure, x);
            return;
          }
        }

        // Deliver the event.
        Animator anim = concurrency.anim;
        
        if (null != anim) {
          try {
            anim.enqueueForced(handler, e);
            return;
          } catch (IllegalStateException x) {
            // Continue below.
          }
        }

        if (concurrency instanceof NestedConcurrencyDomain) {
          // A root environment service has been terminated.
          NestedConcurrencyDomain c = (NestedConcurrencyDomain)concurrency;
          
          raise(e.source, e.closure, new
            ResourceRevokedException(c.service));
        } else {
          raise(e.source, e.closure, new
            NotActiveException(concurrency.id.toString()));
        }
      }

      /**
       * Raise the specified exception. This method attempts to send
       * an exceptional event wrapping the specified exception to the
       * specified event handler. If the specified event handler is
       * not a wrapped event handler, or if the wrapped handler's
       * protection domain does not have a class loader, or the
       * wrapped handler's concurrency domain is inactive, or the
       * specified closure cannot be projected into the wrapped
       * handler's protection domain, an exception is thrown directly.
       * The thrown exception is the specified exception if it is a
       * runtime exception and an illegal state exception if the
       * specified exception is not a runtime exception.
       *
       * @param   h        The event handler to send the exception to.
       * @param   closure  The closure for the exceptional event.
       * @param   x        The exception to raise.
       */
      private void raise(EventHandler h, Object closure, Exception x) {

        // Do we have a wrapped event handler?
        if ((null == h) || (! (h instanceof Handler))) {
          // We can't deliver the exception.
          if (x instanceof RuntimeException) {
            throw (RuntimeException)x;
          } else {
            throw new IllegalStateException(x.toString());
          }
        }

        // Determine the protection and concurrency domain.
        Handler          handler = (Handler)h;
        ProtectionDomain prot    = handler.getProtectionDomain();
        ClassLoader      loader  = prot.loader;
        Animator         anim    = handler.getConcurrencyDomain().anim;

        // Do we have a class loader and animator?
        if ((null == loader) || (null == anim)) {
          // We can't deliver the exception.
          if (x instanceof RuntimeException) {
            throw (RuntimeException)x;
          } else {
            throw new IllegalStateException(x.toString());
          }
        }

        // Project the closure.
        if (null != closure) {
          try {
            closure = Tuple.project(closure, loader, prot.id);
          } catch (IllegalStateException xx) {
            // We can't project the closure.
            if (x instanceof RuntimeException) {
              throw (RuntimeException)x;
            } else {
              throw new IllegalStateException(x.toString());
            }
          }
        }

        // Send the exceptional event.
        anim.enqueueForced(handler.handler, new
          ExceptionalEvent(ExceptionHandler.HANDLER, closure, x));
      }

      /** Get a string representation of this call handler. */
      public String toString() {
        return "#[Wrapped handler " + handler + " for concurrency domain " +
          concurrency.id + "]";
      }

    }

    /**
     * The callback wrapper for this call wrapper.
     *
     * @serial  Must not be <code>null</code>.
     */
    final     Call              callback;
    
    /**
     * The concurrency domain exporting the event handlers wrapped by
     * this call wrapper. This field is only non-final because it is
     * explicitly restored during deserialization.
     */
    transient ConcurrencyDomain concurrency;

    /**
     * The protection domain exporting the event handlers wrapped by
     * this call wrapper. This field is only non-final because it is
     * explicitly restored during deserialization.
     */
    transient ProtectionDomain  protection;

    /**
     * The flag for whether to project events before delivering them
     * to the event handlers wrapped by this call wrapper.
     *
     * @see  Tuple#projectTo
     */
    final     boolean           project;

    /**
     * Create a new call wrapper.
     *
     * @param   callback     The call-back wrapper for the new call
     *                       wrapper.
     * @param   concurrency  The concurrency domain for the new call
     *                       wrapper.
     * @param   protection   The protection domain for the new call
     *                       wrapper.
     */
    private Call(Call callback,
                 ConcurrencyDomain concurrency,
                 ProtectionDomain  protection) {
      this.callback    = callback;
      this.concurrency = concurrency;
      this.protection  = protection;
      project          = (! protection.equals(callback.protection));
    }
    
    /**
     * Create a new call wrapper.
     *
     * @param   sourceC  The concurrency domain exporting the event handlers
     *                   to be wrapped by the new call wrapper.
     * @param   sourceP  The protection domain extporting the event handlers
     *                   to be wrapped by the new call wrapper.
     * @param   targetC  The concurrency domain importing the event handlers
     *                   to be wrapped by the new call wrapper.
     * @param   targetP  The protection domain importing the event handlers
     *                   to be wrapped by the new call wrapper.
     */
    Call(ConcurrencyDomain sourceC, ProtectionDomain sourceP,
         ConcurrencyDomain targetC, ProtectionDomain targetP) {
      concurrency = sourceC;
      protection  = sourceP;
      if (sourceC.equals(targetC)) {
        callback  = this;
      } else {
        callback  = new Call(this, targetC, targetP);
      }
      project     = (! sourceP.equals(targetP));
    }

    /**
     * Serialize this call wrapper.
     *
     * @serialData  The default fields, followed by the concurrency
     *              domain exporting the event handlers to be wrapped
     *              by this call wrapper or <code>null</code> if that
     *              concurrency domain is the root environment's
     *              concurrency domain, followed by the protection
     *              domain exporting the event handlers to be wrapped
     *              by this call wrapper of <code>null</code> if that
     *              protection domain is the root environment's
     *              protection domain.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
      out.defaultWriteObject();

      if (Environment.getRootConcurrency().equals(concurrency)) {
        out.writeObject(null);
      } else {
        out.writeObject(concurrency);
      }

      if (Environment.root.protection.equals(protection)) {
        out.writeObject(null);
      } else {
        out.writeObject(protection);
      }
    }

    /** Deserialize a call wrapper. */
    private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {

      in.defaultReadObject();

      Object o = in.readObject();
      if (null == o) {
        concurrency = Environment.getRootConcurrency();
      } else {
        concurrency = (ConcurrencyDomain)o;
      }

      o = in.readObject();
      if (null == o) {
        protection = Environment.root.protection;
      } else {
        protection = (ProtectionDomain)o;
      }
    }
    
    /** Wrap the specified event handler. */
    public EventHandler wrap(EventHandler handler) {
      if ((null == handler) || (handler instanceof SymbolicHandler)) {
        return handler;

      } else if (handler instanceof Handler) {
        // The handler has already been wrapped before. We need to
        // rewrap it, if callbacks are not wrapped for this call
        // wrapper's concurrency domain.
        Handler h        = (Handler)handler;
        Call    callback = h.getCallback();

        if (callback.concurrency.equals(this.concurrency)) {
          // Callbacks are wrapped for this call wrapper's concurrency
          // domain. No need to rewrap.
          return handler;
        } else {
          // We need a new call wrapper. The source is the original
          // concurrency domain of the handler. The target is this
          // call wrapper's concurrency domain. And, the handler to be
          // wrapped is the vanilla, i.e., unwrapped, handler.
          Call c = new Call(h.getConcurrencyDomain(), h.getProtectionDomain(),
                            concurrency,              protection);

          return c.wrap(h.handler);
        }

      } else {
        return new Handler(handler);
      }
    }
    
  }


  // =======================================================================
  //                            Instance fields
  // =======================================================================

  /**
   * The ID for this domain. Concrete subclasses of this class need to
   * explicitly serialize the ID. This field is only non-final so that
   * it can be restored during deserialization.
   */
  Guid id;


  // =======================================================================
  //                             Constructors
  // =======================================================================

  /** Create a new domain. */
  Domain() {
    id = new Guid();
  }

  /**
   * Create a new domain with the specified ID.
   *
   * @param  id  The ID for the newly created domain.
   */
  Domain(Guid id) {
    this.id = id;
  }


  // =======================================================================
  //                          Public functionality
  // =======================================================================

  /**
   * Get the ID for this domain.
   *
   * @return  The ID for this domain.
   */
  public final Guid getId() {
    return id;
  }

  /** Return a hash code for this domain. */
  public final int hashCode() {
    return id.hashCode();
  }

  /**
   * Determine whether this domain equals the specified object. This
   * domain equals the specified object if the specified object is a
   * domain with the same ID.
   */
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof Domain)) return false;
    return id.equals(((Domain)o).id);
  }


  // =======================================================================
  //                      Dealing with wrapped handlers
  // =======================================================================

  /**
   * Determine whether the specified event handler has been wrapped.
   * This method returns <code>true</code> if the specified event
   * handler has been wrapped, for example, because it has been passed
   * to a different concurrency domain than the one that implements
   * the handler.
   *
   * <p>Note that this method does <i>not</i> determine whether a
   * component importer is effectively wrapped and treats component
   * importers as not wrapped. {@link Component.Importer#isWrapped()}
   * must instead be used for component importers.</p>
   *
   * @param   handler  The event handler to test.
   * @return           <code>true</code> if the event handler has
   *                   been wrapped.
   */
  public static boolean isWrapped(EventHandler handler) {
    return (handler instanceof Call.Handler);
  }

  /**
   * Get the concurrency domain for the specified event handler. If
   * the specified event handler has been wrapped, this method returns
   * the event handler's concurrency domain.
   *
   * @see     #isWrapped(EventHandler)
   * 
   * @param   handler  The event handler.
   * @return           The event handler's concurrency domain.
   * @throws  IllegalArgumentException
   *                   Signals that the specified event handler has
   *                   not been wrapped.
   */
  public static ConcurrencyDomain getConcurrencyDomain(EventHandler handler) {
    if (handler instanceof Call.Handler) {
      return ((Call.Handler)handler).getConcurrencyDomain();
    } else {
      throw new IllegalArgumentException("Event handler has not been " +
                                         "wrapped (" + handler + ")");
    }
  }

  /**
   * Get the protection domain for the specified event handler. If
   * the specified event handler has been wrapped, this method returns
   * the event handler's protection domain.
   *
   * @see     #isWrapped(EventHandler)
   * 
   * @param   handler  The event handler.
   * @return           The event handler's protection domain.
   * @throws  IllegalArgumentException
   *                   Signals that the specified event handler has
   *                   not been wrapped.
   */
  public static ProtectionDomain getProtectionDomain(EventHandler handler) {
    if (handler instanceof Call.Handler) {
      return ((Call.Handler)handler).getProtectionDomain();
    } else {
      throw new IllegalArgumentException("Event handler has not been " +
                                         "wrapped (" + handler + ")");
    }
  }

  /**
   * Unwrap the specified event handler. If the specified event
   * handler has been wrapped, this method returns the original event
   * handler.
   *
   * @param   handler  The event handler.
   * @return           The original event handler.
   * @throws  SecurityException
   *                   Signals that the caller does not have
   *                   permission to manage environments.
   * @throws  IllegalArgumentException
   *                   Signals that the specified event handler has
   *                   not been wrapped.
   */
  public static EventHandler unwrap(EventHandler handler) {
    Environment.ensurePermission();

    if (handler instanceof Call.Handler) {
      return ((Call.Handler)handler).handler;
    } else {
      throw new IllegalArgumentException("Event handler has not been " +
                                         "wrapped (" + handler + ")");
    }
  }

  /**
   * Determine whether the specified event handler is actively
   * processing events. This method determines whether the specified
   * event handler is currently processing events. It distinguishes
   * between two types of event handlers. First, event handlers that
   * are not wrapped are always treated as active. Second, wrapped
   * event handlers are treated as active if the event handler's
   * concurrency domain is currently processing events. They are
   * treated as not active if the event handler's concurrency domain
   * is currently quiesced. Note that a concurrency domain is
   * temporarily quiesced in order to produce a consistent snapshot
   * while the corresponding environment is checkpointed, moved, or
   * copied.
   *
   * <p>Note that this method does <i>not</i> determine whether a
   * component importer is effectively active and always treats
   * component importers as active. {@link
   * Component.Importer#isActive()} must instead be used for component
   * importers.</p>
   *
   * @see     #isWrapped
   *
   * @param   handler  The event handler to test.
   * @return           <code>true</code> if the event handler is
   *                   currently processing events.
   * @throws  NotActiveException
   *                   Signals that the specified event handler is
   *                   a wrapped event handler whose environment is
   *                   not active (typically because it has been
   *                   terminated).
   */
  public static boolean isActive(EventHandler handler) {
    if (handler instanceof Call.Handler) {
      ConcurrencyDomain concurrency =
        ((Call.Handler)handler).getConcurrencyDomain();
      Animator          anim        = concurrency.anim;

      if (null == anim) {
        throw new NotActiveException(concurrency.id.toString());
      } else {
        return (Animator.ACTIVE == anim.getStatus());
      }

    } else {
      return true;
    }
  }

}
