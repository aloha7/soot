//
// Copyright (C) 2006 United States Government as represented by the
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

import gov.nasa.jpf.jvm.KernelState;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.jvm.ThreadInfo;


/**
 * Load double from local variable
 * ... => ..., value
 */
public class DLOAD extends LocalVariableInstruction {

  public Instruction execute (SystemState ss, KernelState ks, ThreadInfo th) {
    //th.longPush(th.getLongLocalVariable(index));
    th.pushLongLocal(index);

    return getNext(th);
  }

  public int getLength() {
    if (index > 3){
      return 2;  // opcode, index
    } else {
      return 1;
    }
  }
  
  public int getByteCode () {
    switch (index) {
      case 0: return 0x26;
      case 1: return 0x27;
      case 2: return 0x28;
      case 3: return 0x29;
    }
    
    return 0x18; // wide version?
  }
  
  public String getMnemonic() {
    String mnemonic = "dload";
    
    if (index >=0) {
      return mnemonic + '_' + index;
    } else {
      return mnemonic;
    }
  }
}
