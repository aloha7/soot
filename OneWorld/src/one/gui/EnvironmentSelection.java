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

package one.gui;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import one.util.Bug;
import one.util.Guid;

import one.world.core.Environment;

/**
 * Implementation of an environment selection. An environment
 * selection is a transferabled that conveys the ID of an
 * environment. Environment selections support three data flavors.
 * The {@link #getPreferredFlavor() preferred flavor} is the
 * environment selection itself. To aid with debugging, environment
 * selections also support {@link #getStringFlavor() string} and
 * {@link #getTextFlavor() plain text} data flavors. Note that while
 * the latter data flavor is deprecated by Sun, its support is
 * necessary for dropping environment selections into native
 * applications (as of JDK 1.3). Further note, that the transfer data
 * for the plain text data flavor is a reader not an input stream.
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public class EnvironmentSelection
  implements Transferable, ClipboardOwner, java.io.Serializable {

  /** The preferred data flavor describing an environment selection. */
  static final DataFlavor PREFERRED_FLAVOR = new
    DataFlavor(EnvironmentSelection.class, "An environment ID");

  /** The string data flavor. */
  static final DataFlavor STRING_FLAVOR = new
    DataFlavor(String.class, "A Unicode string");

  /** The plain text data flavor. */
  static final DataFlavor TEXT_FLAVOR = new
    DataFlavor("text/plain; charset=unicode", "Plain text");

  /**
   * The environment ID.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final Guid id;

  /**
   * Create a new environment selection for the specified environment.
   *
   * @param   env  The environment.
   * @throws  NullPointerException
   *               Signals that <code>env</code> is <code>null</code>.
   */
  public EnvironmentSelection(Environment env) {
    id = env.getId();
  }

  /**
   * Create a new environment selection for the environment with the
   * specified ID.
   *
   * @param   id  The environment ID.
   * @throws  NullPointerException
   *              Signals that <code>id</code> is <code>null</code>.
   */
  public EnvironmentSelection(Guid id) {
    if (null == id) {
      throw new NullPointerException("Null environment ID");
    }
    this.id = id;
  }

  /**
   * Get the environment ID.
   *
   * @return  The environment ID.
   */
  public Guid getId() {
    return id;
  }

  /** Return a hash code for this environment selection. */
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * Determine whether this environment selection equals the specified
   * object.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (! (o instanceof EnvironmentSelection)) return false;
    return id.equals(((EnvironmentSelection)o).id);
  }

  /** Return the data flavors supported by this environment selection. */
  public DataFlavor[] getTransferDataFlavors() {
    return new DataFlavor[] { getPreferredFlavor(),
                              getStringFlavor(),
                              getTextFlavor()       };
  }

  /** Determine whether the specified data flavor is supported. */
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return (PREFERRED_FLAVOR.equals(flavor) ||
            STRING_FLAVOR.equals(flavor) ||
            TEXT_FLAVOR.equals(flavor));
  }

  /** Get the transfer data for the specified data flavor. */
  public Object getTransferData(DataFlavor flavor)
    throws UnsupportedFlavorException {

    if (PREFERRED_FLAVOR.equals(flavor)) {
      return this;
    } else if (STRING_FLAVOR.equals(flavor)) {
      return toString();
    } else if (TEXT_FLAVOR.equals(flavor)) {
      return new java.io.StringReader(toString());
    } else {
      throw new UnsupportedFlavorException(flavor);
    }
  }

  /** Handle a lost ownership notification. */
  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    // Like we care.
  }

  /** Return a string representation for this environment selection. */
  public String toString() {
    return id.toString();
  }

  /**
   * Get the preferred data flavor for environment selection
   * transferables. The preferred data flavor for environment
   * selections has the
   * "<code>application/x-java-serialized-object</code>" MIME type and
   * this class as its representation class. In other words, the
   * preferred data flavor for an environment selection is the
   * environment selection itself.
   *
   * <p>The preferred data flavor is defined by a static method that
   * returns a copy of an internal value rather than being defined by
   * a publically accessible static field, because data flavors are
   * externalizable, which makes them mutable.</p>
   * 
   * @return  The preferred data flavor for environment selections.
   */
  public static DataFlavor getPreferredFlavor() {
    try {
      return (DataFlavor)PREFERRED_FLAVOR.clone();
    } catch (CloneNotSupportedException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    }
  }

  /**
   * Get the string data flavor. Environment selections can be
   * represented by a string, which is the string representation of
   * the environment ID.
   *
   * <p>The string data flavor is defined by a static method that
   * returns a copy of an internal value rather than relying on
   * <code>DataFlavor</code>'s predefined constant, because data
   * flavors are externalizalbe, which makes them mutable.</p>
   *
   * @return  The string data flavor.
   */
  public static DataFlavor getStringFlavor() {
    try {
      return (DataFlavor)STRING_FLAVOR.clone();
    } catch (CloneNotSupportedException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    }
  }

  /**
   * Get the plain text data flavor. Environment selections can
   * be represented as plain text, which simply is the string
   * representation of the environment ID.
   *
   * <p>The plain text data flavor is defined by a static method that
   * returns a copy of an internal value rather than relying on 
   * <code>DataFlavor</code>'s predefined constants, because data
   * flavors are externalizable, which makes them mutable.</p>
   *
   * @return  The plain text data flavor.
   */
  public static DataFlavor getTextFlavor() {
    try {
      return (DataFlavor)TEXT_FLAVOR.clone();
    } catch (CloneNotSupportedException x) {
      throw new Bug("Unexpected exception (" + x + ")");
    }
  }

}


