import java.io.IOException;

public class batch {
	public static void main(String[] args)
        {
        	int node_ID = 181;
        	String command = "rsh node \"home/sdi/hwang/ICSE09_renew/ICSE09_renew/run2.sh\"";
            for(int i = 0;i<29;i++)
            {
                    BatchThread thread = new BatchThread("gd"+String.valueOf(node_ID),command);
                    thread.start();
                    node_ID++;
            }
            try {
				Thread.sleep(1000000000L);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
}

class BatchThread extends Thread {
	private String node = "";
	private String command = "";

	public BatchThread(String node, String command) {
		this.node = node;
		this.command = command;
	}

	public void run() {
		try {
			Runtime.getRuntime().exec("echo OK."+node);
			Runtime.getRuntime().exec(convert(node, command));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	String convert(String node, String command) {
		return command.replaceAll("node", node);
	}
}