package one.tools;


import java.io.*;
import java.net.*;

import one.util.Stats;

class UDPForwarder {
  public static void doReceive(int port,InetAddress forwardAddr,int forwardPort,int forwardSize) throws Exception {
    DatagramSocket s = new DatagramSocket(port);
    DatagramSocket outSocket = new DatagramSocket();
    outSocket.connect(forwardAddr,forwardPort);
    int totPacket;
    int prevPacket;
    long startTime;
    long curTime;
    long prevTime;
    boolean alive;
    Stats stat = new Stats();


    DatagramPacket packet = new DatagramPacket(new byte[100000], 100000);
    DatagramPacket outpacket = new DatagramPacket(new byte[forwardSize], forwardSize);
    startTime=System.currentTimeMillis(); 
    prevTime=startTime;
    totPacket=0;
    prevPacket=0;
    alive=true;
    while(true) {
      s.receive(packet);
      outSocket.send(outpacket);
      totPacket++;
      curTime=System.currentTimeMillis(); 
      if (alive && ((curTime-prevTime)>=1000)) {
        double lastRate=1000.*(totPacket-prevPacket)/(double)(curTime-prevTime);
        prevTime=curTime;
        prevPacket=totPacket;
        stat.add(lastRate);
        System.out.println("lastrate: "+lastRate);
        if (curTime-startTime>=30000) {
          System.out.println("Rate: "+stat.average()+" +- "+stat.stdev());
          alive=false;
        }
      }
    }
  }
  public static void main(String arg[]) throws Exception {
    int localPort = Integer.parseInt(arg[0]);
    InetAddress remoteAddr = InetAddress.getByName(arg[1]);
    int remotePort = Integer.parseInt(arg[2]);
    int bufferSize = Integer.parseInt(arg[3]);

    doReceive(localPort,remoteAddr,remotePort,bufferSize);
  }
}

