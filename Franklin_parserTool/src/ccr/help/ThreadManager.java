package ccr.help;



public class ThreadManager {
	public long limit_time =  60 * 1000; // 1 min
	public long sleep_time = 30 * 1000; // 30 secs;
	public boolean finished = false; 
	
	
	public ThreadManager(){
		
	}
	
	public ThreadManager(long limit_time, long sleep_time){
		this.limit_time = limit_time;
		this.sleep_time = sleep_time;
	}
	
	public void startThread(ProgramRunnable programRunnable){
		try {
			Thread t = new Thread(programRunnable);
			t.start();
			long start = System.currentTimeMillis();
			while(t.isAlive() && System.currentTimeMillis() - start < limit_time){
				Thread.sleep(sleep_time);
			}
			
			if(t.isAlive()){	// time over		
				t.stop();
				finished = false;
			}else{
				finished = true;
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Time Limit:" + limit_time + " is over!");
	}
	
	public static void main(String[] args){
		ThreadManager manager = new ThreadManager();
		manager.startThread(new ProgramRunnable());		
	}
	
}