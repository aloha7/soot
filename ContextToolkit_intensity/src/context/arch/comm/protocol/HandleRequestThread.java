package context.arch.comm.protocol;

import java.net.Socket;

public class HandleRequestThread extends Thread{
	private Socket sock;
	private TCPServerSocket server;
	
	public HandleRequestThread(TCPServerSocket i_server, Socket data){
		server = i_server;
		sock = data;	
	}
	
	public void run(){
		server.handleIncomingRequest(sock);
//		System.out.println("handling one data");
	}
}
