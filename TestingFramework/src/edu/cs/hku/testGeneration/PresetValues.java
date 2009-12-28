package edu.cs.hku.testGeneration;

public class PresetValues{
	
	  /* primitive types */
	  protected static final Expression[] booleanPlans = new Expression[]{
	    "false","true"};
	  protected static final Expression[] bytePlans = new Expression[]{
	    "" + ((byte)0), 
	    "" + ((byte)255)};
	  protected static final Expression[] charPlans = new Expression[]{
	    ""+(char)' ',
	    ""+(char)('\n')};
	  protected static final Expression[] shortPlans = new Expression[]{
	    "" + ((short) -1),
	    "" + ((short) 0)};
	  protected static final Expression[] intPlans = new Expression[]{
	    "" + (-1),
	    "" + (0),
	    "" + (1)};
	  protected static final Expression[] longPlans = new Expression[]{
	    "" + (-1),
	    "" + (0)};
	  protected static final Expression[] floatPlans = new Expression[]{
	    "" + (-100.123456789f),
	    "" + (0.0f)};
	  protected static final Expression[] doublePlans = new Expression[]{
	    "" + (-1.123456789d),
	    "" + (0.0d)};


	  /* Complex types */
	  protected static final Expression[] classPlans = new Expression[]{"Null"};
	  protected static final Expression[] stringPlans = new Expression[]{
	    "", "\"\n\\.`\'@#$%^&/({<[|\\n:.,;"};	
	  
	  public static Expression[] getPreset(final Class<?> pClass) {
		    /* Primitive */
		    if (pClass.equals(boolean.class)) {
		      return booleanPlans;
		    }
		    if (pClass.equals(byte.class)) {
		      return bytePlans;
		    }
		    if (pClass.equals(char.class)) {
		      return charPlans;
		    }
		    if (pClass.equals(short.class)) {
		      return shortPlans;
		    }
		    if (pClass.equals(int.class)) {
		      return intPlans;
		    }
		    if (pClass.equals(long.class)) {
		      return longPlans;
		    }
		    if (pClass.equals(float.class)) {
		      return floatPlans;
		    }
		    if (pClass.equals(double.class)) {
		      return doublePlans;
		    }

		    /* Complex */
//		    if (pClass.equals(Class.class)) {
//		      return classPlans;
//		    }
//		    if (pClass.equals(Comparable.class)) {
//		      return stringPlans;
//		    }
//		    if (pClass.equals(String.class)) {
//		      return stringPlans;
//		    }
//		    if (pClass.equals(Object.class)) {
//		      return getObject();
//		    }
//
//		    if (pClass.equals(java.util.List.class)) {
//		      return getVector();
//		    }
//		    if (pClass.equals(java.util.Map.class)) {
//		      return getHashtable();
//		    }
//		    if (pClass.equals(java.util.Vector.class)) {
//		      return getVector();
//		    }
//
//		    /* Array */
//		     
//		    if (pClass.equals(int[].class)) {
//		      return getIntArray1();
//		    }
//		    
//		    if (pClass.equals(String[].class)) {
//		      return getStringArray1();
//		    }
//		    
//		    //FIXME: Add following back: 
//		    if (pClass.isArray()) {
//		      return getEmptyArray(pClass);
//		    }

		    /* No preset plans for other complex types */
		    return new Expression[0];
		  }
	  
}
