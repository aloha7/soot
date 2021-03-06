//
// Copyright (C) 2007 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

package gov.nasa.jpf.jvm.bytecode;

import java.util.List;

import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.Constants;

import gov.nasa.jpf.jvm.ChoiceGenerator;
import gov.nasa.jpf.jvm.InstructionFactory;
import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.Ref;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Types;
import gov.nasa.jpf.jvm.choice.InvocationCG;
import gov.nasa.jpf.util.Invocation;

/**
 * a sytnthetic INVOKE instruction that gets it's parameters from an
 * InvocationCG. Whoever uses this better makes sure the frame this
 * executes in has enough operand space (e.g. a DirectCallStackFrame).
 * 
 */
public class INVOKECG extends Instruction {

  List<Invocation>  invokes;
  InvokeInstruction realInvoke;
  
  public INVOKECG() {}

  public void setPeer (org.apache.bcel.generic.Instruction i, ConstantPool cp) {
    // nothing - this is an artificial instruction
  }
  
  public void setInvokes(List<Invocation> invokes) {
    this.invokes = invokes;
  }
  
  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo ti) {
    
    if (!ti.isFirstStepInsn()) {
      ChoiceGenerator cg = new InvocationCG(invokes);
      ss.setNextChoiceGenerator(cg);
      return this;
      
    } else {
      ChoiceGenerator cg = ss.getChoiceGenerator();
      assert (cg != null) && (cg instanceof InvocationCG) : "expected InvocationCG, got: " + cg;

      Invocation call = ((InvocationCG)cg).getNextChoice();
      MethodInfo callee = call.getMethodInfo();
      InstructionFactory insnFactory = MethodInfo.getInstructionFactory();

      if (callee.isStatic()){
        realInvoke = (InvokeInstruction) insnFactory.create(null, Constants.INVOKESTATIC);
      } else {
        realInvoke = (InvokeInstruction) insnFactory.create(null, Constants.INVOKENONVIRTUAL);
      }
      realInvoke.init(mi, offset, position);
      realInvoke.setInvokedMethod( callee.getClassInfo().getName(),
                                   callee.getName(), callee.getSignature());
      
      pushArguments(ti, call.getArguments(), call.getAttrs());
      
      return realInvoke;
    }
  }

  void pushArguments (ThreadInfo ti, Object[] args, Object[] attrs){
    if (args != null){
      for (int i=0; i<args.length; i++){
        Object a = args[i];
        boolean isLong = false;
        
        if (a != null){
          if (a instanceof Ref){
            ti.push(((Ref)a).getReference(), true);
          } else if (a instanceof Boolean){
            ti.push((Boolean)a ? 1 : 0, false);
          } else if (a instanceof Integer){
            ti.push((Integer)a, false);
          } else if (a instanceof Long){
            ti.longPush((Long)a);
            isLong = true;
          } else if (a instanceof Double){
            ti.longPush(Types.doubleToLong((Double)a));
            isLong = true;
          } else if (a instanceof Byte){
            ti.push((Byte)a, false);
          } else if (a instanceof Short){
            ti.push((Short)a, false);
          } else if (a instanceof Float){
            ti.push(Types.floatToInt((Float)a), false);
          }
        }

        if (attrs != null && attrs[i] != null){
          if (isLong){
            ti.setLongOperandAttr(attrs[i]);
          } else {
            ti.setOperandAttr(attrs[i]);
          }
        }
      }
    }
  }
  
  public boolean isExtendedInstruction() {
    return true;
  }
  
  public int getByteCode () {
    return 258;
  }
  
}
