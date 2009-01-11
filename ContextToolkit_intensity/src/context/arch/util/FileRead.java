package context.arch.util;

import java.net.URL;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class is a utility class that reads in the contents of a file.
 */
public class FileRead {

  private static BufferedReader reader = null;

  /**
   * Constructor that takes the filename to be read.
   *
   * @param file Name of the file to read
   */
  public FileRead(String file) {
    try {
      reader = new BufferedReader(new FileReader(file));
    } catch (IOException ioe) {
        // should mean file doesn't exist
    }
  }

  /**
   * This method reads in the file and returns the result in a string.
   *
   * @return String containing the contents of the file.
   */
  public static String read() {
    if (reader == null) {
      return new String();
    }
    try {
      StringBuffer sb = new StringBuffer();
      String line = "\n";
      while (line != null) {
      line = reader.readLine();
      if (line != null) {
          sb.append(line+"\n");
        }
      }
      return sb.toString();
    } catch (IOException ioe) {
        return new String();
    }
  }
}
