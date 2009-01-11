package context.apps.Tour;

/**
 *
 */
public class DemoVisit {


  private String demo;
  private String demourl;
  private String demoerurl;
  private long time;
  private String interest;

  /**
   * Basic constructor that creates a DemoInterest object.
   */
  public DemoVisit() {
  }

  public void setDemoName(String demo) {
    this.demo = demo;
  }

  public String getDemoName() {
    return demo;
  }

  public void setTime(String stime) {
    time = Long.parseLong(stime);
  }

  public void setTime(long time) {
    this.time = time;
  }

  public long getTime() {
    return time;
  }

  public void setDemoUrl(String demourl) {
    this.demourl = demourl;
  }

  public String getDemoUrl() {
    return demourl;
  }

  public void setDemoerUrl(String demoerurl) {
    this.demoerurl = demoerurl;
  }

  public String getDemoerUrl() {
    return demoerurl;
  }

  public void setInterest(String interest) {
    this.interest = interest;
  }

  public String getInterest() {
    return interest;
  }

}
