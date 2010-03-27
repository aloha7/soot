/*
 * Copyright (c) 1999, 2000, Markus Dahm, Robert Grimm.
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

package one.util;

import de.fub.bytecode.Constants;
import de.fub.bytecode.classfile.ClassParser;
import de.fub.bytecode.classfile.Code;
import de.fub.bytecode.classfile.ConstantPool;
import de.fub.bytecode.classfile.JavaClass;
import de.fub.bytecode.classfile.Method;
import de.fub.bytecode.generic.ConstantPoolGen;
import de.fub.bytecode.generic.Instruction;
import de.fub.bytecode.generic.InstructionConstants;
import de.fub.bytecode.generic.InstructionHandle;
import de.fub.bytecode.generic.InstructionList;
import de.fub.bytecode.generic.INVOKESTATIC;
import de.fub.bytecode.generic.MethodGen;

/**
 * Utility to patch explicit monitor operations in class files. This
 * utility reads the specified class files, replaces all invocations
 * to {@link one.util.Monitor#acquire(Object)} and {@link
 * one.util.Monitor#release(Object)} with the corresponding
 * <code>monitorenter</code> and <code>monitorexit</code> JVM
 * instructions, and replaces the original class files with the
 * patched class files. Class files must be specified by their file
 * names, including the "<code>class</code>" file name extension.
 *
 * <p>This class depends on the <a
 * href="http://bcel.sourceforge.net/">Byte Code Engineering
 * Library</a> to perform the actual work.</p>
 *
 * @see      Monitor
 * 
 * @author   Original code provided by
 *           <a href="http://www.inf.fu-berlin.de/~dahm">Markus Dahm</a>,
 *           with modifications by <a
 *           href="mailto:rgrimm@alum.mit.edu">Robert Grimm</a>.
 * @version  $Revision: 1.6 $
 */
public final class MonitorFix {
  private static String          class_name;
  private static ConstantPoolGen cp;

  private MonitorFix() {
    // Nothing to construct.
  }

  /**
   * Run the utility.
   *
   * @param  argv  The classes to patch, specified as file names.
   */
  public static void main(String[] argv) { 
    try {
      for(int i=0; i < argv.length; i++) {
	if(argv[i].endsWith(".class")) {
          JavaClass       java_class = new ClassParser(argv[i]).parse();
	  ConstantPool    constants  = java_class.getConstantPool();
          String          file_name  = argv[i];
	  cp = new ConstantPoolGen(constants);

	  /* Patch all methods.
	   */
          Method[] methods = java_class.getMethods();

	  for(int j=0; j < methods.length; j++)
	    methods[j] = fixMethod(methods[j]);

	  /* Finally dump it back to a file.
	   */
	  java_class.setConstantPool(cp.getFinalConstantPool());
	  java_class.dump(file_name);
          System.out.println(";;;    patched " + argv[i]);
	}
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Patch the specified method.
   *
   * @param  m  The method to patch.
   */
  private static Method fixMethod(Method m) {
    Code   code  = m.getCode();
    int    flags = m.getAccessFlags();
    String name  = m.getName();
    
    // Sanity check
    if(   ((flags & Constants.ACC_NATIVE) != 0)
       || ((flags & Constants.ACC_ABSTRACT) != 0)
       || (code == null))
      return m;
    
    MethodGen           mg  = new MethodGen(m, class_name, cp);
    InstructionList     il  = mg.getInstructionList();

    for(InstructionHandle ih = il.getStart(); ih != null; ih = ih.getNext()) {
      Instruction i = ih.getInstruction();

      if(i instanceof INVOKESTATIC) {
	INVOKESTATIC s = (INVOKESTATIC)i;

        if (s.getClassName(cp).equals("one.util.Monitor")) {

          if (s.getMethodName(cp).equals("acquire")) {
            ih.setInstruction(InstructionConstants.MONITORENTER);
          } else if (s.getMethodName(cp).equals("release")) {
            ih.setInstruction(InstructionConstants.MONITOREXIT);
          }
        }
      }
    }
    
    return mg.getMethod();
  }
}
