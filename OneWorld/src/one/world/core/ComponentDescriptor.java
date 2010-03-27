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

/**
 * Implementation of a component descriptor. A component descriptor
 * describes a component by providing its name, a short description of
 * the component, and a flag for whether the component is
 * concurrency-safe. The event handlers imported and exported by a
 * component are described by a separate set of event handler
 * descriptors. The component descriptor and the event handler
 * descriptors for a component are accessible through the component
 * class.
 *
 * @see      EventHandlerDescriptor
 * @see      Component
 *
 * @version  $Revision: 1.7 $
 * @author   Robert Grimm
 */
public final class ComponentDescriptor extends Descriptor {

  /** The serial version ID for this class. */


  /**
   * Flag for whether the component is concurrency-safe. A component
   * is concurrency-safe if it can concurrently process multiple
   * events, because, for example, it uses locks to protect access to
   * its internal modifiable data structures.
   *
   * @serial
   */
  public boolean concurrencySafe;

  /** Create a new, empty component descriptor. */
  public ComponentDescriptor() {
    // Nothing to do.
  }

  /**
   * Create a new component descriptor.
   *
   * @param   name             The name of the component.
   * @param   description      A short description of the component.
   * @param   concurrencySafe  The flag for whether the component is
   *                           concurrency-safe.
   */
  public ComponentDescriptor(String name, String description,
                             boolean concurrencySafe) {
    super(name, description);
    this.concurrencySafe = concurrencySafe;
  }

}
