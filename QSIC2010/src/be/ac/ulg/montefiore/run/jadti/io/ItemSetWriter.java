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

package be.ac.ulg.montefiore.run.jadti.io;

import be.ac.ulg.montefiore.run.jadti.*;
import java.io.*;
import java.util.*;


/**
 * This class converts an {@link ItemSet ItemSet} to database file.<p>
 * The database format is very simple. The first line is the name of the 
 * database. The second line contains the attribute names immediately followed
 * by their types: <tt>numerical</tt> or <tt>symbolic</tt>. Attribute name
 * should not contain any space or tab caracter.<p>
 * Each line corresponds to one item and contains the values of the
 * attributes separated by white spaces. The special value '?' denotes an
 * unknown value.<p>
 * If the first attribute name is object and its type is name, then the first 
 * column is the object name (otherwise the name will be compute on the fly
 * based on the line number).<p>
 * Comment lines start with a ';'.<p>
 */
public class ItemSetWriter {

    private static final char UNKNOWN_VALUE = '?';
    private static final String dbName = "Database";
    
        
    /**
     * Writes a database file.
     * 
     * @param writer Holds the observation sequence file writer.
     * @param set The set to write to file.
     */
    static public void write(Writer writer, ItemSet set) 
	throws IOException {
	
	writeFirstLine(writer);
	writeAttributesLine(writer, set);

	for (int i = 0; i < set.size(); i++)
	    writeItem(writer, set, set.item(i));
	    
	writer.close();
    }
    

    /* Initialize the syntax table of a stream tokenizer */
    static private void writeFirstLine(Writer writer) 
	throws IOException {
	writer.write("jaDTi_generated_database\n");
    }
    
    
    /* Writes the attribute line */
    static private void writeAttributesLine(Writer writer, ItemSet set) 
	throws FileFormatException, IOException {

	for (int i = 0; i < set.attributeSet().size(); i++) {
	    Attribute attribute = set.attributeSet().attribute(i);
	    
	    writer.write(attribute.name() + " " + 
			 ((attribute instanceof SymbolicAttribute) ?
			  "symbolic " : "numerical "));
	}
	
	writer.write("\n");
    }

    
    /* Writes an item */
    static private void writeItem(Writer writer, ItemSet set, Item item)
	throws FileFormatException, IOException {
	
	for (int i = 0; i < item.nbAttributes(); i++) {
	    AttributeValue value = item.valueOf(i);
	    
	    writer.write(value + " ");
	}
	
	writer.write("\n");
    }
}
