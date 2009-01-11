Context Toolkit         http://www.cc.gatech.edu/fce/contexttoolkit

This README is very sparse. For more information, see
  http://www.cc.gatech.edu/fce/contexttoolkit/ and in particular
  http://www.cc.gatech.edu/fce/contexttoolkit/documentation/UserGuide.html

Installation:
----------------

1.Download and install the Java Development Kit into a directory <JDK>. 
  I recommend using JDK 1.2.2, although I have used JDK 1.1 in the past. 
  When asked if you want to install the Java Runtime Environment, select 
  this option. 

2.Download the context.zip file. For now, to get the location of this 
  file, you must contact anind@cc.gatech.edu.

3.Unzip the context.zip file into a directory <DIRECTORY>. 

4.Add the <DIRECTORY> directory to your CLASSPATH 

5.Move the jar files in <DIRECTORY>/context/jars to <JDK>/jre/lib/ext. 
  The jar files are 3rd party jar files. gwe.jar is from GWE Technologies 
  and provides the JDBC access to the MySQL database. aelfred.jar is from 
  Microstar Software and provides XML decoding. sax.jar is from Megginson 
  Technologies and provides support for multiple XML decoders. ntp.jar is 
  from Limburgs Universitair Centrum and provides support for talking to a 
  Network Time Protocol server. To retrieve ntp.jar, unzip the Ntp.zip 
  file. All of the jar files are free to use and redistribute.

Running a simple Application:
-----------------------------

1.In one window, run the widget: 
    1.To see the widget's parameter list: 
      java context.arch.widget.WPersonNamePresence2 
    2.Run the widget:
      java context.arch.widget.WPersonNamePresence2 test 5000 false 
    3.The widget should load a GUI (6 buttons) 

2.In another window, run the application: 
    1.To see the application's parameter list: 
      java context.apps.PersonPresenceApp.SimpleApp 
    2.Run the application:
      java context.apps.PersonPresenceApp.SimpleApp test 5001 127.0.0.1 5000 
    3.The application will subscribe to the widget 

3.Now interact with the widget's GUI, pressing buttons to simulate the 
  coming and going of various people. On the application window, you should 
  see print statements showing the results of the subscription callbacks. 

More information
----------------

Look at the online documentation at 
http://www.cc.gatech.edu/fce/contexttoolkit/documentation/UserGuide.html
This includes a tutorial, a more substantial installation guide and a
copy of the javadoc-generated documentation (also in the docs directory
in the distribution).

Look at some of the publications online at:
http://www.cc.gatech.edu/fce/contexttoolkit

Join the mailing list for the Context Toolkit:
Send a mail to majordomo@cc.gatech.edu with a line in the body of the
message that says: subscribe ctk

==========================================================================