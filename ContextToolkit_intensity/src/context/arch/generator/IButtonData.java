package context.arch.generator;

/**
 * This class is a container for IButton data.  This includes the IButton id,
 * the location of the IButton and the timestamp when the IButton was docked.
 */
public class IButtonData {
  
  private String location;
  private String id; 
  private String timestamp;

  /**
   * Basic constructor
   *
   * @param location Location of the IButton
   * @param id Id of the IButton
   * @param timestamp Timestamp when the IButton was docked
   */
  public IButtonData(String location, String id, String timestamp) {
    this.location = location;
    this.id = id;
    this.timestamp = timestamp;
  }

  /**
   * Returns the location of the IButton
   * 
   * @return the location of the IButton
   */
  public String getLocation() {
    return location;
  }

  /**
   * Returns the id of the IButton
   * 
   * @return the id of the IButton
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the timestamp of the IButton docking
   * 
   * @return the timestamp of the IButton docking
   */
  public String getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the location of the IButton
   * 
   * @param location the location of the IButton
   */
  public void setLocation(String location) {
    this.location = location;
  }

  /**
   * Sets the id of the IButton
   * 
   * @param id the id of the IButton
   */
  public void setId(String id) {
    this.id = id;
  }
  
  /**
   * Sets the timestamp of the IButton docking
   * 
   * @param timestamp the timestamp of the IButton docking
   */
  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }
}
