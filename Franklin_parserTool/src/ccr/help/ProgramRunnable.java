package ccr.help;

public class ProgramRunnable implements Runnable {
	
	public Object output = null;
	
	public void run() {
		// TODO Auto-generated method stub
		try {
			int i = 0;
			while(true){
				i ++;
				Thread.sleep(1000);
				System.out.println(i);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
