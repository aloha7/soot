package trivia;

import static edu.cs.hku.testGeneration.Constants.LS;

public class Human extends Animal {
	private static int legs = 2;
	private static double ages = 88.0d;
	private static String name = "HW";
	private static char firstName = 'h';
	private static long hairs = 10000;
	private static short fingers = 10;
	private static float teeth = 22.0f;
	private static boolean isMale = true;
	private static Human son = new Human();
	
	public static void print(){
		 System.out.println("Legs(int):" + legs + 	LS 
						+	"Ages(double):" + ages + 	LS
						+	"Name(String):" +  name + 	LS
						+	"First Name(char):" + firstName + LS
						+ 	"Hairs(long):" + hairs +	LS
						+	"Fingers(short):" + fingers + LS
						+	"Teeth(float):" +	teeth	+ LS
						+	"IsMale(boolean):" + isMale + LS
						+	"Son(Human):" + son + LS);
	}
	
	
	
}
