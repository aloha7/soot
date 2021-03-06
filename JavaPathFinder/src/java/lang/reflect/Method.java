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
package java.lang.reflect;

import java.lang.annotation.Annotation;

/**
 * minimal Method reflection support.
 * Note that we share peer code between Method and Constructor (which aren't
 * really different on the JPF side), so don't change field names!
 */
public final class Method extends AccessibleObject implements Member {
  int regIdx; // the link to the corresponding MethodInfo
  String name; // deferred set by the NativePeer getName()

  public native String getName();
  public String toGenericString() {
	  // TODO: return real generic string
	  return toString();
  }
  public native Object invoke (Object object, Object... args)
        throws IllegalAccessException, InvocationTargetException;

  public native int getModifiers();
  public native Class<?> getReturnType();
  public native Class<?>[] getParameterTypes();
  public native Type[] getGenericParameterTypes();

  public native Class<?> getDeclaringClass();

  public native Annotation[] getAnnotations();

  public native <T extends Annotation> T getAnnotation( Class<T> annotationCls);

  public boolean isSynthetic () {
    // ?? don't know of others
    return (name.startsWith("access$"));
  }

  public native String toString();

  // for Annotations - return the default value of the annotation member
  // represented by this method
  public Object getDefaultValue() {
    throw new UnsupportedOperationException("Method.getDefaultValue() not supported yet");
  }
}
