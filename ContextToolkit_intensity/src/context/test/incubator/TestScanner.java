package context.test.incubator;

import java.util.Scanner;

public class TestScanner {

	public static void main(String[] args){
		Scanner scanner = new Scanner(System.in);
		int testSetNum = scanner.nextInt();
		String instruction = scanner.next();
		double min_CI = scanner.nextDouble();
		double max_CI = scanner.nextDouble();

		String date = scanner.next();
		String criterion = scanner.next();
		int testSuiteSize = scanner.nextInt();
		String randomOrCriterion = scanner.next();
		int start = 1;
		int end = 141;
		if(scanner.hasNextInt()){
			start = scanner.nextInt();
			end = scanner.nextInt();
		}
	}
	
}
