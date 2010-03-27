package one.tools;

import java.net.*;


/**
 * Utilitiy to calculate the clock offset between two machines.
 * Simply records the delivery times of both sides of a single
 * ping-pong delivery to estimate the clock offsets.
 *
 * <p>This utility is not overly optimized since it it uses the
 * standard <code>System.currentTimeMillis()</code>.  So, at best, it
 * has no better precision than the precision of this clock.</p>
 *
 * <p>Usage:
 * <pre>
 *   java one.tools.OffsetTimer listen <remotehost>
 *   java one.tools.OffsetTimer send <remotehost>
 * </pre></p>
 *
 * @version $Revision: 1.2 $
 * @author  Eric Lemar
 */
public class OffsetTimer {
  /** The UDP socket */
  static DatagramSocket socket;

  /** 
   * Bind the UDP socket to the given host.
   */
  static void bindSocket(String remoteHost) throws Exception{
   
    InetAddress remoteAddress = InetAddress.getByName(remoteHost);
    socket = new DatagramSocket(9832);
    socket.connect(remoteAddress, 9832);
  }

  /**
   * Convert a byte array to a Long(avoid serialization)
   * NOTE: doesn't work for negative numbers
   */
  static long byteToLong(byte[] b) {
    int i;
    long l=0;
    for (i=15;i>=0;i--) {
      l *= 128;
      l |= b[i];
    }
    return l;
  }

  /**
   * Convert a long to a byte array(avoid serialization)
   * NOTE: doesn't work for negative numbers
   */
  static void longToByte(long l,byte[] b) {
    int i;
    for (i=0;i<16;i++) {
      b[i] = (byte)(l % 128);
      l    /=128; 
    }
  }

  /**
   * Main routine for the listener.
   */ 
  static void listenMain() throws Exception{
    long curTime;
    byte[] buff; 
    DatagramPacket sendp;
    DatagramPacket recvp;
    buff = new byte[16];
    recvp = new DatagramPacket(new byte[16],16);

     
    while(true) {
      socket.receive(recvp);
      curTime = System.currentTimeMillis();
      longToByte(curTime,buff);
      sendp = new DatagramPacket(buff,16);
      socket.send(sendp);
    } 
  }

  /**
   * Main routine for the sender.
   */ 
  static void sendMain() throws Exception {
    long sendTime;
    long receiveTime;
    long listenerTime;
    DatagramPacket sendp;
    DatagramPacket recvp;
    byte buff[];
    buff = new byte[16];
    recvp = new DatagramPacket(buff,16);
    sendp = new DatagramPacket(new byte[16],16);
    int i;
    long minDiff=10000000;
    long off=10000000;
  
    for (i=0;i<20;i++) { 
      sendTime = System.currentTimeMillis();
      socket.send(sendp);
      socket.receive(recvp);
      receiveTime = System.currentTimeMillis();
      listenerTime = byteToLong(buff);
      System.out.println("-------------");
      System.out.println("Send: "+sendTime);
      System.out.println("listenerTime: "+listenerTime);
      System.out.println("receiveTime: "+receiveTime);
      if (minDiff>(receiveTime-sendTime)) {
        minDiff = receiveTime - sendTime;
        long middle = ((receiveTime+sendTime)/2L);
        off = middle - listenerTime;
      }
    }
    System.out.println("Minimum send/receive offset: "+minDiff);
    System.out.println("Offset (us - them): "+off);
  }


  public static void main(String str[]) throws Exception{ 
    bindSocket(str[1]);
    if ("listen".equals(str[0])) {
      listenMain();  
    } else if ("send".equals(str[0])) {
      sendMain();
    }
  }
}

