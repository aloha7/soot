package context.arch.generator;

import java.lang.Math;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import javax.comm.SerialPort;
import javax.comm.CommPortIdentifier;
import javax.comm.UnsupportedCommOperationException;
import javax.comm.NoSuchPortException;
import javax.comm.PortInUseException;
import java.util.Date;

import context.arch.widget.Widget;

/**
 * This class acts as a wrapper around a temperature sensor.  Whenever
 * the temperature sensor changes by more than 0.5 degrees, the
 * temperature, the units, and the timestamp are stored and made available
 * to the context-aware infrastructure for polling, and can notify 
 * context widgets when data changes.
 *
 * @see context.arch.widget.Widget
 */
public class TemperatureSensor {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * Change in temperature threshold
   */
  public static final double THRESHOLD = 0.5;

  private double olddeg=0.0;
  private Widget widget = null;

  /**
   * Constructor that allows this object to read data from the serial port
   * to which the temperature sensor is connected. It sets the context widget 
   * to notify when there's a temperature change greater than THRESHOLD
   *
   * @param widget Widget to notify when the temperature changes greater than THRESHOLD
   */
  public TemperatureSensor(Widget widget) {
    this.widget = widget;
    SerialPort serialPort = configPort(9600,SerialPort.DATABITS_8,
                            SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);

    BufferedReader kin = null;
    try {
      InputStream inputStream = serialPort.getInputStream();
      kin = new BufferedReader(new InputStreamReader(inputStream));
    } catch (IOException ioe) {
        System.out.println ("IOException: "+ioe);
    }

    while(true) {
      try {
        String line = kin.readLine();
       
        if (DEBUG) {
          System.out.println("Your input was: "+line);
        }

        int first = line.indexOf(",");
        if (first != -1) {
          String denum = line.substring(0,first);
          String denum2 = denum.substring(denum.length()-2);
          Double degree = Double.valueOf(denum2);
          double degrees = degree.doubleValue();

          String units = line.substring(first+1);    

          if (DEBUG) {
            System.out.println("The temperature is "+degrees+units);
          }

          if (Math.abs(degrees-olddeg)>THRESHOLD){

            if (DEBUG) {
              System.out.println("over the threshold, notifying widget");
            }

            long currentTime = new Date().getTime();

            if (widget != null) {
              widget.notify(Widget.UPDATE, new TemperatureData(denum2, units, Long.toString(currentTime)));
            }
            olddeg=degrees;
          }
//          if(units.equals("C")){
//            double newdegrees = ((9/5)*(degrees))+32;
//            degrees = newdegrees;
//            type = "F";
//          }
        }
      } catch (IOException ioe){
          System.out.println("Exception: "+ioe);
      }
    }
  }

  /**
   * Method that configures the serial port
   *
   * @param baudRate Baud rate of communications
   * @param databits Number of databits
   * @param stopbits Number of stopbits
   * @param parity Parity for communications
   */
  public SerialPort configPort(int baudRate, int databits, int stopbits, int parity) {
    CommPortIdentifier portId;
    SerialPort sp = null;
    try {
      portId = CommPortIdentifier.getPortIdentifier("COM1");
      if (portId == null) {
        System.out.println("Can't get COM1 handle.");
        return null;
      }
      sp = (SerialPort)portId.open("SerialPort", 2000);
      if (sp == null) {
        System.out.println("Unable to open the com1 port.");
        return null;
      }
      sp.setSerialPortParams(baudRate,databits,stopbits,parity);
    } catch (UnsupportedCommOperationException ucoe) {
        System.out.println("UnsupportedCommOperation: "+ucoe);
        return null;
    } catch (NoSuchPortException nspe) {
        System.out.println("NoSuchPort: "+nspe);
        return null;
    } catch (PortInUseException piue) {
        System.out.println("PortInUse: "+piue);
        return null;
    }
    return sp;
  }

}
 
