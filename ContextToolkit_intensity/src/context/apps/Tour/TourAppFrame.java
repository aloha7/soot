package context.apps.Tour;

import java.awt.Frame;
import java.awt.Label;
import java.awt.Button;
import java.awt.Panel;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class TourAppFrame extends Frame implements Runnable, ActionListener {

  private Label recommend;
  private boolean first = true;
  private Button demo;
  private Button demoer;
  String demoerUrl;
  String demoUrl;

  /**
   */
  public TourAppFrame(String reco) {
    setTitle("GVU Tour Guide");
    setLayout(new BorderLayout());

    Label label = new Label("Following are recommended demos for you to visit: ");
    add("North",label);
  
    if (reco == null) {
      recommend = new Label("");
    }
    else {
      recommend = new Label(reco);
    }
    add("Center",recommend);
}

  
  public void run() {
    setVisible(true);
  }

  public void setRecommendation(String reco) {
    recommend.setText(reco);
  }

  public void setUrls(String demoUrl, String demoerUrl) {
    this.demoUrl = demoUrl;
    this.demoerUrl = demoerUrl;
    if (first) {
      GridBagLayout gridbag = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();

      Panel p1 = new Panel();
      p1.setLayout(gridbag);
      c.fill = GridBagConstraints.BOTH;
      c.weightx = 1.0;

      c.gridwidth = GridBagConstraints.REMAINDER;
      demo = new Button("Press to view demo web page");
      demo.addActionListener(this);
      demo.setActionCommand(demoUrl);
      gridbag.setConstraints(demo,c);
      p1.add(demo);

      c.gridwidth = GridBagConstraints.REMAINDER;
      demoer = new Button("Press to view demoer's web page");
      demoer.addActionListener(this);
      demoer.setActionCommand(demoerUrl);
      gridbag.setConstraints(demoer,c);
      p1.add(demoer);

      add("South",p1);
      pack();
      show();
      first = false;
    }
  }

  public void actionPerformed(ActionEvent evt) {
    String url = evt.getActionCommand();
    String cmd = new String("rundll32 url.dll,FileProtocolHandler "+url);
    try {
      Process p = Runtime.getRuntime().exec(cmd);
    } catch (IOException ioe) {
        System.out.println("RecommendationFrame IO: "+ioe);
    }
  }
}
