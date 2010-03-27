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

package one.tools;

import java.io.Writer;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import one.world.core.Event;
import one.world.core.EventHandler;
import one.world.core.ExceptionalEvent;

/**
 * Implementation of a utility program to generate skeleton component
 * source files. The main method for this class generates a skeleton
 * source file for a component, based on a very simple description,
 * which is read in from another file.
 *
 * @version  $Revision: 1.3 $
 * @author   Robert Grimm
 */
public final class Skeletor {

  /** Hide the constructor. */
  private Skeletor() {
    // Nothing to do.
  }

  /** The copyright statement. */
  private static final String[] COPYRIGHT = new String[] {
    "/*",
    " * Copyright (c) 2001, University of Washington, Department of",
    " * Computer Science and Engineering.",
    " * All rights reserved.",
    " *",
    " * Redistribution and use in source and binary forms, with or without",
    " * modification, are permitted provided that the following conditions",
    " * are met:",
    " *",
    " * 1. Redistributions of source code must retain the above copyright",
    " * notice, this list of conditions and the following disclaimer.",
    " *",
    " * 2. Redistributions in binary form must reproduce the above",
    " * copyright notice, this list of conditions and the following",
    " * disclaimer in the documentation and/or other materials provided",
    " * with the distribution.",
    " *",
    " * 3. Neither name of the University of Washington, Department of",
    " * Computer Science and Engineering nor the names of its contributors",
    " * may be used to endorse or promote products derived from this",
    " * software without specific prior written permission.",
    " *",
    " * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS",
    " * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT",
    " * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS",
    " * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE",
    " * REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,",
    " * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES",
    " * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR",
    " * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)",
    " * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,",
    " * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)",
    " * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED",
    " * OF THE POSSIBILITY OF SUCH DAMAGE.",
    " */"};

  /** The import statements for the one.world.core package. */
  private static final String[] IMPORT_CORE = new String[] {
    "import one.world.core.Component;",
    "import one.world.core.ComponentDescriptor;",
    "import one.world.core.Environment;",
    "import one.world.core.Event;",
    "import one.world.core.EventHandler;",
    "import one.world.core.ExportedDescriptor;",
    "import one.world.core.ImportedDescriptor;"
  };

  /** The import statements for the one.world.util package. */
  private static final String[] IMPORT_UTIL = new String[] {
    "import one.world.util.AbstractHandler;"
  };

  /** The body of an exported event handler. */
  private static final String[] HANDLER = new String[] {
    "    /** Handle the specified event. */",
    "    protected boolean handle1(Event e) {",
    "",
    "      // XXX",
    "",
    "      return false;",
    "    }",
    "  }"
  };

  /** The accessor for the component descriptor. */
  private static final String[] DESCRIPTOR = new String[] {
    "  /** Get the component descriptor. */",
    "  public ComponentDescriptor getDescriptor() {",
    "    return (ComponentDescriptor)SELF.clone();",
    "  }"
  };

  /** The new line separator. */
  private static final String NEWLINE = System.getProperty("line.separator");

  /** The writer to write the component skeleton to. */
  private static Writer    out;

  /** The package of the component. */
  private static String    pack;

  /** The name of the component. */
  private static String    name;

  /** The description of the component. */
  private static String    description;

  /** The thread-safe flag for the component. */
  private static boolean   threadSafe;

  /** The list of exported event handlers. */
  private static List      exported;

  /** the list of link-forced flags for the exported event handlers. */
  private static List      exportedLinkForced;

  /** The list of descriptions for the exported event handlers. */
  private static List      exportedDescription;

  /** The list of names for the imported event handlers. */
  private static List      imported;

  /** The list of link-forced flags for the imported event handlers. */
  private static List      importedLinkForced;

  /** The list of link-one flags for the imported event handlers. */
  private static List      importedLinkOne;

  /** The list of descriptions for the imported event handlers. */
  private static List      importedDescription;

  /** Write the skeleton source file. */
  private static void writeClass() throws IOException {
    writeHead();
    writeHandlers();
    writeDescriptors();
    writeInstances();
    writeConstructor();
    writeTail();
  }

  /** Write the head. */
  private static void writeHead() throws IOException {
    write(COPYRIGHT);
    write();
    write("package " + pack + ";");
    write();
    if (! "one.world.core".equals(pack)) {
      write(IMPORT_CORE);
      write();
    }
    if (! "one.world.util".equals(pack)) {
      write(IMPORT_UTIL);
      write();
    }
    writeComment();
    write("public final class " + name + " extends Component {");
    write();
  }

  /** Write the class comment. */
  private static void writeComment() throws IOException {
    write("/**");
    write(" * XXX --- Modify code that is marked with XXX");
    if ((0 != exported.size()) || (0 != imported.size())) {
      write(" *");
      write(" * <p><b>Imported and Exported Event Handlers</b></p>");
    }
    if (0 != exported.size()) {
      write(" *");
      write(" * <p>Exported event handlers:<dl>");
      for (int i=0; i<exported.size(); i++) {
        write(" *    <dt>" + (String)exported.get(i) + "</dt>");
        write(" *    <dd> XXX");
        write(" *        </dd>");
      }
      write(" * </dl></p>");
    }
    if (0 != imported.size()) {
      write(" *");
      write(" * <p>Imported event handlers:<dl>");
      for (int i=0; i<imported.size(); i++) {
        write(" *    <dt>" + (String)imported.get(i) + "</dt>");
        write(" *    <dd> XXX");
        write(" *        </dd>");
      }
      write(" * </dl></p>");
    }
    write(" *");
    write(" * @version  $" + "Revision$");
    write(" * @author   XXX");
    write(" */");
  }

  /** Write the nested exported event handlers. */
  private static void writeHandlers() throws IOException {
    for (int i=0; i<exported.size(); i++) {
      writeHandler((String)exported.get(i));
    }
  }

  /** Write the nested exported event handler with the specified name. */
  private static void writeHandler(String name) throws IOException {
    writeComment("The " + name + " handler");
    write();
    write("  /** The " + name + " exported event handler. */");
    write("  final class " + upFirst(name) + "Handler extends " +
          "AbstractHandler {");
    write();
    write(HANDLER);
    write();
    write();
  }

  /** Write the descriptors. */
  private static void writeDescriptors() throws IOException {
    writeComment("Descriptors");
    write();
    write("  /** The component descriptor. */");
    write("  private static final ComponentDescriptor SELF =");
    write("    new ComponentDescriptor(\"" + pack + "." + name + "\",");
    write("                            \"" + description + "\",");
    write("                            " + (threadSafe?"true":"false") + ");");
    write();
    for (int i=0; i<exported.size(); i++) {
      writeExported((String)exported.get(i), 
                    (String)exportedDescription.get(i),
                    ((Boolean)exportedLinkForced.get(i)).booleanValue());
    }
    for (int i=0; i<imported.size(); i++) {
      writeImported((String)imported.get(i),
                    (String)importedDescription.get(i),
                    ((Boolean)importedLinkForced.get(i)).booleanValue(),
                    ((Boolean)importedLinkOne.get(i)).booleanValue());
    }
    write();
  }

  /** Write the specified exported event handler descriptor. */
  private static void writeExported(String name, String description,
                                    boolean linkForced)
    throws IOException {

    write("  /** The exported event handler descriptor for the " + name +
          " handler. */");
    write("  private static final ExportedDescriptor " + name.toUpperCase() +
          " =");
    write("    new ExportedDescriptor(\"" + name + "\",");
    write("                           \"" + description + "\",");
    write("                           null,   // XXX");
    write("                           null,   // XXX");
    write("                           " + (linkForced?"true":"false") + ");");
    write();
  }

  /** Write the specified imported event handler descriptor. */
  private static void writeImported(String name, String description,
                                    boolean linkForced, boolean linkOne) 
    throws IOException {

    write("  /** The imported event handler descriptor for the " + name +
          " handler. */");
    write("  private static final ImportedDescriptor " + name.toUpperCase() +
          " =");
    write("    new ImportedDescriptor(\"" + name + "\",");
    write("                           \"" + description + "\",");
    write("                           null,   // XXX");
    write("                           null,   // XXX");
    write("                           " + (linkForced?"true":"false") + ",");
    write("                           " + (linkOne?"true":"false") + ");");
    write();
  }

  /** Write the instance fields for the imported event handlers. */
  private static void writeInstances() throws IOException {
    if ((0 == exported.size()) && (0 == imported.size())) {
      return;
    }

    writeComment("Instance fields");
    write();
    for (int i=0; i<exported.size(); i++) {
      write("  /**");
      write("   * The " + (String)exported.get(i) + " exported event handler.");
      write("   *");
      write("   * @serial  Must not be <code>null</code>.");
      write("   */");
      write("  final EventHandler       " + (String)exported.get(i) +
            ";");
      write();
    }

    for (int i=0; i<imported.size(); i++) {
      write("  /**");
      write("   * The " + (String)imported.get(i) + " imported event handler.");
      write("   *");
      write("   * @serial  Must not be <code>null</code>.");
      write("   */");
      write("  final Component.Importer " + (String)imported.get(i) +
            ";");
      write();
    }
    write();
  }

  /** Write the constructor. */
  private static void writeConstructor() throws IOException {
    writeComment("Constructor");
    write();
    write("  /**");
    write("   * Create a new instance of <code>" + name + "</code>.");
    write("   *");
    write("   * @param  env  The environment for the new instance.");
    write("   */");
    write("  public " + name + "(Environment env) {");
    write("    super(env);");
    for (int i=0; i<exported.size(); i++) {
      write("    " + (String)exported.get(i) + " = declareExported(" +
            ((String)exported.get(i)).toUpperCase() + ", new " +
            upFirst((String)exported.get(i)) + "Handler());");
    }
    for (int i=0; i<imported.size(); i++) {
      write("    " + (String)imported.get(i) + " = declareImported(" +
            ((String)imported.get(i)).toUpperCase() + ");");
    }
    write("  }");
    write();
    write();
  }

  /** Write the tail. */
  private static void writeTail() throws IOException {
    writeComment("Component support");
    write();
    write(DESCRIPTOR);
    write();
    write("}");
  }

  /** Write the specified comment. */
  private static void writeComment(String comment) throws IOException {
    write("  // ====================================================" +
          "===================");
    write("  //                           " + comment);
    write("  // ====================================================" +
          "===================");
  }

  /** Write a newline. */
  private static void write() throws IOException {
    out.write(NEWLINE);
  }

  /** Write the specified string. */
  private static void write(String s) throws IOException {
    out.write(s);
    out.write(NEWLINE);
  }
  
  /** Write the specified string array. */
  private static void write(String[] a) throws IOException {
    for (int i=0; i<a.length; i++) {
      write(a[i]);
    }
  }

  /** Return the specified string with the first character capitalized. */
  private static String upFirst(String s) {
    return (new Character(Character.toUpperCase(s.charAt(0)))).toString()
      + s.substring(1);
  }

  /** Consume the rest of the specified string tokenizer into a string. */
  private static String consume(StringTokenizer tok) {
    StringBuffer buf = null;

    while (tok.hasMoreTokens()) {
      if (null == buf) {
        buf = new StringBuffer();
      } else {
        buf.append(' ');
      }
      buf.append(tok.nextToken());
    }

    if (null == buf) {
      return "";
    } else {
      return buf.toString();
    }
  }

  /**
   * Run skeletor with the specified arguments. If no arguments are
   * specified, this method writes a description of how to run it to
   * the standard output. Otherwise, it treats the first argument as
   * the name of a file that describes the component to be created and
   * creates the skeleton source file for that component. The
   * resulting source file is written to the current directory and has
   * the name of the component appended with "<code>.java</code>".
   *
   * @param  args  The arguments.
   */
  public static void main(String[] args) {
    // Do we need to print instructions?
    if (0 == args.length) {
      System.out.println();
      System.out.println("Skeletor generates a source file skeleton for a " +
                         "new component. It");
      System.out.println("takes a single argument, which is the name of a " +
                         "file describing the");
      System.out.println("new component. That file has the following format:");
      System.out.println();
      System.out.println("component <package-name> <class-name> " +
                         "<thread-safe> <description>");
      System.out.println("(import <name> <link-forced> <link-one> " +
                         "<description>)*");
      System.out.println("(export <name> <link-forced> <description>)*");
      System.out.println();
      System.out.println("<thread-safe>, <link-forced>, and <link-one> must " +
                         "be \"true\" or \"false\".");
      System.out.println("<description> may contain spaces and is " +
                         "terminated by the end of the");
      System.out.println("line. Error checking is minimal.");

      return;
    }

    // Initialize the lists.
    exported            = new ArrayList();
    exportedLinkForced  = new ArrayList();
    exportedDescription = new ArrayList();
    imported            = new ArrayList();
    importedLinkForced  = new ArrayList();
    importedLinkOne     = new ArrayList();
    importedDescription = new ArrayList();

    try {
      // Open the specified file with the description of the
      // component.
      BufferedReader in = new
        BufferedReader(new
          InputStreamReader(new
            FileInputStream(args[0])));

      // Read the description in and fill in the internal fields.
      String s = in.readLine();
      while (null != s) {

        // Consume line in individual tokens.
        StringTokenizer tok  = new StringTokenizer(s, " ");
        String          type = tok.nextToken();

        if ("component".equals(type)) {                   // Component.
          pack = tok.nextToken();       // Package name.
          name = tok.nextToken();       // Class name.
          type = tok.nextToken();       // Thread-safe?
          if ("true".equals(type)) {
            threadSafe = true;
          } else if ("false".equals(type)) {
            threadSafe = false;
          } else {
            System.out.println("\"" + type + "\" neither \"true\" nor " +
                               "\"false\"");
            return;
          }
          description  = consume(tok);

        } else if ("import".equals(type)) {               // Imported handler.
          imported.add(tok.nextToken());  // Handler name.
          type = tok.nextToken();         // Link-forced?
          if ("true".equals(type)) {
            importedLinkForced.add(Boolean.TRUE);
          } else if ("false".equals(type)) {
            importedLinkForced.add(Boolean.FALSE);
          } else {
            System.out.println("\"" + type + "\" neither \"true\" nor " +
                               "\"false\"");
            return;
          }
          type = tok.nextToken();         // Link-one?
          if ("true".equals(type)) {
            importedLinkOne.add(Boolean.TRUE);
          } else if ("false".equals(type)) {
            importedLinkOne.add(Boolean.FALSE);
          } else {
            System.out.println("\"" + type + "\" neither \"true\" nor " +
                               "\"false\"");
            return;
          }
          importedDescription.add(consume(tok));

        } else if ("export".equals(type)) {               // Exported handler.
          exported.add(tok.nextToken());  // Handler name.
          type = tok.nextToken();         // Link-forced?
          if ("true".equals(type)) {
            exportedLinkForced.add(Boolean.TRUE);
          } else if ("false".equals(type)) {
            exportedLinkForced.add(Boolean.FALSE);
          } else {
            System.out.println("\"" + type + "\" neither \"true\" nor " +
                               "\"false\"");
            return;
          }
          exportedDescription.add(consume(tok));

        } else {
          System.out.println("Line type \"" + type + "\" unrecognized!");
          return;
        }

        s = in.readLine();
      }
      
      // Create the source file and write it out.
      out = new BufferedWriter(new
        OutputStreamWriter(new
          FileOutputStream(name + ".java")));
      writeClass();
      out.flush();
      out.close();

    } catch (IOException x) {
      System.out.println(x.toString());
    } catch (NoSuchElementException x) {
      System.out.println("Incomplete line!");
    }
  }

}
