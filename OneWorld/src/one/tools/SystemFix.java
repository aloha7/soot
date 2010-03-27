/*
 * Copyright (c) 2002, Markus Dahm, Robert Grimm.
 *    All rights reserved.
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
 * 3. Neither name of Markus Dahm and Robert Grimm nor the names of
 * their contributors may be used to endorse or promote products
 * derived from this software without specific prior written
 * permission.
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

import de.fub.bytecode.Constants;
import de.fub.bytecode.classfile.ClassParser;
import de.fub.bytecode.classfile.Code;
import de.fub.bytecode.classfile.ConstantPool;
import de.fub.bytecode.classfile.JavaClass;
import de.fub.bytecode.classfile.Method;
import de.fub.bytecode.generic.ConstantPoolGen;
import de.fub.bytecode.generic.GETSTATIC;
import de.fub.bytecode.generic.Instruction;
import de.fub.bytecode.generic.InstructionConstants;
import de.fub.bytecode.generic.InstructionFactory;
import de.fub.bytecode.generic.InstructionHandle;
import de.fub.bytecode.generic.InstructionList;
import de.fub.bytecode.generic.INVOKESTATIC;
import de.fub.bytecode.generic.MethodGen;

/**
 * Utility to patch references to <code>java.lang.System</code> and
 * <code>java.lang.Thread</code> in class files.  This utility reads
 * the specified class files and replaces all invocations to
 * <code>arraycopy()</code>, <code>currentTimeMillis()</code>,
 * <code>getProperties</code>, and <code>getProptery()</code> in
 * <code>java.lang.System</code> with the appropriate invocations in
 * {@link one.world.util.SystemUtilities}.  Invocations to
 * <code>sleep()</code> in <code>java.lang.Thread</code> are replaced
 * as well.  Finally, references to <code>out</code> and
 * <code>err</code> in <code>java.lang.System</code> are mapped to
 * {@link one.world.Shell#console}.  Class files must be specified by
 * their file names, including the "<code>.class</code>" file name
 * extension. Note that this utility overwrites patched class files in
 * place.
 *
 * <p>This class depends on the <a
 * href="http://bcel.sourceforge.net/">Byte Code Engineering
 * Library</a> to perform the actual work.</p>
 * 
 * @author   Original code provided by
 *           <a href="http://www.inf.fu-berlin.de/~dahm">Markus Dahm</a>,
 *           with modifications by <a
 *           href="mailto:rgrimm@alum.mit.edu">Robert Grimm</a>.
 * @version  $Revision: 1.2 $
 */
public final class SystemFix {
  private static String             class_name;
  private static ConstantPoolGen    cp;
  private static InstructionFactory ifac;

  private SystemFix() {
    // Nothing to construct.
  }

  /**
   * Run the utility.
   *
   * @param  argv  The classes to patch, specified as file names.
   */
  public static void main(String[] argv) { 
    if ((null == argv) ||
        (0 == argv.length)) {
      System.out.println("Rewrites class files to use one.world.Shell and " +
                         "one.world.util.SystemUtilities");
      System.out.println("instead of java.lang.System and java.lang.Thread.");
      System.out.println();
      System.out.println("Usage: java one.tools.SystemFix <file>+");
      return;
    }

    try {
      for(int i=0; i < argv.length; i++) {
	if(argv[i].endsWith(".class")) {
          JavaClass       java_class = new ClassParser(argv[i]).parse();
	  ConstantPool    constants  = java_class.getConstantPool();
          String          file_name  = argv[i];
          boolean         changed    = false;

	  cp   = new ConstantPoolGen(constants);
          ifac = new InstructionFactory(cp);

	  /* Patch all methods.
	   */
          Method[] methods = java_class.getMethods();

	  for(int j=0; j < methods.length; j++) {
            Method m = fixMethod(methods[j]);
            if (null != m) {
              methods[j] = m;
              changed    = true;
            }
          }

	  /* Finally dump it back to a file.
	   */
          if (changed) {
            java_class.setConstantPool(cp.getFinalConstantPool());
            java_class.dump(file_name);
            System.out.println(";;;    patched " + argv[i]);
          } else {
            System.out.println(";;;    scanned " + argv[i]);
          }
	}
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Patch the specified method.
   *
   * @param   m  The method to patch.
   * @return     The patched method or <code>null</code> if nothing
   *             changed.
   */
  private static Method fixMethod(Method m) {
    Code    code    = m.getCode();
    int     flags   = m.getAccessFlags();
    String  name    = m.getName();
    boolean changed = false;
    
    // Sanity check
    if(   ((flags & Constants.ACC_NATIVE) != 0)
       || ((flags & Constants.ACC_ABSTRACT) != 0)
       || (code == null))
      return null;
    
    MethodGen           mg  = new MethodGen(m, class_name, cp);
    InstructionList     il  = mg.getInstructionList();

    for(InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
      Instruction i = ih.getInstruction();

      if(i instanceof INVOKESTATIC) {
	INVOKESTATIC s = (INVOKESTATIC)i;

        if (s.getClassName(cp).equals("java.lang.System")) {
          String methodName = s.getMethodName(cp);

          if ("arraycopy".equals(methodName) ||
              "currentTimeMillis".equals(methodName) ||
              "getProperties".equals(methodName) ||
              "getProperty".equals(methodName)) {
            Instruction iii =
              ifac.createInvoke("one.world.util.SystemUtilities",
                                methodName,
                                s.getReturnType(cp),
                                s.getArgumentTypes(cp),
                                Constants.INVOKESTATIC);
            ih.setInstruction(iii);
            changed = true;
          }

        } else if (s.getClassName(cp).equals("java.lang.Thread")) {
          String methodName = s.getMethodName(cp);

          if ("sleep".equals(methodName)) {

            Instruction iii =
              ifac.createInvoke("one.world.util.SystemUtilities",
                                methodName,
                                s.getReturnType(cp),
                                s.getArgumentTypes(cp),
                                Constants.INVOKESTATIC);
            ih.setInstruction(iii);
            changed = true;
          }
        }

      } else if (i instanceof GETSTATIC) {
        GETSTATIC g = (GETSTATIC)i;
        
        if (g.getClassName(cp).equals("java.lang.System")) {
          String fieldName = g.getFieldName(cp);

          if ("out".equals(fieldName) || "err".equals(fieldName)) {
            Instruction iii =
              ifac.createGetStatic("one.world.Shell",
                                   "console",
                                   g.getFieldType(cp));
            ih.setInstruction(iii);
            changed = true;
          }
        }
      }
    }
    
    if (changed) {
      return mg.getMethod();
    } else {
      return null;
    }
  }
}
