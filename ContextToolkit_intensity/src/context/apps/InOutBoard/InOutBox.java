package context.apps.InOutBoard;

import java.awt.*;


public class InOutBox extends Component {

	private final int WIDTH  = 305;
	private final int HEIGHT = 50;
	
	private final Color BGCOLOR = Color.blue;
	private final Color FGCOLOR = Color.white;
	
	private final int NAME_HOFFSET = 6;
	private final int NAME_VOFFSET = 30;
	
	private final int STATUS_RADIUS = HEIGHT/2 - 5;
	private final int STATUS_HOFFSET = 164;
	private final int STATUS_VOFFSET = 5;
	
	private final int INFO_HOFFSET = 207;
	private final int INFO_VOFFSET = NAME_VOFFSET;
	
	private final Font nameFont = new Font ("Helvetica", Font.ITALIC + Font.BOLD, 18);
	private final Font infoFont = new Font ("Helvetica", Font.BOLD, 12);
	
	private String name;
	private Color  status;
	private String info;
	private int    w, h;
	
	
	public InOutBox (String n, Color s, String i) {
		
		super ();
		
		name = n;
		status = s;
		info = i;
		
		w = WIDTH;
		h = HEIGHT;
		
		setSize (w, h);
		
		setBackground (BGCOLOR);
		setForeground (FGCOLOR);
		
		this.setVisible (true);
	}
	
	public void updateContent (String n, Color s, String i) {
	
		name = n;
		status = s;
		info = i;
	
		repaint ();
	}
	
	
  public Dimension getMinimumSize () {
    return new Dimension (w, h);
  }
  
  
  public Dimension getPreferredSize () {
    return getMinimumSize ();
  }


	public void paint (Graphics g) {
	
		if (this.name != null) {		// don't paint dummy boxes
			Rectangle r = g.getClipBounds ();
			
			g.setColor (getBackground ());
			g.fillRect (r.x, r.y, r.width, r.height);	// this may cause flicker :-(
			
			g.setFont (nameFont);
			g.setColor (getForeground ());
			g.drawString (name, NAME_HOFFSET, NAME_VOFFSET);
			
			g.setColor (status);
			g.fillArc (r.x + STATUS_HOFFSET, r.y + STATUS_VOFFSET, 2*STATUS_RADIUS, 2*STATUS_RADIUS, 0, 360);
			
			g.setFont (infoFont);
			g.setColor (getForeground ());
			g.drawString (info, INFO_HOFFSET, INFO_VOFFSET);
			
		}
				
	}

}
