
public class CoverageTest {
	
	static int a = 1;
	
//	public CoverageTest(){
//		int b = 0;
//	}
	
	
	
	public static void run(){
		int b = 0;
		if(a == 0){
			b = 10;
		}else if (a > 0){
			b = -10;
		}else {
			b = 0;
		}
		System.out.println("" + (a + b));
	}

	public static void main(String[] args) {
		new CoverageTest();
		run();

	}

}
