package context.apps.Tour;

import java.util.Vector;

/**
 *
 */
public class DemoVisits extends Vector{
  
  public DemoVisits() {
    super();
  }

  public void addDemoVisit(String demo, String demourl, String demoerurl, String time) {
    DemoVisit dv = new DemoVisit();
    dv.setDemoName(demo);
    dv.setDemoUrl(demourl);
    dv.setDemoerUrl(demoerurl);
    dv.setTime(time);
    addElement(dv);
  }

  public DemoVisit getDemoVisitAt(int i) {
    return (DemoVisit)elementAt(i);
  }

  public DemoVisit getDemoVisit(String demo) {
    for (int i=0; i<size(); i++) {
      DemoVisit dv = (DemoVisit)elementAt(i);
      if (dv.getDemoName().equals(demo)) {
        return dv;
      }
    }
    return null;
  }

  public int numDemoVisits() {
    return size();
  }
}
