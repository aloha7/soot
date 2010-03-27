/*
 * Copyright (c) 2001, University of Washington, Department of
 * Computer Science and Engineering.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither name of the University of Washington, Department of
 * Computer Science and Engineering nor the names of its contributors
 * may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package one.fonda.java;

import java.net.*;
import java.io.*;
import java.util.Date;

/** 
 * <p>
 * A TCP echo server.  This class is provided as a utility for use in
 * testing and simply echoes raw bytes.  Some benchmarks, such as
 * {@link one.world.io.BenchmarkNetworkIO} and {@link
 * one.fonda.java.BenchmarkTcp}, require the use of a TCP echo server.  
 * </p>
 *
 * <p>To run the server, type: <code>java one.fonda.java.TcpEchoServer
 * [server_port]</code>.  <p>The default server port is 5007.
 *
 * @author  Adam MacBeth
 * @version $Revision: 1.1 $ */
public class TcpEchoServer {

  /** The server port. */
  private static int port = 5007;
  
  public static void main(String args[]) throws Exception {
    if (1 == args.length) {
      port = Integer.parseInt(args[0]); 
    }
    else if (args.length > 1) {
      System.out.println("Usage: TcpEchoServer [server_port]");
      System.exit(-1);
    }
    ServerSocket s = new ServerSocket(port);
    while(true) {
      try {
	Socket client_socket = s.accept();
	System.out.println("Got connection from " 
			   + client_socket.getInetAddress()
			   + " at " 
			   + new Date(System.currentTimeMillis()));
	InputStream in = client_socket.getInputStream();
	OutputStream out = client_socket.getOutputStream();
	
	boolean done = false;
	byte[] b = new byte[1000];
	int num;
	while(!done) {
	  num = in.read(b);

	  //System.out.println();
	  //System.out.println("Read " + num + " bytes");
	  //for(int i = 0; i<num; i++) {
	  //System.out.print(Integer.toHexString(b[i]) + " ");
	  //}

	  out.write(b,0,num);
	}
	client_socket.close();
      } catch (Exception e) {
	e.printStackTrace();
      }
    }
  }
}





