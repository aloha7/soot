package context.test.incubator;

import context.test.util.Logger;

public class TestFile {
	
	public static void main(String[] args){
		String file = "C:\\Consistency";
		Logger.getInstance().delete(file);
	}
	
}
