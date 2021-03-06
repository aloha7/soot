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
package gov.nasa.jpf.jvm;

import java.util.Locale;

public class JPF_java_util_Locale {

  static Locale getLocale (MJIEnv env, int locref) {
    String country = env.getStringObject(env.getReferenceField(locref, "country"));
    String language = env.getStringObject(env.getReferenceField(locref, "language"));
    String variant = env.getStringObject(env.getReferenceField(locref, "variant"));
    
    Locale locale = new Locale(language,country,variant); 
    return locale;
  }
  
  public static int getDisplayName__Ljava_util_Locale_2__Ljava_lang_String_2 (MJIEnv env, int objref, int locref) {
    Locale locale = getLocale(env, locref);
    String name = locale.getDisplayName();
    return env.newString(name);
  }
  
  public static int getDisplayVariant__Ljava_util_Locale_2__Ljava_lang_String_2 (MJIEnv env, int objref, int locref) {
    Locale locale = getLocale(env, locref);
    String variant = locale.getDisplayVariant();
    return env.newString(variant);    
  }
  
  public static int getDisplayCountry__Ljava_util_Locale_2__Ljava_lang_String_2 (MJIEnv env, int objref, int locref) {
    Locale locale = getLocale(env, locref);
    String country = locale.getDisplayCountry();
    return env.newString(country);

  }

  public static int getDisplayLanguage__Ljava_util_Locale_2__Ljava_lang_String_2 (MJIEnv env, int objref, int locref) {
    Locale locale = getLocale(env, locref);
    String language = locale.getDisplayLanguage();
    return env.newString(language);
  }

  public static int getISO3Country____Ljava_lang_String_2 (MJIEnv env, int objref) {
    Locale locale = getLocale(env, objref);
    String s = locale.getISO3Country();
    return env.newString(s);    
  }

  public static int getISO3Language____Ljava_lang_String_2 (MJIEnv env, int objref) {
    Locale locale = getLocale(env, objref);
    String s = locale.getISO3Language();
    return env.newString(s);
  }

  //--- the static ones
  public static int getISOCountries_____3Ljava_lang_String_2 (MJIEnv env, int clsref) {
    String[] s = Locale.getISOCountries();

    int aref = env.newObjectArray("java.lang.String", s.length);
    for (int i=0; i<s.length; i++) {
      env.setReferenceArrayElement(aref, i, env.newString(s[i]));
    }
    
    return aref;
  }
  
  public static int getISOLanguages_____3Ljava_lang_String_2 (MJIEnv env, int clsref) {
    String[] s = Locale.getISOLanguages();

    int aref = env.newObjectArray("java.lang.String", s.length);
    for (int i=0; i<s.length; i++) {
      env.setReferenceArrayElement(aref, i, env.newString(s[i]));
    }
    
    return aref;    
  }

}
