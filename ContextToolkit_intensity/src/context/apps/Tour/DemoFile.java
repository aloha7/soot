package context.apps.Tour;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * This class is a container for a configuration file
 */
public class DemoFile {

  public static final String DEMO_NAME = "DEMO NAME:";
  public static final String KEYWORDS = "KEYWORDS:";
  public static final String DEMO_URL = "DEMO URL:";
  public static final String DEMOER_URL = "DEMOER URL:";
  public static final String DESCRIPTION = "DESCRIPTION:";

  private Demos demos;

  /**
   * Constructor that takes the demo information filename in the form of a url
   *
   * @param file Url for the demo information file
   */
  public DemoFile(String filename) {
    try {
      URL url = new URL(filename);

      String line=new String();
      BufferedReader reader = null;
      StringBuffer sb = new StringBuffer();
      try {
        reader = new BufferedReader(new InputStreamReader(url.openStream()));
      } catch(IOException e) {
          System.out.println(e);
      }

      demos = new Demos();

      while(true) {
        try {
          line=reader.readLine();
        } catch(IOException e) {
            System.out.println(e);
        }

        if(line==null) break;
        sb.append(line+"\n");
      }

      try {
        reader.close();
      } catch(IOException e) {
          System.out.println(e);
      }

      String file = sb.toString();
      int index = file.indexOf(DEMO_NAME);
      while (index != -1) {
        int index2 = file.indexOf(DEMO_URL,index);
        String demoName = file.substring(index+DEMO_NAME.length(),index2).trim();
        index = file.indexOf(DEMOER_URL,index2);
        String demoUrl = file.substring(index2+DEMO_URL.length(),index).trim();
        index2 = file.indexOf(KEYWORDS,index);
        String demoerUrl = file.substring(index+DEMOER_URL.length(),index2).trim();
        index = file.indexOf(DESCRIPTION,index2);
        String keywords = file.substring(index2+KEYWORDS.length(),index).trim();
        index2 = file.indexOf(DEMO_NAME,index);
        String description = null;
        if (index2 < index) {
          description = file.substring(index+DESCRIPTION.length()).trim();
        }
        else {
          description = file.substring(index+DESCRIPTION.length(),index2).trim();
        }
        index = index2;
        demos.addDemo(demoName,demoUrl,demoerUrl,keywords,description);
      }

    }
    catch ( MalformedURLException e ) {
      System.out.println( "SetupFile: MalformedURLException - " + e );
    }
  }

  public Demo getDemo(String demoName) {
    return demos.getDemo(demoName);
  }

  public String getDemoUrl(String demoName) {
    return demos.getDemo(demoName).getDemoUrl();
  }

  public String getDemoerUrl(String demoName) {
    return demos.getDemo(demoName).getDemoerUrl();
  }

  public String getDemoKeywords(String demoName) {
    return demos.getDemo(demoName).getKeywords();
  }

  public String getDemoDescription(String demoName) {
    return demos.getDemo(demoName).getDescription();
  }

  public Demos getDemos() {
    return demos;
  }
}
