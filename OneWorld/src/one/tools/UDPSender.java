package one.tools;

import java.io.*;
import java.net.*;

class UDPSender
{
 
  public static void doSend(InetAddress address, int port,int packetSize,
                       int rate) throws Exception{
    long period;
    byte outBuffer[] = new byte[packetSize];
    DatagramSocket s = new DatagramSocket();
    long startTime,prevTime,curTime;
    DatagramPacket packet;
    int cnt;
    int prevCnt;

    s.connect(address,port);
    period = (long)(0.5+(1000./(double)(rate)));
    System.out.println("Period="+period);
    packet = new DatagramPacket(outBuffer, outBuffer.length,
                                address, port);

    startTime=System.currentTimeMillis(); 
    prevTime = startTime;
    cnt=0;
    prevCnt=0;
    long curPeriod;
    while(true) {
      s.send(packet);
      cnt++;
      curTime=System.currentTimeMillis(); 
      curPeriod=(cnt-prevCnt)*period-(curTime-prevTime);
      if (curPeriod>0) {
        Thread.sleep(curPeriod);
      }
      if ((curTime-prevTime)>5000) {
        System.out.println("Current rate: "+(1000.*(cnt-prevCnt)/(double)(curTime-prevTime))+"  overall: "+(1000.*cnt/(double)(curTime-startTime)));
        prevCnt = cnt;
        prevTime = curTime;
      }
    }
  }

  public static void main(String args[]) throws Exception {
    InetAddress dest=InetAddress.getByName(args[0]);
    int port        =Integer.parseInt(args[1]);
    int bufferSize  =Integer.parseInt(args[2]);
    int rate        =Integer.parseInt(args[3]);
    doSend(dest,port,bufferSize,rate);
  }

}
