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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * Implementation of a solid, rectangular icon. The icon's color can
 * be changed.
 *
 * @version  $Revision: 1.1 $
 * @author   Robert Grimm
 */
public class SolidIcon implements Icon, java.io.Serializable {

  /**
   * The solid icon's width.
   *
   * @serial  Must be a positive number.
   */
  private final int width;

  /**
   * The solid icon's height.
   *
   * @serial  Must be a positive number.
   */
  private final int height;

  /**
   * The current color.
   *
   * @serial  Must not be <code>null</code>.
   */
  private Color     color;

  /**
   * Create a new solid icon.
   *
   * @param   width
   * @param   height
   * @param   color
   * @throws  NullPointerException
   *                  Signals that <code>color</code> is <code>null</code>.
   * @throws  IllegalArgumentException
   *                  Signals that <code>width</code> or <code>height</code>
   *                  is not positive.
   */
  public SolidIcon(int width, int height, Color color) {
    if (null == color) {
      throw new NullPointerException("Null color");
    } else if (0 >= width) {
      throw new IllegalArgumentException("Non-positive width");
    } else if (0 >= height) {
      throw new IllegalArgumentException("Non-positive height");
    }

    this.width  = width;
    this.height = height;
    this.color  = color;
  }

  /**
   * Get this solid icon's current color.
   *
   * @return  The current color.
   */
  public Color getColor() {
    return color;
  }

  /**
   * Set this solid icon's current color.
   *
   * @param   color  The new color.
   * @throws  NullPointerException
   *                 Signals that <code>color</code> is <code>null</code>.
   */
  public void setColor(Color color) {
    if (null == color) {
      throw new NullPointerException("Null color");
    }

    this.color = color;
  }

  /** Paint this solid icon. */
  public void paintIcon(Component c, Graphics g, int x, int y) {
    g.setColor(color);
    g.fillRect(x, y, width, height);
  }

  /** Get this solid icon's width. */
  public int getIconWidth() {
    return width;
  }

  /** Get this solid icon's height. */
  public int getIconHeight() {
    return height;
  }

}


