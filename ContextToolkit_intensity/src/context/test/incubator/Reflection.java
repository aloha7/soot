package context.test.incubator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Reflection {

	double sum = -1;

	public Reflection(int v1, double v2) {
		sum = v1 + v2;
		System.out.println("Succeed to construct an object");
	}

	public void print(int i) {
		System.out.println(i);
	}

	public void print(int v1, double v2) {
		System.out.println(v1 + "" + v2);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		try {
			Class ownClass = Class
					.forName("context.test.contextIntensity.Reflection");
			int a = 1;
			double b = 2.0;

			Object[] values = new Object[2];
			values[0] = a;
			values[1] = b;

			Class[] types = new Class[values.length];
			types[0] = new Integer(a).TYPE;
			types[1] = new Double(b).TYPE;

			
			Constructor[] cs = ownClass.getConstructors();
			for (Constructor c : cs) {
				Class[] arguments = c.getParameterTypes();
				for (Class arg : arguments) {
					System.out.println(arg.toString());
				}
			}

			try {
				Object instance = ownClass.getConstructor(types).newInstance(values);
				Method method = ownClass.getMethod("print", types);
				method.invoke(instance, values);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/*	Method method =  ownClass.getMethod("print", types);
				method.invoke(instance, values);*/
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
