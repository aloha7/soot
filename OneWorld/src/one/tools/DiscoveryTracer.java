package one.tools;

import one.world.Constants;

import one.util.BufferOutputStream;
import one.util.Guid;

import java.lang.Runnable;
import java.lang.Thread;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OptionalDataException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.security.AccessController;
import java.security.PrivilegedAction;


class ListenThread implements Runnable 
{
  MulticastSocket socket;
  int port;
  InetAddress addr;
  String name;
  public static Object lock = new Object();

  public ListenThread(InetAddress addr,int port,String name) {
    this.port = port;
    this.addr = addr;
    this.name = name;
  };

  public void run() {
    DatagramPacket packet;
    byte[] buffer = new byte[650000]; //Bigger than we need
    ObjectInputStream in;

    try {
      socket = new MulticastSocket(port);
      socket.joinGroup(addr);
    } catch (Exception x) {
      System.out.println("Error starting: "+x);
      System.exit(0);
    }
    synchronized(lock) {  
      System.out.println("Listening for "+name+" on " +addr+" port "+port);
    }
    while(true) { 
      try {
        packet = new DatagramPacket(buffer, buffer.length) ;
        socket.receive(packet);
        synchronized(lock) {  
          System.out.println("Got a packet on "+name+" address="+
                 packet.getAddress()+" port="+packet.getPort());

          try {
            // Read tuple from packet
            in = new ObjectInputStream(
                    new ByteArrayInputStream(packet.getData()));
            Object obj= in.readObject();
            System.out.println("Contents: "+obj+"\n");

  
          } catch (Exception x) {
            System.out.println("Got an exception deserializing");
            System.out.println(""+x+"\n");
          }
        }

      } catch (Exception x) {
        synchronized(lock) {  
          System.out.println("Got an exception on receive for "+name);
        }
        System.out.println(""+x+"\n");
      }

    }
  }
}


/**
 * A raw java program which listens on the Discovery Multicast ports 
 * and prints out any traffic that it sees.
 *
 * <p>When a packet is received, the source and port of the packet is printed.  An
 * attempt is made to deserialize the packet.  If successful, the .toString()
 * method is printed.</p>  
 *
 * <p>By default this program binds to the election and announcent address/port
 * pairs listed in Constants.java</p>
 *
 * <p>The classpath must be the normal one.world classpath.  To run with the 
 * default ports, simply run:</p>
 *
 * <p><code>java one.tools.DiscoveryTracer</code></p>
 *
 * <p>To get usage infromation, run:</p>
 *
 * <p><code>java one.tools.DiscoveryTracer -help</code></p>
 */
public class DiscoveryTracer
{

  private DiscoveryTracer() {

  }

  static void printUsage() {
    System.out.println(
      "\n"+
      "Reports traffic on the Discovery announce and election multicast ports.\n"+
      "USAGE:\n"+
      "java one.tools.DiscoveryTracer [OPTION]...\n\n"+
      "  Options:\n" +
      "    -announceport PORT\n" +
      "    -announceaddress ADDRESS\n" +
      "    -electport PORT\n" +
      "    -electaddress ADDRESS\n\n" +
      "If no options are specified, the defaults in Constants.java are used\n"+
      "These defaults currently are:\n"+
      "  Announce address = "+Constants.DISCOVERY_ANNOUNCE_ADDR+"\n"+
      "  Announce port    = "+Constants.DISCOVERY_ANNOUNCE_PORT+"\n"+
      "  Elect address    = "+Constants.DISCOVERY_ELECTION_ADDR+"\n"+
      "  Elect port       = "+Constants.DISCOVERY_ELECTION_PORT+"\n");
  }

  /**
   * The main method.
   *
   * Run with -help for usage information.
   *
   * @param str The command line arguments
   */
  public static void main(String[] str) {
    String announceAddressStr = Constants.DISCOVERY_ANNOUNCE_ADDR;
    String electAddressStr    = Constants.DISCOVERY_ELECTION_ADDR;
    int announcePort          = Constants.DISCOVERY_ANNOUNCE_PORT;
    int electPort             = Constants.DISCOVERY_ELECTION_PORT;
    InetAddress announceAddress;
    InetAddress electAddress;
    int i;

    for (i=0;i<str.length;i++) {
      if (str[i].equals("-announceport")) {
        announcePort =  Integer.parseInt(str[i+1]);
        i++;
      } else if (str[i].equals("-electport")) {
        electPort =  Integer.parseInt(str[i+1]);
        i++;
      } else if (str[i].equals("-announceaddress")) {
        announceAddressStr=str[i+1];
        i++;
      } else if (str[i].equals("-electaddress")) {
        electAddressStr=str[i+1];
        i++;
      } else {
        System.out.println("Invalid option: "+str[i]);
        printUsage();
        System.exit(0);
      }
    }

 
    try {
      announceAddress = InetAddress.getByName(announceAddressStr);
      electAddress    = InetAddress.getByName(electAddressStr);
    } catch (Exception x) {
      System.out.println("Error starting: "+x);
      System.exit(0);
      return;//Make java not complain
    }
    Thread t1 = new Thread(new ListenThread(announceAddress,announcePort,"Announce"));  
    Thread t2 = new Thread(new ListenThread(electAddress,electPort,"Elect"));  
    t1.start();
    t2.start();
  }
}
