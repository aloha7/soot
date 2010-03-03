/* jaDTi package - v0.6.1 */

/*
 *  Copyright (c) 2004, Jean-Marc Francois.
 *
 *  This file is part of jaDTi.
 *  jaDTi is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  jaDTi is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jahmm; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 */

package be.ac.ulg.montefiore.run.jadti;


/**
 * This class implements the value of a symbolic attribute.
 **/
public final class UnknownSymbolicValue
    extends SymbolicValue {
    
    public boolean isUnknown() {
	return true;
    }

    /**
     * Compares this value to another.  Behaves like <code>Object.equals</code>.
     *
     * @param o The object to test.
     * @return <code>true</code> iff <code>o == this</code>.
     **/
    public boolean equals(Object o) {
	return super.equals(o);
    }

    public String toString() {
	return "?";
    }
}
