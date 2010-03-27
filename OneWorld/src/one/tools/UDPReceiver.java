package one.tools;


import java.io.*;
import java.net.*;

import one.util.Stats;

class UDPReceiver {
  public static void doReceive(int port) throws Exception {
    DatagramSocket s = new DatagramSocket(port);
    int totPacket;
    int prevPacket;
    long startTime;
    long curTime;
    long prevTime;
    Stats stat = new Stats();


    DatagramPacket packet = new DatagramPacket(new byte[100000], 100000);
    startTime=System.currentTimeMillis(); 
    prevTime=startTime;
    totPacket=0;
    prevPacket=0;
    while(true) {
      s.receive(packet);
      totPacket++;
      curTime=System.currentTimeMillis(); 
      if ((curTime-prevTime)>=1000) {
        double lastRate=1000.*(totPacket-prevPacket)/(double)(curTime-prevTime);
        prevTime=curTime;
        prevPacket=totPacket;
        stat.add(lastRate);
        System.out.println("lastrate: "+lastRate);
        if (curTime-startTime>=30000) {
          System.out.println("Rate: "+stat.average()+" +- "+stat.stdev());
          System.exit(0);
        }
      }
    }
  }
  public static void main(String arg[]) throws Exception {
    doReceive(Integer.parseInt(arg[0]));
  }
}

